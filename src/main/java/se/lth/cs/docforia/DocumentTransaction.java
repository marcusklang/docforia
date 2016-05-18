package se.lth.cs.docforia;
/*
 * Copyright 2016 Marcus Klang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import it.unimi.dsi.fastutil.objects.*;
import se.lth.cs.docforia.data.DataRef;
import se.lth.cs.docforia.exceptions.TransactionException;
import se.lth.cs.docforia.util.Iterables;

import java.util.*;
import java.util.stream.StreamSupport;

/**
 * Tracks and caches changes made to the document model and provides a mechanism for batch applying them.
 */
public class DocumentTransaction implements DocumentProxy {
    public interface TransactionStore {

    }

    public interface TransactionNodeStore extends TransactionStore {
        NodeRef real();
    }

    public interface TransactionEdgeStore extends TransactionStore {
        EdgeRef real();
    }

    protected Document doc;

    public DocumentTransaction(Document doc) {
        this.doc = doc;
    }

    protected ObjectSet<NodeRef> removedNodes = new ObjectOpenHashSet<>();
    protected ObjectSet<EdgeRef> removedEdges = new ObjectOpenHashSet<>();

    protected ObjectSet<NodeRef> addedNodes = new ObjectOpenHashSet<>();
    protected ObjectSet<EdgeRef> addedEdges = new ObjectOpenHashSet<>();

    protected Reference2ObjectOpenHashMap<NodeRef,Node> nodes = new Reference2ObjectOpenHashMap<>();
    protected Reference2ObjectOpenHashMap<EdgeRef,Edge> edges = new Reference2ObjectOpenHashMap<>();

    private static class WrappedNodeStore extends NodeStore implements TransactionNodeStore {
        private DocumentTransaction parent;
        private Node instance;
        private NodeStore internal;
        private HashMap<String,DataRef> modifiedProperties = new HashMap<>();
        private HashSet<String> removedProperties = new HashSet<>();
        private boolean modifiedRange;
        private boolean modifiedVariant;
        private String variant;
        private int start;
        private int end;
        private boolean pureNode = false;

        public WrappedNodeStore(DocumentTransaction parent, Node instance, NodeStore internal) {
            this.parent = parent;
            this.instance = instance;
            this.internal = internal;
        }

        @Override
        public Document parent() {
            return parent.doc;
        }

        @Override
        public NodeRef real() {
            if(internal == null)
                throw new TransactionException("A wrapped node which have been removed is required by another transaction.");

            if(internal instanceof TransactionNodeStore)
                return ((TransactionNodeStore) internal).real();
            else
                return internal;
        }

        @Override
        public String getLayer() {
            return internal.getLayer();
        }

        @Override
        public String getVariant() {
            if(modifiedVariant)
                return variant;
            else
                return internal.getVariant();
        }

        @Override
        public boolean valid() {
            return internal != null;
        }

        @Override
        public void setVariant(String variant) {
            modifiedVariant = true;
            this.variant = variant;
        }

        @Override
        public int getStart() {
            return modifiedRange ?  start : internal.getStart();
        }

        @Override
        public int numProperties() {
            return (int)StreamSupport.stream(properties().spliterator(), false).count();
        }

        @Override
        public NodeStore get() {
            return this;
        }

        @Override
        public DocumentNodeLayer layer() {
            throw new UnsupportedOperationException("Not yet implemented.");
        }

        @Override
        public int getEnd() {
            return modifiedRange ? end : internal.getStart();
        }

        @Override
        public void setRanges(int start, int end) {
            modifiedRange = true;
            pureNode = false;
            this.start = start;
            this.end = end;
        }

        @Override
        public void setNoRanges() {
            modifiedRange = true;
            this.start = Integer.MIN_VALUE;
            this.end = Integer.MIN_VALUE;
            pureNode = true;
        }

        @Override
        public boolean isAnnotation() {
            return modifiedRange ? !pureNode : internal.isAnnotation();
        }

        @Override
        public void putProperty(String key, DataRef value) {
            modifiedProperties.put(key, value);
            removedProperties.remove(key);
        }

        @Override
        public <T extends DataRef> T getRefProperty(String key) {
            if(removedProperties.contains(key))
                return null;

            DataRef intermResult = modifiedProperties.get(key);
            if(intermResult == null)
                return internal.getRefProperty(key);
            else
                return (T)intermResult;
        }

        @Override
        public boolean hasProperty(String key) {
            return !removedProperties.contains(key) && (modifiedProperties.containsKey(key) || internal.hasProperty(key));
        }

        @Override
        public void removeProperty(String key) {
            removedProperties.add(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Iterable<Map.Entry<String, DataRef>> properties() {
            return Iterables.<Map.Entry<String,DataRef>>concat(
                    new Iterable<Map.Entry<String, DataRef>>() {
                        @Override
                        public Iterator<Map.Entry<String, DataRef>> iterator() {
                            final Iterator<Map.Entry<String,DataRef>> iter = internal.properties().iterator();
                            return new Iterator<Map.Entry<String, DataRef>>() {
                                private Map.Entry<String,DataRef> current;

                                private boolean moveForward() {
                                    if(current == null) {
                                        while(iter.hasNext()) {
                                            this.current = iter.next();
                                            if (!removedProperties.contains(current.getKey()) && !modifiedProperties.containsKey(current.getKey())) {
                                                return true;
                                            }
                                        }

                                        return false;
                                    }
                                    else
                                        return true;
                                }

                                @Override
                                public boolean hasNext() {
                                    return moveForward();
                                }

                                @Override
                                public Map.Entry<String, DataRef> next() {
                                    if(!moveForward())
                                        throw new NoSuchElementException();

                                    Map.Entry<String, DataRef> retval = this.current;
                                    this.current = null;

                                    return retval;
                                }
                            };
                        }
                    },
                    modifiedProperties.entrySet()
            );
        }
    }

    private static class WrappedEdgeStore extends EdgeStore implements EdgeRef, TransactionEdgeStore {
        private DocumentTransaction parent;
        private Edge instance;
        private EdgeStore internal;
        private boolean modifiedHeadTail;
        private boolean modifiedVariant;
        private String variant;
        private NodeRef head;
        private NodeRef tail;

        private HashMap<String,DataRef> modifiedProperties = new HashMap<>();
        private HashSet<String> removedProperties = new HashSet<>();

        public WrappedEdgeStore(DocumentTransaction parent, Edge instance, EdgeStore internal) {
            this.parent = parent;
            this.instance = instance;
            this.internal = internal;
        }

        @Override
        public Document parent() {
            return parent.doc;
        }

        @Override
        public EdgeRef real() {
            if(internal == null)
                throw new TransactionException("A wrapped edge that is required by another transaction has been removed.");

            if(internal instanceof TransactionEdgeStore)
                return ((TransactionEdgeStore) internal).real();
            else
                return internal;
        }

        @Override
        public NodeRef getHead() {
            return modifiedHeadTail ? head : internal.getHead();
        }

        @Override
        public NodeRef getTail() {
            return modifiedHeadTail ? tail : internal.getTail();
        }

        @Override
        public void setHead(NodeRef head) {
            if(modifiedHeadTail) {
                this.head = head;
            } else {
                modifiedHeadTail = true;
                this.head = head;
                this.tail = internal.getTail();
            }
        }

        @Override
        public void setTail(NodeRef tail) {
            if(modifiedHeadTail) {
                this.tail = tail;
            } else {
                modifiedHeadTail = true;
                this.head = internal.getHead();
                this.tail = tail;
            }
        }

        @Override
        public int numProperties() {
            return (int)StreamSupport.stream(properties().spliterator(), false).count();
        }

        @Override
        public boolean valid() {
            return internal != null;
        }

        @Override
        public void connect(NodeRef tail, NodeRef head) {
            modifiedHeadTail = true;
            this.head = head;
            this.tail = tail;
        }

        @Override
        public EdgeStore get() {
            return this;
        }

        @Override
        public DocumentEdgeLayer layer() {
            throw new UnsupportedOperationException("Not yet implemented.");
        }

        @Override
        public String getLayer() {
            return internal.getLayer();
        }

        @Override
        public String getVariant() {
            return modifiedVariant ? variant : internal.getVariant();
        }

        @Override
        public void setVariant(String variant) {
            this.modifiedVariant = true;
            this.variant = variant;
        }

        @Override
        public void putProperty(String key, DataRef ref) {
            modifiedProperties.put(key, ref);
        }

        @Override
        public <T extends DataRef> T getRefProperty(String key) {
            if(removedProperties.contains(key))
                return null;

            DataRef intermResult = modifiedProperties.get(key);
            if(intermResult == null)
                return internal.getRefProperty(key);
            else
                return (T)intermResult;
        }

        @Override
        public boolean hasProperty(String key) {
            return !removedProperties.contains(key) && (modifiedProperties.containsKey(key) || internal.hasProperty(key));
        }

        @Override
        public void removeProperty(String key) {
            modifiedProperties.remove(key);
            removedProperties.add(key);
        }

        @Override
        public Iterable<Map.Entry<String, DataRef>> properties() {
            return Iterables.<Map.Entry<String,DataRef>>concat(
                    new Iterable<Map.Entry<String, DataRef>>() {
                        @Override
                        public Iterator<Map.Entry<String, DataRef>> iterator() {
                            final Iterator<Map.Entry<String,DataRef>> iter = internal.properties().iterator();
                            return new Iterator<Map.Entry<String, DataRef>>() {
                                private Map.Entry<String,DataRef> current;

                                private boolean moveForward() {
                                    if(current == null) {
                                        while(iter.hasNext()) {
                                            this.current = iter.next();
                                            if (!removedProperties.contains(current.getKey()) && !modifiedProperties.containsKey(current.getKey())) {
                                                return true;
                                            }
                                        }

                                        return false;
                                    }
                                    else
                                        return true;
                                }

                                @Override
                                public boolean hasNext() {
                                    return moveForward();
                                }

                                @Override
                                public Map.Entry<String, DataRef> next() {
                                    if(!moveForward())
                                        throw new NoSuchElementException();

                                    Map.Entry<String, DataRef> retval = this.current;
                                    this.current = null;

                                    return retval;
                                }
                            };
                        }
                    },
                    modifiedProperties.entrySet()
            );
        }
    }

    private class TransLayerRef implements LayerRef {
        private String layer;
        private String variant;

        public TransLayerRef(String layer, String variant) {
            this.layer = layer;
            this.variant = variant;
        }

        @Override
        public boolean equal(LayerRef ref) {
            return ref.getLayer().equals(getLayer()) && Objects.equals(ref.getVariant(),getVariant());
        }

        @Override
        public String getLayer() {
            return layer;
        }

        @Override
        public String getVariant() {
            return variant;
        }
    }

    private static class TransNodeStore extends NodeStore implements NodeRef, TransactionNodeStore {
        private DocumentTransaction parent;
        private Object2ObjectOpenHashMap<String,DataRef> props = new Object2ObjectOpenHashMap<>();
        private LayerRef layer;
        private int start=-1, end=-1;
        private NodeRef real;
        private Node instance;

        public TransNodeStore(DocumentTransaction parent, Node instance, LayerRef layer) {
            this.parent = parent;
            this.instance = instance;
            this.layer = layer;
        }

        @Override
        public Document parent() {
            return parent.doc;
        }

        @Override
        public int numProperties() {
            return props.size();
        }

        @Override
        public NodeRef real() {
            if(parent == null && real == null)
                throw new TransactionException("Referenced node was deleted from another transaction.");
            else if(real != null)
                return real;
            else
                throw new TransactionException("Referenced node "
                                                       + this.toString()
                                                       + " contained in transaction "
                                                       + parent
                                                       + " has not been commited!");
        }

        @Override
        public String getLayer() {
            return layer.getLayer();
        }

        @Override
        public String getVariant() {
            return layer.getVariant();
        }

        @Override
        public void setVariant(String variant) {
            this.layer = parent.getLayerRef(layer.getLayer(), variant);
        }

        @Override
        public NodeStore get() {
            return this;
        }

        @Override
        public DocumentNodeLayer layer() {
            throw new UnsupportedOperationException("Not yet implemented.");
        }

        @Override
        public boolean valid() {
            return parent != null;
        }

        @Override
        public int getStart() {
            return start;
        }

        @Override
        public int getEnd() {
            return end;
        }

        @Override
        public boolean isAnnotation() {
            return this.start != Integer.MIN_VALUE && this.end != Integer.MIN_VALUE;
        }

        @Override
        public void setNoRanges() {
            this.start = Integer.MIN_VALUE;
            this.end = Integer.MIN_VALUE;
        }

        @Override
        public void setRanges(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public <T extends DataRef> T getRefProperty(String key) {
            return (T)props.get(key);
        }

        @Override
        public boolean hasProperty(String key) {
            return props.containsKey(key);
        }

        @Override
        public void putProperty(String key, DataRef ref) {
            props.put(key,ref);
        }

        @Override
        public void removeProperty(String key) {
            props.remove(key);
        }

        @Override
        public Iterable<Map.Entry<String, DataRef>> properties() {
            return props.entrySet();
        }
    }

    private static class TransEdgeStore extends EdgeStore implements EdgeRef, TransactionEdgeStore {
        private DocumentTransaction parent;
        private Object2ObjectOpenHashMap<String,DataRef> props = new Object2ObjectOpenHashMap<>();
        private LayerRef layer;
        private NodeRef head;
        private NodeRef tail;
        private EdgeRef real;
        private Edge instance;

        public TransEdgeStore(DocumentTransaction parent, Edge instance, LayerRef layer) {
            this.parent = parent;
            this.instance = instance;
            this.layer = layer;
        }

        @Override
        public Document parent() {
            return parent.doc;
        }

        @Override
        public int numProperties() {
            return props.size();
        }

        @Override
        public EdgeRef real() {
            if(parent == null && real == null)
                throw new TransactionException("Referenced edge was deleted from another transaction.");
            else if(real != null)
                return real;
            else
                throw new TransactionException("Referenced edge "
                                                       + this.toString()
                                                       + " contained in transaction "
                                                       + parent
                                                       + " has not been commited!");
        }

        @Override
        public EdgeStore get() {
            return this;
        }

        @Override
        public DocumentEdgeLayer layer() {
            throw new UnsupportedOperationException("Not yet implemented.");
        }

        @Override
        public NodeRef getHead() {
            return head;
        }

        @Override
        public NodeRef getTail() {
            return tail;
        }

        @Override
        public boolean valid() {
            return parent != null;
        }

        @Override
        public void setHead(NodeRef head) {
            this.head = head;
        }

        @Override
        public void setTail(NodeRef tail) {
            this.tail = tail;
        }

        @Override
        public String getLayer() {
            return layer.getLayer();
        }

        @Override
        public String getVariant() {
            return layer.getVariant();
        }

        @Override
        public void setVariant(String variant) {
            layer = parent.getLayerRef(layer.getLayer(), variant);
        }

        @Override
        public void connect(NodeRef tail, NodeRef head) {
            this.head = head;
            this.tail = tail;
        }

        @Override
        public <T extends DataRef> T getRefProperty(String key) {
            return (T)props.get(key);
        }

        @Override
        public boolean hasProperty(String key) {
            return props.containsKey(key);
        }

        @Override
        public void putProperty(String key, DataRef ref) {
            props.put(key,ref);
        }

        @Override
        public void removeProperty(String key) {
            props.remove(key);
        }

        @Override
        public Iterable<Map.Entry<String, DataRef>> properties() {
            return props.entrySet();
        }
    }

    public LayerRef getLayerRef(String layer, String variant) {
        return new TransLayerRef(layer, variant);
    }

    @Override
    public <N extends Node> N add(N node) {
        if(node.doc != null && node.doc != this)
            throw new IllegalArgumentException("Node " + node.toString() + " is already added!");
        else if(node.doc == this)
            return node;

        NodeRef ref = new TransNodeStore(this, node, getLayerRef(Document.nodeLayer(node.getClass()), null));
        node.doc = this;
        node.store = ref.get();
        nodes.put(ref, node);
        addedNodes.add(ref);
        return node;
    }

    @Override
    public DynamicNode add(DynamicNode node, String layer) {
        if(node.doc != null && node.doc != this)
            throw new IllegalArgumentException("Node " + node.toString() + " is already bound, cannot add again!");
        else if(node.doc == this) {
            if(!node.store.getLayer().equals(Document.nodeLayer(layer)))
                throw new IllegalArgumentException("Node " + node.toString()
                                                           + " is already bound, but to a different layer: "
                                                           + node.store.getLayer() + " instead of " + layer);

            return node;
        }

        NodeRef ref = new TransNodeStore(this, node, getLayerRef(Document.nodeLayer(layer), null));
        node.doc = this;
        node.store = ref.get();
        nodes.put(ref, node);
        addedNodes.add(ref);
        return node;
    }

    @Override
    public <E extends Edge> E add(E edge) {
        if(edge.doc != null && edge.doc != this)
            throw new IllegalArgumentException("Edge "
                                   + edge.toString()
                                   + " is already bound to a transaction, cannot add again!");
        else if(edge.doc == this)
            return edge;

        EdgeRef ref = new TransEdgeStore(this, edge, getLayerRef(Document.edgeLayer(edge.getClass()), null));
        edge.doc = this;
        edge.store = ref.get();
        edges.put(ref, edge);
        addedEdges.add(ref);
        return edge;
    }

    @Override
    public <E extends Edge> E add(E edge, Node tail, Node head) {
        if(edge.doc != null && edge.doc != this)
            throw new IllegalArgumentException("Edge "
                                   + edge.toString()
                                   + " is already bound to a transaction, cannot add again!");
        else if(edge.doc == this)
            return edge;

        EdgeRef ref = new TransEdgeStore(this, edge, getLayerRef(Document.edgeLayer(edge.getClass()), null));
        edge.doc = this;
        edge.store = ref.get();
        edge.connect(tail, head);
        edges.put(ref, edge);
        addedEdges.add(ref);
        return edge;
    }

    @Override
    public DynamicEdge add(DynamicEdge edge, String layer) {
        return null;
    }

    @Override
    public DynamicEdge add(DynamicEdge edge, String layer, Node tail, Node head) {
        return null;
    }

    @Override
    public String text() {
        return doc.text();
    }

    @Override
    public String text(int start, int end) {
        return doc.text(start, end);
    }

    @Override
    public int length() {
        return doc.length();
    }

    @Override
    public int transform(int pos) {
        return doc.transform(pos);
    }

    @Override
    public int inverseTransform(int pos) {
        return doc.inverseTransform(pos);
    }

    @Override
    public void inverseTransform(MutableRange range) {
        doc.inverseTransform(range);
    }

    @SuppressWarnings("unchecked")
    public <N extends Node> N get(N node) {
        if(node.doc != this)
        {
            return (N)representation(node.store);
        }
        else
            return node;
    }

    @SuppressWarnings("unchecked")
    public <E extends Edge> E get(E edge) {
        if(edge.doc != this)
        {
            return (E)representation(edge.store);
        }
        else
            return edge;
    }

    @Override
    public Node representation(NodeRef ref) {
        if(ref == null)
            throw new NullPointerException("ref");

        Node n = nodes.get(ref);
        if(n != null)
            return n;

        Node repr = doc.representations().create(ref);
        repr.doc = this;
        repr.store = new WrappedNodeStore(this, repr, ref.get());
        nodes.put(ref, repr);
        nodes.put(repr.store, repr);
        return repr;
    }

    @Override
    public Edge representation(EdgeRef ref) {
        if(ref == null)
            throw new NullPointerException("ref");

        Edge e = edges.get(ref);
        if(e != null)
            return e;

        Edge repr = doc.representations().create(ref);
        repr.doc = this;
        repr.store = new WrappedEdgeStore(this, repr, ref.get());
        edges.put(ref, repr);
        edges.put(repr.store, repr);
        return repr;
    }

    public void remove(Node node) {
        if(node.getRef() instanceof WrappedNodeStore && node.doc == this) {
            removedNodes.add(((WrappedNodeStore)node.getRef()).internal);
            nodes.remove(node.getRef());
            nodes.remove(node.store);
        } else if(node.getRef() instanceof TransNodeStore && node.doc == this) {
            removedNodes.add(node.getRef());
            addedNodes.remove(node.getRef());
            nodes.remove(node.getRef());
        } else {
            removedNodes.add(node.getRef());
        }
    }

    public void remove(Edge edge) {
        if(edge.getRef() instanceof WrappedEdgeStore && edge.doc == this) {
            removedEdges.add(((WrappedEdgeStore)edge.getRef()).internal);
            edges.remove(edge.getRef());
        } else if(edge.getRef() instanceof TransEdgeStore && edge.doc == this) {
            removedEdges.add(edge.getRef());
            addedEdges.remove(edge.getRef());
            edges.remove(edge.getRef());
        } else {
            removedEdges.add(edge.getRef());
        }
    }

    private NodeRef resolve(NodeRef ref, boolean ignoreInvalid) {
        if(ref instanceof TransactionNodeStore) {
            return ((TransactionNodeStore) ref).real();
        }
        else if(!ignoreInvalid && !ref.valid()) {
            throw new TransactionException("Referenced node " + ref.toString() + " has been removed!");
        }
        else
            return ref;
    }

    private EdgeRef resolve(EdgeRef ref, boolean ignoreInvalid) {
        if(ref instanceof TransactionEdgeStore) {
            return ((TransactionEdgeStore) ref).real();
        }
        else if(!ignoreInvalid && !ref.valid()) {
            throw new TransactionException("Referenced node " + ref.toString() + " has been removed!");
        }
        else
            return ref;
    }

    public void commit() {
        //1. Remove values
        for (EdgeRef edge : removedEdges) {
            EdgeRef resolvedEdge = resolve(edge, true);
            if(resolvedEdge.valid())
                doc.store().remove(resolvedEdge);
        }

        for (NodeRef node : removedNodes) {
            NodeRef resolvedNode = resolve(node, true);
            if(resolvedNode.valid())
                doc.store().remove(resolvedNode);
        }

        //2. Add nodes
        for (NodeRef node : addedNodes) {
            TransNodeStore transStore = (TransNodeStore)node;
            NodeRef ref = doc.store().createNode(transStore.getLayer(), transStore.getVariant());
            transStore.real = ref;

            NodeStore nodeStore = ref.get();
            for (Map.Entry<String, DataRef> entry : transStore.properties()) {
                nodeStore.putProperty(entry.getKey(), entry.getValue());
            }

            if(transStore.isAnnotation()) {
                nodeStore.setRanges(transStore.start, transStore.end);
            } else {
                nodeStore.setNoRanges();
            }

            transStore.parent = null;
            transStore.props = null;
            transStore.layer = null;
            transStore.instance.doc = doc;
            transStore.instance.store = ref.get();
        }

        for (EdgeRef addedEdge : addedEdges) {
            TransEdgeStore transStore = (TransEdgeStore)addedEdge;
            EdgeRef ref = doc.store().createEdge(transStore.getLayer(), transStore.getVariant());
            EdgeStore edgeStore = ref.get();
            transStore.real = transStore;

            for (Map.Entry<String, DataRef> entry : transStore.properties()) {
                edgeStore.putProperty(entry.getKey(), entry.getValue());
            }

            NodeRef realHead = resolve(transStore.getHead(), false);
            NodeRef realTail = resolve(transStore.getTail(), false);
            edgeStore.connect(realTail, realHead);

            transStore.parent = null;
            transStore.props = null;
            transStore.layer = null;
            transStore.instance.doc = doc;
            transStore.instance.store = ref.get();
        }

        //3. Modify nodes
        for (Reference2ObjectMap.Entry<NodeRef, Node> entry : nodes.reference2ObjectEntrySet()) {
            NodeRef key = entry.getKey();
            if(key instanceof WrappedNodeStore) {
                WrappedNodeStore modifiedNode = (WrappedNodeStore)key;
                NodeStore nodeStore = modifiedNode.real().get();
                for (String removedProperty : modifiedNode.removedProperties) {
                    nodeStore.removeProperty(removedProperty);
                }

                for (Map.Entry<String, DataRef> prop : modifiedNode.modifiedProperties.entrySet()) {
                    nodeStore.putProperty(prop.getKey(), prop.getValue());
                }

                if(modifiedNode.modifiedVariant) {
                    nodeStore.setVariant(modifiedNode.variant);
                }

                if(modifiedNode.modifiedRange) {
                    if(modifiedNode.isAnnotation())
                        nodeStore.setRanges(modifiedNode.start, modifiedNode.end);
                    else
                        nodeStore.setNoRanges();
                }

                modifiedNode.parent = null;
                modifiedNode.instance.doc = doc;
                modifiedNode.instance.store = nodeStore;
            }
        }

        //4. Modify edges
        for (Reference2ObjectMap.Entry<EdgeRef, Edge> entry : edges.reference2ObjectEntrySet()) {
            EdgeRef key = entry.getKey();
            if(key instanceof WrappedEdgeStore) {
                WrappedEdgeStore modifiedEdge = (WrappedEdgeStore)key;
                EdgeStore edgeStore = modifiedEdge.real().get();
                for (String removedProperty : modifiedEdge.removedProperties) {
                    edgeStore.removeProperty(removedProperty);
                }

                for (Map.Entry<String, DataRef> prop : modifiedEdge.modifiedProperties.entrySet()) {
                    edgeStore.putProperty(prop.getKey(), prop.getValue());
                }

                if(modifiedEdge.modifiedVariant) {
                    edgeStore.setVariant(modifiedEdge.variant);
                }

                if(modifiedEdge.modifiedHeadTail) {
                    NodeRef head = resolve(modifiedEdge.head, false);
                    NodeRef tail = resolve(modifiedEdge.tail, false);
                    edgeStore.connect(tail, head);
                }

                modifiedEdge.parent = null;
                modifiedEdge.instance.doc = doc;
                modifiedEdge.instance.store = edgeStore;
            }
        }
    }
}

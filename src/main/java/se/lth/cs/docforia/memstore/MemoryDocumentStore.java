package se.lth.cs.docforia.memstore;
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

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import se.lth.cs.docforia.*;
import se.lth.cs.docforia.data.DataRef;
import se.lth.cs.docforia.data.StringRef;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Memory Document Storage implementation, contains all data.
 */
public class MemoryDocumentStore extends DocumentStore {
    protected MemoryDocument doc;

    /* TODO: Property Index
    protected Object2ReferenceOpenHashMap<String,ReferenceOpenHashSet<NodeRef>> nodesWithProperty;
    protected Object2ReferenceOpenHashMap<String,ReferenceOpenHashSet<NodeRef>> edgesWithProperty;
    */

    @Override
    public Document getDocument() {
        return doc;
    }

    protected String text = "";

    protected Object2ReferenceAVLTreeMap<MemoryNodeCollection.Key,MemoryNodeCollection> nodes;
    protected Object2ReferenceAVLTreeMap<MemoryEdgeCollection.Key,MemoryEdgeCollection> edges;

    protected Object2IntOpenHashMap<MemoryNodeCollection.Key> nodelayer2id = new Object2IntOpenHashMap<>();
    protected Object2IntOpenHashMap<MemoryEdgeCollection.Key> edgelayer2id = new Object2IntOpenHashMap<>();

    protected int nodelayerIdCounter = 1;
    protected int edgelayerIdCounter = 1;

    protected Object2ObjectOpenHashMap<String,DataRef> properties;

    public MemoryDocumentStore() {
        nodes = new Object2ReferenceAVLTreeMap<>();
        edges = new Object2ReferenceAVLTreeMap<>();
        properties = new Object2ObjectOpenHashMap<>();
    }

    @Override
    public Iterable<EdgeRef> edges() {
        return edgeIterable;
    }

    @Override
    public Iterable<EdgeRef> inboundEdges(NodeRef node) {
        return (Iterable<EdgeRef>)(Iterable)((MemoryNode)node).inlinks;
    }

    @Override
    public Iterable<EdgeRef> outboundEdges(NodeRef node) {
        return (Iterable<EdgeRef>)(Iterable)((MemoryNode)node).outlinks;
    }

    private final Iterable<NodeRef> nodeIterable = new Iterable<NodeRef>() {
        @Override
        public Iterator<NodeRef> iterator() {
            final ObjectIterator<MemoryNodeCollection> iterator = nodes.values().iterator();

            return new Iterator<NodeRef>() {
                Iterator<NodeRef> nodeIter;
                NodeRef next;

                private boolean moveForward() {
                    if(next == null) {
                        if(nodeIter != null && nodeIter.hasNext()) {
                            next = nodeIter.next();
                            return true;
                        } else {
                            while(iterator.hasNext()) {
                                nodeIter = iterator.next().iterator();
                                if(nodeIter.hasNext()) {
                                    next = nodeIter.next();
                                    return true;
                                }
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
                public NodeRef next() {
                    if(!moveForward())
                        throw new NoSuchElementException();

                    NodeRef retval = next;
                    next = null;

                    return retval;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    };

    private final Iterable<EdgeRef> edgeIterable = new Iterable<EdgeRef>() {
        @Override
        public Iterator<EdgeRef> iterator() {
            final ObjectIterator<MemoryEdgeCollection> iterator = edges.values().iterator();

            return new Iterator<EdgeRef>() {
                Iterator<EdgeRef> edgeIter;
                EdgeRef next;

                private boolean moveForward() {
                    if(next == null) {
                        if(edgeIter != null && edgeIter.hasNext()) {
                            next = edgeIter.next();
                            return true;
                        } else {
                            while(iterator.hasNext()) {
                                edgeIter = iterator.next().iterator();
                                if(edgeIter.hasNext()) {
                                    next = edgeIter.next();
                                    return true;
                                }
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
                public EdgeRef next() {
                    if(!moveForward())
                        throw new NoSuchElementException();

                    EdgeRef retval = next;
                    next = null;

                    return retval;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    };

    @Override
    public Iterable<NodeRef> nodes() {
        return nodeIterable;
    }

    @Override
    public void remove(NodeRef nodeId) {
        MemoryNode node = ((MemoryNode)nodeId);
        if(node.instance != null) {
            markNodeAsRemoved(node.instance);
            node.instance = null;
        }

        MemoryNodeCollection storage = node.storage;

        storage.remove(node);
        if(storage.isEmpty())
            nodes.remove(storage.key);
    }

    @Override
    public void remove(EdgeRef edgeId) {
        MemoryEdge e = ((MemoryEdge) edgeId);
        MemoryEdgeCollection storage = e.storage;
        if(e.instance != null) {
            markEdgeAsRemoved(e.instance);
            e.instance = null;
        }

        storage.remove(e);

        if(storage.isEmpty())
            edges.remove(storage.key);
    }

    protected void migrate(MemoryNodeCollection.Key oldKey, MemoryNodeCollection.Key newKey, MemoryNodeCollection collection) {
        int nodeId = nodelayer2id.getInt(oldKey);
        nodelayer2id.remove(oldKey);
        nodelayer2id.put(newKey, nodeId);
        nodes.remove(oldKey);
        nodes.put(newKey, collection);
    }

    protected void migrate(MemoryEdgeCollection.Key oldKey, MemoryEdgeCollection.Key newKey, MemoryEdgeCollection collection) {

    }

    @Override
    public EdgeRef getEdge(String uniqueRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeRef getNode(String uniqueRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public int numProperties() {
        return properties.size();
    }

    @Override
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    @Override
    public void putProperty(String key, DataRef ref) {
        properties.put(key, ref);
    }

    @Override
    public <T extends DataRef> T getRefProperty(String key) {
        return (T)properties.get(key);
    }

    @Override
    public void putProperty(String key, String value) {
        properties.put(key, new StringRef(value));
    }

    @Override
    public Iterable<Map.Entry<String, DataRef>> properties() {
        return properties.entrySet();
    }

    @Override
    public void removeProperty(String key) {
        properties.remove(key);
    }

    protected MemoryEdgeCollection getEdgeCollection(LayerRef ref) {
        MemoryEdgeCollection.Key key = (MemoryEdgeCollection.Key)ref;
        MemoryEdgeCollection edgeRefs = edges.get(key);
        if(edgeRefs == null) {
            edgeRefs = new MemoryEdgeCollection(this, key);
            key.id = edgelayerIdCounter;
            edges.put(key, edgeRefs);
            edgelayer2id.put(key, edgelayerIdCounter);
            edgelayerIdCounter++;
        }

        return edgeRefs;
    }

    protected MemoryEdgeCollection getEdgeCollection(String edgeLayer, String edgeVariant) {
        MemoryEdgeCollection.Key key = new MemoryEdgeCollection.Key(edgeLayer, edgeVariant);
        MemoryEdgeCollection edgeRefs = edges.get(key);
        if(edgeRefs == null) {
            edgeRefs = new MemoryEdgeCollection(this, key);
            key.id = edgelayerIdCounter;
            edges.put(key, edgeRefs);
            edgelayer2id.put(key, edgelayerIdCounter);
            edgelayerIdCounter++;
        }

        return edgeRefs;
    }

    protected MemoryNodeCollection getNodeCollection(LayerRef ref) {
        MemoryNodeCollection.Key key = (MemoryNodeCollection.Key)ref;
        MemoryNodeCollection nodeRefs = nodes.get(key);
        if(nodeRefs == null) {
            nodeRefs = new MemoryNodeCollection(this, key);
            key.id = nodelayerIdCounter;
            nodes.put(key, nodeRefs);
            nodelayer2id.put(key, nodelayerIdCounter);
            nodelayerIdCounter++;
        }

        return nodeRefs;
    }

    protected MemoryNodeCollection getNodeCollection(String nodeLayer, String nodeVariant) {
        MemoryNodeCollection.Key key = new MemoryNodeCollection.Key(nodeLayer, nodeVariant);
        MemoryNodeCollection nodeRefs = nodes.get(key);
        if(nodeRefs == null) {
            nodeRefs = new MemoryNodeCollection(this, key);
            key.id = nodelayerIdCounter;
            nodes.put(key, nodeRefs);
            nodelayer2id.put(key, nodelayerIdCounter);
            nodelayerIdCounter++;
        }

        return nodeRefs;
    }

    @Override
    public DocumentNodeLayer nodeLayer(String nodeLayer, String nodeVariant) {
        return getNodeCollection(nodeLayer, nodeVariant);
    }

    @Override
    public DocumentEdgeLayer edgeLayer(String edgeLayer, String edgeVariant) {
        return getEdgeCollection(edgeLayer, edgeVariant);
    }

    @Override
    public EdgeRef createEdge(String edgeLayer, String edgeVariant) {
        return getEdgeCollection(edgeLayer, edgeVariant).create();
    }

    @Override
    public NodeRef createNode(String nodeLayer, String nodeVariant) {
        return getNodeCollection(nodeLayer, nodeVariant).create();
    }

    @Override
    public EdgeRef createEdge(String edgeLayer) {
        return createEdge(edgeLayer, null);
    }

    @Override
    public NodeRef createNode(String nodeLayer) {
        return createNode(nodeLayer, null);
    }

    @Override
    public void migrateNodesToVariant(String nodeLayer, String prevVariant, String newVariant) {
        MemoryNodeCollection.Key sourceKey = new MemoryNodeCollection.Key(nodeLayer, prevVariant);

        MemoryNodeCollection nodeRefs = nodes.get(sourceKey);
        if(nodeRefs != null) {
            MemoryNodeCollection target = getNodeCollection(nodeLayer, newVariant);
            for (NodeRef nodeRef : nodeRefs) {
                target.add((MemoryNode) nodeRef);
            }

            nodes.remove(sourceKey);
        }
    }

    @Override
    public void migrateEdgesToVariant(String edgeLayer, String prevVariant, String newVariant) {
        MemoryEdgeCollection.Key sourceKey = new MemoryEdgeCollection.Key(edgeLayer, prevVariant);

        MemoryEdgeCollection edgeRefs = edges.get(sourceKey);
        if(edgeRefs != null) {
            MemoryEdgeCollection target = getEdgeCollection(edgeLayer, newVariant);
            for (EdgeRef edgeRef : edgeRefs) {
                target.add((MemoryEdge) edgeRef);
            }

            edges.remove(sourceKey);
        }
    }

    @Override
    public String toString() {
        return "Memory Document Storage with " + nodes.keySet().size() + " node layers and " + edges.keySet().size() + " edge layers.";
    }
}

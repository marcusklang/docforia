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

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import se.lth.cs.docforia.DocumentNodeLayer;
import se.lth.cs.docforia.LayerRef;
import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.util.AnnotationIndex;
import se.lth.cs.docforia.util.DocumentIterable;
import se.lth.cs.docforia.util.DocumentIterableBase;
import se.lth.cs.docforia.util.Iterables;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

/**
 * Memory Node Collection
 */
public class MemoryNodeCollection extends DocumentIterableBase<NodeRef> implements DocumentIterable<NodeRef>, DocumentNodeLayer {
    protected final MemoryDocumentStore store;
    protected AnnotationIndex<MemoryNode> annotations = new AnnotationIndex<>();
    protected ReferenceOpenHashSet<MemoryNode> nodes = new ReferenceOpenHashSet<>();
    protected Key key;

    public static class Key implements Comparable<Key>, LayerRef {
        protected final String layer;
        protected final String variant;
        protected int id;

        public Key(String layer, String variant) {
            this.layer = layer;
            this.variant = variant;
        }

        @Override
        public boolean equal(LayerRef ref) {
            return id == ((Key)ref).id;
        }

        @Override
        public String getLayer() {
            return layer;
        }

        @Override
        public String getVariant() {
            return variant;
        }

        @Override
        public int compareTo(Key o) {
            int result = layer.compareTo(o.layer);
            if(result == 0) {
                if(variant == null || o.variant == null)
                    return -Boolean.compare(variant == null, o.variant == null);
                else {
                    return variant.compareTo(o.variant);
                }
            }
            else
                return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!layer.equals(key.layer)) return false;
            return !(variant != null ? !variant.equals(key.variant) : key.variant != null);
        }

        @Override
        public int hashCode() {
            int result = layer.hashCode();
            result = 31 * result + (variant != null ? variant.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "( " + layer + ", " + Objects.toString(variant) + " )";
        }
    }

    public MemoryNodeCollection(MemoryDocumentStore store, Key key) {
        this.store = store;
        this.key = key;
    }

    @Override
    public String getLayer() {
        return key.layer;
    }

    @Override
    public boolean equal(LayerRef ref) {
        return key.equal(((MemoryNodeCollection)ref).key);
    }

    @Override
    public String getVariant() {
        return key.variant;
    }

    public Key getKey() {
        return key;
    }

    public int size() {
        return nodes.size() + annotations.size();
    }

    @Override
    public Iterator<NodeRef> iterator() {
        if(nodes.isEmpty() && annotations.isEmpty())
            return Collections.emptyIterator();

        return Iterables.<NodeRef>concat((Iterable)nodes, (Iterable)annotations).iterator();
    }

    public MemoryNode create() {
        MemoryNode memoryNode = new MemoryNode(this);
        memoryNode.start = Integer.MIN_VALUE;
        memoryNode.end = Integer.MAX_VALUE;

        add(memoryNode);
        return memoryNode;
    }

    public MemoryNode create(int start, int end) {
        MemoryNode memoryNode = new MemoryNode(this);
        memoryNode.start = start;
        memoryNode.end = end;

        add(memoryNode);
        return memoryNode;
    }

    public void add(MemoryNode node) {
        node.storage = this;
        if(node.isAnnotation()) {
            node.entry = annotations.add(node.getStart(), node.getEnd(), node);
        } else {
            nodes.add(node);
        }
    }

    @Override
    public void migrate(String newLayer) {
        migrate(newLayer, key.variant);
    }

    @Override
    public void migrate(String newLayer, String variant) {
        Key oldKey = key;
        Key newKey = new Key(newLayer, variant);

        store.migrate(oldKey, newKey, this);
        for (MemoryNode node : nodes) {
            node.instance = null;
        }

        for (MemoryNode memoryNode : annotations) {
            memoryNode.instance = null;
        }

        this.key = newKey;
    }

    @Override
    public void remove(NodeRef ref) {
        MemoryNode memref = (MemoryNode) ref;
        if(memref.storage != this)
            throw new IllegalArgumentException("Node does not belong to this layer!");

        remove(memref);
    }

    public void remove(MemoryNode node) {
        unlink(node);
        node.remove();
    }

    private void unlink(MemoryNode node) {
        if(node.isAnnotation()) {
            annotations.remove(node.entry);
        } else {
            nodes.remove(node);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MemoryNodeCollection nodeCollection = (MemoryNodeCollection) o;

        return store == nodeCollection.store && key.equal(nodeCollection.key);
    }

    @Override
    public int hashCode() {
        int result = store.hashCode();
        result = 31 * result + key.hashCode();
        return result;
    }

    public boolean isEmpty() {
        return nodes.isEmpty() && annotations.isEmpty();
    }

    public void variantChanged(MemoryNode node, String variant) {
        if(!Objects.equals(variant,this.key.variant)) {
            unlink(node);
            store.getNodeCollection(this.key.layer, variant).add(node);
        }
    }

    public void rangeChanged(MemoryNode node, int start, int end) {
        unlink(node);
        node.start = start;
        node.end = end;
        add(node);
    }

    @Override
    public String toString() {
        return nodes.size() + " nodes and " + annotations.size() + " annotations in node layer " + key.layer;
    }
}

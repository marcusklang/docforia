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

import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import se.lth.cs.docforia.DocumentEdgeLayer;
import se.lth.cs.docforia.EdgeRef;
import se.lth.cs.docforia.LayerRef;
import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.util.DocumentIterable;
import se.lth.cs.docforia.util.DocumentIterableBase;

import java.util.Iterator;
import java.util.Objects;

/**
 * Memory Edge Collection
 */
public class MemoryEdgeCollection extends DocumentIterableBase<EdgeRef> implements DocumentIterable<EdgeRef>, DocumentEdgeLayer {
    public final MemoryDocumentStore store;
    public final ReferenceLinkedOpenHashSet<MemoryEdge> edges = new ReferenceLinkedOpenHashSet<>();
    public Key key;

    public static class Key implements Comparable<Key>, LayerRef {
        public final String layer;
        public final String variant;
        public int id;

        public Key(String layer, String variant) {
            this.layer = layer;
            this.variant = variant;
            this.id = -1;
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
                    return Boolean.compare(variant == null, o.variant == null);
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

    @Override
    public void migrate(String newLayer) {
        migrate(newLayer, key.variant);
    }

    @Override
    public void migrate(String newLayer, String variant) {
        Key oldKey = key;
        Key newKey = new Key(newLayer, variant);
        store.migrate(oldKey, newKey, this);

        for (MemoryEdge edge : edges) {
            edge.instance = null;
        }

        this.key = newKey;
    }

    @Override
    public String getLayer() {
        return key.layer;
    }

    @Override
    public String getVariant() {
        return key.variant;
    }

    @Override
    public boolean equal(LayerRef ref) {
        return ref == this || ((ref instanceof MemoryEdgeCollection.Key) && ref.equal(this.key));
    }

    public Key getKey() {
        return key;
    }

    public MemoryEdgeCollection(MemoryDocumentStore store, Key key) {
        this.store = store;
        this.key = key;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Iterator<EdgeRef> iterator() {
        return ((Iterable)edges).iterator(); //MemoryEdges are EdgeRef
    }

    @Override
    public MemoryEdge create(NodeRef tail, NodeRef head) {
        MemoryEdge edge = new MemoryEdge(this);
        edge.connect(tail, head);
        return add(edge);
    }

    public MemoryEdge create() {
        MemoryEdge edge = new MemoryEdge(this);
        return add(edge);
    }

    public MemoryEdge add(MemoryEdge e) {
        e.storage = this;
        edges.add(e);
        return e;
    }

    @Override
    public int size() {
        return edges.size();
    }

    @Override
    public void remove(EdgeRef ref) {
        MemoryEdge memref = (MemoryEdge)ref;
        if(memref.storage != this)
            throw new IllegalArgumentException("This edge does not belong to this layer!");

        remove(memref);
    }


    public void remove(MemoryEdge edge) {
        edges.remove(edge);
        MemoryNode head = (MemoryNode) edge.getHead();
        MemoryNode tail = (MemoryNode) edge.getTail();
        if(head != null)
            head.inlinks.remove(edge);

        if(tail != null)
            tail.outlinks.remove(edge);

        edge.remove();
    }

    public boolean isEmpty() {
        return edges.isEmpty();
    }

    public void variantChanged(MemoryEdge edge, String variant) {
        if(!variant.equals(this.key.variant)) {
            remove(edge);
            store.getEdgeCollection(key.layer, variant).add(edge);
            if(isEmpty()) {
                store.edges.remove(this.key);
            }
        }
    }

    @Override
    public String toString() {
        return size() + " edges in edge layer " + key.layer;
    }
}

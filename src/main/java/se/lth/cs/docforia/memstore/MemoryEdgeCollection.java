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
    public final MemoryDocumentStore doc;
    public final Key key;
    public final ReferenceLinkedOpenHashSet<MemoryEdge> edges = new ReferenceLinkedOpenHashSet<>();

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

    public Key getKey() {
        return key;
    }

    public MemoryEdgeCollection(MemoryDocumentStore doc, Key key) {
        this.doc = doc;
        this.key = key;
    }

    @Override
    public Iterator<EdgeRef> iterator() {
        return (Iterator<EdgeRef>)((Iterable<? extends EdgeRef>)edges).iterator(); //Performance reason!
    }

    @Override
    public LayerRef layer() {
        return key;
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
            doc.getEdgeCollection(key.layer, variant).add(edge);
            if(isEmpty()) {
                doc.edges.remove(this.key);
            }
        }
    }

    @Override
    public String toString() {
        return size() + " edges in edge layer " + key.layer;
    }
}

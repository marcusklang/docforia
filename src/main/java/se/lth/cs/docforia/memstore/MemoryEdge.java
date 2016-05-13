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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import se.lth.cs.docforia.*;
import se.lth.cs.docforia.data.DataRef;

import java.util.Map;

/**
 * Memory Edge
 */
public class MemoryEdge extends EdgeStore implements EdgeRef {
    protected MemoryEdgeCollection storage;
    protected Edge instance;
    protected MemoryNode head;
    protected MemoryNode tail;
    protected Object2ObjectOpenHashMap<String,DataRef> properties = new Object2ObjectOpenHashMap<>();

    public MemoryEdge(MemoryEdgeCollection storage) {
        this.storage = storage;
    }

    @Override
    public Document parent() {
        return storage.doc.doc;
    }

    @Override
    public LayerRef layer() {
        return storage.key;
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
        return storage != null;
    }

    public void remove() {
        this.storage = null;
        this.head = null;
        this.tail = null;
        this.properties = null;
    }

    @Override
    public void setHead(NodeRef head) {
        this.head = (MemoryNode)head;
    }

    @Override
    public void setTail(NodeRef tail) {
        this.tail = (MemoryNode)tail;
    }

    @Override
    public void connect(NodeRef tail, NodeRef head) {
        MemoryNode tailnode = ((MemoryNode)tail);
        MemoryNode headnode = ((MemoryNode)head);

        if(this.head != null)
            ((MemoryNode) head).inlinks.remove(this);

        if(this.tail != null)
            ((MemoryNode) tail).outlinks.remove(this);

        this.head = headnode;
        this.tail= tailnode;

        tailnode.outlinks.add(this);
        headnode.inlinks.add(this);
    }

    @Override
    public String getLayer() {
        return storage.key.layer;
    }

    @Override
    public String getVariant() {
        return storage.key.variant;
    }

    @Override
    public void setVariant(String variant) {
        storage.variantChanged(this, variant);
    }

    @Override
    public <T extends DataRef> T getRefProperty(String key) {
        return (T)properties.get(key);
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
    public void removeProperty(String key) {
        properties.remove(key);
    }

    @Override
    public int numProperties() {
        return properties.size();
    }

    @Override
    public Iterable<Map.Entry<String, DataRef>> properties() {
        return properties.entrySet();
    }

    @Override
    public final EdgeStore get() {
        return this;
    }

    @Override
    public EdgeRef getRef() {
        return this;
    }

    @Override
    public String reference() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return String.valueOf(tail) + " -> " + String.valueOf(head) + " : " + super.toString();
    }
}

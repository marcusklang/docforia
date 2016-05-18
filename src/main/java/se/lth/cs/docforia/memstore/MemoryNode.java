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
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.Node;
import se.lth.cs.docforia.NodeStore;
import se.lth.cs.docforia.data.DataRef;
import se.lth.cs.docforia.data.StringRef;
import se.lth.cs.docforia.util.AnnotationIndex;

import java.util.Map;

/**
 * Memory Node
 */
public class MemoryNode extends NodeStore {
    protected MemoryNodeCollection storage;
    protected Node instance;
    protected Object2ObjectOpenHashMap<String,DataRef> properties = new Object2ObjectOpenHashMap<>();
    protected ObjectLinkedOpenHashSet<MemoryEdge> inlinks = new ObjectLinkedOpenHashSet<MemoryEdge>();
    protected ObjectLinkedOpenHashSet<MemoryEdge> outlinks = new ObjectLinkedOpenHashSet<MemoryEdge>();

    protected int start = Integer.MIN_VALUE;
    protected int end = Integer.MIN_VALUE;
    protected AnnotationIndex<MemoryNode>.Entry entry = null;

    public MemoryNode(MemoryNodeCollection storage) {
        this.storage = storage;
    }

    @Override
    public Document parent() {
        return storage.store.doc;
    }

    @Override
    public MemoryNodeCollection layer() {
        return storage;
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
    public boolean isAnnotation() {
        return this.start != Integer.MIN_VALUE && this.end != Integer.MIN_VALUE;
    }

    @Override
    public void setNoRanges() {
        this.start = Integer.MIN_VALUE;
        this.end = Integer.MAX_VALUE;
    }

    @Override
    public void setVariant(String variant) {
        storage.variantChanged(this, variant);
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
    public void setRanges(int start, int end) {
        storage.rangeChanged(this, start, end);
    }

    @Override
    public boolean valid() {
        return storage != null;
    }

    protected void remove() {
        this.storage = null;
        this.properties = null;
        this.inlinks = null;
        this.outlinks = null;
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
    public void putProperty(String key, String value) {
        properties.put(key, new StringRef(value));
    }

    @Override
    public void removeProperty(String key) {
        properties.remove(key);
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
    public Iterable<Map.Entry<String, DataRef>> properties() {
        return properties.entrySet();
    }

    @Override
    public final NodeStore get() {
        return this;
    }

    @Override
    public String reference() {
        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public String toString() {
        String header = layer().getLayer() + (layer().getVariant() != null ? ", " + layer().getVariant() : "");
        if(isAnnotation()) {
            return header + " : Annotation {" + storage.store.getText().substring(start,end) + "}";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(header).append(" : Node ");
            sb.append(super.toString());
            return sb.toString();
        }
    }
}

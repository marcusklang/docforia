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

import se.lth.cs.docforia.data.DataRef;
import se.lth.cs.docforia.util.FilteredDocumentIterable;

import java.util.Map;

/**
 * View storage implementation
 */
public class ViewStore extends DocumentStore {

    private final View view;
    private final DocumentStore store;

    public ViewStore(View view) {
        this.view = view;
        this.store = view.parent.store();
    }

    @Override
    public Iterable<EdgeRef> edges() {
        return store.edges();
    }

    @Override
    public Iterable<EdgeRef> inboundEdges(NodeRef node) {
        return store.inboundEdges(node);
    }

    @Override
    public Iterable<EdgeRef> outboundEdges(NodeRef node) {
        return store.outboundEdges(node);
    }

    private boolean isOverlapping(NodeStore store) {
        int start = store.getStart();
        int end = store.getEnd();
        return view.start <= start && view.end >= end; //overlapping completely
    }

    @Override
    public int numProperties() {
        return store.numProperties();
    }

    @Override
    public Iterable<NodeRef> nodes() {
        return new FilteredDocumentIterable<NodeRef>(store.nodes()) {
            @Override
            protected boolean accept(NodeRef value) {
                NodeStore store = value.get();
                if(store.isAnnotation())
                    return isOverlapping(store);
                else
                    return true;
            }
        };
    }

    @Override
    public String getVariantMetadata(String variant, String key) {
        return store.getVariantMetadata(variant, key);
    }

    @Override
    public boolean hasVariantMetadata(String variant, String key) {
        return store.hasVariantMetadata(variant, key);
    }

    @Override
    public void putVariantMetadata(String variant, String key, String value) {
        store.putVariantMetadata(variant, key, value);
    }

    @Override
    public void removeVariantMetadata(String variant) {
        store.removeVariantMetadata(variant);
    }

    @Override
    public void removeVariantMetadata(String variant, String key) {
        store.removeVariantMetadata(variant, key);
    }

    @Override
    public Iterable<Map.Entry<String, String>> variantMetadata(String variant) {
        return store.variantMetadata(variant);
    }

    @Override
    public Iterable<String> variantsWithMetadata() {
        return store.variantsWithMetadata();
    }

    @Override
    public void remove(NodeRef nodeId) {
        store.remove(nodeId);
    }

    @Override
    public void remove(EdgeRef edgeId) {
        store.remove(edgeId);
    }

    @Override
    public EdgeRef getEdge(String uniqueRef) {
        return store.getEdge(uniqueRef);
    }

    @Override
    public NodeRef getNode(String uniqueRef) {
        return store.getNode(uniqueRef);
    }

    @Override
    public String getText() {
        return store.getText().substring(view.start, view.end);
    }

    @Override
    public void setText(String text) {
        throw new UnsupportedOperationException("Not supported on a view!");
    }

    @Override
    public String getProperty(String key) {
        return store.getProperty(key);
    }

    @Override
    public boolean hasProperty(String key) {
        return store.hasProperty(key);
    }

    @Override
    public void putProperty(String key, DataRef ref) {
        store.putProperty(key, ref);
    }

    @Override
    public <T extends DataRef> T getRefProperty(String key) {
        return (T)store.getRefProperty(key);
    }

    @Override
    public Iterable<Map.Entry<String, DataRef>> properties() {
        return store.properties();
    }

    @Override
    public void removeProperty(String key) {
        store.removeProperty(key);
    }

    @Override
    public EdgeRef createEdge(String edgeLayer) {
        return store.createEdge(edgeLayer);
    }

    @Override
    public NodeRef createNode(String nodeLayer) {
        return store.createNode(nodeLayer);
    }
}

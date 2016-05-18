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

import se.lth.cs.docforia.util.DocumentIterable;
import se.lth.cs.docforia.util.FilteredDocumentIterable;

import java.util.Optional;

/**
 * View engine implementation
 */
public class ViewEngine extends DocumentEngine {
    private final View view;
    private final DocumentEngine engine;

    public ViewEngine(View parent) {
        super();
        this.view = parent;
        this.engine = view.parent.engine();
    }

    @Override
    public DocumentStore store() {
        return engine.store();
    }

    @Override
    public DocumentIterable<EdgeRef> edges(String edgeLayer, boolean onlyDefaultVariant) {
        return engine.edges(edgeLayer, onlyDefaultVariant);
    }

    @Override
    public DocumentIterable<EdgeRef> edges(String edgeLayer, String edgeVariant) {
        return engine.edges(edgeLayer, edgeVariant);
    }

    @Override
    public DocumentIterable<EdgeRef> edges(NodeRef start, Direction dir) {
        return engine.edges(start, dir);
    }

    @Override
    public DocumentIterable<EdgeRef> edges(NodeRef start, String layer, Direction dir) {
        return engine.edges(start, layer, dir);
    }

    @Override
    public DocumentIterable<EdgeRef> edges(NodeRef start, String layer, String variant, Direction dir) {
        return engine.edges(start, layer, variant, dir);
    }

    private boolean isOverlapping(NodeStore store) {
        int start = store.getStart();
        int end = store.getEnd();
        return view.start <= start && view.end >= end; //overlapping completely
    }

    public DocumentIterable<NodeRef> wrapNode(DocumentIterable<NodeRef> iter) {
        return new FilteredDocumentIterable<NodeRef>(iter) {
            @Override
            protected boolean accept(NodeRef value) {
                NodeStore store = value.get();
                return !store.isAnnotation() || isOverlapping(store);
            }
        };
    }

    @Override
    public DocumentIterable<NodeRef> nodes(String nodeLayer) {
        return wrapNode(engine.nodes(nodeLayer));
    }

    @Override
    public DocumentIterable<String> nodeLayers() {
        return engine.nodeLayers();
    }

    @Override
    public DocumentIterable<String> nodeLayerVariants(String layer) {
        return engine.nodeLayerVariants(layer);
    }

    @Override
    public DocumentIterable<Optional<String>> nodeLayerAllVariants(String layer) {
        return engine.nodeLayerAllVariants(layer);
    }

    @Override
    public DocumentIterable<NodeRef> nodes(String nodeLayer, String variant) {
        return wrapNode(engine.nodes(nodeLayer, variant));
    }

    @Override
    public DocumentIterable<NodeRef> coveredAnnotation(String nodeLayer, String nodeVariant, int from, int to) {
        return engine.coveredAnnotation(nodeLayer, nodeVariant, view.inverseTransform(from), view.inverseTransform(to));
    }

    @Override
    public void removeAllNodes(String nodeLayer, String nodeVariant) {
        engine.removeAllNodes(nodeLayer, nodeVariant);
    }

    @Override
    public void removeAllEdges(String edgeType, String edgeVariant) {
        engine.removeAllEdges(edgeType, edgeVariant);
    }

    @Override
    public String toString(Range range) {
        return engine.toString(range);
    }

    @Override
    public DocumentIterable<EdgeRef> edges(boolean onlyDefaultVariant) {
        return engine.edges(onlyDefaultVariant);
    }

    @Override
    public DocumentIterable<NodeRef> nodesWithProperty(String key, String value) {
        return wrapNode(engine.nodesWithProperty(key, value));
    }

    @Override
    public DocumentIterable<NodeRef> nodesWithProperty(String nodeLayer, String key, String value) {
        return wrapNode(engine.nodesWithProperty(nodeLayer, key, value));
    }

    @Override
    public DocumentIterable<NodeRef> nodesWithProperty(String nodeLayer, String nodeVariant, String key, String value) {
        return wrapNode(engine.nodesWithProperty(nodeLayer, nodeVariant, key, value));
    }

    @Override
    public DocumentIterable<EdgeRef> edgesWithProperty(String key, String value) {
        return engine.edgesWithProperty(key, value);
    }

    @Override
    public DocumentIterable<EdgeRef> edgesWithProperty(String edgeLayer, String key, String value) {
        return engine.edgesWithProperty(edgeLayer, key, value);
    }

    @Override
    public DocumentIterable<EdgeRef> edgesWithProperty(String edgeLayer, String edgeVariant, String key, String value) {
        return engine.edgesWithProperty(edgeLayer, edgeVariant, key, value);
    }
}

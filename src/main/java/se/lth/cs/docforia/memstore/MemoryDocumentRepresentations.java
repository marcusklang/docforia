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

import se.lth.cs.docforia.*;

import java.util.Map;

/**
 * Memory Document Node/Edge Representation instance abstraction
 */
public class MemoryDocumentRepresentations extends DocumentRepresentations {
    public MemoryDocumentRepresentations(Document doc) {
        super(doc);
    }

    @Override
    public Edge create(EdgeRef ref) {
        MemoryEdge edge = ((MemoryEdge)ref);
        MemoryEdgeFactory factory = MemoryCoreEdgeLayer.fromLayerName(edge.storage.key.layer).factory;
        return factory != null ? factory.create() : super.create(ref);
    }

    @Override
    public Node create(NodeRef ref) {
        MemoryNode node = ((MemoryNode)ref);
        MemoryNodeFactory factory = MemoryCoreNodeLayer.fromLayerName(node.storage.key.layer).factory;
        return factory != null ? factory.create() : super.create(ref);
    }

    @Override
    public Edge get(EdgeRef ref) {
        MemoryEdge eref = (MemoryEdge) ref;
        if(eref.instance != null) {
            return eref.instance;
        }
        else
        {
            Edge edge = create(ref);
            eref.instance = edge;
            initialize(edge, ref);
            return edge;
        }
    }

    @Override
    public Node get(NodeRef ref) {
        MemoryNode nref = (MemoryNode) ref;
        if(nref.instance != null) {
            return nref.instance;
        }
        else
        {
            Node node = create(ref);
            nref.instance = node;
            initialize(node, ref);
            return node;
        }
    }

    @Override
    protected void resetRepresentations() {
        for (Map.Entry<EdgeRef, Edge> entry : this.indexEdgeRef.entrySet()) {
            ((MemoryEdge)entry.getKey()).instance = null;
        }

        for (Map.Entry<NodeRef, Node> entry : this.indexNodeRef.entrySet()) {
            ((MemoryNode)entry.getKey()).instance = null;
        }

        super.resetRepresentations();
    }

    @Override
    public void register(Edge edge, EdgeRef ref) {
        ((MemoryEdge)ref).instance = edge;
        initialize(edge, ref);
    }

    @Override
    public void register(Node node, NodeRef ref) {
        ((MemoryNode)ref).instance = node;
        initialize(node, ref);
    }
}

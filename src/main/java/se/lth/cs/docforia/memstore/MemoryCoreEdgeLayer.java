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
import se.lth.cs.docforia.Edge;
import se.lth.cs.docforia.graph.Relationship;
import se.lth.cs.docforia.graph.ast.AstEdge;
import se.lth.cs.docforia.graph.text.DependencyRelation;
import se.lth.cs.docforia.graph.text.ParseTreeEdge;
import se.lth.cs.docforia.graph.text.SemanticRole;

/**
 * Core Edge Layers
 */
public enum MemoryCoreEdgeLayer {
    UNKNOWN(-1, null, null),
    RELATIONSHIP(0, Relationship.class, Relationship::new),
    DEPENDENCY_REL(1, DependencyRelation.class, DependencyRelation::new),
    SEMANTIC_ROLE(2, SemanticRole.class, SemanticRole::new),
    AST_EDGE(3, AstEdge.class, AstEdge::new),
    PARSE_TREE_EDGE(4, ParseTreeEdge.class, ParseTreeEdge::new);

    public final int id;
    public final String layer;
    public final MemoryEdgeFactory factory;

    private static final Object2ObjectOpenHashMap<String,MemoryCoreEdgeLayer> name2layer;

    static {
        name2layer = new Object2ObjectOpenHashMap<>();
        name2layer.defaultReturnValue(MemoryCoreEdgeLayer.UNKNOWN);
        for (MemoryCoreEdgeLayer memoryCoreEdgeLayer : MemoryCoreEdgeLayer.values()) {
            if(memoryCoreEdgeLayer.id != -1)
                name2layer.put(memoryCoreEdgeLayer.layer, memoryCoreEdgeLayer);
        }
    }

    MemoryCoreEdgeLayer(int id, Class<? extends Edge> layer, MemoryEdgeFactory factory) {
        this.id = id;
        this.layer = layer == null ? null : layer.getName();
        this.factory = factory;
    }

    public static MemoryCoreEdgeLayer fromLayerName(String layer) {
        return name2layer.get(layer);
    }

    public static MemoryCoreEdgeLayer fromId(int id) {
        if(id >= MemoryCoreEdgeLayer.values().length-1)
            return UNKNOWN;

        return MemoryCoreEdgeLayer.values()[id+1];
    }
}

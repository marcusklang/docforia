package se.lth.cs.docforia.query;
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

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.Edge;
import se.lth.cs.docforia.Node;

import java.util.Comparator;
import java.util.function.Function;

/**
 * Stream utilities for the query system
 */
public class StreamUtils {
    /**
     * Transfrom proposition into node representations
     * @param var the node to convert into
     */
    public static Function<Proposition, Node> toNode(NodeVar var) {
        return p -> p.get(var);
    }

    /**
     * Transfrom proposition into node representations
     * @param var the node to convert into
     * @param <T> type of node representation
     */
    public static <T extends Node> Function<Proposition,T> toNode(NodeTVar<T> var) {
        return p -> p.get(var);
    }

    /**
     * Transfrom proposition into edge representations
     * @param var the edge to convert into
     */
    public static Function<Proposition, Edge> toEdge(EdgeVar var) {
        return p -> p.get(var);
    }

    /**
     * Transfrom proposition into edge representations
     * @param var the edge to convert into
     * @param <T> type of edge representation
     */
    public static <T extends Edge> Function<Proposition,T> toEdge(EdgeTVar<T> var) {
        return p -> p.get(var);
    }


    /**
     * Create a range comparator for the given vars
     * @param vars the nodes to sort by
     * @return new comparator
     */
    public static Comparator<Proposition> orderBy(NodeVar...vars) {
        return new PropositionRangeComparator(vars);
    }

    /**
     * Create a function that extracts parts from a proposition efficiently.
     * @param doc  the document that we are working with
     * @param vars the vars to be included in the result
     * @return extraction function
     */
    public static Function<Proposition, Proposition> subset(Document doc, Var...vars) {
        Reference2IntOpenHashMap<Var> varIndex = new Reference2IntOpenHashMap<>();
        for (int i = 0; i < vars.length; i++) {
            varIndex.put(vars[i], i);
        }

        PropositionContext context = new PropositionContext(doc, varIndex);
        return p -> p.subset(context);
    }
}

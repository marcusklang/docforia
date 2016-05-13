package se.lth.cs.docforia.query.dsl;
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

import se.lth.cs.docforia.Edge;
import se.lth.cs.docforia.Node;
import se.lth.cs.docforia.query.*;
import se.lth.cs.docforia.query.predicates.EdgePredicate;
import se.lth.cs.docforia.query.predicates.NodePredicate;
import se.lth.cs.docforia.query.predicates.NonePredicate;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Base Clause
 */
public abstract class CommonClause {
    protected abstract QueryClause root();

    public WhereClause where(Var...vars) {
        return root().where(vars);
    }

    /**
     * Custom predicate bound on var
     * @param var  the typed node var
     * @param pred the predicate
     * @param <T>  the node type
     */
    public <T extends Node> QueryClause where(NodeTVar<T> var, Function<T,Boolean> pred) {
        root().select(var);
        root().predicates.add(new NodePredicate<T>(root().context, var, pred));

        return root();
    }

    /**
     * Custom predicate bound on var
     * @param var  the type edge var
     * @param pred the predicate
     * @param <T>  the node type
     */
    public <T extends Edge> QueryClause where(EdgeTVar<T> var, Function<T,Boolean> pred) {
        root().select(var);
        root().predicates.add(new EdgePredicate<T>(root().context,var,pred));

        return root();
    }

    /**
     * Custom predicate bound on var
     * @param var  the node var
     * @param pred the predicate
     */
    public QueryClause where(NodeVar var, Function<Node,Boolean> pred) {
        root().select(var);
        root().predicates.add(new NodePredicate<Node>(root().context,var,pred));

        return root();
    }

    /**
     * Custom predicate bound on var
     * @param var  the edge var
     * @param pred the predicate
     */
    public QueryClause where(EdgeVar var, Function<Edge,Boolean> pred) {
        root().select(var);
        root().predicates.add(new EdgePredicate<Edge>(root().context,var,pred));

        return root();
    }

    protected void emptyResult() {
        root().predicates.add(0, new NonePredicate(root().context));
    }

    /**
     * Compile the query and get a stream
     */
    public Stream<Proposition> stream() {
        return root().stream();
    }

    /**
     * Compile the query
     */
    public CompiledQuery compile() {
        return root().compile();
    }
}

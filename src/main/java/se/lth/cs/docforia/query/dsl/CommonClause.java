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
import se.lth.cs.docforia.util.DocumentIterable;

import java.util.function.Function;

/**
 * Base Clause
 */
public abstract class CommonClause {
    protected abstract QueryClause root();

    public WhereClause where(Var...vars) {
        return root().where(vars);
    }

    /** Specify ordering on vars, will propagate into grouped queries. */
    public QueryClause orderByRange(NodeVar...vars) {
        return root().orderByRange(vars);
    }

    public GroupQueryClause groupBy(Var...vars) {
        for(Var var : vars) {
            if (!root().selectVars.contains(var))
                throw new IllegalArgumentException(var.toString() + " is not selected!");
        }

        return new GroupQueryClause(root(), vars);
    }

    /**
     * Custom predicate bound on var
     * @param var  the typed node var
     * @param pred the predicate
     * @param <T>  the node type
     */
    public <T extends Node> QueryClause where(NodeTVar<T> var, Function<T,Boolean> pred) {
        root().predicates.add(new NodePredicate<T>(root().doc, var, pred));

        return root();
    }

    /**
     * Custom predicate bound on var
     * @param var  the type edge var
     * @param pred the predicate
     * @param <T>  the node type
     */
    public <T extends Edge> QueryClause where(EdgeTVar<T> var, Function<T,Boolean> pred) {
        root().predicates.add(new EdgePredicate<T>(root().doc,var,pred));

        return root();
    }

    /**
     * Custom predicate bound on var
     * @param var  the node var
     * @param pred the predicate
     */
    public QueryClause where(NodeVar var, Function<Node,Boolean> pred) {
        root().predicates.add(new NodePredicate<Node>(root().doc,var,pred));

        return root();
    }

    /**
     * Custom predicate bound on var
     * @param var  the edge var
     * @param pred the predicate
     */
    public QueryClause where(EdgeVar var, Function<Edge,Boolean> pred) {
        root().predicates.add(new EdgePredicate<Edge>(root().doc,var,pred));

        return root();
    }

    protected void emptyResult() {
        root().predicates.add(0, new NonePredicate(root().doc));
    }

    /**
     * Query the document model
     * @return iterable of results
     */
    public DocumentIterable<Proposition> query() {
        return root().query();
    }

    /**
     * Query the document model
     * @param n expected number of results, will stop evaluating once n is reached.
     * @return iterable of results
     */
    public DocumentIterable<Proposition> query(int n) {
        return root().query(n);
    }

    /**
     * Query the document model with computation complexity awareness
     * <b>Remarks:</b> It will always throw combinatoric for unbounded vars that always generate O(N^k) queries, where k >= 2.
     *
     * @param enableCombinatoricExplosionException defaults to false, set to true to let the query engine
     *                                             throw excepetions in case of excessive complexitiy => above or equal to N^2.
     */
    //TODO: Implement this!
    /*public DocumentIterable<Proposition> query(boolean enableCombinatoricExplosionException) {
        return root().query();
    }*/
}

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

import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.Edge;
import se.lth.cs.docforia.Node;
import se.lth.cs.docforia.query.*;
import se.lth.cs.docforia.query.predicates.AnyPredicate;
import se.lth.cs.docforia.util.DocumentIterable;
import se.lth.cs.docforia.util.DocumentIterables;

import java.util.*;
import java.util.function.Function;

/**
 * Top Level Query builder clause
 */
public class QueryClause extends CommonClause {
    protected final ArrayList<Var> selects;
    protected final HashSet<Var> selectVars = new HashSet<Var>();
    protected final Document doc;
    protected List<Predicate> predicates = new ArrayList<Predicate>();
    protected List<NodeVar> orderByRange = new ArrayList<NodeVar>();
    protected boolean distinct = false;
    protected boolean compiled = false;

    protected void select(Var var) {
        if(!selectVars.contains(var)) {
            var.setIndex(selectVars.size());
            selectVars.add(var);
        }
    }

    public QueryClause(Document doc, Var...vars) {
        if(vars.length == 0)
            throw new IllegalArgumentException("At least 1 var must be selected!");

        if(doc == null)
            throw new NullPointerException("doc");

        this.doc = doc;
        this.selects = new ArrayList<Var>(Arrays.asList(vars));

        for (int i = 0; i < vars.length; i++) {
            vars[i].setIndex(i);
            selectVars.add(vars[i]);
        }
    }

    @Override
    protected QueryClause root() {
        return this;
    }

    /** Add an AND constraint on the selected vars */
    @Override
    public WhereClause where(Var...vars) {
        for (Var var : vars){
            if(!selectVars.contains(var)) {
                select(var);
            }
        }

        return new WhereClause(this, vars);
    }

    /** Add an AND constraint on the selected var, constraint is specified by {@code pred}*/
    @Override
    public QueryClause where(NodeVar var, final Function<Node,Boolean> pred) {
        predicates.add(new Predicate(doc, var) {
            @Override
            public boolean eval(Proposition proposition) {
                return pred.apply(proposition.get(var));
            }
        });

        return this;
    }

    /** Add an AND constraint on the selected var, constraint is specified by {@code pred} */
    @Override
    public QueryClause where(EdgeVar var, final Function<Edge,Boolean> pred) {
        predicates.add(new Predicate(doc, var) {
            @Override
            public boolean eval(Proposition proposition) {
                return pred.apply(proposition.get(var));
            }
        });

        return this;
    }

    /** Add an AND constraint on the selected var, constraint is specified by {@code pred} */
    @Override
    public <N extends Node> QueryClause where(NodeTVar<N> var, final Function<N,Boolean> pred) {
        predicates.add(new Predicate(doc, var) {
            @Override
            public boolean eval(Proposition proposition) {
                return pred.apply(proposition.get(var));
            }
        });

        return this;
    }

    /** Add an AND constraint on the selected var, constraint is specified by {@code pred} */
    @Override
    public <E extends Edge> QueryClause where(EdgeTVar<E> var, final Function<E,Boolean> pred) {
        predicates.add(new Predicate(doc, var) {
            @Override
            public boolean eval(Proposition proposition) {
                return pred.apply(proposition.get(var));
            }
        });

        return this;
    }

    /** Specify ordering on vars, will propagate into grouped queries. */
    @Override
    public QueryClause orderByRange(NodeVar...vars) {
        for(NodeVar var : vars) {
            if(!selectVars.contains(var))
                throw new IllegalArgumentException(var.toString() + " is not selected!");

            orderByRange.add(var);
        }
        return this;
    }

    /** Set distinct output. */
    public QueryClause distinct() {
        this.distinct = true;
        return this;
    }

    protected Comparator<Proposition> orderByComparator() {
        return (o1, o2) -> {
            for(int i = 0; i < orderByRange.size(); i++) {
                int result = o1.get(orderByRange.get(i)).compareTo(o2.get(orderByRange.get(i)));
                if(result != 0)
                    return result;
            }

            return 0;
        };
    }

    protected void compile() {
        if(compiled)
            return;

        //1. Check that all selectVars have been bound, otherwise infer an always true predicate.
        HashSet<Var> remaningVars = new HashSet<Var>(selectVars);
        for (Predicate predicate : predicates) {
            for (Var var : predicate.vars()) {
                remaningVars.remove(var);
            }
        }

        //2. For the remaining insert a always true predicate
        if(remaningVars.size() > 0) {
            predicates.add(new AnyPredicate(doc, remaningVars.toArray(new Var[remaningVars.size()])));
        }
    }

    protected List<Proposition> result(int n) {
        compile();
        Query q = new Query(doc, new ArrayList<Var>(selectVars), predicates);

        List<Proposition> evaluate;
        if(n <= 0) {
            evaluate = q.evaluate();
        } else {
            evaluate = q.evaluate(n);
        }

        if(distinct) {
            HashSet<Proposition> propositions = new HashSet<>();
            for (Proposition proposition : evaluate) {
                propositions.add(proposition);
            }

            evaluate = new ArrayList<>(propositions);
        }

        if(orderByRange.size() > 0) {
            Collections.sort(evaluate, orderByComparator());
        }

        return evaluate;
    }

    @Override
    public DocumentIterable<Proposition> query() {
        return DocumentIterables.wrap(result(-1));
    }

    @Override
    public DocumentIterable<Proposition> query(int n) {
        return DocumentIterables.wrap(result(n));
    }
}

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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Top Level Query builder clause
 */
public class QueryClause extends CommonClause {
    protected final Document doc;
    protected final HashSet<Var> outputVars = new HashSet<>();
    protected List<Predicate> predicates = new ArrayList<Predicate>();
    protected QueryContext context;

    protected void select(Var var) {
        context.addVar(var);
    }

    public QueryClause(Document doc, Var...vars) {
        if(vars.length == 0)
            throw new IllegalArgumentException("At least 1 var must be selected!");

        if(doc == null)
            throw new NullPointerException("doc");

        context = new QueryContext(doc);

        this.doc = doc;
        for (Var var : vars) {
            context.addVar(var);
            outputVars.add(var);
        }
    }

    @Override
    protected QueryClause root() {
        return this;
    }

    /** Add an AND constraint on the selected vars */
    @Override
    public WhereClause where(Var...vars) {
        for (Var var : vars) {
            context.addVar(var);
        }

        return new WhereClause(this, vars);
    }

    /** Add an AND constraint on the selected var, constraint is specified by {@code pred}*/
    @Override
    public QueryClause where(NodeVar var, final Function<Node,Boolean> pred) {
        context.addVar(var);
        predicates.add(new Predicate(context, var) {
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
        context.addVar(var);
        predicates.add(new Predicate(context, var) {
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
        context.addVar(var);
        predicates.add(new Predicate(context, var) {
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
        context.addVar(var);
        predicates.add(new Predicate(context, var) {
            @Override
            public boolean eval(Proposition proposition) {
                return pred.apply(proposition.get(var));
            }
        });

        return this;
    }

    /** Specify ordering on vars, will propagate into grouped queries. */
    /*@Override
    public QueryClause orderByRange(NodeVar...vars) {
        for(NodeVar var : vars) {
            if(!selectVars.contains(var))
                throw new IllegalArgumentException(var.toString() + " is not selected!");

            orderByRange.add(var);
        }
        return this;
    }*/
/*
    protected Comparator<Proposition> orderByComparator() {
        return (o1, o2) -> {
            for(int i = 0; i < orderByRange.size(); i++) {
                int result = o1.get(orderByRange.get(i)).compareTo(o2.get(orderByRange.get(i)));
                if(result != 0)
                    return result;
            }

            return 0;
        };
    }*/

    protected void addAnyPredicates() {
        //1. Check that all selectVars have been bound, otherwise infer an always true predicate.
        HashSet<Var> remaningVars = new HashSet<Var>(outputVars);
        for (Predicate predicate : predicates) {
            for (Var var : predicate.vars()) {
                remaningVars.remove(var);
            }
        }

        //2. For the remaining insert a always true predicate
        if(remaningVars.size() > 0) {
            predicates.add(new AnyPredicate(context, remaningVars.toArray(new Var[remaningVars.size()])));
        }
    }
/*
    protected List<Proposition> result(int n) {
        compile();
        Query q = new Query(doc, new ArrayList<Var>(queryVars), predicates);

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
    }*/

    public Stream<Proposition> stream() {
        addAnyPredicates();
        return StreamSupport.stream(new SpliteratableQuery(context, new HashSet<>(outputVars), predicates.toArray(new Predicate[predicates.size()])), false);
    }

    @Override
    public CompiledQuery compile() {
        addAnyPredicates();
        return new CompiledQuery(context, new HashSet<>(outputVars), predicates.toArray(new Predicate[predicates.size()]));
    }

    /*
    @Override
    public DocumentIterable<Proposition> query() {
        return DocumentIterables.wrap(result(-1));
    }

    @Override
    public DocumentIterable<Proposition> query(int n) {
        return DocumentIterables.wrap(result(n));
    }*/
}

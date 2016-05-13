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

import se.lth.cs.docforia.query.filter.BasicEdgeFilter;
import se.lth.cs.docforia.query.filter.BasicNodeFilter;
import se.lth.cs.docforia.query.filter.Filter;

/**
 * Base class for all predicates
 */
public abstract class Predicate
{
    protected final QueryContext context;
    protected final Var[] vars;
    protected final int[] varIndex;
    protected final Filter[] filters;
/*
    protected final boolean[] constant;
    protected PropositionIterator iterator;
    private boolean evaluated = false;*/

    public Predicate(QueryContext queryContext, Var...vars) {
        this.context = queryContext;
        this.vars = vars;
        //this.constant = new boolean[vars.length];
        this.filters = new Filter[vars.length];
        this.varIndex = new int[vars.length];

        for (int i = 0; i < vars.length; i++) {
            if(vars[i] instanceof NodeVar)
                filters[i] = new BasicNodeFilter(context.doc.engine(), ((NodeVar)vars[i]).type, ((NodeVar)vars[i]).variant);
            else if(vars[i] instanceof EdgeVar)
                filters[i] = new BasicEdgeFilter(context.doc.engine(), ((EdgeVar)vars[i]).type, ((EdgeVar)vars[i]).variant);
            else
                throw new UnsupportedOperationException("Unsupported variable!");

            varIndex[i] = context.var2index.getInt(vars[i]);
        }
    }

    public Var[] vars() {
        return vars;
    }

    public PredicateState createState() {
        return new PredicateState(new boolean[vars.length], null, false);
    }

    protected PropositionIterator suggest(PredicateState state, Proposition proposition) {
        return new CombinationIterator(context, vars, filters, state.constant);
    }

    public final void enter(PredicateState state, Proposition proposition) {
        int constants = 0;
        for (int i = 0; i < vars.length; i++) {
            state.constant[i] = proposition.data[varIndex[i]] != null;
            constants += state.constant[i] ? 1 : 0;
        }

        if(constants == vars.length)
            return;

        state.iterator = suggest(state, proposition);
    }

    public final boolean next(PredicateState state, Proposition proposition) {
        if(state.iterator == null) { //special case: all are constants, return one result if true
            if(state.evaluated)
                return false;
            else {
                state.evaluated = true;
                return eval(proposition);
            }
        }

        while(state.iterator.next(proposition)) {
            if(eval(proposition))
                return true;
        }

        return false;
    }

    public final void exit(PredicateState state, Proposition proposition) {
        for (int i = 0; i < vars.length; i++) {
            if(!state.constant[i]) {
                proposition.data[varIndex[i]] = null;
            }
        }
        state.evaluated = false;
    }

    public abstract boolean eval(Proposition proposition);
}

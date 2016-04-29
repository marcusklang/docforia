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

import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.StoreRef;
import se.lth.cs.docforia.query.filter.BasicEdgeFilter;
import se.lth.cs.docforia.query.filter.BasicNodeFilter;
import se.lth.cs.docforia.query.filter.Filter;

/**
 * Base class for all predicates
 */
public abstract class Predicate
{
    protected final Document doc;
    protected final Var[] vars;
    protected final Filter[] filters;
    protected final boolean[] constant;

    protected static StoreRef ref(Proposition prop, Var var) {
        return prop.proposition[var.getIndex()];
    }

    protected static NodeRef noderef(Proposition prop, NodeVar var) {
        return (NodeRef)prop.proposition[var.getIndex()];
    }

    protected static NodeRef edgeref(Proposition prop, EdgeVar var) {
        return (NodeRef)prop.proposition[var.getIndex()];
    }

    public Predicate(Document doc, Var...vars) {
        this.doc = doc;
        this.vars = vars;
        this.constant = new boolean[vars.length];
        this.filters = new Filter[vars.length];

        for (int i = 0; i < vars.length; i++) {
            if(vars[i] instanceof NodeVar)
                filters[i] = new BasicNodeFilter(doc.engine(), ((NodeVar)vars[i]).type, ((NodeVar)vars[i]).variant);
            else if(vars[i] instanceof EdgeVar)
                filters[i] = new BasicEdgeFilter(doc.engine(), ((EdgeVar)vars[i]).type, ((EdgeVar)vars[i]).variant);
            else
                throw new UnsupportedOperationException("Unsupported variable!");
        }
    }

    public Var[] vars() {
        return vars;
    }

    protected PropositionIterator iterator;

    protected PropositionIterator suggest(Proposition proposition) {
        return new CombinationIterator(vars, filters, constant);
    }

    public final void enter(Proposition proposition) {
        int constants = 0;
        for (int i = 0; i < vars.length; i++) {
            constant[i] = proposition.proposition[vars[i].index] != null;
            constants += constant[i] ? 1 : 0;
        }

        if(constants == vars.length)
            return;

        iterator = suggest(proposition);
    }

    private boolean evaluated = false;

    public final boolean next(Proposition proposition) {
        if(iterator == null) { //special case: all are constants, return one result if true
            if(evaluated)
                return false;
            else {
                evaluated = true;
                return eval(proposition);
            }
        }

        while(iterator.next(proposition)) {
            if(eval(proposition))
                return true;
        }

        return false;
    }

    public final void exit(Proposition proposition) {
        for (int i = 0; i < vars.length; i++) {
            if(!constant[i]) {
                proposition.proposition[vars[i].index] = null;
            }
        }
        evaluated = false;
    }

    public abstract boolean eval(Proposition proposition);
}

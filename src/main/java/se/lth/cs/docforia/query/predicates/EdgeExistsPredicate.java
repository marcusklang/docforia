package se.lth.cs.docforia.query.predicates;
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

import se.lth.cs.docforia.Direction;
import se.lth.cs.docforia.EdgeRef;
import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.StoreRef;
import se.lth.cs.docforia.query.*;

/**
 * Edge exist predicate
 */
public class EdgeExistsPredicate extends Predicate {
    private static final int TAIL = 0;
    private static final int HEAD = 1;
    private static final int EDGE = 2;

    public EdgeExistsPredicate(QueryContext context, NodeVar tail, NodeVar head, EdgeVar edgeVar) {
        super(context, tail, head, edgeVar);
    }

    @Override
    protected PropositionIterator suggest(final PredicateState state, final Proposition proposition) {
        if(state.constant[EDGE]) {
            EdgeRef edge = proposition.edgeref(vars[EDGE]);
            NodeRef head = edge.get().getHead();
            NodeRef tail = edge.get().getTail();

            if(state.constant[TAIL]) {
                if(!tail.equals(proposition.get(vars[TAIL]))) {
                    return EmptyPropositionIterator.instance();
                }
            }
            else if(state.constant[HEAD]) {
                if(!head.equals(proposition.get(vars[HEAD]))) {
                    return EmptyPropositionIterator.instance();
                }
            }
            else {
                if(!tail.equals(proposition.get(vars[TAIL])) || !head.equals(proposition.get(vars[HEAD]))) {
                    return EmptyPropositionIterator.instance();
                }
            }

            return new SinglePropositionIterator(context, new Var[] {vars[0], vars[1], vars[2]}, new StoreRef[] {tail, head, edge});
        } else {
            //Operation: find Edge!
            if(state.constant[TAIL] && state.constant[HEAD]) {
                NodeRef tail = proposition.noderef(vars[TAIL]);
                NodeRef head = proposition.noderef(vars[HEAD]);
                return new StoreRefPropositionIterator(context, vars[EDGE], context.doc.engine().edges(tail, head, vars[EDGE].getLayer(), vars[EDGE].getVariant()));
            }
            else if(state.constant[TAIL]) {
                NodeRef tail = proposition.noderef(vars[TAIL]);
                return new EdgeRefPropositionIterator(context, vars, context.doc.engine().edges(tail, vars[EDGE].getLayer(), vars[EDGE].getVariant(), Direction.OUT).iterator());
            }
            else if(state.constant[HEAD]) {
                NodeRef head = proposition.noderef(vars[HEAD]);
                return new EdgeRefPropositionIterator(context, vars, context.doc.engine().edges(head, vars[EDGE].getLayer(), vars[EDGE].getVariant(), Direction.IN).iterator());
            }
            else {
                return new EdgeRefPropositionIterator(context, vars, context.doc.engine().edges(vars[EDGE].getLayer(), vars[EDGE].getVariant()).iterator());
            }
        }
    }

    @Override
    public boolean eval(Proposition proposition) {
        NodeRef head = proposition.noderef(vars[HEAD]);
        NodeRef tail = proposition.noderef(vars[TAIL]);
        EdgeRef edge = proposition.edgeref(vars[EDGE]);

        return edge.get().getHead().equals(head) &&
               edge.get().getTail().equals(tail);
    }
}

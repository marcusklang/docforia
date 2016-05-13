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

import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.NodeStore;
import se.lth.cs.docforia.Range;
import se.lth.cs.docforia.query.*;
import se.lth.cs.docforia.util.AnnotationNavigator;
import se.lth.cs.docforia.util.Annotations;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Node window predicate
 */
public class NodeInWindowPredicate extends Predicate {

    private final Set<NodeRef> nodes;
    private final ArrayDeque<NodeRef> ordered;
    private final int minStart;
    private final int maxEnd;

    public NodeInWindowPredicate(QueryContext context, NodeVar target, String layer, String variant, Range range, int pre, int post, boolean flexible) {
        super(context, target);
        nodes = new HashSet<>();
        ordered = new ArrayDeque<>();
        AnnotationNavigator<NodeRef> annotations = context.doc.engine().annotations(layer, variant);

        if(annotations.next(range.getStart())) {
            int start = -1;
            int end = -1;

            int i = pre;

            NodeRef startel = annotations.current();
            NodeRef last = null;

            while(annotations.prev() && i > 0 && annotations.current().get().isAnnotation()) {
                nodes.add(last = annotations.current());
                ordered.addFirst(last);
                i--;
            }

            if(annotations.current() != startel)
                annotations.next(range.getStart());

            if(last != null)
                start = last.get().getStart();
            else
                start = startel.get().getStart();

            nodes.add(startel);
            ordered.addLast(startel);

            int k = post;
            while(annotations.next()) {
                NodeStore nodeStore = annotations.current().get();
                if(Annotations.coveredBy(nodeStore.getStart(), nodeStore.getEnd(), range.getStart(), range.getEnd())) {
                    nodes.add(last = annotations.current());
                    ordered.addLast(last);
                }
                else
                {
                    if(k > 0) {
                        k--;
                        nodes.add(last = annotations.current());
                        ordered.addLast(last);
                    }

                    break;
                }
            }

            if(!annotations.hasReachedEnd()) {
                while (annotations.next() && k > 0) {
                    nodes.add(last = annotations.current());
                    ordered.addLast(last);
                    k--;
                }
            }

            end = last == null ? -1 : last.get().getEnd();

            if(!flexible && (i != 0 || k != 0)) {
                nodes.clear();
                minStart = -1;
                maxEnd = -1;
            }
            else {
                minStart = start;
                maxEnd = end;
            }
        }
        else {
            minStart = -1;
            maxEnd = -1;
        }
    }

    @Override
    protected PropositionIterator suggest(final PredicateState state, Proposition proposition) {
        if(state.constant[0])
            return super.suggest(state, proposition);
        else
            return new PropositionIterator() {
                private Iterator<NodeRef> iter = ordered.iterator();

                @Override
                public boolean next(Proposition proposition) {
                    if(!iter.hasNext())
                        return false;

                    proposition.data[varIndex[0]] = iter.next();
                    return true;
                }
            };
    }

    public int getStart() {
        return minStart;
    }

    public int getEnd() {
        return maxEnd;
    }

    @Override
    public boolean eval(Proposition proposition) {
        return nodes.contains((NodeRef)proposition.data[varIndex[0]]);
    }
}

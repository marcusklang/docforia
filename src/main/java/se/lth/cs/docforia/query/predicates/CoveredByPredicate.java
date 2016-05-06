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

import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.NodeStore;
import se.lth.cs.docforia.query.NodeVar;
import se.lth.cs.docforia.query.Predicate;
import se.lth.cs.docforia.query.Proposition;
import se.lth.cs.docforia.query.PropositionIterator;
import se.lth.cs.docforia.util.AnnotationNavigator;

/**
 * Covered by predicate
 */
public class CoveredByPredicate extends Predicate {

    public CoveredByPredicate(Document doc, NodeVar child, NodeVar parent) {
        super(doc, parent, child);
    }

    private static final int PARENT = 0;
    private static final int CHILD = 1;

    @Override
    protected PropositionIterator suggest(Proposition proposition) {
        //4 cases:
        // * Parent and child constant
        // * Child constant
        // * Parent constant
        // * None constant

        if(constant[PARENT] && constant[CHILD]) {
            throw new RuntimeException("Incorrect behaviour, this case is never supposed to be called!");
        }
        else if(constant[PARENT] && !constant[CHILD]) {
            NodeRef parent = (NodeRef) ref(proposition, vars[PARENT]);
            if(parent.get().isAnnotation()) {
                int start = parent.get().getStart();
                int end = parent.get().getEnd();
                String type = vars[CHILD].getLayer();
                String variant = vars[CHILD].getVariant();

                return new StoreRefPropositionIterator(
                        vars[CHILD],
                        doc.engine().coveredAnnotation(type, variant, start, end)
                );
            }
            else
                return EmptyPropositionIterator.instance();
        }
        else if(constant[CHILD] && !constant[PARENT]) {
            NodeRef child = noderef(proposition, (NodeVar)vars[CHILD]);
            if(child.get().isAnnotation()) {
                int start = child.get().getStart();
                int end = child.get().getEnd();
                String type = vars[PARENT].getLayer();
                String variant = vars[PARENT].getVariant();

                return new StoreRefPropositionIterator(vars[PARENT], doc.engine().coveringAnnotation(type, variant, start, end));
            }
            else
                return EmptyPropositionIterator.instance();
        }
        else {
            final AnnotationNavigator<NodeRef> parentNavigator = doc.engine().annotations(vars[PARENT].getLayer(), vars[PARENT].getVariant());
            final AnnotationNavigator<NodeRef> childNavigator = doc.engine().annotations(vars[CHILD].getLayer(), vars[CHILD].getVariant());

            if(!parentNavigator.next())
                return EmptyPropositionIterator.instance();
            else {
                return prop -> {
                    if(childNavigator.hasReachedEnd() || parentNavigator.hasReachedEnd())
                        return false;

                    if(!childNavigator.next())
                        return false;

                    int childStart = childNavigator.start();
                    int childEnd = childNavigator.end();

                    int parentStart = parentNavigator.start();
                    int parentEnd = parentNavigator.end();

                    while(!coveredBy(childStart, childEnd, parentStart, parentEnd)) {
                        if(childEnd >= parentEnd) {
                            if(!parentNavigator.next())
                                break;

                            //move parent forward
                            //if(!parentNavigator.nextFloor(childStart))
                                //break;
                        }
                        else if(parentStart > childStart)
                        {
                            //move child forward
                            if(!childNavigator.next(parentStart))
                                break;
                        }

                        childStart = childNavigator.start();
                        childEnd = childNavigator.end();

                        parentStart = parentNavigator.start();
                        parentEnd = parentNavigator.end();
                    }

                    if(parentNavigator.hasReachedEnd() || childNavigator.hasReachedEnd())
                        return false;

                    prop.proposition[vars[PARENT].getIndex()] = parentNavigator.current();
                    prop.proposition[vars[CHILD].getIndex()] = childNavigator.current();
                    return true;
                };
            }

        }
    }

    protected boolean coveredBy(int child_start, int child_end, int parent_start, int parent_end) {
        return parent_start <= child_start && parent_end >= child_end && (child_start != parent_end || child_start == parent_start);
    }

    @Override
    public boolean eval(Proposition proposition) {
        NodeStore child = proposition.noderef(vars[CHILD]).get();
        NodeStore parent = proposition.noderef(vars[PARENT]).get();

        return !(!child.isAnnotation() || !parent.isAnnotation())
                && coveredBy(doc.transform(child.getStart()),  doc.transform(child.getEnd()),
                             doc.transform(parent.getStart()), doc.transform(parent.getEnd()));

    }
}

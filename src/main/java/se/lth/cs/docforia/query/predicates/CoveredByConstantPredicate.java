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
import se.lth.cs.docforia.query.filter.CoveredByFilter;

/**
 * Covered by predicate with a specified range
 */
public class CoveredByConstantPredicate extends Predicate {

    private final int parent_from;
    private final int parent_to;

    public CoveredByConstantPredicate(Document doc, NodeVar child, int from, int to) {
        super(doc, child);
        filters[0] = new CoveredByFilter(doc.engine(), child.getLayer(), child.getVariant(), from, to);
        this.parent_from = from;
        this.parent_to = to;
    }

    @Override
    protected PropositionIterator suggest(Proposition proposition) {
        return new StoreRefPropositionIterator(vars[0], doc.engine().coveredAnnotation(vars[0].getLayer(),vars[0].getVariant(),parent_from, parent_to));
    }

    public boolean coveredBy(int child_start, int child_end, int parent_start, int parent_end) {
        return parent_start <= child_start && parent_end >= child_end && (child_start != parent_end || child_start == parent_start);
    }

    @Override
    public boolean eval(Proposition proposition) {
        NodeRef child = proposition.noderef(vars[0]);

        if(!child.get().isAnnotation())
            return false;

        NodeStore store = child.get();

        return coveredBy(doc.transform(store.getStart()), doc.transform(store.getEnd()), parent_from, parent_to);
    }
}

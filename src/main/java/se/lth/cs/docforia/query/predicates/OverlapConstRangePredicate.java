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

import se.lth.cs.docforia.NodeStore;
import se.lth.cs.docforia.query.NodeVar;
import se.lth.cs.docforia.query.Predicate;
import se.lth.cs.docforia.query.Proposition;
import se.lth.cs.docforia.query.QueryContext;
import se.lth.cs.docforia.query.filter.OverlapFilter;

/**
 * Range intersect predicate
 */
public class OverlapConstRangePredicate extends Predicate {
    private final int start;
    private final int end;

    public OverlapConstRangePredicate(QueryContext context, NodeVar var, int start, int end) {
        super(context, var);
        this.filters[0] = new OverlapFilter(context.doc.engine(), var.getLayer(), var.getVariant(), start, end);
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean eval(Proposition proposition) {
        NodeStore nodeStore = proposition.noderef(vars[0]).get();
        return nodeStore.isAnnotation() && nodeStore.getEnd() > start && nodeStore.getStart() < end;
    }
}

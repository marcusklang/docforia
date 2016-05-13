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
import se.lth.cs.docforia.query.*;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Predicate that checks against a set of nodes
 */
public class ConstantNodeCandidates extends Predicate {
    private HashSet<NodeRef> nodeRefs;

    public ConstantNodeCandidates(QueryContext context, NodeVar nodeVar, HashSet<NodeRef> nodeRefs) {
        super(context, nodeVar);
        this.nodeRefs = nodeRefs;
    }

    @Override
    protected PropositionIterator suggest(PredicateState state, final Proposition proposition) {
        return new PropositionIterator() {
            private Iterator<NodeRef> refIterator = nodeRefs.iterator();

            @Override
            public boolean next(Proposition proposition) {
                if(!refIterator.hasNext())
                    return false;

                proposition.data[varIndex[0]] = refIterator.next();
                return true;
            }
        };
    }

    @Override
    public boolean eval(Proposition proposition) {
        return nodeRefs.contains(proposition.noderef(vars[0]));
    }
}

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

import se.lth.cs.docforia.StoreRef;
import se.lth.cs.docforia.query.Proposition;
import se.lth.cs.docforia.query.PropositionIterator;
import se.lth.cs.docforia.query.QueryContext;
import se.lth.cs.docforia.query.Var;

/**
 * Single result suggester
 */
public class SinglePropositionIterator implements PropositionIterator {

    private int[] vars;
    private StoreRef[] storeRefs;

    public SinglePropositionIterator(QueryContext context, Var[]vars, StoreRef[] storeRefs) {
        this.vars = new int[vars.length];
        for (int i = 0; i < vars.length; i++) {
            this.vars[i] = context.indexOf(vars[i]);
        }
        this.storeRefs = storeRefs;
    }

    private boolean read = false;

    @Override
    public boolean next(Proposition proposition) {
        if(read)
            return false;
        else {
            read = true;
            for (int i = 0; i < vars.length; i++) {
                proposition.data[vars[i]] = storeRefs[i];
            }
            return true;
        }
    }
}

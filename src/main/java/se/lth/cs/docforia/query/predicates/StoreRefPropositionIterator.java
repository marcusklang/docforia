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
import se.lth.cs.docforia.query.Var;

import java.util.Iterator;

/**
 * Basic suggester of layer contents
 */
public class StoreRefPropositionIterator implements PropositionIterator {
    private final int index;
    private final Iterator<? extends StoreRef> iter;

    public <T extends StoreRef> StoreRefPropositionIterator(int index, Iterator<T> iter) {
        this.index = index;
        this.iter = iter;
    }

    public <T extends StoreRef> StoreRefPropositionIterator(Var var, Iterator<T> iter) {
        this(var.getIndex(), iter);
    }

    public <T extends StoreRef> StoreRefPropositionIterator(Var var, Iterable<T> iter) {
        this(var.getIndex(),iter.iterator());
    }

    @Override
    public boolean next(Proposition proposition) {
        if(iter.hasNext()) {
            proposition.proposition[index] = iter.next();
            return true;
        }
        else
            return false;
    }
}

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

import se.lth.cs.docforia.query.filter.Filter;
import se.lth.cs.docforia.query.predicates.StoreRefPropositionIterator;

/**
 * Iterates all combinations of input filters.
 * <b>Remarks:</b> Does not support a special case when all inputs are constants.
 */
public class CombinationIterator implements PropositionIterator
{
    private abstract class ResetablePropositionIterator implements PropositionIterator {
        public abstract void reset();
    }

    private final ResetablePropositionIterator[] iterators;
    private boolean first = true;
    private int dynamicLen = 0;

    public CombinationIterator(Var[] vars, Filter[] filters, boolean[] constant)
    {
        this.iterators = new ResetablePropositionIterator[vars.length];

        int startIndex = 0;

        for(int i = 0; i < vars.length; i++) {
            if(!constant[i])
            {
                //Found dynamic
                final Var var = vars[i];
                final Filter filter = filters[i];

                this.iterators[startIndex] = new ResetablePropositionIterator() {
                    private PropositionIterator iter = new StoreRefPropositionIterator(var.getIndex(),filter.newIterator());

                    @Override
                    public void reset() {
                        iter = new StoreRefPropositionIterator(var.getIndex(),filter.newIterator());
                    }

                    @Override
                    public boolean next(Proposition proposition) {
                        return iter.next(proposition);
                    }
                };

                startIndex++;
            }
        }

        dynamicLen = startIndex;
        if(dynamicLen == 0)
            throw new UnsupportedOperationException("All inputs are constant, no need for a combination iterator!");
    }

    @Override
    public boolean next(Proposition proposition) {
        if(first)
        {
            for(int i = 0; i < dynamicLen; i++) {
                if(!iterators[i].next(proposition))
                    return false;
            }
            first = false;

            return true;
        }

        if(iterators[0].next(proposition)) {
            return true;
        }
        else if(iterators.length > 1) {
            int pos = 1;

            //iterate parents
            while(pos < dynamicLen) {
                if (iterators[pos].next(proposition)) {
                    break;
                } else {
                    pos++;
                }
            }

            if(pos == iterators.length)
                return false; //all possibilites has been iterated through.

            pos--;
            while(pos >= 0) {
                iterators[pos].reset();
                iterators[pos].next(proposition);
                pos--;
            }
            pos = 0;
        }
        else
            return false;

        return true;
    }
}

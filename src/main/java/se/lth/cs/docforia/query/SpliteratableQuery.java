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

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * Compiled spliterable query
 */
public class SpliteratableQuery implements Spliterator<Proposition> {
    private final QueryContext context;
    private final PropositionContext outputPropositionContext;
    private final PropositionContext queryPropositionContext;
    private final Predicate[] predicates;
    private final PredicateState[] states;

    public SpliteratableQuery(QueryContext context, Set<Var> outputVars, Predicate[] predicates) {
        this.context = context;
        Reference2IntOpenHashMap<Var> outputVar2Index = new Reference2IntOpenHashMap<>();
        for (Var outputVar : outputVars) {
            outputVar2Index.put(outputVar, outputVar2Index.size());
        }

        this.queryPropositionContext = new PropositionContext(context.doc, context.var2index);
        this.outputPropositionContext = new PropositionContext(context.doc, outputVar2Index);
        this.predicates = predicates;
        this.current = new Proposition(queryPropositionContext);
        this.states = new PredicateState[predicates.length];
        for (int i = 0; i < this.predicates.length; i++) {
            this.states[i] = predicates[i].createState();
        }

        this.predicates[0].enter(states[0], current);
    }

    private int p = 0;
    private Proposition current;

    private boolean step()
    {
        while(p != -1) {
            Predicate pred = predicates[p];
            PredicateState state = states[p];

            if(!pred.next(state, current)) {
                //no more propositions
                pred.exit(state, current);
                p--;
            }
            else
            {
                p++;
                if(p == predicates.length) {
                    //goal reached!
                    p--;
                    return true;
                }
                else
                {
                    predicates[p].enter(states[p], current);
                }
            }
        }

        return false;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Proposition> action) {
        boolean retval;
        if(retval = step()) {
            action.accept(current.subset(outputPropositionContext));
        }
        return retval;
    }

    @Override
    public void forEachRemaining(Consumer<? super Proposition> action) {
        while(step()) {
            action.accept(current.subset(outputPropositionContext));
        }
    }

    @Override
    public Spliterator<Proposition> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return IMMUTABLE | NONNULL;
    }
}

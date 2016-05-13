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

import se.lth.cs.docforia.EdgeRef;
import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.query.*;

import java.util.Iterator;
import java.util.Objects;

/**
 * Edge suggester
 */
public class EdgeRefPropositionIterator implements PropositionIterator {
    private final int edge;
    private final Iterator<EdgeRef> iter;
    private final NodeVar head;
    private final NodeVar tail;
    private final int headIndex;
    private final int tailIndex;

    public EdgeRefPropositionIterator(QueryContext context, Var[]var, Iterator<EdgeRef> iter) {
        this.tail = (NodeVar)var[0];
        this.tailIndex = context.indexOf(var[0]);
        this.head = (NodeVar)var[1];
        this.headIndex = context.indexOf(var[1]);
        this.edge = context.indexOf(var[2]);
        this.iter = iter;
    }

    @Override
    public boolean next(Proposition proposition) {
        while(iter.hasNext())
        {
            EdgeRef edge = iter.next();
            NodeRef head = edge.get().getHead();
            NodeRef tail = edge.get().getTail();

            if(this.head.getLayer().equals(head.get().getLayer())
                    && Objects.equals(this.head.getVariant(), head.get().getVariant())

                    && this.tail.getLayer().equals(tail.get().getLayer())
                    && Objects.equals(this.tail.getVariant(), tail.get().getVariant()))
            {

                proposition.data[this.edge] = edge;
                proposition.data[this.headIndex] = head;
                proposition.data[this.tailIndex] = tail;
                return true;
            }
        }

        return false;
    }
}

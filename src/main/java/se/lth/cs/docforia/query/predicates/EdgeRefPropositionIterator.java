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
import se.lth.cs.docforia.query.NodeVar;
import se.lth.cs.docforia.query.Proposition;
import se.lth.cs.docforia.query.PropositionIterator;
import se.lth.cs.docforia.query.Var;

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

    public EdgeRefPropositionIterator(Var[] var, Iterator<EdgeRef> iter) {
        this.edge = var[2].getIndex();
        this.head = (NodeVar)var[1];
        this.tail = (NodeVar)var[0];
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

                proposition.proposition[this.edge] = edge;
                proposition.proposition[this.head.getIndex()] = head;
                proposition.proposition[this.tail.getIndex()] = tail;
                return true;
            }
        }

        return false;
    }
}

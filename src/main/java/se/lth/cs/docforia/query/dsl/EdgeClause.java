package se.lth.cs.docforia.query.dsl;
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

import se.lth.cs.docforia.LayerRef;
import se.lth.cs.docforia.exceptions.QueryException;
import se.lth.cs.docforia.query.EdgeVar;
import se.lth.cs.docforia.query.NodeVar;
import se.lth.cs.docforia.query.Var;
import se.lth.cs.docforia.query.predicates.EdgeExistsPredicate;
import se.lth.cs.docforia.query.predicates.EdgeNotExistsPredicate;

/**
 * Edge Clause with edge specific options
 */
public class EdgeClause {
    protected final WhereClause parent;
    protected final EdgeVar edgeVar;
    protected final boolean exists;

    public EdgeClause(WhereClause parent, EdgeVar edgeVar, boolean exists) {
        this.parent = parent;
        this.edgeVar = edgeVar;
        this.exists = exists;
    }

    public WhereClause to(NodeVar head) {
        parent.parent.select(head);

        for (Var tail : parent.vars) {
            if(!(tail instanceof NodeVar)) {
                throw new QueryException("var in where clause is not a NodeVar: " + tail.toString());
            }

            if(exists)
                parent.parent.predicates.add(new EdgeExistsPredicate(parent.parent.doc, (NodeVar)tail, head, edgeVar));
            else
                parent.parent.predicates.add(new EdgeNotExistsPredicate(parent.parent.doc, (NodeVar)tail, head, edgeVar));
        }

        return parent;
    }

    public WhereClause from(NodeVar tail) {
        parent.parent.select(tail);

        for (Var head : parent.vars) {
            if(!(head instanceof NodeVar)) {
                throw new QueryException("var in where clause is not a NodeVar: " + head.toString());
            }

            if(exists)
                parent.parent.predicates.add(new EdgeExistsPredicate(parent.parent.doc, tail, (NodeVar)head, edgeVar));
            else
                parent.parent.predicates.add(new EdgeNotExistsPredicate(parent.parent.doc, tail, (NodeVar)head, edgeVar));
        }

        return parent;
    }

    public WhereClause fromTo(final NodeVar headOrTail) {
        final LayerRef layerRef = this.parent.root().doc.engine().nodeLayer(headOrTail.getLayer(), headOrTail.getVariant());

        parent.where(edgeVar, in -> layerRef.equal(in.getHead().getRef().layer()) || layerRef.equal(in.getTail().getRef().layer()));

        return parent;
    }
}

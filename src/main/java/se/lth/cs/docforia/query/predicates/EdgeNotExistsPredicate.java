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

import se.lth.cs.docforia.query.EdgeVar;
import se.lth.cs.docforia.query.NodeVar;
import se.lth.cs.docforia.query.Proposition;
import se.lth.cs.docforia.query.QueryContext;

/**
 * Inverted edge exist predicate
 */
public class EdgeNotExistsPredicate extends EdgeExistsPredicate {

    public EdgeNotExistsPredicate(QueryContext context, NodeVar tail, NodeVar head, EdgeVar edgeVar) {
        super(context, tail, head, edgeVar);
    }

    @Override
    public boolean eval(Proposition proposition) {
        return !super.eval(proposition);
    }
}

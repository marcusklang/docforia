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

import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.data.DataRef;
import se.lth.cs.docforia.query.Predicate;
import se.lth.cs.docforia.query.Proposition;
import se.lth.cs.docforia.query.Var;

import java.util.function.Function;
/**
 * General property predicate
 */
public class PropertyPredicate extends Predicate {
    private final String property;
    private final Function<DataRef,Boolean> pred;

    public PropertyPredicate(Document doc, Var var, String property, Function<DataRef,Boolean> pred) {
        super(doc, var);
        this.property = property;
        this.pred = pred;
    }

    @Override
    public boolean eval(Proposition proposition) {
        return proposition.get(vars[0]).hasProperty(property) && pred.apply(proposition.get(vars[0]).getRefProperty(property));
    }
}

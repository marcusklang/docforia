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

import se.lth.cs.docforia.query.Predicate;
import se.lth.cs.docforia.query.Proposition;
import se.lth.cs.docforia.query.QueryContext;
import se.lth.cs.docforia.query.Var;

/**
 * Property equals predicate
 */
public class PropertyEqualsPredicate  extends Predicate {
    private final String property;
    private final String value;

    public PropertyEqualsPredicate(QueryContext context, Var var, String property, String value) {
        super(context, var);
        this.property = property;
        this.value = value;
    }

    @Override
    public boolean eval(Proposition proposition) {
        return value.equals(proposition.get(vars[0]).getProperty(property));
    }
}

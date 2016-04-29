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

import se.lth.cs.docforia.data.DataRef;
import se.lth.cs.docforia.query.Var;
import se.lth.cs.docforia.query.predicates.*;

import java.util.function.Function;

/**
 * Property Clause for property based queries
 */
public class PropertyClause {
    protected final WhereClause parent;
    protected final String property;

    public PropertyClause(WhereClause parent, String property) {
        this.parent = parent;
        this.property = property;
    }

    public WhereClause exists() {
        for (Var var : parent.vars) {
            parent.parent.predicates.add(new PropertyExistsPredicate(parent.parent.doc, var, property));
        }

        return parent;
    }

    public WhereClause notExists() {
        for (Var var : parent.vars) {
            parent.parent.predicates.add(new PropertyNotExistsPredicate(parent.parent.doc, var, property));
        }

        return parent;
    }

    public WhereClause equals(String value) {
        for (Var var : parent.vars) {
            parent.parent.predicates.add(new PropertyEqualsPredicate(parent.parent.doc, var, property, value));
        }

        return parent;
    }

    public WhereClause notEquals(String value) {
        for (Var var : parent.vars) {
            parent.parent.predicates.add(new PropertyNotEqualsPredicate(parent.parent.doc, var, property, value));
        }

        return parent;
    }

    public WhereClause equalsAny(String...values) {
        for (Var var : parent.vars) {
            parent.parent.predicates.add(new PropertyEqualsAnyPredicate(parent.parent.doc, var, property, values));
        }

        return parent;
    }

    public WhereClause predicate(Function<DataRef,Boolean> pred) {
        for (Var var : parent.vars) {
            parent.parent.predicates.add(new PropertyPredicate(parent.parent.doc, var, property, pred));
        }

        return parent;
    }
}

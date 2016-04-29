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
import se.lth.cs.docforia.PropertyContainer;
import se.lth.cs.docforia.query.Predicate;
import se.lth.cs.docforia.query.Proposition;
import se.lth.cs.docforia.query.Var;

/**
 * Property equals any in list of values predicate
 */
public class PropertyEqualsAnyPredicate extends Predicate {
    private final String[] values;
    private final String key;

    public PropertyEqualsAnyPredicate(Document doc, Var candidate, String key, String[] values) {
        super(doc, candidate);
        this.key = key;
        this.values = values;
    }

    @Override
    public boolean eval(Proposition proposition) {
        PropertyContainer propertyContainer = proposition.get(vars[0]);
        if(!propertyContainer.hasProperty(key))
            return false;

        String source = propertyContainer.getProperty(key);

        for (String target : values) {
            if(target.equals(source))
                return true;
        }

        return false;
    }
}

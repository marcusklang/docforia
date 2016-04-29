package se.lth.cs.docforia.graph.text;
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

import se.lth.cs.docforia.DocumentProxy;
import se.lth.cs.docforia.Edge;
import se.lth.cs.docforia.EdgeRef;
import se.lth.cs.docforia.query.EdgeTVar;

/**
 * Semantic Role Edge, used to define relationships between Predicate and Arguments
 */
public class SemanticRole extends Edge<SemanticRole> {
    public static final String ROLE_PROPERTY = "role";

    public SemanticRole() {
        super();
    }

    public SemanticRole(DocumentProxy doc) {
        super(doc);
    }

    public SemanticRole(DocumentProxy doc, EdgeRef edge) {
        super(doc, edge);
    }

    public SemanticRole setRole(String role) {
        putProperty(ROLE_PROPERTY, role);
        return this;
    }

    public String getRole() {
        return getProperty(ROLE_PROPERTY);
    }

    public static EdgeTVar<SemanticRole> var() {
        return new EdgeTVar<SemanticRole>(SemanticRole.class);
    }

    public static EdgeTVar<SemanticRole> var(String variant) {
        return new EdgeTVar<SemanticRole>(SemanticRole.class, variant);
    }
}

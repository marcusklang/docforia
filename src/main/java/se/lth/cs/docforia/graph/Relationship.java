package se.lth.cs.docforia.graph;
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
 * Generic relationship edge
 */
public class Relationship extends Edge<Relationship> {
    public Relationship() {
        super();
    }

    public Relationship(DocumentProxy doc) {
        super(doc);
    }

    public Relationship(DocumentProxy doc, EdgeRef edge) {
        super(doc, edge);
    }

    public static EdgeTVar<Relationship> var() {
        return new EdgeTVar<Relationship>(Relationship.class);
    }

    public static EdgeTVar<Relationship> var(String variant) {
        return new EdgeTVar<Relationship>(Relationship.class, variant);
    }
}

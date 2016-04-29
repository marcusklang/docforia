package se.lth.cs.docforia.graph.ast;
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
import se.lth.cs.docforia.query.EdgeTVar;

public class AstEdge extends Edge<AstEdge> {
    /** Syntax type property key */
    public static final String PROPERTY_TYPE = "type";

	public AstEdge() {
		super();
	}

	public AstEdge(DocumentProxy doc) {
		super(doc);
	}

	public AstEdge setType(String type) {
		return putProperty(PROPERTY_TYPE, type);
	}

	public String getType() {
		return getProperty(PROPERTY_TYPE);
	}

	public static EdgeTVar<AstEdge> var() {
		return new EdgeTVar<AstEdge>(AstEdge.class);
	}

	public static EdgeTVar<AstEdge> var(String variant) {
		return new EdgeTVar<AstEdge>(AstEdge.class, variant);
	}
}

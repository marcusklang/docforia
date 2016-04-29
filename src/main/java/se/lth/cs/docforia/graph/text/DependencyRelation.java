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
 * Dependency tree relation
 */
public class DependencyRelation extends Edge<DependencyRelation> {

	public static final String RELATION_PROPERTY = "relation";

	public DependencyRelation(DocumentProxy doc) {
		super(doc);
	}

	public DependencyRelation() {
		super();
	}

	public DependencyRelation(DocumentProxy doc, EdgeRef ref) {
		super(doc, ref);
	}

	public DependencyRelation setRelation(String relation) {
		putProperty(RELATION_PROPERTY, relation);
		return this;
	}

	public String getRelation() {
		return getProperty(RELATION_PROPERTY);
	}

	public static EdgeTVar<DependencyRelation> var() {
		return new EdgeTVar<DependencyRelation>(DependencyRelation.class);
	}

	public static EdgeTVar<DependencyRelation> var(String variant) {
		return new EdgeTVar<DependencyRelation>(DependencyRelation.class, variant);
	}
}

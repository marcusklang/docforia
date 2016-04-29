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
import se.lth.cs.docforia.Node;
import se.lth.cs.docforia.query.NodeTVar;

/** Named entity annotation */
public class NamedEntity extends Node<NamedEntity> {

	public static final String PROPERTY_LABEL = "label";

	public NamedEntity() {
		super();
	}

	public NamedEntity(DocumentProxy doc) {
		super(doc);
	}

	public String getLabel() {
		return getProperty(PROPERTY_LABEL);
	}

	public boolean hasLabel() {
		return getProperty(PROPERTY_LABEL) != null;
	}

	public NamedEntity setLabel(String label) {
		putProperty(PROPERTY_LABEL, label);
		return this;
	}


	public static NodeTVar<NamedEntity> var() {
		return new NodeTVar<NamedEntity>(NamedEntity.class);
	}

	public static NodeTVar<NamedEntity> var(String variant) {
		return new NodeTVar<NamedEntity>(NamedEntity.class, variant);
	}
}

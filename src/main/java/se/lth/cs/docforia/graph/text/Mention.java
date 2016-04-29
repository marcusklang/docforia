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

/** Mention annotation */
public class Mention extends Node<Mention> {

	public Mention() {
		super();
	}

	public Mention(DocumentProxy doc) {
		super(doc);
	}

	public static NodeTVar<Mention> var() {
		return new NodeTVar<Mention>(Mention.class);
	}

	public static NodeTVar<Mention> var(String variant) {
		return new NodeTVar<Mention>(Mention.class, variant);
	}
}

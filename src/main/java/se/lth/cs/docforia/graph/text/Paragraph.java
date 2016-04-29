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

/** Paragraph annotation */
public class Paragraph extends Node<Paragraph> {

	public final static String HEADER_PROPERTY = "header";

	public Paragraph() {
		super();
	}

	public Paragraph(DocumentProxy doc) {
		super(doc);
	}

	public Paragraph setHeader(String header) {
		putProperty(HEADER_PROPERTY, header);
		return this;
	}

	public String getHeader() {
		return getProperty(HEADER_PROPERTY);
	}

	public static NodeTVar<Paragraph> var() {
		return new NodeTVar<Paragraph>(Paragraph.class);
	}

	public static NodeTVar<Paragraph> var(String variant) {
		return new NodeTVar<Paragraph>(Paragraph.class, variant);
	}
}

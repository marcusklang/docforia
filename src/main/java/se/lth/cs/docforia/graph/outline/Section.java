package se.lth.cs.docforia.graph.outline;
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

/**
 * Section annotation, e.g. annotation of a collection of paragraphs
 */
public class Section extends Node<Section> {

    /** Title property key */
	public static final String TITLE_PROPERTY = "title";
	
	public Section() {
		super();
	}

	public Section(DocumentProxy doc) {
		super(doc);
	}

	public Section setTitle(String title) {
		putProperty(TITLE_PROPERTY, title);
		return this;
	}

	public String getTitle() {
		return getProperty(TITLE_PROPERTY);
	}

	public static NodeTVar<Section> var() {
		return new NodeTVar<Section>(Section.class);
	}

	public static NodeTVar<Section> var(String variant) {
		return new NodeTVar<Section>(Section.class, variant);
	}
	
}

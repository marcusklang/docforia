package se.lth.cs.docforia;
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

import se.lth.cs.docforia.query.NodeVar;

/**
 * Dynamic node layer representation, or fallback for unknown classes.
 */
public class DynamicNode extends Node {

	public DynamicNode() {
		super();
	}

	public DynamicNode(DocumentProxy doc, String layer) {
		super();
		doc.add(this, layer);
	}

	public static NodeVar var(String type) {
		return new NodeVar("@" + type);
	}

	public static NodeVar var(String type, String variant) {
		return new NodeVar("@" + type, variant);
	}

	@Override
	public boolean hasDynamicLayer() {
		return true;
	}
}

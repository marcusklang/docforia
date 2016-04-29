package se.lth.cs.docforia.graph.hypertext;
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
 * Hypertext anchor representation
 */
public class Anchor extends Node<Anchor> {
	/** Unresolved target property key, in wikipedia this is the wikimarkup page link */
	public static final String TARGET_PROPERTY = "target";

	/** Type of anchor property key, internal, external, or custom */
	public static final String TYPE_PROPERTY = "type";

	/** Resolved entity property key, e.g. resolving wikipedia pages to wikidata */
    public static final String ENTITY_PROPERTY = "entity";

	/** Internal type */
	public static final String INTERNAL_TYPE = "internal";

	/** External type */
	public static final String EXTERNAL_TYPE = "external";

    /** Create a uninitialized representation */
	public Anchor() {
		super();
	}

    /** Create and attach representation to a document */
	public Anchor(DocumentProxy doc) {
		super(doc);
	}

    /** Checks if target startsWith # to indicate relative local link in hrefs*/
	public boolean isLocal() {
        return getTarget().startsWith("#");
    }

	/** Get raw target */
	public String getTarget() {
		return getProperty(TARGET_PROPERTY);
	}

	/** Get resolved entity */
    public String getEntity() {
        return getProperty(ENTITY_PROPERTY);
    }

	/** Set resolved entity */
    public Anchor setEntity(String entity) {
        putProperty(ENTITY_PROPERTY, entity);
        return this;
    }

	/** Check if entity has been resolved */
    public boolean isEntity() {
        return hasProperty(ENTITY_PROPERTY);
    }

	/** Get the type of link, internal, external or custom, null if no type has been specified */
	public String getType() {
		return getProperty(TYPE_PROPERTY);
	}

	/** Set the type of link */
	public Anchor setType(String type) {
		putProperty(TYPE_PROPERTY, type);
		return this;
	}

	/** Set the target of the anchor */
	public Anchor setTarget(String target) {
		putProperty(TARGET_PROPERTY, target);
		return this;
	}

	/** Create a typed query variable */
	public static NodeTVar<Anchor> var() {
		return new NodeTVar<Anchor>(Anchor.class);
	}

	/** Create a typed query variable for a variant */
	public static NodeTVar<Anchor> var(String variant) {
		return new NodeTVar<Anchor>(Anchor.class, variant);
	}
}

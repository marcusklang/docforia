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
 * List section, e.g. unordered, ordered, definition lists
 */
public class ListSection extends Node<ListSection> {
    public static final String PROPERTY_TYPE = "type";
    public static final String NOT_DEFINED_TYPE = "na";
    public static final String ORDERED_TYPE = "ol";
    public static final String UNORDERED_TYPE = "ul";

    public ListSection(DocumentProxy doc) {
        super(doc);
    }

    public ListSection() {
        super();
    }

    public String getType() {
        String type = getProperty(PROPERTY_TYPE);
        return type == null ? NOT_DEFINED_TYPE : type;
    }

    public ListSection setType(String type) {
        putProperty(PROPERTY_TYPE, type);
        return this;
    }

    public static NodeTVar<ListSection> var() {
        return new NodeTVar<ListSection>(ListSection.class);
    }

    public static NodeTVar<ListSection> var(String variant) {
        return new NodeTVar<ListSection>(ListSection.class, variant);
    }
}

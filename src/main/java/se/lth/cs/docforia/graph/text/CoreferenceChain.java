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

/**
 * Coreference Chain node
 */
public class CoreferenceChain extends Node<CoreferenceChain> {
    public CoreferenceChain() {
        super();
    }

    public CoreferenceChain(DocumentProxy doc) {
        super(doc);
    }

    /** id property key */
    public static final String PROPERTY_ID = "id";

    public CoreferenceChain setId(int id) {
        putProperty(PROPERTY_ID, String.valueOf(id));
        return this;
    }

    public CoreferenceChain setId(String id) {
        putProperty(PROPERTY_ID, id);
        return this;
    }

    public int getIntId() {
        return getIntProperty(PROPERTY_ID);
    }

    public String getId() {
        return getProperty(PROPERTY_ID);
    }

    public static NodeTVar<CoreferenceChain> var() {
        return new NodeTVar<CoreferenceChain>(CoreferenceChain.class);
    }

    public static NodeTVar<CoreferenceChain> var(String variant) {
        return new NodeTVar<CoreferenceChain>(CoreferenceChain.class, variant);
    }
}

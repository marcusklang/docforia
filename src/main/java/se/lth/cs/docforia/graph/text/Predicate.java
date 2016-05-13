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
 * Predicate annotation
 */
public class Predicate extends Node<Predicate> {
    public static final String PROPERTY_SENSE = "sense";
    public static final String PROPERTY_PREDCATE = "pred";
    public static final String PROPERTY_FRAMESET = "frameset";

    public Predicate() {
        super();
    }

    public Predicate(DocumentProxy doc) {
        super(doc);
    }

    public Predicate setSense(String sense) {
        putProperty(PROPERTY_SENSE, sense);
        return this;
    }

    public String getSense() {
        return getProperty(PROPERTY_SENSE);
    }

    public boolean hasSense() {
        return hasProperty(PROPERTY_SENSE);
    }

    public Predicate setPredicate(String predicate) {
        putProperty(PROPERTY_PREDCATE, predicate);
        return this;
    }

    public Predicate setFrameset(String frameset) {
        putProperty(PROPERTY_FRAMESET, frameset);
        return this;
    }

    public String getFrameset() {
        return getProperty(PROPERTY_FRAMESET);
    }

    public String getPredicate() {
        return getProperty(PROPERTY_PREDCATE);
    }

    public static NodeTVar<Predicate> var() {
        return new NodeTVar<Predicate>(Predicate.class);
    }

    public static NodeTVar<Predicate> var(String variant) {
        return new NodeTVar<Predicate>(Predicate.class, variant);
    }
}

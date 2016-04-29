package se.lth.cs.docforia.graph.disambig;
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
 * Named Entity Disambiguation
 */
public class NamedEntityDisambiguation extends Node<NamedEntityDisambiguation> {
    /** Identifier property key */
    public static final String IDENTIFIER_PROPERTY = "id";

    /** Score property key */
    public static final String SCORE_PROPERTY = "score";

    public NamedEntityDisambiguation() {
        super();
    }

    public NamedEntityDisambiguation(DocumentProxy doc) {
        super(doc);
    }

    /** Get entity disambiguation score, 0.0 if no score */
    public double getScore() {
        return getDoubleProperty(SCORE_PROPERTY);
    }

    /** Set score for entity disambiguation */
    public NamedEntityDisambiguation setScore(double score) {
        putProperty(SCORE_PROPERTY, score);
        return this;
    }

    /** Get disambiguated identifier */
    public String getIdentifier() {
        return getProperty(IDENTIFIER_PROPERTY);
    }

    /** Set identifier */
    public NamedEntityDisambiguation setIdentifier(String identifier) {
        putProperty(IDENTIFIER_PROPERTY, identifier);
        return this;
    }

    public static NodeTVar<NamedEntityDisambiguation> var() {
        return new NodeTVar<NamedEntityDisambiguation>(NamedEntityDisambiguation.class);
    }

    public static NodeTVar<NamedEntityDisambiguation> var(String variant) {
        return new NodeTVar<NamedEntityDisambiguation>(NamedEntityDisambiguation.class, variant);
    }
}

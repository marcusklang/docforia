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
 * Word Sense Disambiguation
 */
public class WordSenseDisambiguation extends Node<WordSenseDisambiguation> {
    /** Score property key */
    public static final String SCORE_PROPERTY = "score";

    /** Sense property key */
    public static final String SENSE_PROPERTY = "sense";

    public WordSenseDisambiguation() {

    }

    public WordSenseDisambiguation(DocumentProxy doc) {
        super(doc);
    }

    /** Get score */
    public double getScore() {
        return getDoubleProperty(SCORE_PROPERTY);
    }

    /** Set score */
    public WordSenseDisambiguation setScore(double score) {
        putProperty(SCORE_PROPERTY, score);
        return this;
    }

    /** Get sense */
    public String getSense() {
        return getProperty(SENSE_PROPERTY);
    }

    /** Set sense */
    public WordSenseDisambiguation setSense(String sense) {
        putProperty(SENSE_PROPERTY, sense);
        return this;
    }

    public static NodeTVar<WordSenseDisambiguation> var() {
        return new NodeTVar<WordSenseDisambiguation>(WordSenseDisambiguation.class);
    }

    public static NodeTVar<WordSenseDisambiguation> var(String variant) {
        return new NodeTVar<WordSenseDisambiguation>(WordSenseDisambiguation.class, variant);
    }
}

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
import se.lth.cs.docforia.graph.TokenProperties;
import se.lth.cs.docforia.query.NodeTVar;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Token annotation */
public class Token extends Node<Token> {

    public Token() {
		super();
	}

	public Token(DocumentProxy doc) {
		super(doc);
	}

    public Token setPartOfSpeech(String postag) {
        putProperty(TokenProperties.POS, postag);
        return this;
    }

    public Token setPredictedPartOfSpeech(String ppostag) {
        putProperty(TokenProperties.PPOS, ppostag);
        return this;
    }

    public Token setCoarsePartOfSpeech(String postag) {
        putProperty(TokenProperties.CPOSTAG, postag);
        return this;
    }

    public Token setLemma(String lemma) {
        putProperty(TokenProperties.LEMMA, lemma);
        return this;
    }

    public Token setPredictedLemma(String plemma) {
        putProperty(TokenProperties.PLEMMA, plemma);
        return this;
    }

    /** Set features, built in convention that "|" is a seperator for features */
    public Token setFeatures(String features) {
        putProperty(TokenProperties.FEATS, features);
        return this;
    }

    /** Set features, "|" will be used as a seperator */
    public Token setFeatures(String...features) {
        putProperty(TokenProperties.FEATS, Arrays.stream(features).collect(Collectors.joining("|")));
        return this;
    }

    /** Get part of speech, will fallback to coarse part-of-speech tag if part-of-speech does not exist */
    public String getPartOfSpeech() {
        String pos = getProperty(TokenProperties.POS);
        return pos == null ? getCoarsePartOfSpeech() : pos;
    }

    /** Get predicted part of speech */
    public String getPredictedPartOfSpeech() {
        return getProperty(TokenProperties.PPOS);
    }

    public boolean hasPartOfSpeech() {
        return hasProperty(TokenProperties.POS);
    }

    public String getCoarsePartOfSpeech() {
        return getProperty(TokenProperties.CPOSTAG);
    }

    /** Get lemma if exists otherwise fallback to text */
    public String getLemma() {
        String lemma = getProperty(TokenProperties.LEMMA);
        return lemma == null ? text() : lemma;
    }

    /** Get lemma if exists otherwise fallback to text */
    public String getPredictedLemma() {
        String lemma = getProperty(TokenProperties.PLEMMA);
        return lemma == null ? text() : lemma;
    }

    public boolean hasLemma() {
        return hasProperty(TokenProperties.LEMMA);
    }

    public boolean hasPredictedLemma() {
        return hasProperty(TokenProperties.PLEMMA);
    }

    /** Get features as is */
    public String getFeatures() {
        return getProperty(TokenProperties.FEATS);
    }

    /** Get feature array split by "|" */
    public String[] getFeatureArray() {
        String features = getFeatures();
        return features == null ? null : features.split("|");
    }

	public static NodeTVar<Token> var() {
		return new NodeTVar<Token>(Token.class);
	}

	public static NodeTVar<Token> var(String variant) {
		return new NodeTVar<Token>(Token.class, variant);
	}
	
}

package se.lth.cs.docforia.io.text;
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

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.NodeStore;
import se.lth.cs.docforia.graph.text.Sentence;

import java.util.ArrayList;

/**
 * Text sentence containing tokens
 */
public class TextSentence extends ArrayList<TextToken> {
    private NodeRef sentence;
    private Document document;
    private int numCols;
    private Reference2IntOpenHashMap<NodeRef> tokenIndex = new Reference2IntOpenHashMap<>();

    public TextSentence(Document document) {
        this.document = document;
        tokenIndex.defaultReturnValue(-1);
    }

    public TextSentence(Document document, NodeRef sentence) {
        this.document = document;
        this.sentence = sentence;
        tokenIndex.defaultReturnValue(-1);
    }

    public TextSentence(Document document, int initialCapacity, NodeRef sentence) {
        super(initialCapacity);
        this.document = document;
        this.sentence = sentence;
        tokenIndex.defaultReturnValue(-1);
    }

    public TextToken add() {
        TextToken token = new TextToken(this);
        add(token);
        return token;
    }

    public TextToken add(NodeRef ref) {
        TextToken token = new TextToken(this);
        token.setToken(ref);
        tokenIndex.put(ref, size());
        add(token);
        return token;
    }

    public int index(NodeRef ref) {
        return tokenIndex.getInt(ref);
    }

    public void setMinCols(int numCols) {
        this.numCols = Math.max(this.numCols, numCols);
    }

    public int getMinNumCols() {
        return numCols;
    }

    public Document getDocument() {
        return document;
    }

    public NodeStore sentence() {
        return sentence.get();
    }

    public Sentence representation() {
        return (Sentence)document.representation(sentence);
    }

    public void setSentence(NodeRef sentence) {
        this.sentence = sentence;
    }
}

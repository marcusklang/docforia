package se.lth.cs.docforia.io.text.columns;
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

import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.DocumentNodeLayer;
import se.lth.cs.docforia.Node;
import se.lth.cs.docforia.NodeStore;
import se.lth.cs.docforia.graph.text.Token;
import se.lth.cs.docforia.io.text.ColumnReader;
import se.lth.cs.docforia.io.text.ColumnWriter;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;
import se.lth.cs.docforia.query.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes spans properties for a given layer
 */
public class SpanColumnRW implements ColumnReader, ColumnWriter {
    private String nodeLayer;
    private String labelProperty;
    private int offset;
    private boolean init = false;
    private String initText = null;

    /**
     * Static Layer full constructor
     * @param nodeLayer     the node layer
     * @param labelProperty the property to read label into, or null if unlabelled
     * @param offset        offset position (or -1 if add to end, writing mode only)
     * @param initText      the string that all values should be initialized to
     * @param <T>           Node type
     */
    public <T extends Node> SpanColumnRW(Class<T> nodeLayer, String labelProperty, String initText, int offset) {
        this.nodeLayer = Document.nodeLayer(nodeLayer);
        this.labelProperty = labelProperty;
        this.offset = offset;
        this.init = true;
        this.initText = initText;
    }

    /**
     * Static Layer full constructor with no initialization
     * @param nodeLayer     the node layer
     * @param labelProperty the property to read label into, or null if unlabelled
     * @param offset        offset position (or -1 if add to end, writing mode only)
     * @param <T>           Node type
     */
    public <T extends Node> SpanColumnRW(Class<T> nodeLayer, String labelProperty, int offset) {
        this.nodeLayer = Document.nodeLayer(nodeLayer);
        this.labelProperty = labelProperty;
        this.offset = offset;
    }

    /**
     * Dynamic layer full constructor
     * @param nodeLayer     the dynamic node layer
     * @param labelProperty the property to read label into, or null if unlabelled
     * @param offset        offset position (or -1 if add to end, writing mode only)
     * @param initText      the string that all values should be initialized to
     */
    public SpanColumnRW(String nodeLayer, String labelProperty, String initText, int offset) {
        this.nodeLayer = Document.nodeLayer(nodeLayer);
        this.labelProperty = labelProperty;
        this.offset = offset;
    }

    /**
     * Dynamic layer full constructor with no initialization
     * @param nodeLayer     the dynamic node layer
     * @param labelProperty the property to read label into, or null if unlabelled
     * @param offset        offset position (or -1 if add to end, writing mode only)
     */
    public SpanColumnRW(String nodeLayer, String labelProperty, int offset) {
        this.nodeLayer = Document.nodeLayer(nodeLayer);
        this.labelProperty = labelProperty;
        this.offset = offset;
    }

    /**
     * Layer unlabelled constructor
     * @param nodeLayer     the node layer
     * @param offset        offset position (or -1 if add to end, writing mode only)
     */
    public <T extends Node> SpanColumnRW(Class<T> nodeLayer, int offset) {
        this.nodeLayer = Document.nodeLayer(nodeLayer);
        this.offset = offset;
    }

    /**
     * Dynamic layer unlabelled constructor
     * @param nodeLayer     the node layer
     * @param offset        offset position (or -1 if add to end, writing mode only)
     */
    public SpanColumnRW(String nodeLayer, int offset) {
        this.nodeLayer = Document.nodeLayer(nodeLayer);
        this.offset = offset;
    }

    @Override
    public void load(Document doc, List<TextSentence> sentences) {
        DocumentNodeLayer layer = doc.store().nodeLayer(nodeLayer, null);

        for (TextSentence sentence : sentences) {
            int lastStart = -1;
            String lastSpanName = "";
            for (int i = 0; i < sentence.size(); i++) {
                TextToken token = sentence.get(i);

                String s = token.getProperty(offset);
                if(s != null && !s.equals("*")) {
                    if (s.startsWith("(")){
                        if (s.endsWith("*")) {
                            //starts here
                            lastStart = i;
                            lastSpanName = s.substring(1,s.length()-1);
                        } else if (s.endsWith(")")) {
                            NodeStore nodeStore = layer.create(token.getStart(), token.getEnd()).get();
                            if(labelProperty != null) {
                                nodeStore.putProperty(labelProperty, s.substring(1,s.length()-1));
                            }
                        }
                        else {
                            lastStart = i;
                            lastSpanName = s.substring(1,s.length());
                        }
                    } else if (s.endsWith(")")) {
                        NodeStore nodeStore = layer.create(sentence.get(lastStart).getStart(), token.getEnd()).get();
                        if(labelProperty != null) {
                            nodeStore.putProperty(labelProperty, lastSpanName);
                            lastStart = -1;
                            lastSpanName = null;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void save(Document document, ArrayList<TextSentence> sentences) {
        for (TextSentence sentence : sentences) {
            int realoffset = offset == -1 ? sentence.getMinNumCols() : offset;

            if(init) {
                for (TextToken token : sentence) {
                    token.setProperty(realoffset, initText);
                }
            }

            NodeVar L = new NodeVar(nodeLayer);
            NodeTVar<Token> T = Token.var();

            NodeStore sent = sentence.sentence();

            List<PropositionGroup> query = document.select(L, T)
                                                   .where(L).coveredBy(sent.getStart(), sent.getEnd())
                                                   .where(T).coveredBy(L)
                                                   .stream()
                                                   .collect(QueryCollectors.groupBy(document, L).orderByValue(T).collector());

            for (PropositionGroup group : query) {
                NodeStore nodeKeyStore = group.key().noderef(L).get();

                List<Proposition> tokens = group.values();
                int start = sentence.index(tokens.get(0).noderef(T));
                int end = sentence.index(tokens.get(tokens.size()-1).noderef(T));

                TextToken startToken = sentence.get(start);

                String label = labelProperty != null ? nodeKeyStore.getProperty(labelProperty) : "";
                if(label == null)
                    label = "";

                startToken.setProperty(realoffset, "(" + label + startToken.getPropertyOrEmpty(realoffset));

                TextToken endToken = sentence.get(end);

                endToken.setProperty(realoffset, endToken.getPropertyOrEmpty(realoffset) + ")");
            }

            sentence.setMinCols(realoffset+1);
        }
    }
}

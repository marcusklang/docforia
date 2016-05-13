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
import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.NodeStore;
import se.lth.cs.docforia.graph.text.CoreferenceChain;
import se.lth.cs.docforia.graph.text.CoreferenceChainEdge;
import se.lth.cs.docforia.graph.text.CoreferenceMention;
import se.lth.cs.docforia.graph.text.Token;
import se.lth.cs.docforia.io.text.ColumnWriter;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;
import se.lth.cs.docforia.query.EdgeTVar;
import se.lth.cs.docforia.query.NodeTVar;
import se.lth.cs.docforia.query.PropositionGroup;
import se.lth.cs.docforia.query.QueryCollectors;

import java.util.ArrayList;
import java.util.List;

/**
 * Coreference property writer
 *
 * This implementation reads CoreferenceChain nodes, CoreferenceMention nodes and CoreferenceChainEdge edges
 */
public class CoreferenceWriter implements ColumnWriter {
    private int offset;

    /**
     * Append to end constructor
     */
    public CoreferenceWriter() {
        this(-1);
    }

    /**
     * Full constructor
     * @param offset the offset or -1 to append to end
     */
    public CoreferenceWriter(int offset) {
        this.offset = offset;
    }

    @Override
    public void save(Document document, ArrayList<TextSentence> sentences) {
        NodeTVar<CoreferenceMention> CM = CoreferenceMention.var();
        EdgeTVar<CoreferenceChainEdge> CCE = CoreferenceChainEdge.var();
        NodeTVar<CoreferenceChain> CC = CoreferenceChain.var();
        NodeTVar<Token> T = Token.var();

        for (TextSentence sentence : sentences) {
            int realOffset = offset == -1 ? sentence.getMinNumCols() : offset;
            NodeStore sent = sentence.sentence();

            List<PropositionGroup> query
                    = document.select(CM, CCE, CC, T)
                              .where(CM).coveredBy(sent.getStart(), sent.getEnd())
                              .where(CM).hasEdge(CCE).to(CC)
                              .where(T).coveredBy(CM)
                              .stream()
                              .collect(QueryCollectors.groupBy(document, CC, CM)
                                                      .values(T)
                                                      .orderByValue(T)
                                                      .collector());

            for (PropositionGroup group : query) {
                NodeRef chain = group.key().noderef(CC);
                NodeRef firstToken = group.values().get(0).noderef(T);
                NodeRef lastToken = group.values().get(group.values().size()-1).noderef(T);

                int startIndex = sentence.index(firstToken);
                int endIndex = sentence.index(lastToken);

                String id = chain.get().getProperty(CoreferenceChain.PROPERTY_ID);
                if(startIndex == endIndex) {
                    TextToken token = sentence.get(startIndex);
                    String text = token.getProperty(realOffset);
                    if(text != null) {
                        token.setProperty(realOffset, text + "|(" + id + ")");
                    } else {
                        token.setProperty(realOffset, "(" + id + ")");
                    }
                } else {
                    {
                        TextToken startToken = sentence.get(startIndex);
                        String startText = startToken.getProperty(realOffset);
                        if (startText != null) {
                            startToken.setProperty(realOffset, "(" + id + "|" + startText);
                        } else {
                            startToken.setProperty(realOffset, "(" + id);
                        }
                    }
                    {
                        TextToken endToken = sentence.get(endIndex);
                        String endText = endToken.getProperty(realOffset);
                        if (endText != null) {
                            endToken.setProperty(realOffset, endText + "|" + id + ")");
                        } else {
                            endToken.setProperty(realOffset, id + ")");
                        }
                    }
                }
            }

            sentence.setMinCols(realOffset+1);
        }
    }
}

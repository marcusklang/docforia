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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.DocumentEdgeLayer;
import se.lth.cs.docforia.DocumentNodeLayer;
import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.graph.text.CoreferenceChain;
import se.lth.cs.docforia.graph.text.CoreferenceChainEdge;
import se.lth.cs.docforia.graph.text.CoreferenceMention;
import se.lth.cs.docforia.io.text.ColumnReader;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Coreference reader, supports overlapped mentions
 * <p>
 * This implementation creates CoreferenceChain nodes, CoreferenceMention nodes and CoreferenceChainEdge edges
 */
public class CoreferenceReader implements ColumnReader {
    private boolean suffixMode;
    private int col;

    private Pattern pattern = Pattern.compile("\\(([^\\|\\(\\)]+)\\)|\\(([^\\|\\(\\)]+)|([^\\|\\(\\)]+)\\)", Pattern.CASE_INSENSITIVE);

    /**
     * Standard constructor, suffixmod = false
     * @param col column
     */
    public CoreferenceReader(int col) {
        this.col = col;
        this.suffixMode = false;
    }

    /**
     * Full constructor
     * @param suffixMode true if col is relative to end
     * @param col column
     */
    public CoreferenceReader(boolean suffixMode, int col) {
        this.suffixMode = suffixMode;
        this.col = col;
    }

    @Override
    public void load(Document doc, List<TextSentence> sentences) {
        Object2ObjectOpenHashMap<String, IntArrayList> startMap = new Object2ObjectOpenHashMap<>();
        Object2ObjectOpenHashMap<String, NodeRef> chains = new Object2ObjectOpenHashMap<>();

        DocumentEdgeLayer corefChainEdge = doc.store().edgeLayer(Document.edgeLayer(CoreferenceChainEdge.class), null);
        DocumentNodeLayer corefMentionLayer = doc.store().nodeLayer(Document.nodeLayer(CoreferenceMention.class), null);
        DocumentNodeLayer corefChainLayer = doc.store().nodeLayer(Document.nodeLayer(CoreferenceChain.class), null);

        for (TextSentence sentence : sentences) {
            for (int i = 0; i < sentence.size(); i++) {
                TextToken token = sentence.get(i);
                String s;
                if(suffixMode)
                    s = token.getProperty(token.size()-col-1);
                else
                    s = token.getProperty(col);

                if(s != null) {
                    Matcher matcher = pattern.matcher(s);
                    while(matcher.find()) {
                        String id;
                        if((id = matcher.group(1)) != null) { //Single token coreference
                            NodeRef corefMention = corefMentionLayer.create(token.getStart(), token.getEnd());

                            NodeRef chain = chains.get(id);
                            if(chain == null) {
                                chain = corefChainLayer.create();
                                chain.get().putProperty(CoreferenceChain.PROPERTY_ID, id);
                                chains.put(id, chain);
                            }

                            corefChainEdge.create(corefMention, chain);

                        } else if((id = matcher.group(2)) != null) { //Start
                            IntArrayList list = startMap.get(id);
                            if(list == null) {
                                list = new IntArrayList();
                                startMap.put(id, list);
                            }

                            list.push(i);
                        } else if((id = matcher.group(3)) != null) { //End
                            IntArrayList list = startMap.get(id);
                            int start = list.popInt();
                            int startPos = sentence.get(start).getStart();
                            int endPos = token.getEnd();

                            NodeRef corefMention = corefMentionLayer.create(startPos, endPos);

                            NodeRef chain = chains.get(id);
                            if(chain == null) {
                                chain = corefChainLayer.create();
                                chain.get().putProperty(CoreferenceChain.PROPERTY_ID, id);
                                chains.put(id, chain);
                            }

                            corefChainEdge.create(corefMention, chain);
                        }
                    }
                }
            }
            startMap.clear();
        }
    }
}

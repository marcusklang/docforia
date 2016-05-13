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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import se.lth.cs.docforia.*;
import se.lth.cs.docforia.graph.text.Argument;
import se.lth.cs.docforia.graph.text.Predicate;
import se.lth.cs.docforia.graph.text.SemanticRole;
import se.lth.cs.docforia.io.text.ColumnReader;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Predicate Argument Structure Property Reader
 */
public class PredArgColumnReader implements ColumnReader {
    int predicateOffset;
    int wordSenseOffset;
    int framesetSenseOffset;
    int argStartOffset;

    /**
     * Standard constructor
     * @param predicateOffset the predicate column
     * @param framesetSenseOffset frameset indicator (-1 if it does not exist)
     * @param wordSenseOffset the word sense column, -1 if word sense is embedded with predicate
     * @param argStartOffset the argument start column
     */
    public PredArgColumnReader(int predicateOffset, int framesetSenseOffset, int wordSenseOffset,  int argStartOffset) {
        this.predicateOffset = predicateOffset;
        this.wordSenseOffset = wordSenseOffset;
        this.framesetSenseOffset = framesetSenseOffset;
        this.argStartOffset = argStartOffset;
    }

    private Pattern pattern = Pattern.compile("\\(([^\\|\\(\\)\\*]+)\\*?\\)|\\(([^\\|\\(\\)\\*]+)\\*?|(\\*?\\))", Pattern.CASE_INSENSITIVE);

    @Override
    public void load(Document doc, List<TextSentence> sentences) {
        DocumentNodeLayer predicateLayer = doc.store().nodeLayer(Document.nodeLayer(Predicate.class), null);
        DocumentNodeLayer argumentLayer = doc.store().nodeLayer(Document.nodeLayer(Argument.class), null);
        DocumentEdgeLayer semanticRole = doc.store().edgeLayer(Document.edgeLayer(SemanticRole.class), null);

        ObjectLinkedOpenHashSet a;

        for (TextSentence sentence : sentences) {
            ArrayList<NodeRef> predicates = new ArrayList<>();

            for (TextToken token : sentence) {
                String pred = token.getProperty(predicateOffset);
                if(pred != null) {
                    NodeRef nodeRef = predicateLayer.create(token.getStart(), token.getEnd());
                    NodeStore nodeStore = nodeRef.get();

                    if(wordSenseOffset != -1 || framesetSenseOffset != -1) {
                        String sense = token.getProperty(wordSenseOffset);
                        if(sense != null)
                            nodeStore.putProperty(Predicate.PROPERTY_SENSE, sense);

                        String frameset = token.getProperty(framesetSenseOffset);
                        if(frameset != null)
                            nodeStore.putProperty(Predicate.PROPERTY_FRAMESET, frameset);

                        nodeStore.putProperty(Predicate.PROPERTY_PREDCATE, pred);

                        if(frameset != null) {
                            predicates.add(nodeRef);
                        }
                    }
                }
            }

            IntArrayList[] startStack = new IntArrayList[predicates.size()];

            @SuppressWarnings("unchecked")
            ObjectArrayList<String>[] lastArgument = new ObjectArrayList[predicates.size()];

            for (int i = 0; i < startStack.length; i++) {
                startStack[i] = new IntArrayList();
                lastArgument[i] = new ObjectArrayList<>();
            }

            for (int k = 0; k < sentence.size(); k++) {
                TextToken token = sentence.get(k);
                for (int i = 0; i < predicates.size(); i++) {
                    String arg = token.getProperty(argStartOffset+i);
                    if(arg != null) {
                        Matcher matcher = pattern.matcher(arg);
                        while(matcher.find()) {
                            if(matcher.group(1) != null) {
                                //start and ending
                                NodeRef nodeRef = argumentLayer.create(token.getStart(), token.getEnd());
                                semanticRole.create(predicates.get(i), nodeRef).get().putProperty(SemanticRole.ROLE_PROPERTY, matcher.group(1));
                            }
                            else if(matcher.group(2) != null) {
                                //starting
                                startStack[i].push(k);
                                lastArgument[i].push(matcher.group(2));
                            }
                            else if(matcher.group(3) != null) {
                                //ending
                                int start =  startStack[i].popInt();
                                arg = lastArgument[i].pop();

                                NodeRef nodeRef = argumentLayer.create(sentence.get(start).getStart(), token.getEnd());
                                semanticRole.create(predicates.get(i), nodeRef).get().putProperty(SemanticRole.ROLE_PROPERTY, arg);
                            }
                        }
                    }
                }
            }
        }
    }
}

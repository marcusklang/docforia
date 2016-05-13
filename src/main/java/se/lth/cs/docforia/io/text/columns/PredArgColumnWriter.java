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

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import se.lth.cs.docforia.*;
import se.lth.cs.docforia.graph.text.Argument;
import se.lth.cs.docforia.graph.text.Predicate;
import se.lth.cs.docforia.graph.text.SemanticRole;
import se.lth.cs.docforia.graph.text.Token;
import se.lth.cs.docforia.io.text.ColumnWriter;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;
import se.lth.cs.docforia.query.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Predicate Argument structure writer
 */
public class PredArgColumnWriter implements ColumnWriter {
    private int predOffset;
    private int senseOffset;
    private int framesetOffset;
    private int argOffsetStart;

    /**
     * Full Predicate argument structure constructor
     * @param predOffset    the offset of predicate or -1 if append to end,
     * @param framesetOffset the offset of framset or -1 if appended to end, must be larger than sense
     * @param senseOffset   the offset of sense or -1 if append to end, must be larger than pred
     * @param argOffsetStart     the offset of argument or -1 if append to end, must be larger than sense
     */
    public PredArgColumnWriter(int predOffset, int framesetOffset, int senseOffset, int argOffsetStart) {
        if(framesetOffset < predOffset)
            throw new IllegalArgumentException("Framset offset is less than predicate offset!");

        if(senseOffset < framesetOffset)
            throw new IllegalArgumentException("Sense offset is less than frameset offset!");

        if(argOffsetStart < senseOffset)
            throw new IllegalArgumentException("Argument start offset is less than sense offset.");

        this.predOffset = predOffset;
        this.framesetOffset = framesetOffset;
        this.senseOffset = senseOffset;
        this.argOffsetStart = argOffsetStart;
    }

    @Override
    public void save(Document document, ArrayList<TextSentence> sentences) {
        DocumentEngine engine = document.engine();

        NodeTVar<Predicate> P = Predicate.var();
        NodeTVar<Argument> A = Argument.var();
        NodeTVar<Token> T = Token.var();
        EdgeTVar<SemanticRole> SR = SemanticRole.var();

        for (TextSentence sentence : sentences) {
            NodeStore sentNode = sentence.sentence();
            int sentStart = sentNode.getStart();
            int sentEnd = sentNode.getEnd();

            int realPredOffset = predOffset != -1 ? predOffset : sentence.getMinNumCols();
            int realFramsetOffset = framesetOffset != -1 ? framesetOffset : Math.max(sentence.getMinNumCols()+1, realPredOffset+1);
            int realSenseOffset = senseOffset != -1 ? senseOffset : Math.max(sentence.getMinNumCols()+2, realFramsetOffset+1);
            int realArgStartOffset = argOffsetStart != -1 ? argOffsetStart : Math.max(sentence.getMinNumCols()+3, realSenseOffset+1);

            NodeStore sent = sentence.sentence();

            Reference2IntOpenHashMap<NodeRef> pred2ArgCol = new Reference2IntOpenHashMap<>();

            //1. Find all predicates
            for (PropositionGroup group :
                    document.select(P, T)
                            .where(P).coveredBy(sentStart, sentEnd)
                            .where(T).coveredBy(P)
                            .stream()
                            .collect(QueryCollectors.groupBy(document, P)
                                                    .orderByKey(P)
                                                    .collector())) {
                NodeRef pred = group.key().noderef(P);
                if(pred.get().hasProperty(Predicate.PROPERTY_FRAMESET)) {
                    pred2ArgCol.put(pred, pred2ArgCol.size() + realArgStartOffset);
                }

                for (Proposition prop : group.values()) {
                    int index = sentence.index(prop.noderef(T));

                    TextToken token = sentence.get(index);

                    token.setProperty(realPredOffset, pred.get().getProperty(Predicate.PROPERTY_PREDCATE));
                    token.setProperty(realFramsetOffset, pred.get().getProperty(Predicate.PROPERTY_FRAMESET));
                    token.setProperty(realSenseOffset, pred.get().getProperty(Predicate.PROPERTY_SENSE));
                }
            }

            for (TextToken token : sentence) {
                for(int i = argOffsetStart; i < argOffsetStart+pred2ArgCol.size(); i++) {
                    token.setProperty(i, "*");
                }
            }

            if(pred2ArgCol.size() > 0) {
                List<PropositionGroup> query
                        = document.select(A, P, SR, T)
                                   .where(A).coveredBy(sentStart, sentEnd)
                                   .where(A).hasEdge(SR).from(P)
                                   .where(P).coveredBy(sentStart, sentEnd)
                                   .where(T).coveredBy(A)
                                   .stream()
                                   .collect(QueryCollectors.groupBy(document, P, SR, A).orderByKey(A).orderByValue(T).collector());

                for (PropositionGroup group : query) {
                    Proposition key = group.key();
                    NodeRef pred = key.noderef(P);
                    EdgeRef semRole = key.edgeref(SR);

                    int argCol = pred2ArgCol.getInt(pred);
                    NodeRef firstToken = group.values().get(0).noderef(T);
                    NodeRef lastToken = group.values().get(group.size()-1).noderef(T);
                    {
                        int startToken = sentence.index(firstToken);
                        String startText = sentence.get(startToken).getProperty(argCol);

                        sentence.get(startToken).setProperty(argCol, "(" + semRole.get().getProperty(SemanticRole.ROLE_PROPERTY) + startText);
                    }
                    {
                        int endToken = sentence.index(lastToken);
                        String endText  = sentence.get(endToken).getProperty(argCol);

                        sentence.get(endToken).setProperty(argCol, endText + ")");
                    }
                }
            }

            sentence.setMinCols(realArgStartOffset+pred2ArgCol.size());
        }
    }
}

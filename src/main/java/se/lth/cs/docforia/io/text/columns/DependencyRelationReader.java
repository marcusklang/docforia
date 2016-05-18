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
import se.lth.cs.docforia.DocumentEdgeLayer;
import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.graph.text.DependencyRelation;
import se.lth.cs.docforia.io.text.ColumnReader;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;

import java.util.List;

/**
 * Dependency Relation reader
 * <p>
 * Creates DependencyRelation edges
 */
public class DependencyRelationReader implements ColumnReader {
    private int headOffset;
    private int relationOffset;
    private boolean relativeToRight = false;

    /**
     * Full constructor
     * @param headOffset     column number
     * @param relationOffset column number, or -1 if unlabelled
     * @param relativeToRight true if offsets are relative to end, offset = 0 is right most.
     */
    public DependencyRelationReader(int headOffset, int relationOffset, boolean relativeToRight) {
        this.headOffset = headOffset;
        this.relationOffset = relationOffset;
        this.relativeToRight = relativeToRight;
    }

    @Override
    public void load(Document doc, List<TextSentence> sentences) {
        DocumentEdgeLayer deplayer = doc.store().edgeLayer(Document.edgeLayer(DependencyRelation.class), null);

        for (TextSentence sentence : sentences) {
            for (TextToken token : sentence) {
                String head = token.getProperty(relativeToRight ? token.size()-headOffset-1 : headOffset);
                String label = relationOffset != -1 ? token.getProperty(relativeToRight ? token.size()-relationOffset-1 : relationOffset) : null;

                int idxhead = Integer.parseInt(head);
                if(idxhead != 0) {
                    NodeRef headref = sentence.get(idxhead-1).token();
                    NodeRef tailref = token.token();
                    if(label != null) {
                        deplayer.create(tailref, headref).get().putProperty(DependencyRelation.RELATION_PROPERTY, label);
                    } else {
                        deplayer.create(tailref, headref);
                    }
                }
            }
        }
    }
}

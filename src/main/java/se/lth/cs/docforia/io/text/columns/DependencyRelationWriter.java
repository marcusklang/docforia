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

import se.lth.cs.docforia.*;
import se.lth.cs.docforia.exceptions.DocumentWriterException;
import se.lth.cs.docforia.graph.text.DependencyRelation;
import se.lth.cs.docforia.io.text.ColumnWriter;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Dependency relation property writer
 */
public class DependencyRelationWriter implements ColumnWriter {
    private int headOffset;
    private int relationOffset;
    private boolean unlabelled;

    /** Add to end offset constructor, head and then relation */
    public DependencyRelationWriter(boolean unlabelled) {
        headOffset = -1;
        relationOffset = -1;
        this.unlabelled = unlabelled;
    }

    /** Full constructor */
    public DependencyRelationWriter(int headOffset, int relationOffset, boolean unlabelled) {
        this.headOffset = headOffset;
        this.relationOffset = relationOffset;
        this.unlabelled = unlabelled;
    }

    @Override
    public void save(Document document, ArrayList<TextSentence> sentences) {
        DocumentEngine engine = document.engine();
        DocumentEdgeLayer dependencyLayer = document.store().edgeLayer(Document.edgeLayer(DependencyRelation.class), null);

        for (TextSentence sentence : sentences) {
            int realHeadoffset = headOffset == -1 ? sentence.getMinNumCols() : headOffset;
            int realRelationOffset = unlabelled ? -1 : (relationOffset == -1 ? sentence.getMinNumCols()+1 : relationOffset);

            for (TextToken token : sentence) {
                NodeStore tok = token.token();

                Iterator<EdgeRef> edges = engine.edges(tok, dependencyLayer, Direction.OUT).iterator();
                boolean found = edges.hasNext();
                if(found) {
                    EdgeRef edgeRef = edges.next();
                    NodeRef head = edgeRef.get().getHead();
                    int index = sentence.index(head);
                    if(index == -1)
                        throw new DocumentWriterException("Edge: " + edgeRef.toString() + " points to a token outside a sentence which is not allowed!");

                    token.setProperty(realHeadoffset, String.valueOf(index+1));
                    String prop = edgeRef.get().getProperty(DependencyRelation.RELATION_PROPERTY);
                    if(!unlabelled)
                        token.setProperty(realRelationOffset, prop);
                } else {
                    token.setProperty(realHeadoffset, "0");
                    if(!unlabelled)
                        token.setProperty(realRelationOffset, "ROOT");
                }
            }

            sentence.setMinCols(sentence.getMinNumCols()+(unlabelled ? 1 : 2));
        }
    }
}

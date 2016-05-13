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

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import se.lth.cs.docforia.*;
import se.lth.cs.docforia.exceptions.DocumentWriterException;
import se.lth.cs.docforia.graph.text.ParseTreeEdge;
import se.lth.cs.docforia.graph.text.ParseTreeNode;
import se.lth.cs.docforia.graph.text.Token;
import se.lth.cs.docforia.io.text.ColumnWriter;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;
import se.lth.cs.docforia.util.DocumentIterable;

import java.util.*;

/**
 * Parse Tree writer
 */
public class ParseTreeWriter implements ColumnWriter {
    private int offset;

    /**
     * Full constructor
     * @param offset the offset to write everything to
     */
    public ParseTreeWriter(int offset) {
        this.offset = offset;
    }

    private static class State implements Comparable<State> {
        public int min = -1;
        public int max = -1;

        public void merge(State child) {
            if(min == -1)
            {
                min = child.min;
                max = child.max;
            } else {
                min = Math.min(min, child.min);
                max = Math.max(max, child.max);
            }
        }

        public void expand(int token) {
            if(min == -1) {
                min = token;
                max = token;
            } else {
                min = Math.min(token, min);
                max = Math.max(token, max);
            }
        }

        @Override
        public int compareTo(State o) {
            int res = Integer.compare(min, o.min);
            return res != 0 ? res : Integer.compare(max, o.max);
        }
    }

    private NodeRef findTopLevel(DocumentEngine engine, TextSentence sentence, int k, LayerRef edgeLayer, LayerRef nodeLayer) {
        NodeRef topLevel = null;
        ReferenceOpenHashSet<NodeRef> visited = new ReferenceOpenHashSet<>();

        for (TextToken token : sentence) {
            ArrayDeque<NodeRef> stack = new ArrayDeque<>();
            stack.push(token.ref());
            while (!stack.isEmpty()) {
                NodeRef current = stack.pop();
                visited.add(current);

                DocumentIterable<NodeRef> nodes = engine.edgeNodes(current, edgeLayer, nodeLayer, Direction.IN);

                boolean foundNodes = false;
                for (NodeRef node : nodes) {
                    if (!visited.contains(node)) {
                        stack.push(node);
                    }
                    foundNodes = true;
                }

                if (!foundNodes) {
                    if(topLevel == null)
                        topLevel = current;
                    else
                        throw new DocumentWriterException("Only one top/root node is supported per sentence, found multiple in sentence " + k + ": '" + sentence.representation().toString() + "'");
                }
            }
        }

        return topLevel;
    }

    private State processChild(DocumentEngine engine, TextSentence sentence, LayerRef edgeLayer, LayerRef tokenLayer, State state, NodeRef start) {
        for (NodeRef nodeRef : engine.edgeNodes(start, edgeLayer, tokenLayer, Direction.OUT)) {
            int index = sentence.index(nodeRef);
            if(index == -1)
                throw new DocumentWriterException("Token '" + sentence.getDocument().text(nodeRef.get().getStart(), nodeRef.get().getEnd()) + "' is outside current sentence: '"+ sentence.representation().toString() + "'");

            state.expand(index);
        }

        return state;
    }

    private Reference2ObjectOpenHashMap<NodeRef, State> buildMinMaxMap(
            DocumentEngine engine,
            TextSentence sentence,
            LayerRef edgeLayer,
            LayerRef nodeLayer,
            LayerRef tokenLayer,
            NodeRef rootNode)
    {
        //2. Go top down, and maintain min, max for every node while walking, when backtracking, use its children min, max.
        ReferenceOpenHashSet<NodeRef> visited = new ReferenceOpenHashSet<>();
        Reference2ObjectOpenHashMap<NodeRef, State> states = new Reference2ObjectOpenHashMap<>();

        ArrayDeque<Iterator<NodeRef>> childrens = new ArrayDeque<>();
        ArrayDeque<NodeRef> node = new ArrayDeque<>();

        //Initial state
        node.push(rootNode);
        childrens.push(engine.edgeNodes(rootNode, edgeLayer, nodeLayer, Direction.OUT).iterator());
        states.put(rootNode, processChild(engine, sentence, edgeLayer, tokenLayer, new State(), rootNode));

        while (!node.isEmpty()) {
            Iterator<NodeRef> peek = childrens.peek();
            if(peek.hasNext()) {
                NodeRef child = peek.next();
                if(visited.contains(child)) {
                    throw new DocumentWriterException("There is a cycle in the ParseTree of sentence: '" + sentence.representation().toString()  + "', this is not allowed!");
                }

                node.push(child);
                childrens.push(engine.edgeNodes(child, edgeLayer, nodeLayer, Direction.OUT).iterator());
                states.put(child, processChild(engine, sentence, edgeLayer, tokenLayer, new State(), child));
                visited.add(child);
            } else {
                NodeRef current = node.pop();
                childrens.pop();

                State state = states.get(current);
                for (NodeRef nodeRef : engine.edgeNodes(current, edgeLayer, nodeLayer, Direction.OUT)) {
                    state.merge(states.get(nodeRef));
                }
            }
        }

        return states;
    }

    private List<NodeRef> orderedChildren(
            DocumentEngine engine,
            NodeRef start,
            Reference2ObjectOpenHashMap<NodeRef, State> minMax,
            LayerRef edgeLayer,
            LayerRef nodeLayer) {
        List<NodeRef> children = engine.edgeNodes(start, edgeLayer, nodeLayer, Direction.OUT).toList();
        Collections.sort(children, (x,y) -> minMax.get(x).compareTo(minMax.get(y)));
        return children;
    }

    private void bfsInsertion(
            DocumentEngine engine,
            NodeRef rootLevel,
            TextSentence sentence,
            Reference2ObjectOpenHashMap<NodeRef, State> minMax,
            LayerRef edgeLayer,
            LayerRef nodeLayer,
            String propertyKey)
    {
        int realoffset = offset != -1 ? offset : sentence.getMinNumCols();

        //Fill with leafs
        for (TextToken token : sentence) {
            token.setProperty(realoffset, "*");
        }

        //BFS
        ArrayDeque<NodeRef> queue = new ArrayDeque<>();
        queue.addFirst(rootLevel);

        ArrayDeque<NodeRef> bforder = new ArrayDeque<>(minMax.size());
        bforder.add(rootLevel);

        //Forward BFS
        while(!queue.isEmpty()) {
            NodeRef current = queue.removeFirst();
            List<NodeRef> orderedChildren = orderedChildren(engine, current, minMax, edgeLayer, nodeLayer);
            queue.addAll(orderedChildren);
            bforder.addAll(orderedChildren);
        }

        //Backwards BFS
        while(!bforder.isEmpty()) {
            NodeRef current = bforder.removeLast();

            String label = current.get().getProperty(propertyKey);
            label = (label == null ? "" : label);

            State state = minMax.get(current);
            TextToken startToken = sentence.get(state.min);
            startToken.setProperty(realoffset, "(" + label + startToken.getProperty(realoffset));

            TextToken endToken = sentence.get(state.max);
            endToken.setProperty(realoffset, endToken.getProperty(realoffset) + ")");
        }

        sentence.setMinCols(realoffset+1);
    }

    @Override
    public void save(Document document, ArrayList<TextSentence> sentences) {
        DocumentEngine engine = document.engine();
        DocumentEdgeLayer parseTreeEdges = document.store().edgeLayer(Document.edgeLayer(ParseTreeEdge.class), null);
        DocumentNodeLayer parseNodeLayer = document.store().nodeLayer(Document.nodeLayer(ParseTreeNode.class), null);
        DocumentNodeLayer tokenLayer = document.store().nodeLayer(Document.nodeLayer(Token.class), null);
        LayerRef parseEdgeLayerRef = parseTreeEdges.layer();
        LayerRef parseNodeLayerRef = parseNodeLayer.layer();
        LayerRef tokenLayerRef = tokenLayer.layer();

        for (int k = 0; k < sentences.size(); k++) {
            TextSentence sentence = sentences.get(k);
            NodeRef topLevel = findTopLevel(engine, sentence, k, parseEdgeLayerRef, parseNodeLayerRef);
            Reference2ObjectOpenHashMap<NodeRef, State> minMax
                    = buildMinMaxMap(engine, sentence, parseEdgeLayerRef, parseNodeLayerRef, tokenLayerRef, topLevel);

            bfsInsertion(engine, topLevel, sentence, minMax, parseEdgeLayerRef, parseNodeLayerRef, ParseTreeNode.PROPERTY_LABEL);
        }
    }
}

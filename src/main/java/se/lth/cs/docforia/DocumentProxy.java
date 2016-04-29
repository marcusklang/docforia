package se.lth.cs.docforia;
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

/**
 * Document proxy, that gives essentials for implementing transactions.
 */
public interface DocumentProxy {
    /**
     * Add node
     * @param node
     * @param <N>
     * @return
     */
    <N extends Node> N add(N node);

    /**
     * Add node with a dynamic layer type
     * @param node the dynamic node
     * @param layer the dynamic layer
     * @return node
     */
    DynamicNode add(DynamicNode node, String layer);

    /**
     * Add edge
     * @param edge
     * @param <E>
     * @return
     */
    <E extends Edge> E add(E edge);

    /**
     * Add edge
     * @param edge
     * @param <E>
     * @return
     */
    <E extends Edge> E add(E edge, Node tail, Node head);

    /**
     * Add node with a dynamic layer type
     * @param edge the dynamic node
     * @param layer the dynamic layer
     * @return node
     */
    DynamicEdge add(DynamicEdge edge, String layer);

    /**
     * Add node with a dynamic layer type
     * @param edge the dynamic node
     * @param layer the dynamic layer
     * @return node
     */
    DynamicEdge add(DynamicEdge edge, String layer, Node tail, Node head);

    String text();
    String text(int start, int end);
    int length();

    int transform(int pos);
    int inverseTransform(int pos);
    void inverseTransform(MutableRange range);

    /**
     * Get representation of NodeRef
     * @param ref the reference
     * @return representation of Node
     */
    Node representation(NodeRef ref);

    /**
     * Get representation of EdgeRef
     * @param ref the reference
     * @return representation of Edge
     */
    Edge representation(EdgeRef ref);
}

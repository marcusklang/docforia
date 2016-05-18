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
 * Document edge layer representation, suitable for batch insertions
 */
public interface DocumentEdgeLayer extends Iterable<EdgeRef>, LayerRef {
    /** Create edge */
    EdgeRef create();

    /** Create edge ith tail and head */
    EdgeRef create(NodeRef tail, NodeRef head);

    /**
     * Used to change type of an existing layer to something else
     * <p>
     * <b>Remarks:</b> Retains any variant if specified
     * @param newLayer the new raw layer type
     */
    default void migrate(String newLayer) {
        migrate(newLayer, getVariant());
    }

    /**
     * Used to change type of an existing layer to something else
     * <p>
     * <b>Remarks:</b> Retains any variant if specified
     * @param newLayer the new raw layer type
     */
    void migrate(String newLayer, String variant);

    /**
     * Remove node, should belong to this layer.
     */
    void remove(EdgeRef ref);

    /** Get the count of edges in this layer */
    int size();
}

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
 * Document node layer representation, suitable for batch insertions
 * <p>
 * <b>Remarks:</b> For iteration: non annotation nodes comes first, then comes all annotations
 */
public interface DocumentNodeLayer extends Iterable<NodeRef>, LayerRef {

    /** Create a node in this layer */
    NodeRef create();

    /** Create a annotation with given start, end
     *  <p>
     *  <b>Remarks:</b> This is a low level API with minimal or no checks.
     **/
    NodeRef create(int start, int end);

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
    void remove(NodeRef ref);

    /** Get the number of stored nodes */
    int size();
}

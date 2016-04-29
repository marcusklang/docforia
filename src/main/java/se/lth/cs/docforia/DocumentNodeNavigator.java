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
 * Node navigator abstraction for annotations in a particular layer
 */
public interface DocumentNodeNavigator {
    /**
     * Get the current annotation
     * @return Current node reference or <code>null</code> if there is none.
     */
    NodeRef current();

    /**
     * Move to the next annotation
     * @return true if a next annotation exists, false if not
     */
    boolean next();

    /**
     * Move to the annotation that is annotation.start &gt;= start with annotation.end &gt; annotation.start
     * @param start the start
     * @return true if success
     */
    boolean nextFloor(int start);

    /**
     * @return true if no more annotations
     */
    boolean hasReachedEnd();

    /**
     * Move to the prev annotation
     * @return true if a next annotation exists, false if not
     */
    boolean prev();

    /**
     * Reset search
     */
    void reset();

    /**
     * Move to the annotation that is annotation.start &gt;= start
     * @param start the start
     * @return
     */
    boolean next(int start);

    /**
     * The current start
     * @return start position
     */
    int start();

    /**
     * The current end
     * @return end position
     */
    int end();

}

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
 * Storage level hint
 * <p>
 * Controls and sets the balanace between space and time when converting documents
 * into other formats such as JSON and binary.
 */
public enum DocumentStorageLevel {
    /**
     * As simple as possible
     */
    LEVEL_0,

    /**
     * Level 1, sacrifice some speed to minimize storage.
     */
    LEVEL_1,

    /**
     * Level 2, the best tradeoff between speed and minimal storage
     */
    LEVEL_2,

    /**
     * Level 3, minimize storage harder than LEVEL_2
     */
    LEVEL_3,

    /**
     * Level 4, compress as hard as possible regardless of CPU time and memory.
     */
    LEVEL_4
}

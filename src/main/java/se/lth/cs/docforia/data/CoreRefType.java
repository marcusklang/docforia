package se.lth.cs.docforia.data;
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
 * CoreRef enumeration
 */
public enum CoreRefType {
    NULL            (0),
    BINARY          (1),
    STRING          (2),
    INT             (3),
    LONG            (4),
    FLOAT           (5),
    DOUBLE          (6),
    BOOLEAN         (7),
    STRING_ARRAY    (8),
    INT_ARRAY       (9),
    LONG_ARRAY      (10),
    FLOAT_ARRAY     (11),
    DOUBLE_ARRAY    (12),
    BOOLEAN_ARRAY   (13),
    PROPERTY_MAP    (14),
    DOCUMENT        (15),
    DOCUMENT_ARRAY  (16),
    RESERVED        (255);

    public final byte value;

    CoreRefType(int value) {
        this.value = (byte)value;
    }
}

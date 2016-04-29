package se.lth.cs.docforia.memstore.encoders;
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

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import se.lth.cs.docforia.StoreRef;
import se.lth.cs.docforia.io.mem.Input;

/**
 * String field decoder
 */
public class StringFieldDecoder {

    public static void decode(Input reader, String text, String key, int count,
                              int[] propertySetMapping,
                              IntArrayList psets,
                              Int2ReferenceOpenHashMap<? extends StoreRef> refs) {
        int id = Byte.toUnsignedInt(reader.readByte());
        StringCodecs.codecs[id].decode(reader, text, key, count, propertySetMapping, psets, refs);
    }
}

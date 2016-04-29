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
import it.unimi.dsi.fastutil.ints.IntListIterator;
import se.lth.cs.docforia.StoreRef;
import se.lth.cs.docforia.data.DataRef;
import se.lth.cs.docforia.io.mem.Input;
import se.lth.cs.docforia.io.mem.Output;

/**
 * Equal length string codec
 */
public class EqualLenStringCodec extends StringCodec {

    public static final EqualLenStringCodec INSTANCE = new EqualLenStringCodec();

    @Override
    public void encode(Output writer, String text, String key, StringFieldEncoder prober, Iterable<? extends StoreRef> entries) {
        writer.writeByte(StringCodecs.EQUAL_LEN);
        //writer.writeVarInt(prober.lastlen, true);
        for (StoreRef storeRef : entries) {
            DataRef property = storeRef.get().getRefProperty(key);
            writer.writeString(property.stringValue());
        }
    }

    @Override
    public void decode(Input reader, String text, String key, int count, int[] propertySetMapping, IntArrayList psets, Int2ReferenceOpenHashMap<? extends StoreRef> refs) {
        //int len = reader.readPosVarInt();

        IntListIterator noderange = psets.iterator();
        while (noderange.hasNext()) {
            int psetid = noderange.nextInt();
            final int start = propertySetMapping[psetid];
            final int end = propertySetMapping[psetid + 1];

            for (int i = start; i < end; i++) {
                refs.get(i).get().putProperty(key, reader.readString());
            }
        }
    }
}

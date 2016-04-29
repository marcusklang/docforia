package se.lth.cs.docforia.memstore;
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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import se.lth.cs.docforia.DocumentStorageLevel;
import se.lth.cs.docforia.io.mem.Input;
import se.lth.cs.docforia.io.mem.Output;

/**
 * Memory Binary I/O
 */
public class MemoryBinary {
    public enum Options {
        SPLITTABLE
    }

    public static class DocumentIndex {
        public IntArrayList splitPoints;
        public int head;
        public int body;

        public IntArrayList nodeLayers;
        public ObjectArrayList<String> nodeLayerName;

        public IntArrayList edgeLayers;
        public ObjectArrayList<String> edgeLayerName;
    }

    public static MemoryDocument decode(byte[] data) {
        return decode(new Input(data));
    }

    public static MemoryDocument decode(Input data) {
        if(data.available() < 4)
            throw new IllegalArgumentException("Invalid format, smaller than header! Length: " + data.available());

        int b0 = Byte.toUnsignedInt(data.readByte());
        int b1 = Byte.toUnsignedInt(data.readByte());
        int ver = Byte.toUnsignedInt(data.readByte());
        int lvl = Byte.toUnsignedInt(data.readByte());

        if(b0 == 'D' && b1 == 'M') {
            if(ver != '1') {
                throw new UnsupportedOperationException("Unsupported format, Version = " + (char)ver);
            }

            switch (lvl) {
                case '0':
                    return MemoryBinaryV1L0Codec.INSTANCE.decode(data);
                case '1':
                    return MemoryBinaryV1L1Codec.INSTANCE.decode(data);
                case '2':
                    return MemoryBinaryV1L2Codec.INSTANCE.decode(data);
                default:
                    throw new UnsupportedOperationException("Level not implemented.");
            }
        } else {
            throw new IllegalArgumentException("Invalid format, unknown magic header: " + String.format("%02X%02X", (byte)b0, (byte)b1));
        }
    }

    public static MemoryBinaryCodec latest(DocumentStorageLevel level) {
        switch (level) {
            case LEVEL_0:
                return MemoryBinaryV1L0Codec.INSTANCE;
            case LEVEL_1:
                return MemoryBinaryV1L1Codec.INSTANCE;
            case LEVEL_2:
            case LEVEL_3:
            case LEVEL_4:
                return MemoryBinaryV1L2Codec.INSTANCE;
            default:
                throw new UnsupportedOperationException("Unsupported level");
        }
    }

    public static byte[] encode(DocumentStorageLevel level, MemoryDocument doc) {
        DocumentIndex idx = new DocumentIndex();
        Output writer = new Output(512, 2<<29);
        latest(level).encode(doc, writer, idx);
        return writer.toBytes();
    }

    public static void encode(DocumentStorageLevel level, MemoryDocument doc, Output writer) {
        DocumentIndex idx = new DocumentIndex();
        latest(level).encode(doc, writer, idx);
    }
}

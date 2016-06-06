package se.lth.cs.docforia.io.mem;
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

import java.io.IOError;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * GZIP utilities
 */
public class GzipUtil {
    public static byte[] compress(byte[] data) {
        try {
            //Heuristic, 75% compression.
            Output compressed = new Output(32,2147483647);
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(compressed, 1 << 12);

            Output output = new Output(gzipOutputStream);
            output.writeVarInt(data.length, true);
            output.write(data);
            output.close();

            return compressed.toBytes();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static ByteBuffer compress(ByteBuffer data) {
        try {
            //Heuristic, 75% compression.
            ByteBufferOutputStream bufferOutputStream = new ByteBufferOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bufferOutputStream, 1 << 12);

            Output output = new Output(gzipOutputStream);
            output.writeVarInt(data.remaining(), true);
            output.write(data);
            output.close();

            ByteBuffer buffer = bufferOutputStream.buffer();
            buffer.flip();
            return buffer;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static ByteBuffer decompress(ByteBuffer data) {
        try {
            GZIPInputStream inputStream = new GZIPInputStream(new ByteBufferInputStream(data));
            Input input = new Input(inputStream);
            int size = input.readVarInt(true);

            ByteBuffer decompressed = ByteBuffer.allocate(size);
            input.readByteBuffer(decompressed);

            decompressed.flip();
            return decompressed;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static byte[] decompress(byte[] bytes) {
        try {
            GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(bytes));
            Input input = new Input(inputStream);
            int size = input.readVarInt(true);

            byte[] decompressed = new byte[size];
            input.readBytes(decompressed);

            return decompressed;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}

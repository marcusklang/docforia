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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * ByteBuffer input stream adapter
 */
public class ByteBufferInputStream extends InputStream {
    private ByteBuffer buffer;

    public ByteBufferInputStream(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public int read() throws IOException {
        if(!buffer.hasRemaining())
            return -1;

        return buffer.get() & 0xFF;
    }

    @Override
    public synchronized void reset() throws IOException {
        buffer.reset();
    }

    @Override
    public int available() throws IOException {
        return buffer.remaining();
    }

    @Override
    public long skip(long n) throws IOException {
        int oldPosition = buffer.position();
        int newPosition = Math.min(buffer.position()+(int)n, buffer.limit());

        buffer.position(newPosition);
        return newPosition-oldPosition;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if(buffer.hasRemaining()) {
            if(len > buffer.remaining()) {
                len = buffer.remaining();
            }

            buffer.get(b, off, len);
            return len;
        } else {
            return -1;
        }
    }
}

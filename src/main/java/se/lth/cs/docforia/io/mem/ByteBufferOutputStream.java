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
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * ByteBuffer OutputStream adapter
 * <p>
 * <b>Remarks:</b> Expands and copies its internal buffer when more space is needed (factor 2 expansion)
 */
public class ByteBufferOutputStream extends OutputStream {
    private ByteBuffer buffer;
    private boolean direct;

    public void require(int size) {
        if(buffer.remaining() < size) {
            int minCapacity = buffer.capacity()+size;
            if(minCapacity < 0)
                throw new UnsupportedOperationException("Requires more than 2 GB space, not supported.");

            int capacity = buffer.capacity();
            int lastCapacity = capacity;
            boolean overflow = false;

            while(capacity < minCapacity && !overflow) {
                capacity*=2;

                if (capacity != 0 && capacity / lastCapacity != 2) {
                    overflow = true;
                } else {
                    lastCapacity = capacity;
                }
            }

            if(overflow)
                capacity = Integer.MAX_VALUE;

            ByteBuffer temp = direct ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity);
            buffer.flip();
            temp.put(buffer);
            this.buffer = temp;
        }
    }

    public ByteBufferOutputStream() {
        this(1<<16);
    }

    public ByteBufferOutputStream(int initialCapacity) {
        this(initialCapacity, false);
    }

    public ByteBufferOutputStream(int initialCapacity, boolean direct) {
        if(initialCapacity <= 0)
            throw new IllegalArgumentException("initialCapacity must be > 0");

        this.direct = direct;

        if(direct)
            this.buffer = ByteBuffer.allocateDirect(initialCapacity);
        else
            this.buffer = ByteBuffer.allocate(initialCapacity);
    }


    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if(buffer.capacity() < buffer.position() + len)
            require(len);

        buffer.put(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        if(buffer.capacity() == buffer.position())
            require(1);

        buffer.put((byte)b);
    }

    public ByteBuffer buffer() {
        return this.buffer;
    }
}

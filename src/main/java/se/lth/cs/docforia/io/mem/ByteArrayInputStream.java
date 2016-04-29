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

/**
 *  Byte array input stream adapter
 */
public class ByteArrayInputStream extends InputStream {
    private byte[] data;
    private int offset;
    private int position;
    private int limit;

    public ByteArrayInputStream(byte[] data) {
        this(data, 0, data.length);
    }

    public ByteArrayInputStream(byte[] data, int offset, int length) {
        this.data = data;
        this.offset = offset;
        this.position = offset;
        this.limit = length+offset;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if(position == limit)
            return -1;

        int actualLen = Math.min(len,limit-position);
        System.arraycopy(data, position, b, off, actualLen);
        position += actualLen;
        return actualLen;
    }

    @Override
    public int available() throws IOException {
        return limit-position;
    }

    @Override
    public long skip(long n) throws IOException {
        int newPosition = Math.min(limit, position+(int)n);
        int diff=newPosition-position;
        this.position = newPosition;
        return diff;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized void reset() throws IOException {
        this.position = offset;
    }

    @Override
    public int read() throws IOException {
        return (position < limit) ? (data[position++] & 0xff) : -1;
    }
}

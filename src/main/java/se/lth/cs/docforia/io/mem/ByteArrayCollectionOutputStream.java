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
import java.util.ArrayList;

/**
 * Buffering Output Stream, stores all data in memory using an array of byte[].
 */
public class ByteArrayCollectionOutputStream extends OutputStream {
    private byte[] buffer;
    private ArrayList<byte[]> buffers = new ArrayList<>();
    private int bufferSize;
    private int position = 0;
    private int written;

    public ByteArrayCollectionOutputStream(int bufferSize) {
        this.bufferSize = bufferSize;
        this.buffer = new byte[bufferSize];
    }

    @Override
    public void write(byte[] b) throws IOException {
        int left = b.length;
        while(left > 0) {
            if(left < buffer.length - position) {
                System.arraycopy(b, (b.length-left), buffer, position, left);
                position += left;
                left = 0;
            } else {
                System.arraycopy(b, (b.length-left), buffer, position, buffer.length-position);
                left -= buffer.length-position;
                buffers.add(buffer);
                buffer = new byte[bufferSize];
                position = 0;
            }
        }

        written += b.length;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int left = len;
        while(left > 0) {
            if(left < buffer.length - position) {
                System.arraycopy(b, off + (len-left), buffer, position, left);
                position += left;
                left = 0;
            } else {
                System.arraycopy(b, off + (len-left), buffer, position, buffer.length-position);
                left -= buffer.length-position;
                buffers.add(buffer);
                buffer = new byte[bufferSize];
                position = 0;
            }
        }

        written += len;
    }

    @Override
    public void write(int b) throws IOException {
        buffer[position++] = (byte)b;
        if(position == buffer.length)
        {
            buffers.add(buffer);
            buffer = new byte[bufferSize];
        }

        written++;
    }

    public int written() {
        return written;
    }

    /**
     * Send all buffered data to the output
     */
    public void send(Output output) {
        for (byte[] buff : buffers) {
            output.write(buff);
        }

        if(position > 0) {
            output.write(buffer, 0, position);
        }

        buffers.clear();
        position = 0;
        written = 0;
    }
}

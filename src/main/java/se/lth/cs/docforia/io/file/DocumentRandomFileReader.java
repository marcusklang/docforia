package se.lth.cs.docforia.io.file;
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

import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.io.DocumentReader;
import se.lth.cs.docforia.io.mem.Input;
import se.lth.cs.docforia.memstore.MemoryBinary;

import java.io.*;
import java.util.Arrays;

/**
 * Random access sequential document storage reader
 *
 * @see DocumentFileReader
 */
public class DocumentRandomFileReader implements DocumentReader {
    private RandomAccessFile reader;
    private DataFilter filter;

    public DocumentRandomFileReader(File input) {
        this(input, GzipFilter.getInstance());
    }

    public DocumentRandomFileReader(File input, DataFilter filter) {
        try {
            this.filter = filter;
            this.reader = new RandomAccessFile(input, "r");
            if (reader.length() < 6) {
                throw new IOException("Smaller than header size!");
            }

            byte[] header = new byte[4];
            reader.readFully(header);
            byte[] filterId = new byte[2];
            reader.readFully(filterId);

            if (!Arrays.equals(header, DocumentFileWriter.MAGIC_V1)) {
                throw new IOException("Magic bytes does not match, actual: " + new String(header, "ISO-8859-1"));
            }

            if(filter != null && !Arrays.equals(filterId, filter.id()))
            {
                throw new IOException("Incorrect filter, expected: " + new String(filter.id(), "ISO-8859-1") + ", actual: " + new String(filterId, "ISO-8859-1"));
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void seek(long start) {
        try {
            reader.seek(start);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    protected byte[] unapply(byte[] filtered) {
        return filter != null ? filter.unapply(filtered) : filtered;
    }

    public Document read(long frameStart, int frameLength) {
        if(reader == null)
            throw new IllegalStateException("Reader is closed!");

        try {
            reader.seek(frameStart);
            byte[] data = new byte[frameLength];
            reader.readFully(data);

            Input input = new Input(data);
            int length = input.readVarInt(true);
            if (data.length - length == input.position()) {
                return MemoryBinary.decode(input);
            } else {
                throw new IOError(new IOException("Incorrect position, expected a frame size of " + length + " but got " + (frameLength - input.position())));
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public Document next() {
        if(reader == null)
            throw new IllegalStateException("Reader is closed!");

        try {
            if (reader.getFilePointer() == reader.length())
                return null;

            int frameSize = Input.readVarInt(reader, true);
            if (frameSize == 0)
                return null;

            byte[] doc = new byte[frameSize];
            reader.readFully(doc);
            return MemoryBinary.decode(doc);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public void skip(int numDocuments) {
        if(reader == null)
            throw new IllegalStateException("Reader is closed!");

        try {
            if (reader.getFilePointer() == reader.length())
                throw new EOFException("At end of file!");

            while (numDocuments > 0) {
                int frameSize = Input.readVarInt(reader, true);
                if (frameSize == 0)
                    throw new EOFException("Reached the end while skipping!");

                reader.seek(reader.getFilePointer() + frameSize);
                numDocuments--;
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void close() {
        try {
            reader.close();
            reader = null;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}
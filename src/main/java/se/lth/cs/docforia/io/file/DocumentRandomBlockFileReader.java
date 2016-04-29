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
import se.lth.cs.docforia.memstore.MemoryDocument;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/** Random access block based document storage reader
 *
 * @see DocumentBlockFileReader
 */
public class DocumentRandomBlockFileReader implements DocumentReader {
    private RandomAccessFile reader;
    private boolean eof;
    private DataFilter filter;
    private ArrayDeque<MemoryDocument> currentBlock = new ArrayDeque<>();

    public DocumentRandomBlockFileReader(File input) {
        this(input, GzipFilter.getInstance());
    }

    public DocumentRandomBlockFileReader(File input, DataFilter filter) {
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
            currentBlock.clear();
            reader.seek(start);
            eof = start == reader.length();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public List<Document> read() {
        ArrayList<Document> block = new ArrayList<>();
        read(block);
        return block;
    }

    public boolean read(Collection<? super MemoryDocument> documentCollection) {
        if(reader == null)
            throw new IllegalStateException("Reader is closed!");

        if(eof)
            return false;

        try {
            int frameLength = Input.readVarInt(reader, true);
            if(frameLength == 0) {
                eof = true;
                return false;
            }

            byte[] data = new byte[frameLength];
            reader.readFully(data);

            Input input = new Input(data);
            int length = input.readVarInt(true);
            if (data.length - length == input.position()) {
                ByteBuffer block = ByteBuffer.wrap(data, input.position(), input.limit());
                block = filter != null ? filter.unapply(block) : block;

                Input blockReader = new Input(block);
                while(!blockReader.eof()) {
                    int frameSize = blockReader.readVarInt(true);
                    documentCollection.add(MemoryBinary.decode(new Input(blockReader.getBuffer(), blockReader.position(), frameSize)));
                    blockReader.skip(frameSize);
                }

                return true;
            } else {
                throw new IOError(new IOException("Incorrect position, expected a frame size of " + length + " but got " + (frameLength - input.position())));
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public List<Document> read(long frameStart, int frameLength) {
        ArrayList<Document> documentArrayList = new ArrayList<>();
        read(documentArrayList, frameStart, frameLength);
        return documentArrayList;
    }

    public void read(Collection<? super MemoryDocument> documentCollection, long frameStart, int frameLength) {
        if(reader == null)
            throw new IllegalStateException("Reader is closed!");

        try {
            reader.seek(frameStart);
            byte[] data = new byte[frameLength];
            reader.readFully(data);

            Input input = new Input(data);
            int length = input.readVarInt(true);
            if (data.length - length == input.position()) {
                ByteBuffer block = ByteBuffer.wrap(data, input.position(), input.limit());
                block = filter != null ? filter.unapply(block) : block;

                Input blockReader = new Input(block);
                while(!blockReader.eof()) {
                    int frameSize = blockReader.readVarInt(true);
                    documentCollection.add(MemoryBinary.decode(new Input(blockReader.getBuffer(), blockReader.position(), frameSize)));
                    blockReader.skip(frameSize);
                }
            } else {
                throw new IOError(new IOException("Incorrect position, expected a frame size of " + length + " but got " + (frameLength - input.position())));
            }
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public MemoryDocument next() {
        if(reader == null)
            throw new IllegalStateException("Reader is closed!");

        if(eof)
            return null;

        if(currentBlock.isEmpty()) {
            if(!read(currentBlock)) {
                eof = true;
                return null;
            }
        }

        return currentBlock.removeFirst();
    }

    public void skip(int numBlocks) {
        if(reader == null)
            throw new IllegalStateException("Reader is closed!");

        try {
            if (reader.getFilePointer() == reader.length())
                throw new EOFException("At end of file!");

            while (numBlocks > 0) {
                int frameSize = Input.readVarInt(reader, true);
                if (frameSize == 0)
                    throw new EOFException("Reached the end while skipping!");

                reader.seek(reader.getFilePointer() + frameSize);
                numBlocks--;
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
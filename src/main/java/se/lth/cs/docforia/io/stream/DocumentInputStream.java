package se.lth.cs.docforia.io.stream;
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
import se.lth.cs.docforia.memstore.MemoryDocumentIO;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Document storage input stream
 */
public class DocumentInputStream implements DocumentReader {
    private Input input;
    private boolean eof=false;

    public DocumentInputStream(InputStream inputStream) {
        this.input = new Input(inputStream);

        byte[] header = new byte[4];
        input.readBytes(header);

        boolean validHeader = Arrays.equals(header, DocumentOutputStream.MAGIC);
        if(!validHeader)
            throw new IOError(new IOException("Incorrect magic bytes!"));
    }

    /**
     * Read document
     * @return non-null document if success, null if no more.
     */
    public Document next() {
        if(eof)
            return null;

        int frameSize = input.readVarInt(true);
        if(frameSize == 0) {
            eof = true;
            return null;
        }

        byte[] data = new byte[frameSize];
        input.readBytes(data);
        return MemoryDocumentIO.getInstance().fromBytes(data);
    }

    /**
     * Skip documents
     * @param n the number of documents
     * @return actual skip count (might be less than n if EOF was reached)
     */
    public int skip(int n) {
        if(eof)
            return 0;

        int skipped = 0;
        while(skipped < n && !eof) {
            int frameSize = input.readVarInt(true);
            if(frameSize == 0) {
                eof = true;
                return skipped;
            }

            input.skip(frameSize);
            skipped++;
        }

        return skipped;
    }

    public long skipBytes(long bytes) {
        return input.skip(bytes);
    }

    public void close() {
        input.close();
    }
}

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
import se.lth.cs.docforia.DocumentStorageLevel;
import se.lth.cs.docforia.io.DocumentWriter;
import se.lth.cs.docforia.io.mem.Output;

import java.io.OutputStream;

/**
 * Document storage output stream
 */
public class DocumentOutputStream implements DocumentWriter {
    private Output output;
    private DocumentStorageLevel storageLevel;

    static final byte[] MAGIC = new byte[] {
            'D', 'S', 'T', '1' //Document Storage Stream v1
    };

    public DocumentOutputStream(OutputStream outputStream) {
        this(outputStream, DocumentStorageLevel.LEVEL_2);
    }

    public DocumentOutputStream(OutputStream outputStream, DocumentStorageLevel level) {
        this.storageLevel = level;
        this.output = new Output(outputStream, 256*1024*1024);
        this.output.writeBytes(MAGIC);
    }

    /**
     * Write document
     * @param document the document to write
     */
    public void write(Document document) {
        byte[] docBytes = document.toBytes(storageLevel);
        output.writeVarInt(docBytes.length, true);
        output.write(docBytes);
    }

    public long position() {
        return output.total();
    }

    public void close() {
       output.writeVarInt(0,true);
       output.close();
    }
}

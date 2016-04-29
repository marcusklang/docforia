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

import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.DocumentStorageLevel;
import se.lth.cs.docforia.io.DocumentIO;
import se.lth.cs.docforia.io.DocumentReader;
import se.lth.cs.docforia.io.DocumentWriter;
import se.lth.cs.docforia.io.file.DocumentFileReader;
import se.lth.cs.docforia.io.file.DocumentFileWriter;
import se.lth.cs.docforia.io.mem.Input;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Memory document I/O
 */
public class MemoryDocumentIO implements DocumentIO {
    @Override
    public String toJson(Document doc, DocumentStorageLevel opt) {
        if(doc instanceof MemoryDocument) {
            return MemoryJsonLevel0Codec.INSTANCE.encode((MemoryDocument)doc);
        } else {
            throw new IllegalArgumentException("doc is not a MemoryDocument, it is a: " + doc.getClass().getName());
        }
    }

    @Override
    public MemoryDocument fromJson(String json) {
        return MemoryJson.decodeJson(json);
    }

    @Override
    public byte[] toBytes(Document doc, DocumentStorageLevel opt) {
        if(doc instanceof MemoryDocument) {
            return MemoryBinary.encode(opt, (MemoryDocument)doc);
        } else {
            throw new IllegalArgumentException("doc is not a MemoryDocument, it is a: " + doc.getClass().getName());
        }
    }

    @Override
    public MemoryDocument fromBytes(byte[] bytes) {
        return MemoryBinary.decode(bytes);
    }

    @Override
    public MemoryDocument fromBytes(byte[] bytes, int offset, int length) {
        return MemoryBinary.decode(new Input(bytes, offset, length));
    }

    @Override
    public MemoryDocument fromBuffer(ByteBuffer buffer) {
        return buffer.hasArray() ? fromBytes(buffer.array(), buffer.arrayOffset(), buffer.remaining()) : null;
    }

    @Override
    public DocumentReader createReader(File location) {
        return new DocumentFileReader(location);
    }

    @Override
    public DocumentWriter createWriter(File location) {
        try {
            return new DocumentFileWriter(location);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private static final MemoryDocumentIO INSTANCE = new MemoryDocumentIO();

    public static MemoryDocumentIO getInstance() {
        return INSTANCE;
    }
}

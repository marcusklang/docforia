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
import se.lth.cs.docforia.io.mem.Input;
import se.lth.cs.docforia.io.mem.Output;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Memory Document implementation
 */
public class MemoryDocument extends Document implements Externalizable {
    protected MemoryDocumentStore store;
    protected MemoryDocumentEngine engine;
    protected MemoryDocumentRepresentations instances = new MemoryDocumentRepresentations(this);

    protected MemoryDocument(MemoryDocumentStore store) {
        this.store = store;
        this.store.doc = this;
        this.engine = new MemoryDocumentEngine(store);
    }

    public MemoryDocument() {
        this.store = new MemoryDocumentStore();
        this.store.doc = this;
        this.engine = new MemoryDocumentEngine(store);
    }

    public MemoryDocument(String text) {
        if(text == null)
            throw new NullPointerException("text");

        this.store = new MemoryDocumentStore();
        this.store.doc = this;
        this.store.setText(text);
        this.engine = new MemoryDocumentEngine(store);
    }

    public MemoryDocument(String id, String text) {
        if(id == null)
            throw new NullPointerException("id");

        if(text == null)
            throw new NullPointerException("text");

        this.store = new MemoryDocumentStore();
        this.store.doc = this;
        this.setId(id);
        this.store.setText(text);
        this.engine = new MemoryDocumentEngine(store);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        byte[] data = MemoryDocumentIO.getInstance().toBytes(this, DocumentStorageLevel.LEVEL_2);
        Output.writeVarInt(out, data.length, true);
        out.write(data);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int size = Input.readVarInt(in, true);
        byte[] data = new byte[size];
        in.readFully(data);

        MemoryDocument doc = MemoryDocumentIO.getInstance().fromBytes(data);
        this.engine = doc.engine;
        this.instances = doc.instances;
        this.store = doc.store;
        this.store.doc = this;
    }

    @Override
    public MemoryDocumentRepresentations representations() {
        return instances;
    }

    @Override
    public final MemoryDocumentStore store() {
        return store;
    }

    @Override
    public MemoryDocumentEngine engine() {
        return engine;
    }

    @Override
    public MemoryDocumentFactory factory() {
        return MemoryDocumentFactory.getInstance();
    }

    /**
     * Read document from bytes
     */
    public static MemoryDocument fromBytes(byte[] bytes) {
        return MemoryBinary.decode(bytes);
    }

    public static MemoryDocument fromJson(String json) {
        return MemoryJson.decodeJson(json);
    }

}

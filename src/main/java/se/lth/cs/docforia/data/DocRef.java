package se.lth.cs.docforia.data;
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
import se.lth.cs.docforia.memstore.MemoryBinary;
import se.lth.cs.docforia.memstore.MemoryDocument;
import se.lth.cs.docforia.memstore.MemoryDocumentFactory;

/**
 * Document container
 */
public class DocRef extends CoreRef{
    protected MemoryDocument doc;
    protected byte[] packed;

    @Override
    public CoreRefType id() {
        return CoreRefType.DOCUMENT;
    }

    public DocRef(byte[] packed) {
        this.packed = packed;
    }

    public DocRef(Document doc) {
        if(doc == null)
            throw new NullPointerException("doc");

        if(!(doc instanceof MemoryDocument))
            doc = Document.convert(MemoryDocumentFactory.getInstance(), doc);

        this.doc = (MemoryDocument)doc;
    }

    public DocRef(MemoryDocument doc) {
        this.doc = doc;
    }

    private Document unpack() {
        if(doc == null) {
            this.doc = MemoryBinary.decode(packed);
            this.packed = null;
            return doc;
        } else {
            return doc;
        }
    }

    @Override
    public DataRef copy() {
        return new DocRef(Document.convert(MemoryDocumentFactory.getInstance(), unpack()));
    }

    @Override
    public String stringValue() {
        return unpack().getText();
    }

    public Document documentValue() {
        return unpack();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DocRef docRef = (DocRef) o;

        if(docRef.packed == null && this.packed == null) {
            return docRef.doc.deepEquals(this.doc);
        }
        else if(docRef.packed != null && this.packed == null) {
            return docRef.unpack().deepEquals(this.doc);
        }
        else if(docRef.packed == null) {
            return docRef.doc.deepEquals(this.unpack());
        }
        else {
            return docRef.doc.deepEquals(this.doc);
        }
    }

    @Override
    public int hashCode() {
        return unpack().hashCode();
    }

    @Override
    public void write(CoreRefWriter writer) {
        writer.write((MemoryDocument)documentValue());
    }

    @Override
    public byte[] binaryValue() {
        if(packed != null)
            return packed;
        else
            return doc.toBytes();
    }
}

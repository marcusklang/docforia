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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.io.mem.Input;
import se.lth.cs.docforia.io.mem.Output;
import se.lth.cs.docforia.memstore.*;

import java.io.IOError;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Document[] container
 */
public class DocArrayRef extends CoreRef {
    protected MemoryDocument[] docs;
    protected byte[] packed;
    protected int count;

    @Override
    public CoreRefType id() {
        return CoreRefType.DOCUMENT_ARRAY;
    }

    public DocArrayRef(Document[] docs) {
        this.docs = new MemoryDocument[docs.length];
        for (int i = 0; i < docs.length; i++) {
            if(docs[i] == null)
                throw new NullPointerException("docs[" + i + "] == null");

            if(docs[i] instanceof MemoryDocument)
                this.docs[i] = (MemoryDocument)docs[i];
            else
                this.docs[i] = (MemoryDocument)Document.convert(MemoryDocumentFactory.getInstance(), docs[i]);
        }
    }

    public DocArrayRef(byte[] packed, int count) {
        this.packed = packed;
        this.count = count;
    }

    @Override
    public String stringValue() {
        StringBuilder sb = new StringBuilder();
        for (MemoryDocument doc : docs) {
            sb.append(doc.text()).append("\n");
        }

        return sb.toString();
    }

    private Document[] unpack() {
        if(packed != null) {
            docs = new MemoryDocument[count];

            Input reader = new Input(packed);
            for(int i = 0; i < count; i++) {
                int size = reader.readVarInt(true);
                docs[i] = MemoryBinary.decode(new Input(packed, reader.position(), size));
            }

            packed = null;
            return docs;
        }
        else
            return docs;
    }

    public Document[] arrayValue() {
        return unpack();
    }

    @Override
    public DataRef copy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] binaryValue() {
        if(packed != null)
            return packed;
        else {
            Output writer = new Output(512,2<<29); //512 byte - 1 GB
            write(writer);
            return writer.toBytes();
        }
    }

    public static DocArrayRef read(Input reader) {
        int count = reader.readVarInt(true);
        int size = reader.readVarInt(true);
        return new DocArrayRef(reader.readBytes(size), count);
    }

    public static DocArrayRef readJson(JsonNode node) {
        node = node.path("docarray");
        MemoryDocument[] array = new MemoryDocument[node.size()];
        Iterator<JsonNode> iter = node.elements();
        int i = 0;
        while(iter.hasNext()) {
            array[i++] = MemoryJson.decodeJson(iter.next());
        }

        return new DocArrayRef(array);
    }


    @Override
    public void write(Output writer) {
        if(packed != null) {
            writer.writeVarInt(count, true);
            writer.writeVarInt(packed.length, true);
            writer.writeBytes(packed);
        } else {
            writer.writeVarInt(docs.length, true);

            Output docwriter = new Output(512,2<<29);
            for (MemoryDocument doc : docs) {
                byte[] docs = doc.toBytes();
                docwriter.writeVarInt(docs.length, true);
                docwriter.writeBytes(docs);
            }

            writer.writeVarInt(docwriter.position(), true);
            docwriter.writeTo(writer);
        }
    }

    @Override
    public void write(JsonGenerator jsonWriter) {
        try {
            jsonWriter.writeStartObject();
            jsonWriter.writeObjectFieldStart("docarray");

            jsonWriter.writeStartArray(docs.length);
            for (Document document : unpack()) {
                MemoryJsonLevel0Codec.INSTANCE.encode((MemoryDocument)document, jsonWriter);
            }
            jsonWriter.writeEndArray();

            jsonWriter.writeEndObject();
            jsonWriter.writeEndObject();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DocArrayRef that = (DocArrayRef) o;
        if(that.packed == null && this.packed != null) {
            if(that.docs.length != this.count)
                return false;

            Document[] thatArray = that.docs;
            this.unpack();

            for(int i = 0; i < thatArray.length; i++) {
                if(!thatArray[i].deepEquals(this.docs[i]))
                    return false;
            }

            return true;
        }
        else if(that.packed != null && this.packed == null) {
            if(that.count != this.docs.length)
                return false;

            Document[] thatArray = that.unpack();
            for(int i = 0; i < thatArray.length; i++){
                if(!thatArray[i].deepEquals(this.docs[i]))
                    return false;
            }

            return true;
        }
        else if(that.packed == null) {
            Document[] thatArray = that.docs;
            if(thatArray.length != this.docs.length)
                return false;

            for(int i = 0; i < thatArray.length; i++){
                if(!thatArray[i].deepEquals(this.docs[i]))
                    return false;
            }

            return true;
        }
        else {
            return that.count == this.count && Arrays.equals(that.packed, this.packed);
        }
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(unpack());
        return result;
    }
}

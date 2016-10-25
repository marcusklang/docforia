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
import se.lth.cs.docforia.io.mem.Output;
import se.lth.cs.docforia.memstore.MemoryDocument;

public class BinaryCoreWriter implements CoreRefWriter {
    private Output writer;

    public Output getWriter() {
        return writer;
    }

    public BinaryCoreWriter(Output writer) {
        this.writer = writer;
    }

    @Override
    public void write(DataRef ref) {
        throw new UnsupportedOperationException("Writing a generic DataRef is not supported!");
    }

    @Override
    public void write(byte[] binary) {
        writer.writeVarInt(binary.length, true);
        writer.writeBytes(binary);
    }

    @Override
    public void write(String string) {
        writer.writeString(string);
    }

    @Override
    public void write(boolean boolValue) {
        writer.writeByte(boolValue ? 1 : 0);
    }

    @Override
    public void write(int intValue) {
        writer.writeInt(intValue);
    }

    @Override
    public void write(long longValue) {
        writer.writeLong(longValue);
    }

    @Override
    public void write(float floatValue) {
        writer.writeFloat(floatValue);
    }

    @Override
    public void write(double doubleValue) {
        writer.writeDouble(doubleValue);
    }

    @Override
    public void write(MemoryDocument doc) {
        byte[] data = doc.toBytes();
        writer.writeVarInt(data.length,true);
        writer.writeBytes(data);
    }

    @Override
    public void write(PropertyMap propertyMap) {
        writer.writeVarInt(propertyMap.properties.size(), true);
        propertyMap.properties.keySet().forEach(writer::writeString);

        for (String s : propertyMap.properties.keySet()) {
            DataRef ref = propertyMap.properties.get(s);
            if(ref instanceof CoreRef) {
                writer.writeByte(((CoreRef) ref).id().value);
                ((CoreRef) ref).write(this);
            } else {
                throw new UnsupportedOperationException("Only core properties are supported with PropertyMap!");
            }
        }
    }

    @Override
    public void writeBooleanArray(boolean[] boolValues) {
        writer.writeVarInt(boolValues.length, true);
        for (boolean b : boolValues) {
            writer.writeByte(b ? 1 : 0);
        }
    }

    @Override
    public void writeIntArray(int[] intValues) {
        writer.writeVarInt(intValues.length, true);
        for (int value : intValues) {
            writer.writeInt(value);
        }
    }

    @Override
    public void writeLongArray(long[] longValues) {
        writer.writeVarInt(longValues.length, true);
        for (long value : longValues) {
            writer.writeLong(value);
        }
    }

    @Override
    public void writeFloatArray(float[] floatValues) {
        writer.writeVarInt(floatValues.length, true);
        for (float value : floatValues) {
            writer.writeFloat(value);
        }
    }

    @Override
    public void writeDoubleArray(double[] doubleValues) {
        writer.writeVarInt(doubleValues.length,true);
        for (double value : doubleValues) {
            writer.writeDouble(value);
        }
    }

    @Override
    public void writeDocumentArray(MemoryDocument[] docValues) {
        writer.writeVarInt(docValues.length, true);

        Output docwriter = new Output(512,2<<29);
        for (Document doc : docValues) {
            byte[] docs = doc.toBytes();
            docwriter.writeVarInt(docs.length, true);
            docwriter.writeBytes(docs);
        }

        writer.writeVarInt(docwriter.position(), true);
        docwriter.writeTo(writer);
    }

    @Override
    public void writeStringArray(String[] stringValues) {
        writer.writeVarInt(stringValues.length, true);
        for (String s : stringValues) {
            writer.writeString(s);
        }
    }
}

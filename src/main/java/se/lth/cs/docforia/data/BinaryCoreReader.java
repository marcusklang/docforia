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

import se.lth.cs.docforia.io.mem.Input;

public class BinaryCoreReader implements CoreRefReader {
    private Input reader;

    /** Get CoreRef type from byte id
     *
     * @throws UnsupportedOperationException thrown if not supported or unknown.
     **/
    public static CoreRefType fromByteValue(byte id) {
        int idc = id & 0xFF;
        if(idc <= CoreRefType.DOCUMENT_ARRAY.value)
            return CoreRefType.values()[id];
        else if(idc == 0xFF)
            return CoreRefType.RESERVED;
        else
            throw new UnsupportedOperationException("Unknown type: " + idc);
    }

    public BinaryCoreReader(Input reader) {
        this.reader = reader;
    }

    public CoreRef read() {
        return read(reader.readByte());
    }

    public CoreRef read(byte id) {
        return read(fromByteValue(id));
    }

    public IntRef readInt() {
        return new IntRef(reader.readInt());
    }

    public StringRef readString() {
        return new StringRef(reader.readString());
    }

    @Override
    public StringArrayRef readStringArray() {
        String[] data = new String[reader.readVarInt(true)];
        for (int i = 0; i < data.length; i++) {
            data[i] = reader.readString();
        }
        return new StringArrayRef(data);
    }

    public PropertyMapRef readPropertyMap() {
        int numProperties = reader.readVarInt(true);
        String[] keys = new String[numProperties];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = reader.readString();
        }

        PropertyMap map = new PropertyMap();

        for (String key : keys) {
            map.putProperty(key, read(fromByteValue(reader.readByte())));
        }

        return new PropertyMapRef(map);
    }

    public BooleanRef readBoolean() {
        return reader.readByte() == 1 ? BooleanRef.TRUE : BooleanRef.FALSE;
    }

    public LongRef readLong() {
        return new LongRef(reader.readLong());
    }

    public FloatRef readFloat() {
        return new FloatRef(reader.readFloat());
    }

    public DoubleRef readDouble() {
        return new DoubleRef(reader.readDouble());
    }

    public BinaryRef readBinary() {
        int size = reader.readVarInt(true);
        byte[] data = new byte[size];
        reader.readBytes(data);
        return new BinaryRef(data);
    }

    public DocRef readDocument() {
        return new DocRef(reader.readBytes(reader.readVarInt(true)));
    }

    public BooleanArrayRef readBooleanArray() {
        int size = reader.readVarInt(true);
        boolean[] data = new boolean[size];
        for (int i = 0; i < size; i++) {
            data[i] = reader.readByte() == 1;
        }
        return new BooleanArrayRef(data);
    }

    public IntArrayRef readIntArray() {
        int[] data = new int[reader.readVarInt(true)];
        for (int i = 0; i < data.length; i++) {
            data[i] = reader.readInt();
        }

        return new IntArrayRef(data);
    }

    public LongArrayRef readLongArray() {
        long[] data = new long[reader.readVarInt(true)];
        for (int i = 0; i < data.length; i++) {
            data[i] = reader.readLong();
        }
        return new LongArrayRef(data);
    }

    public FloatArrayRef readFloatArray() {
        float[] data = new float[reader.readVarInt(true)];
        for (int i = 0; i < data.length; i++) {
            data[i] = reader.readFloat();
        }

        return new FloatArrayRef(data);
    }

    public DoubleArrayRef readDoubleArray() {
        double[] data = new double[reader.readVarInt(true)];
        for (int i = 0; i < data.length; i++) {
            data[i] = reader.readDouble();
        }

        return new DoubleArrayRef(data);
    }

    public DocArrayRef readDocumentArray() {
        int count = reader.readVarInt(true);
        int size = reader.readVarInt(true);
        return new DocArrayRef(reader.readBytes(size), count);
    }
}

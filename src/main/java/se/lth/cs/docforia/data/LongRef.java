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

/**
 * long container
 */
public class LongRef extends CoreRef {
    protected long value;

    @Override
    public CoreRefType id() {
        return CoreRefType.LONG;
    }

    public LongRef(long value) {
        this.value = value;
    }

    @Override
    public String stringValue() {
        return String.valueOf(value);
    }

    @Override
    public int intValue() {
        return (int)value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public char charValue() {
        return (char)value;
    }

    @Override
    public double doubleValue() {
        return (double)value;
    }

    @Override
    public float floatValue() {
        return (float)value;
    }

    @Override
    public DataRef copy() {
        return this; //immutable reference
    }

    @Override
    public byte[] binaryValue() {
        byte[] buf = new byte[8];
        buf[0] = (byte)value;
        buf[1] = (byte)(value >> 8);
        buf[2] = (byte)(value >> 16);
        buf[3] = (byte)(value >> 24);
        buf[4] = (byte)(value >> 32);
        buf[5] = (byte)(value >> 40);
        buf[6] = (byte)(value >> 48);
        buf[7] = (byte)(value >> 56);
        return buf;
    }

    @Override
    public void write(CoreRefWriter writer) {
        writer.write(value);
    }

    @Override
    public boolean equals(Object o) {
        return this == o
                || o != null
                && o instanceof DataRef
                && (((DataRef) o).longValue() == value);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (value ^ (value >>> 32));
        return result;
    }
}

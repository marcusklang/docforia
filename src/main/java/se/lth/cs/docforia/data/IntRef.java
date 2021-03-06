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
 * int container
 */
public class IntRef extends CoreRef {
    protected int value;

    @Override
    public CoreRefType id() {
        return CoreRefType.INT;
    }

    public IntRef(int value) {
        this.value = value;
    }

    @Override
    public String stringValue() {
        return String.valueOf(value);
    }

    @Override
    public int intValue() {
        return value;
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
        byte[] buf = new byte[4];
        buf[0] = (byte)value;
        buf[1] = (byte)(value >> 8);
        buf[2] = (byte)(value >> 16);
        buf[3] = (byte)(value >> 24);
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
                && (((DataRef) o).intValue() == value);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + value;
        return result;
    }
}

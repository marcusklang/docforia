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
import se.lth.cs.docforia.io.mem.Input;
import se.lth.cs.docforia.io.mem.Output;

import java.io.IOError;
import java.io.IOException;

/**
 * double container
 */
public class DoubleRef extends CoreRef {
    protected double value;

    @Override
    public CoreRefType id() {
        return CoreRefType.DOUBLE;
    }

    public DoubleRef(double value) {
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
        return (long)value;
    }

    @Override
    public boolean booleanValue() {
        return value >= 0.5;
    }

    @Override
    public double doubleValue() {
        return value;
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
        long binary = Double.doubleToRawLongBits(value);
        byte[] buf = new byte[8];
        buf[0] = (byte)binary;
        buf[1] = (byte)(binary >> 8);
        buf[2] = (byte)(binary >> 16);
        buf[3] = (byte)(binary >> 24);
        buf[4] = (byte)(binary >> 32);
        buf[5] = (byte)(binary >> 40);
        buf[6] = (byte)(binary >> 48);
        buf[7] = (byte)(binary >> 56);
        return buf;
    }

    @Override
    public void write(JsonGenerator jsonWriter) {
        try {
            jsonWriter.writeNumber(value);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static DoubleRef read(Input reader) {
        return new DoubleRef(reader.readDouble());
    }

    public static DoubleRef readJson(JsonNode node) {
        return new DoubleRef(node.doubleValue());
    }

    @Override
    public void write(Output writer) {
        writer.writeDouble(value);
    }

    @Override
    public boolean equals(Object o) {
        return this == o
                || o != null
                && o instanceof DataRef
                && Double.compare(((DataRef) o).doubleValue(), value) == 0;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(value);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}

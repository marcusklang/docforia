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
import java.util.Arrays;
import java.util.Iterator;

/**
 * double[] container
 */
public class DoubleArrayRef extends CoreRef {
    protected double[] values;

    @Override
    public CoreRefType id() {
        return CoreRefType.DOUBLE_ARRAY;
    }

    public DoubleArrayRef(double[] values) {
        if(values == null)
            throw new NullPointerException("values");

        this.values = values;
    }

    @Override
    public DataRef copy() {
        return new DoubleArrayRef(Arrays.copyOf(values, values.length));
    }

    @Override
    public String stringValue() {
        return Arrays.toString(values);
    }

    public double[] arrayValue() {
        return values;
    }

    public static DoubleArrayRef read(Input reader) {
        double[] data = new double[reader.readVarInt(true)];
        for (int i = 0; i < data.length; i++) {
            data[i] = reader.readDouble();
        }

        return new DoubleArrayRef(data);
    }

    public static DoubleArrayRef readJson(JsonNode node) {
        node = node.path("doublearray");
        double[] array = new double[node.size()];
        Iterator<JsonNode> iter = node.elements();
        int i = 0;
        while(iter.hasNext()) {
            array[i++] = iter.next().doubleValue();
        }

        return new DoubleArrayRef(array);
    }

    @Override
    public void write(Output writer) {
        writer.writeVarInt(values.length,true);
        for (double value : values) {
            writer.writeDouble(value);
        }
    }

    @Override
    public void write(JsonGenerator jsonWriter) {
        try {
            jsonWriter.writeStartObject();
            jsonWriter.writeArrayFieldStart("doublearray");
            for (double value : values) {
                jsonWriter.writeNumber(value);
            }
            jsonWriter.writeEndArray();
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

        DoubleArrayRef that = (DoubleArrayRef) o;

        return Arrays.equals(values, that.values);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(values);
        return result;
    }
}

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

/** boolean[] container */
public class BooleanArrayRef extends CoreRef {
    private boolean[] array;

    @Override
    public CoreRefType id() {
        return CoreRefType.BOOLEAN_ARRAY;
    }

    public BooleanArrayRef(boolean[] array) {
        if(array == null)
            throw new NullPointerException("values");

        this.array = array;
    }

    @Override
    public String stringValue() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if(array.length > 0) {
            sb.append(array[0] ? 1 : 0);
            for(int i = 1; i < array.length; i++) {
                sb.append(",");
                sb.append(array[i] ? 1 : 0);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public DataRef copy() {
        return new BooleanArrayRef(Arrays.copyOf(array, array.length));
    }

    public boolean[] arrayValue() {
        return array;
    }

    public static BooleanArrayRef read(Input reader) {
        int size = reader.readVarInt(true);
        boolean[] data = new boolean[size];
        for (int i = 0; i < size; i++) {
            data[i] = reader.readByte() == 1;
        }
        return new BooleanArrayRef(data);
    }

    public static BooleanArrayRef readJson(JsonNode node) {
        node = node.path("boolarray");
        boolean[] array = new boolean[node.size()];
        Iterator<JsonNode> iter = node.elements();
        int i = 0;
        while(iter.hasNext()) {
            array[i++] = iter.next().asInt() == 1;
        }

        return new BooleanArrayRef(array);
    }

    @Override
    public void write(Output writer) {
        writer.writeVarInt(array.length, true);
        for (boolean b : array) {
            writer.writeByte(b ? 1 : 0);
        }
    }

    @Override
    public void write(JsonGenerator jsonWriter) {
        try {
            jsonWriter.writeStartObject();
            jsonWriter.writeObjectFieldStart("boolarray");
            jsonWriter.writeStartArray(array.length);
            for (boolean b : array) {
                jsonWriter.writeNumber(b ? 1 : 0);
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

        BooleanArrayRef that = (BooleanArrayRef) o;

        return Arrays.equals(array, that.array);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(array);
        return result;
    }
}

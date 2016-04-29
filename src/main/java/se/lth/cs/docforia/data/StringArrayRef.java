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
 * String[] container
 */
public class StringArrayRef extends CoreRef {
    protected String[] data;

    @Override
    public CoreRefType id() {
        return CoreRefType.STRING_ARRAY;
    }

    public StringArrayRef(String[] data) {
        if(data == null)
            throw new NullPointerException("data");

        this.data = data;
    }

    public String[] arrayValue() {
        return data;
    }

    @Override
    public DataRef copy() {
        return new StringArrayRef(Arrays.copyOf(data, data.length));
    }

    @Override
    public String stringValue() {
        return Arrays.toString(data);
    }

    public static StringArrayRef read(Input reader) {
        String[] data = new String[reader.readVarInt(true)];
        for (int i = 0; i < data.length; i++) {
            data[i] = reader.readString();
        }
        return new StringArrayRef(data);
    }

    @Override
    public void write(Output writer) {
        writer.writeVarInt(data.length, true);
        for (String s : data) {
            writer.writeString(s);
        }
    }

    public static StringArrayRef readJson(JsonNode node) {
        node = node.path("stringarray");
        String[] array = new String[node.size()];
        Iterator<JsonNode> iter = node.elements();
        int i = 0;
        while(iter.hasNext()) {
            array[i++] = iter.next().asText();
        }

        return new StringArrayRef(array);
    }

    @Override
    public void write(JsonGenerator jsonWriter) {
        try {
            jsonWriter.writeStartObject();
            jsonWriter.writeFieldName("stringarray");
            jsonWriter.writeStartArray(data.length);
            for (String s : data) {
                jsonWriter.writeString(s);
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

        StringArrayRef that = (StringArrayRef) o;
        return Arrays.equals(data, that.data);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}

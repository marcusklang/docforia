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
import java.util.Iterator;
import java.util.Map;

/**
 * PropertyMap container
 */
public class PropertyMapRef extends CoreRef {
    protected PropertyMap map;

    @Override
    public CoreRefType id() {
        return CoreRefType.PROPERTY_MAP;
    }

    public PropertyMapRef(PropertyMap map) {
        if(map == null)
            throw new NullPointerException("map");

        this.map = map;
    }

    @Override
    public String stringValue() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, DataRef> entry : map.properties()) {
            if(first) {
               first = false;
            } else {
               sb.append(", ");
            }
            sb.append(entry.getKey()).append(" -> ").append(entry.getValue().stringValue());
        }
        sb.append("}");
        return sb.toString();
    }

    public PropertyMap value() {
        return map;
    }

    @Override
    public DataRef copy() {
        PropertyMap copy = new PropertyMap();
        for (Map.Entry<String, DataRef> entry : map.properties()) {
            copy.putProperty(entry.getKey(), entry.getValue().copy());
        }

        return new PropertyMapRef(copy);
    }

    @Override
    public byte[] binaryValue() {
        Output writer = new Output(512,2<<29);
        write(writer);
        return writer.toBytes();
    }

    public static PropertyMapRef read(Input reader) {
        int numProperties = reader.readVarInt(true);
        String[] keys = new String[numProperties];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = reader.readString();
        }

        PropertyMap map = new PropertyMap();

        for (String key : keys) {
            map.putProperty(key, CoreRefType.fromByteValue(reader.readByte()).read(reader));
        }

        return new PropertyMapRef(map);
    }

    public static PropertyMapRef readJson(JsonNode node) {
        node = node.path("prop");
        PropertyMap map = new PropertyMap();

        Iterator<Map.Entry<String, JsonNode>> fieldIter = node.fields();
        while(fieldIter.hasNext()) {
            Map.Entry<String, JsonNode> entry = fieldIter.next();
            map.putProperty(entry.getKey(), CoreRefType.fromJsonValue(entry.getValue()).readJson(entry.getValue()));
        }

        return new PropertyMapRef(map);
    }

    @Override
    public void write(Output writer) {
        writer.writeVarInt(map.properties.size(), true);
        map.properties.keySet().forEach(writer::writeString);

        for (String s : map.properties.keySet()) {
            DataRef ref = map.properties.get(s);
            if(ref instanceof CoreRef) {
                writer.writeByte(((CoreRef) ref).id().value);
                ((CoreRef) ref).write(writer);
            } else {
                throw new UnsupportedOperationException("Only core properties are supported with PropertyMap!");
            }
        }
    }

    @Override
    public void write(JsonGenerator jsonWriter) {
        try {
            jsonWriter.writeStartObject();
            jsonWriter.writeObjectFieldStart("prop");
            jsonWriter.writeStartObject();
            for (String s : map.properties.keySet()) {
                jsonWriter.writeObjectFieldStart(s);

                DataRef ref = map.properties.get(s);
                if(ref instanceof CoreRef) {
                    ((CoreRef) ref).write(jsonWriter);
                } else {
                    throw new UnsupportedOperationException("Only core properties are supported with PropertyMap!");
                }
            }
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

        PropertyMapRef that = (PropertyMapRef) o;

        return map.equals(that.map);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + map.hashCode();
        return result;
    }
}

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

import com.fasterxml.jackson.databind.JsonNode;
import se.lth.cs.docforia.memstore.MemoryDocument;
import se.lth.cs.docforia.memstore.MemoryJson;

import java.io.IOError;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * JSON Reader
 */
public class JsonCoreReader implements CoreRefReader {
    private JsonNode node;

    /** Get CoreRef type from json value
     *
     * @param node The JSON node to probe
     * @throws UnsupportedOperationException thrown if not supported or unknown.
     */
    public static CoreRefType fromJsonValue(JsonNode node) {
        if(node.isTextual()) {
            return CoreRefType.STRING;
        }
        else if(node.isObject()) {
            Map.Entry<String, JsonNode> data = node.fields().next();
            switch (data.getKey()) {
                case "binary":
                    return CoreRefType.BINARY;
                case "stringarray":
                    return CoreRefType.STRING_ARRAY;
                case "doc":
                    return CoreRefType.DOCUMENT;
                case "docarray":
                    return CoreRefType.DOCUMENT_ARRAY;
                case "boolarray":
                    return CoreRefType.BOOLEAN_ARRAY;
                case "intarray":
                    return CoreRefType.INT_ARRAY;
                case "longarray":
                    return CoreRefType.LONG_ARRAY;
                case "floatarray":
                    return CoreRefType.FLOAT_ARRAY;
                case "doublearray":
                    return CoreRefType.DOUBLE_ARRAY;
                case "prop":
                    return CoreRefType.PROPERTY_MAP;
                default:
                    throw new UnsupportedOperationException("Unknown format: " + data.getKey());
            }
        }
        else if(node.isBoolean()) {
            return CoreRefType.BOOLEAN;
        }
        else if(node.isLong())
        {
            return CoreRefType.LONG;
        }
        else if(node.isInt())
        {
            return CoreRefType.INT;
        }
        else if(node.isDouble())
        {
            return CoreRefType.DOUBLE;
        }
        else if(node.isFloat())
        {
            return CoreRefType.FLOAT;
        }
        else {
            throw new UnsupportedOperationException("Unknown json format: " + node.toString());
        }
    }

    public CoreRef read(JsonNode currentNode) {
        this.node = currentNode;
        return read(fromJsonValue(node));
    }

    @Override
    public StringRef readString() {
        return new StringRef(node.textValue());
    }

    @Override
    public StringArrayRef readStringArray() {
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
    public PropertyMapRef readPropertyMap() {
        JsonNode propnode = node.path("prop");
        PropertyMap map = new PropertyMap();

        Iterator<Map.Entry<String, JsonNode>> fieldIter = propnode.fields();
        while(fieldIter.hasNext()) {
            Map.Entry<String, JsonNode> entry = fieldIter.next();
            map.putProperty(entry.getKey(), read(entry.getValue()));
        }

        return new PropertyMapRef(map);
    }

    @Override
    public BooleanRef readBoolean() {
        return node.asBoolean() ? BooleanRef.TRUE : BooleanRef.FALSE;
    }

    @Override
    public IntRef readInt() {
        return new IntRef(node.intValue());
    }

    @Override
    public LongRef readLong() {
        return new LongRef(node.longValue());
    }

    @Override
    public FloatRef readFloat() {
        return new FloatRef(node.floatValue());
    }

    @Override
    public DoubleRef readDouble() {
        return new DoubleRef(node.doubleValue());
    }

    @Override
    public BinaryRef readBinary() {
        try {
            return new BinaryRef(node.path("binary").binaryValue());
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public DocRef readDocument() {
        return new DocRef(MemoryJson.decodeJson(node.path("doc")));
    }

    @Override
    public BooleanArrayRef readBooleanArray() {
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
    public IntArrayRef readIntArray() {
        node = node.path("intarray");
        int[] array = new int[node.size()];
        Iterator<JsonNode> iter = node.elements();
        int i = 0;
        while(iter.hasNext()) {
            array[i++] = iter.next().intValue();
        }

        return new IntArrayRef(array);
    }

    @Override
    public LongArrayRef readLongArray() {
        node = node.path("longarray");
        long[] array = new long[node.size()];
        Iterator<JsonNode> iter = node.elements();
        int i = 0;
        while(iter.hasNext()) {
            array[i++] = iter.next().asLong();
        }

        return new LongArrayRef(array);
    }

    @Override
    public FloatArrayRef readFloatArray() {
        node = node.path("floatarray");
        float[] array = new float[node.size()];
        Iterator<JsonNode> iter = node.elements();
        int i = 0;
        while(iter.hasNext()) {
            array[i++] = iter.next().floatValue();
        }

        return new FloatArrayRef(array);
    }

    @Override
    public DoubleArrayRef readDoubleArray() {
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
    public DocArrayRef readDocumentArray() {
        node = node.path("docarray");
        MemoryDocument[] array = new MemoryDocument[node.size()];
        Iterator<JsonNode> iter = node.elements();
        int i = 0;
        while(iter.hasNext()) {
            array[i++] = MemoryJson.decodeJson(iter.next());
        }

        return new DocArrayRef(array);
    }
}

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
import se.lth.cs.docforia.io.mem.Input;

import java.util.Map;

/**
 * CoreRef factory enumeration
 */
public enum CoreRefType implements CoreRefJsonReader, CoreRefBinaryReader {
    NULL            (0, null, null),
    BINARY          (1, BinaryRef::read,        BinaryRef::readJson),
    STRING          (2, StringRef::read,        StringRef::readJson),
    INT             (3, IntRef::read,           IntRef::readJson),
    LONG            (4, LongRef::read,          LongRef::readJson),
    FLOAT           (5, FloatRef::read,         FloatRef::readJson),
    DOUBLE          (6, DoubleRef::read,        DoubleRef::readJson),
    BOOLEAN         (7, BooleanRef::read,       BooleanRef::readJson),
    STRING_ARRAY    (8, StringArrayRef::read,   StringArrayRef::readJson),
    INT_ARRAY       (9, IntArrayRef::read,      IntArrayRef::readJson),
    LONG_ARRAY      (10, LongArrayRef::read,    LongArrayRef::readJson),
    FLOAT_ARRAY     (11, FloatArrayRef::read,   FloatArrayRef::readJson),
    DOUBLE_ARRAY    (12, DoubleArrayRef::read,  DoubleArrayRef::readJson),
    BOOLEAN_ARRAY   (13, BooleanArrayRef::read, BooleanArrayRef::readJson),
    PROPERTY_MAP    (14, PropertyMapRef::read,  PropertyMapRef::readJson),
    DOCUMENT        (15, DocRef::read,          DocRef::readJson),
    DOCUMENT_ARRAY  (16, DocArrayRef::read,     DocArrayRef::readJson),
    RESERVED        (255, null, null);

    public final byte value;
    protected final CoreRefBinaryReader binaryReader;
    protected final CoreRefJsonReader jsonReader;

    CoreRefType(int value, CoreRefBinaryReader binaryReader, CoreRefJsonReader jsonReader) {
        this.value = (byte)value;
        this.binaryReader = binaryReader;
        this.jsonReader = jsonReader;
    }

    /** Get CoreRef type from byte id
     *
     * @throws UnsupportedOperationException thrown if not supported or unknown.
     **/
    public static CoreRefType fromByteValue(byte id) {
        int idc = id & 0xFF;
        if(idc <= DOCUMENT_ARRAY.value)
            return CoreRefType.values()[id];
        else if(idc == 0xFF)
            return RESERVED;
        else
            throw new UnsupportedOperationException("Unknown type: " + idc);
    }

    /** Get CoreRef type from json value
     *
     * @param node The JSON node to probe
     * @throws UnsupportedOperationException thrown if not supported or unknown.
     */
    public static CoreRefType fromJsonValue(JsonNode node) {
        if(node.isTextual()) {
            return STRING;
        }
        else if(node.isObject()) {
            Map.Entry<String, JsonNode> data = node.fields().next();
            switch (data.getKey()) {
                case "binary":
                    return BINARY;
                case "stringarray":
                    return STRING_ARRAY;
                case "doc":
                    return DOCUMENT;
                case "docarray":
                    return DOCUMENT_ARRAY;
                case "boolarray":
                    return BOOLEAN_ARRAY;
                case "intarray":
                    return INT_ARRAY;
                case "longarray":
                    return LONG_ARRAY;
                case "floatarray":
                    return FLOAT_ARRAY;
                case "doublearray":
                    return DOUBLE_ARRAY;
                case "prop":
                    return PROPERTY_MAP;
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

    /** Read binary data */
    @Override
    public CoreRef read(Input reader) {
        return binaryReader.read(reader);
    }

    /** Read json */
    @Override
    public CoreRef readJson(JsonNode node) {
        return jsonReader.readJson(node);
    }
}

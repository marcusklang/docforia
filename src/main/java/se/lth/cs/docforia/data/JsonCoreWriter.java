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
import se.lth.cs.docforia.memstore.MemoryDocument;
import se.lth.cs.docforia.memstore.MemoryJsonLevel0Codec;

import java.io.IOError;
import java.io.IOException;

/**
 * Json Writer
 */
public class JsonCoreWriter implements CoreRefWriter {
    private JsonGenerator jsonWriter;

    public JsonCoreWriter(JsonGenerator jsonWriter) {
        this.jsonWriter = jsonWriter;
    }

    @Override
    public void write(byte[] binary) {
        try {
            jsonWriter.writeStartObject();
            jsonWriter.writeBinaryField("binary", binary);
            jsonWriter.writeEndObject();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void write(String string) {
        try {
            jsonWriter.writeString(string);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void write(boolean boolValue) {
        try {
            jsonWriter.writeBoolean(boolValue);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void write(int intValue) {
        try {
            jsonWriter.writeNumber(intValue);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void write(long longValue) {
        try {
            jsonWriter.writeNumber(longValue);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void write(float floatValue) {
        try {
            jsonWriter.writeNumber(floatValue);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void write(double doubleValue) {
        try {
            jsonWriter.writeNumber(doubleValue);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void write(MemoryDocument doc) {
        try {
            jsonWriter.writeStartObject();
            jsonWriter.writeFieldName("doc");

            MemoryJsonLevel0Codec.INSTANCE.encode(doc, jsonWriter);

            jsonWriter.writeEndObject();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void write(PropertyMap propertyMap) {
        try {
            jsonWriter.writeStartObject();
            jsonWriter.writeObjectFieldStart("prop");
            jsonWriter.writeStartObject();
            for (String s : propertyMap.properties.keySet()) {
                jsonWriter.writeObjectFieldStart(s);

                DataRef ref = propertyMap.properties.get(s);
                if(ref instanceof CoreRef) {
                    ((CoreRef) ref).write(this);
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
    public void writeBooleanArray(boolean[] boolValues) {
        try {
            jsonWriter.writeStartObject();
            jsonWriter.writeObjectFieldStart("boolarray");
            jsonWriter.writeStartArray(boolValues.length);
            for (boolean b : boolValues) {
                jsonWriter.writeNumber(b ? 1 : 0);
            }
            jsonWriter.writeEndArray();
            jsonWriter.writeEndObject();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void writeIntArray(int[] intValues) {
        try {
            jsonWriter.writeStartObject();
            jsonWriter.writeArrayFieldStart("intarray");

            for (int value : intValues) {
                jsonWriter.writeNumber(value);
            }

            jsonWriter.writeEndArray();
            jsonWriter.writeEndObject();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void writeLongArray(long[] longValues) {
        try {
            jsonWriter.writeStartObject();
            jsonWriter.writeArrayFieldStart("longarray");

            for (long value : longValues) {
                jsonWriter.writeNumber(value);
            }

            jsonWriter.writeEndArray();
            jsonWriter.writeEndObject();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void writeFloatArray(float[] floatValues) {
        try {
            jsonWriter.writeStartObject();
            jsonWriter.writeArrayFieldStart("floatarray");
            for (float value : floatValues) {
                jsonWriter.writeNumber(value);
            }
            jsonWriter.writeEndArray();
            jsonWriter.writeEndObject();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void writeDoubleArray(double[] doubleValues) {
        try {
            jsonWriter.writeStartObject();
            jsonWriter.writeArrayFieldStart("doublearray");
            for (double value : doubleValues) {
                jsonWriter.writeNumber(value);
            }
            jsonWriter.writeEndArray();
            jsonWriter.writeEndObject();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void writeDocumentArray(MemoryDocument[] docValues) {
        try {
            jsonWriter.writeStartObject();
            jsonWriter.writeObjectFieldStart("docarray");

            jsonWriter.writeStartArray(docValues.length);
            for (MemoryDocument document : docValues) {
                MemoryJsonLevel0Codec.INSTANCE.encode(document, jsonWriter);
            }
            jsonWriter.writeEndArray();

            jsonWriter.writeEndObject();
            jsonWriter.writeEndObject();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public void writeStringArray(String[] stringValues) {
        try {
            jsonWriter.writeStartObject();
            jsonWriter.writeFieldName("stringarray");
            jsonWriter.writeStartArray(stringValues.length);
            for (String s : stringValues) {
                jsonWriter.writeString(s);
            }
            jsonWriter.writeEndArray();
            jsonWriter.writeEndObject();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}

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
 * String container
 */
public class StringRef extends CoreRef {
    protected String value;

    @Override
    public CoreRefType id() {
        return CoreRefType.STRING;
    }

    public StringRef(String value) {
        if(value == null)
            throw new NullPointerException("value");

        this.value = value;
    }

    @Override
    public String stringValue() {
        return value;
    }

    @Override
    public DataRef copy() {
        return this; //immutable reference
    }

    @Override
    public void write(Output writer) {
        writer.writeString(value);
    }

    public static StringRef read(Input reader) {
        return new StringRef(reader.readString());
    }

    public static StringRef readJson(JsonNode node) {
        return new StringRef(node.textValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StringRef stringRef = (StringRef) o;

        return value.equals(stringRef.value);
    }

    @Override
    public void write(JsonGenerator jsonWriter) {
        try {
            jsonWriter.writeString(value);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}

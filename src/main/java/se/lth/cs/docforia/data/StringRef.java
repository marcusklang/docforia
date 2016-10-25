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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StringRef stringRef = (StringRef) o;

        return value.equals(stringRef.value);
    }

    @Override
    public void write(CoreRefWriter writer) {
        writer.write(value);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}

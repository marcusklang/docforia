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

import java.util.Arrays;

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

    @Override
    public void write(CoreRefWriter writer) {
        writer.writeStringArray(data);
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

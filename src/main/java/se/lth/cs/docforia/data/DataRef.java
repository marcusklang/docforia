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

import java.nio.charset.Charset;

/**
 * Base class for all data containers
 */
public abstract class DataRef {
    public abstract String stringValue();

    public int intValue() {
        return Integer.parseInt(stringValue());
    }

    public long longValue() {
        return Long.parseLong(stringValue());
    }

    public float floatValue() {
        return Float.parseFloat(stringValue());
    }

    public double doubleValue() {
        return Double.parseDouble(stringValue());
    }

    public char charValue() {
        String t = stringValue();
        return t.isEmpty() ? '\0' : t.charAt(0);
    }

    public boolean booleanValue() {
        return intValue() == 1;
    }

    /** Cached UTF8 charset */
    protected final static Charset UTF8 = Charset.forName("utf-8");

    public byte[] binaryValue() {
        return stringValue().getBytes(UTF8);
    }

    public abstract DataRef copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataRef dataRef = (DataRef) o;

        return stringValue().equals(dataRef.stringValue());

    }

    @Override
    public int hashCode() {
        return stringValue().hashCode();
    }

    @Override
    public String toString() {
        return stringValue();
    }
}

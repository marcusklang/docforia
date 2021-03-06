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

/** boolean container */
public class BooleanRef extends CoreRef {
    public final static BooleanRef TRUE = new BooleanRef(true);
    public final static BooleanRef FALSE = new BooleanRef(false);

    protected boolean value;
    protected byte[] binary;

    @Override
    public CoreRefType id() {
        return CoreRefType.BOOLEAN;
    }

    private BooleanRef(boolean value) {
        this.value = value;
        this.binary = new byte[] { value ? (byte)1 : (byte)0 };
    }

    public static BooleanRef valueOf(boolean bool) {
        return bool ? TRUE : FALSE;
    }

    @Override
    public String stringValue() {
        return value ? "true" : "false";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BooleanRef that = (BooleanRef) o;

        return value == that.value;
    }

    @Override
    public void write(CoreRefWriter writer) {
        writer.write(value);
    }

    @Override
    public byte[] binaryValue() {
        return this.binary;
    }

    @Override
    public int intValue() {
        return value ? 1 : 0;
    }

    @Override
    public long longValue() {
        return value ? 1L : 0L;
    }

    @Override
    public float floatValue() {
        return value ? 1.0f : 0.0f;
    }

    @Override
    public double doubleValue() {
        return value ? 1.0 : 0.0;
    }

    @Override
    public char charValue() {
        return value ? '1' : '0';
    }

    @Override
    public boolean booleanValue() {
        return value;
    }

    @Override
    public DataRef copy() {
        return this; //Immutable instance
    }

    @Override
    public int hashCode() {
        return (value ? 1 : 0);
    }
}

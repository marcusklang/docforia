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

/** byte[] container */
public class BinaryRef extends CoreRef {
    protected byte[] data;

    @Override
    public CoreRefType id() {
        return CoreRefType.BINARY;
    }

    public BinaryRef(byte[] data) {
        this.data = data;
    }

    private static final char[] hex = new char[]
            {'0', '1', '2', '3', '4', '5', '6', '7','8', '9', 'a', 'b', 'c','d','e','f'};

    @Override
    public String stringValue() {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(hex[b & 0x0F]).append(hex[(b >> 4) & 0x0F]);
        }

        return sb.toString();
    }

    @Override
    public DataRef copy() {
        return new BinaryRef(Arrays.copyOf(data, data.length));
    }

    public byte[] binaryValue() {
        return data;
    }

    @Override
    public void write(CoreRefWriter writer) {
        writer.write(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BinaryRef binaryRef = (BinaryRef) o;

        return Arrays.equals(data, binaryRef.data);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}

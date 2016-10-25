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

/** boolean[] container */
public class BooleanArrayRef extends CoreRef {
    private boolean[] array;

    @Override
    public CoreRefType id() {
        return CoreRefType.BOOLEAN_ARRAY;
    }

    public BooleanArrayRef(boolean[] array) {
        if(array == null)
            throw new NullPointerException("values");

        this.array = array;
    }

    @Override
    public String stringValue() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if(array.length > 0) {
            sb.append(array[0] ? 1 : 0);
            for(int i = 1; i < array.length; i++) {
                sb.append(",");
                sb.append(array[i] ? 1 : 0);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public DataRef copy() {
        return new BooleanArrayRef(Arrays.copyOf(array, array.length));
    }

    public boolean[] arrayValue() {
        return array;
    }

    @Override
    public void write(CoreRefWriter writer) {
        writer.writeBooleanArray(array);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BooleanArrayRef that = (BooleanArrayRef) o;

        return Arrays.equals(array, that.array);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(array);
        return result;
    }
}

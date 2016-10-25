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
 * float[] container
 */
public class FloatArrayRef extends CoreRef {
    protected float[] values;

    @Override
    public CoreRefType id() {
        return CoreRefType.FLOAT_ARRAY;
    }

    public FloatArrayRef(float[] values) {
        if(values == null)
            throw new NullPointerException("values");

        this.values = values;
    }

    @Override
    public DataRef copy() {
        return new FloatArrayRef(Arrays.copyOf(values,values.length));
    }

    @Override
    public String stringValue() {
        return Arrays.toString(values);
    }

    public float[] arrayValue() {
        return values;
    }

    @Override
    public void write(CoreRefWriter writer) {
        writer.writeFloatArray(values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        FloatArrayRef that = (FloatArrayRef) o;

        return Arrays.equals(values, that.values);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(values);
        return result;
    }
}

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

import se.lth.cs.docforia.io.mem.Output;

import java.util.Map;

/**
 * PropertyMap container
 */
public class PropertyMapRef extends CoreRef {
    protected PropertyMap map;

    @Override
    public CoreRefType id() {
        return CoreRefType.PROPERTY_MAP;
    }

    public PropertyMapRef(PropertyMap map) {
        if(map == null)
            throw new NullPointerException("map");

        this.map = map;
    }

    @Override
    public String stringValue() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, DataRef> entry : map.properties()) {
            if(first) {
               first = false;
            } else {
               sb.append(", ");
            }
            sb.append(entry.getKey()).append(" -> ").append(entry.getValue().stringValue());
        }
        sb.append("}");
        return sb.toString();
    }

    public PropertyMap value() {
        return map;
    }

    @Override
    public DataRef copy() {
        PropertyMap copy = new PropertyMap();
        for (Map.Entry<String, DataRef> entry : map.properties()) {
            copy.putProperty(entry.getKey(), entry.getValue().copy());
        }

        return new PropertyMapRef(copy);
    }

    @Override
    public byte[] binaryValue() {
        BinaryCoreWriter writer = new BinaryCoreWriter(new Output(512,2<<29));
        writer.write(map);
        return writer.getWriter().toBytes();
    }

    @Override
    public void write(CoreRefWriter writer) {
        writer.write(map);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PropertyMapRef that = (PropertyMapRef) o;

        return map.equals(that.map);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + map.hashCode();
        return result;
    }
}

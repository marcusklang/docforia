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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import se.lth.cs.docforia.PropertyStore;

import java.util.Map;

/**
 * Property Map, String -&gt; DataRef
 */
public class PropertyMap extends PropertyStore {
    protected Object2ObjectOpenHashMap<String,DataRef> properties = new Object2ObjectOpenHashMap<>();

    public PropertyMap() {

    }

    public PropertyMap(Object2ObjectOpenHashMap<String, DataRef> properties) {
        if(properties == null)
            throw new NullPointerException("properties");

        this.properties = properties;
    }

    @Override
    public void putProperty(String key, DataRef ref) {
        properties.put(key, ref);
    }

    @Override
    public <T extends DataRef> T getRefProperty(String key) {
        return (T)properties.get(key);
    }

    @Override
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    @Override
    public void removeProperty(String key) {
        properties.remove(key);
    }

    @Override
    public int numProperties() {
        return properties.size();
    }

    @Override
    public Iterable<Map.Entry<String, DataRef>> properties() {
        return properties.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertyMap that = (PropertyMap) o;

        return properties.equals(that.properties);

    }

    @Override
    public int hashCode() {
        return properties.hashCode();
    }
}

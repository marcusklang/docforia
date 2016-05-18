package se.lth.cs.docforia;

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
import se.lth.cs.docforia.data.DataRef;
import se.lth.cs.docforia.data.Decoder;
import se.lth.cs.docforia.data.Encoder;
import se.lth.cs.docforia.data.PropertyMap;

/**
 * Property container
 * @param <RETVAL> return class type in fluent interfaces
 */
public interface PropertyStoreProxy<RETVAL extends PropertyStoreProxy<RETVAL>> {
    PropertyStore store();

    /**
     * Get property
     * @param key          key
     * @param defaultValue default value to return
     * @return value or default value if property does not exist
     */
    default String getPropertyOrDefault(String key, String defaultValue) {
        String value = getProperty(key);
        if(value != null)
            return value;
        else
            return defaultValue;
    }

    /**
     * Get property
     * @param key key
     * @return null or value
     */
	default String getProperty(String key) {
        return store().getProperty(key);
    }

    default <T> T getProperty(String key, Decoder<T> decoder) {
        return store().getProperty(key, decoder);
    }

    default <T> T getProperty(String key, T reuse, Decoder<T> decoder) {
        return store().getProperty(key, reuse, decoder);
    }

    default <T extends DataRef> T getRefProperty(String key) {
        return store().getRefProperty(key);
    }

    default <T extends DataRef> T getRefProperty(String key, Class<T> type) {
        return store().getRefProperty(key, type);
    }

    default char getCharProperty(String key) {
        return store().getCharProperty(key);
    }

    default int getIntProperty(String key) {
        return store().getIntProperty(key);
    }

    default long getLongProperty(String key) {
        return store().getLongProperty(key);
    }

    default float getFloatProperty(String key) {
        return store().getFloatProperty(key);
    }

    default double getDoubleProperty(String key) {
        return store().getDoubleProperty(key);
    }

    default boolean getBooleanProperty(String key){
        return store().getBooleanProperty(key);
    }

    default byte[] getBinaryProperty(String key) {
        return store().getBinaryProperty(key);
    }

    default int[] getIntArrayProperty(String key) {
        return store().getIntArrayProperty(key);
    }

    default long[] getLongArrayProperty(String key) {
        return store().getLongArrayProperty(key);
    }

    default float[] getFloatArrayProperty(String key) {
        return store().getFloatArrayProperty(key);
    }

    default double[] getDoubleArrayProperty(String key) {
        return store().getDoubleArrayProperty(key);
    }

    default String[] getStringArrayProperty(String key) {
        return store().getStringArrayProperty(key);
    }

    default PropertyMap getPropertyMapProperty(String key) {
        return store().getPropertyMapProperty(key);
    }

    default Document getDocumentProperty(String key) {
        return store().getDocumentProperty(key);
    }

    default Document[] getDocumentArrayProperty(String key) {
        return store().getDocumentArrayProperty(key);
    }

    default boolean hasProperty(String key) {
        return store().hasProperty(key);
    }

    default RETVAL putProperty(String key, DataRef value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    /**
     * Add/Replace property
     * @param key   key
     * @param value value
     * @return this representation
     */
	default RETVAL putProperty(String key, String value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

	default RETVAL putProperty(String key, char ch) {
        store().putProperty(key, ch);
        return (RETVAL)this;
    }

    default RETVAL putProperty(String key, int value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    default RETVAL putProperty(String key, long value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    default RETVAL putProperty(String key, boolean value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    default RETVAL putProperty(String key, float value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    default RETVAL putProperty(String key, double value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    default RETVAL putProperty(String key, byte[] value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    default RETVAL putProperty(String key, int[] value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    default RETVAL putProperty(String key, long[] value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    default RETVAL putProperty(String key, float[] value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    default RETVAL putProperty(String key, double[] value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    default RETVAL putProperty(String key, boolean[] value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    default RETVAL putProperty(String key, String[] value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    default  RETVAL putProperty(String key, PropertyMap value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    default RETVAL putProperty(String key, Document value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    default RETVAL putProperty(String key, Document[] value) {
        store().putProperty(key, value);
        return (RETVAL)this;
    }

    default RETVAL migrateProperty(String oldKey, String newKey) {
        DataRef value = getRefProperty(oldKey);
        if(value != null)
        {
            putProperty(newKey, value);
            removeProperty(oldKey);
        }

        return (RETVAL)this;
    }

    default <T> RETVAL putProperty(String key, T value, Encoder<T> encoder) {
        store().putProperty(key, value, encoder);
        return (RETVAL)this;
    }

	default RETVAL removeProperty(String key) {
        store().removeProperty(key);
        return (RETVAL)this;
    }
}

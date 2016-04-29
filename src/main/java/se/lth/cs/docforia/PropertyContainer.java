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
public interface PropertyContainer<RETVAL extends PropertyContainer<RETVAL>> {
	String getProperty(String key);

    <T> T getProperty(String key, Decoder<T> decoder);
    <T> T getProperty(String key, T reuse, Decoder<T> decoder);

    <T extends DataRef> T getRefProperty(String key);
    <T extends DataRef> T getRefProperty(String key, Class<T> type);

    char getCharProperty(String key);
    int getIntProperty(String key);
    long getLongProperty(String key);
    float getFloatProperty(String key);
    double getDoubleProperty(String key);
    boolean getBooleanProperty(String key);

    byte[] getBinaryProperty(String key);
    int[] getIntArrayProperty(String key);
    long[] getLongArrayProperty(String key);
    float[] getFloatArrayProperty(String key);
    double[] getDoubleArrayProperty(String key);
    String[] getStringArrayProperty(String key);

    PropertyMap getPropertyMapProperty(String key);
    Document getDocumentProperty(String key);
    Document[] getDocumentArrayProperty(String key);

    boolean hasProperty(String key);

    RETVAL putProperty(String key, DataRef value);
	RETVAL putProperty(String key, String value);

	RETVAL putProperty(String key, char ch);
	RETVAL putProperty(String key, int value);
	RETVAL putProperty(String key, long value);
	RETVAL putProperty(String key, boolean value);
	RETVAL putProperty(String key, float value);
	RETVAL putProperty(String key, double value);

	RETVAL putProperty(String key, byte[] value);
	RETVAL putProperty(String key, int[] value);
	RETVAL putProperty(String key, long[] value);
	RETVAL putProperty(String key, float[] value);
	RETVAL putProperty(String key, double[] value);
	RETVAL putProperty(String key, boolean[] value);
	RETVAL putProperty(String key, String[] value);

    RETVAL putProperty(String key, PropertyMap value);
    RETVAL putProperty(String key, Document value);
    RETVAL putProperty(String key, Document[] value);

    <T> RETVAL putProperty(String key, T value, Encoder<T> encoder);

	void removeProperty(String key);
}

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

import se.lth.cs.docforia.data.*;

import java.util.Map;

/** Property base class */
public abstract class PropertyStore {
	public String getProperty(String key) {
		DataRef val = getRefProperty(key);
		return val == null ? null : val.stringValue();
	}

	public abstract <T extends DataRef> T getRefProperty(String key);

	public <T extends DataRef> T getRefProperty(String key, Class<T> type) {
		DataRef ref = getRefProperty(key);
		if(!ref.getClass().isAssignableFrom(type))
			throw new AssertionError("Property is not of expected type, expected: " + type.getName() + ", actual: " + ref.getClass().getName());

		return (T)ref;
	}

	public <T> T getProperty(String key, Decoder<T> decoder) {
		DataRef value = getRefProperty(key);
		if(value == null)
			return null;
		else
			return decoder.decode(this, key, value);
	}

	public <T> T getProperty(String key, T reuse, Decoder<T> decoder) {
		DataRef value = getRefProperty(key);
		if(value == null)
			return null;
		else
			return decoder.decode(this, key, value, reuse);
	}

	public char getCharProperty(String key) {
		DataRef val = getRefProperty(key);
		return val == null ? (char)0 : val.charValue();
	}

	public int getIntProperty(String key) {
		DataRef val = getRefProperty(key);
		return val == null ? 0 : val.intValue();
	}

	public long getLongProperty(String key) {
		DataRef val = getRefProperty(key);
		return val == null ? 0 : val.longValue();
	}

	public float getFloatProperty(String key) {
		DataRef val = getRefProperty(key);
		return val == null ? 0 : val.floatValue();
	}

	public double getDoubleProperty(String key) {
		DataRef val = getRefProperty(key);
		return val == null ? 0 : val.doubleValue();
	}

	public boolean getBooleanProperty(String key) {
		DataRef val = getRefProperty(key);
		return val != null && val.booleanValue();
	}

	public byte[] getBinaryProperty(String key) {
		DataRef val = getRefProperty(key);
		return val == null ? null : val.binaryValue();
	}

	public int[] getIntArrayProperty(String key) {
		IntArrayRef val = getRefProperty(key);
		return val == null ? null : val.arrayValue();
	}

	public long[] getLongArrayProperty(String key) {
		LongArrayRef val = getRefProperty(key);
		return val == null ? null : val.arrayValue();
	}

	public float[] getFloatArrayProperty(String key) {
		FloatArrayRef val = getRefProperty(key);
		return val == null ? null : val.arrayValue();
	}

	public double[] getDoubleArrayProperty(String key) {
		DoubleArrayRef val = getRefProperty(key);
		return val == null ? null : val.arrayValue();
	}

	public String[] getStringArrayProperty(String key) {
		StringArrayRef val = getRefProperty(key);
		return val == null ? null : val.arrayValue();
	}

    public PropertyMap getPropertyMapProperty(String key) {
        PropertyMapRef val = getRefProperty(key);
        return val == null ? null : val.value();
    }

    public Document getDocumentProperty(String key) {
        DocRef val = getRefProperty(key);
        return val == null ? null : val.documentValue();
    }

    public Document[] getDocumentArrayProperty(String key) {
        DocArrayRef val = getRefProperty(key);
        return val == null ? null : val.arrayValue();
    }

	public abstract boolean hasProperty(String key);

	public abstract int numProperties();

    public abstract void putProperty(String key, DataRef ref);

    public void putProperty(String key, String value) {
        putProperty(key, new StringRef(value));
    }

    public void putProperty(String key, char ch) {
		putProperty(key, String.valueOf(ch));
	}

	public void putProperty(String key, int value) {
		putProperty(key, new IntRef(value));
	}

	public void putProperty(String key, long value) {
		putProperty(key, new LongRef(value));
	}

	public void putProperty(String key, boolean value) {
		putProperty(key, value ? BooleanRef.TRUE : BooleanRef.FALSE);
	}

	public void putProperty(String key, float value) {
		putProperty(key, new FloatRef(value));
	}

	public void putProperty(String key, double value) {
		putProperty(key, new DoubleRef(value));
	}

	public void putProperty(String key, byte[] value) {
		putProperty(key, new BinaryRef(value));
	}

	public void putProperty(String key, int[] value) {
		putProperty(key, new IntArrayRef(value));
	}

	public void putProperty(String key, long[] value) {
		putProperty(key, new LongArrayRef(value));
	}

	public void putProperty(String key, float[] value) {
		putProperty(key, new FloatArrayRef(value));
	}

	public void putProperty(String key, double[] value) {
		putProperty(key, new DoubleArrayRef(value));
	}

	public void putProperty(String key, boolean[] value) {
		putProperty(key, new BooleanArrayRef(value));
	}

	public void putProperty(String key, String[] value) {
		putProperty(key, new StringArrayRef(value));
	}

    public void putProperty(String key, Document value) {
        putProperty(key, new DocRef(value));
    }

    public void putProperty(String key, Document[] value) {
        putProperty(key, new DocArrayRef(value));
    }

    public void putProperty(String key, PropertyMap value) {
        putProperty(key, new PropertyMapRef(value));
    }

	public <T> void putProperty(String key, T value, Encoder<T> encoder) {
		DataRef ref = encoder.encode(this, key, value);
		putProperty(key, ref);
	}

	public abstract void removeProperty(String key);

	public abstract Iterable<Map.Entry<String, DataRef>> properties();

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append("{ ");
		boolean first = true;
		for (Map.Entry<String, DataRef> stringDataRefEntry : properties()) {
			if (first)
				first = false;
			else
				sb.append(", ");

			sb.append(stringDataRefEntry.getKey())
			  .append("=")
			  .append(stringDataRefEntry.getValue().stringValue());
		}

		sb.append(" }");

		return sb.toString();
	}
}

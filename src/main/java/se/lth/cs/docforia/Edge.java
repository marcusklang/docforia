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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Edge abstraction
 * @param <T> The inherting class type, is used to allow chaining with type checking.
 */
public abstract class Edge<T extends Edge<T>> implements Iterable<Map.Entry<String, DataRef>>, PropertyContainer<T> {
	protected EdgeStore store;
	protected DocumentProxy doc;

	public Edge() {

	}

    public Edge(DocumentProxy doc) {
        doc.add(this);

    }
	
	public Edge(DocumentProxy doc, EdgeRef edge) {
		this.store = edge.get();
		this.doc = doc;
	}

    /** Called by the storage implementation, when this representation has been initialized. */
    protected void initialized() {

    }

	/**
	 * Get access to proxy
	 */
	public DocumentProxy getProxy() {
		return doc;
	}

	/**
	 * Get access to the document
	 * @return instance or null if no document exists.
     */
	public final Document getDocument() {
		return (doc instanceof DocumentTransaction) ? null : (Document)doc;
	}
	
	@SuppressWarnings("unchecked")
	public final <T extends Node> T getHead() {
		return (T)doc.representation(store.getHead());
	}
	
	@SuppressWarnings("unchecked")
	public final <T extends Node> T getTail() {
		return (T)doc.representation(store.getTail());
	}
	
	public String getProperty(String key) {
		return store.getProperty(key);
	}
	
	public T putProperty(String key, String value) {
		store.putProperty(key, value);;
		return (T)this;
	}

    @Override
    public <T extends DataRef> T getRefProperty(String key) {
        return store.getRefProperty(key);
    }

    @Override
    public <T extends DataRef> T getRefProperty(String key, Class<T> type) {
        return store.getRefProperty(key, type);
    }

    @Override
    public <T1> T1 getProperty(String key, Decoder<T1> decoder) {
        DataRef ref = getRefProperty(key);
        if(ref == null)
            return null;
        else
            return decoder.decode(store, key, ref);
    }

    @Override
    public <T1> T1 getProperty(String key, T1 reuse, Decoder<T1> decoder) {
        DataRef ref = getRefProperty(key);
        if(ref == null)
            return null;
        else
            return decoder.decode(store, key, ref, reuse);
    }

    @Override
    public char getCharProperty(String key) {
        return store.getCharProperty(key);
    }

    @Override
    public int getIntProperty(String key) {
        return store.getIntProperty(key);
    }

    @Override
    public long getLongProperty(String key) {
        return store.getLongProperty(key);
    }

    @Override
    public float getFloatProperty(String key) {
        return store.getFloatProperty(key);
    }

    @Override
    public double getDoubleProperty(String key) {
        return store.getDoubleProperty(key);
    }

    @Override
    public boolean getBooleanProperty(String key) {
        return store.getBooleanProperty(key);
    }

    @Override
    public byte[] getBinaryProperty(String key) {
        return store.getBinaryProperty(key);
    }

    @Override
    public int[] getIntArrayProperty(String key) {
        return store.getIntArrayProperty(key);
    }

    @Override
    public long[] getLongArrayProperty(String key) {
        return store.getLongArrayProperty(key);
    }

    @Override
    public float[] getFloatArrayProperty(String key) {
        return store.getFloatArrayProperty(key);
    }

    @Override
    public double[] getDoubleArrayProperty(String key) {
        return store.getDoubleArrayProperty(key);
    }

    @Override
    public String[] getStringArrayProperty(String key) {
        return store.getStringArrayProperty(key);
    }

    @Override
    public Document[] getDocumentArrayProperty(String key) {
        return store.getDocumentArrayProperty(key);
    }

    @Override
    public Document getDocumentProperty(String key) {
        return store.getDocumentProperty(key);
    }

    @Override
    public PropertyMap getPropertyMapProperty(String key) {
        return store.getPropertyMapProperty(key);
    }

    @Override
    public T putProperty(String key, DataRef value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public T putProperty(String key, char ch) {
        store.putProperty(key, ch);
        return (T)this;
    }

    @Override
    public T putProperty(String key, int value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public T putProperty(String key, long value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public T putProperty(String key, boolean value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public T putProperty(String key, float value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public T putProperty(String key, double value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public T putProperty(String key, byte[] value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public T putProperty(String key, int[] value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public T putProperty(String key, long[] value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public T putProperty(String key, float[] value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public T putProperty(String key, double[] value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public T putProperty(String key, boolean[] value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public T putProperty(String key, String[] value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public T putProperty(String key, PropertyMap value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public T putProperty(String key, Document value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public T putProperty(String key, Document[] value) {
        store.putProperty(key, value);
        return (T)this;
    }

    @Override
    public <T1> T putProperty(String key, T1 value, Encoder<T1> encoder) {
        store.putProperty(key, encoder.encode(store, key, value));
        return (T)this;
    }

    @Override
	public boolean hasProperty(String key) {
		return store.hasProperty(key);
        //return doc.propertyHook.has(doc, this, key);
	}

	public void removeProperty(String key) {
		store.removeProperty(key);
        //doc.propertyHook.remove(doc, this, key);
	}

	@SuppressWarnings("unchecked")
	public <T extends Node> T getOpposite(Node n) {
		if(n.getRef() == store.getHead())
			return (T)getTail();
		else
			return (T)getHead();
	}

	public T connect(Node tail, Node head) {
		store.connect(tail.store.getRef(), head.store.getRef());
		return (T)this;
	}

	public EdgeRef getRef() {
		return store.getRef();
	}

	public String getLayer() {
		return store.getLayer();
	}
	
	public String getVariant() {
		return store.getVariant();
	}
	
	public void setVariant(String variant) {
		store.setVariant(variant);
	}

    public boolean valid() {
        return this.store != null;
    }

	@Override
	public Iterator<Entry<String, DataRef>> iterator() {
		return new Iterator<Map.Entry<String,DataRef>>() {
			
			final Iterator<Map.Entry<String,DataRef>> iter = store.properties().iterator(); //doc.propertyHook.iterator(doc, Edge.this).iterator();
			Map.Entry<String,DataRef> current;
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			private boolean moveForward() {
				while(iter.hasNext()) {
					current = iter.next();
					if(current.getKey().charAt(0) != '_')
						return true;
				}
				
				current = null;
				return false;
			}
			
			@Override
			public Entry<String, DataRef> next() {
				if(current == null)
					moveForward();
				
				Map.Entry<String, DataRef> retVal = current;
				current = null;
				return retVal;
			}
			
			@Override
			public boolean hasNext() {
				if(current == null)
					return moveForward();
				else
					return true;
			}
		};
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Edge entries = (Edge) o;

        return store.equals(entries.store);
    }

    @Override
    public int hashCode() {
        return store.getRef().hashCode();
    }
}

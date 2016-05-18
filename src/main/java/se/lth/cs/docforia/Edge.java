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

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Edge abstraction
 * @param <T> The inherting class type, is used to allow chaining with type checking.
 */
public abstract class Edge<T extends Edge<T>> implements Iterable<Map.Entry<String, DataRef>>, PropertyStoreProxy<T> {
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

	@SuppressWarnings("unchecked")
	public <T extends Node> T getOpposite(Node n) {
		if(n.getRef() == store.getHead())
			return (T)getTail();
		else
			return (T)getHead();
	}

	public T connect(Node tail, Node head) {
		store.connect(tail.store, head.store);
		return (T)this;
	}

	public EdgeRef getRef() {
		return store;
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
    public PropertyStore store() {
        return store;
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
        return store.hashCode();
    }
}

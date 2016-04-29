package se.lth.cs.docforia.util;
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
import java.util.Iterator;

/**
 * Filter + Map iterable
 * @param <T> Input type
 * @param <M> Outut type
 */
public abstract class FilteredMappedDocumentIterable<T,M> extends DocumentIterableBase<T> implements DocumentIterable<T> {

	private final Iterable<M> iterable;
	
	public FilteredMappedDocumentIterable(Iterable<M> iterable) {
		if(iterable == null)
			throw new NullPointerException("iterable must not be null!");

		this.iterable = iterable;
	}
	
	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {

			private Iterator<M> iter = iterable.iterator();
			private M current;
			
			protected boolean moveForward() {
				while(iter.hasNext()) {
					current = iter.next();
					if(accept(current))
						return true;
				}
				return false;
			}
			
			@Override
			public boolean hasNext() {
				if(current == null)
					return moveForward();
				else
					return true;
			}

			@Override
			public T next() {
				if(current == null) {
					if(!moveForward()) {
						throw new IllegalStateException("next was called when hasNext returns false!");
					}
				}
				
				M retVal = current;
				current = null;
				return map(retVal);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public String toString() {
				return "FilteredMappedIterator<>(iter: " + iter.toString() + ", current: " + current + ")";
			}
		};
	}
	
	protected abstract boolean accept(M value);
	protected abstract T map(M value);

}

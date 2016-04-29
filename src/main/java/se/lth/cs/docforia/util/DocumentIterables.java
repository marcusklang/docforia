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
import java.util.NoSuchElementException;

/** Document iterables functionality */
public class DocumentIterables {
	public static <T> DocumentIterable<T> wrap(final Iterable<T> iterable) {
		if(iterable == null)
			throw new NullPointerException("iterable must not be null!");

		if(iterable instanceof DocumentIterable)
			return (DocumentIterable<T>)iterable;

        return new WrappedDocumentIterable<T>(iterable);
	}

	public static <T> DocumentIterable<T> concat(final Iterable<T>...iteratables) {
		return new DocumentIterableBase<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					int current = 0;
					Iterator<T> currIter = iteratables[0].iterator();
					T next;

					private boolean moveForward() {
						if(next != null)
							return true;

						while(current < iteratables.length) {
							if(currIter.hasNext()) {
								next = currIter.next();
								return true;
							}
							else {
								current++;
								if(current == iteratables.length)
									return false;

								currIter = iteratables[current].iterator();
							}
						}

						return false;
					}


					@Override
					public boolean hasNext() {
						return moveForward();
					}

					@Override
					public T next() {
						if(!moveForward())
							throw new NoSuchElementException();

						T retVal = next;
						next = null;
						return retVal;
					}

					@Override
					public void remove() {

					}
				};
			}
		};
	}

	public static <T> DocumentIterable<T> concat(final Iterable<Iterable<T>> iteratables) {
		return new DocumentIterableBase<T>() {
			@Override
			public Iterator<T> iterator() {
				return new Iterator<T>() {
					Iterator<Iterable<T>> iterators = iteratables.iterator();
					Iterator<T> currIter;
					T next;

					private boolean moveForward() {
						if(next != null)
							return true;

						if(currIter == null || !currIter.hasNext()) {
							while(iterators.hasNext()) {
								currIter = iterators.next().iterator();

								if(currIter.hasNext()) {
									next = currIter.next();
									return true;
								} else {
									currIter = null;
								}
							}
						} else {
							next = currIter.next();
						}

						return false;
					}

					@Override
					public boolean hasNext() {
						return moveForward();
					}

					@Override
					public T next() {
						if(!moveForward())
							throw new NoSuchElementException();

						final T retVal = next;
						next = null;
						return retVal;
					}

					@Override
					public void remove() {

					}
				};
			}
		};
	}
}

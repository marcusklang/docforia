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

import se.lth.cs.docforia.Window;

import java.util.*;
import java.util.function.Function;

/**
 * Base class for iterables
 * @param <T>
 */
public abstract class DocumentIterableBase<T> implements DocumentIterable<T>  {
	@Override
	public T first() {
		Iterator<T> iter = iterator();
		if(iter.hasNext())
			return iter.next();
		else
			return null;
	}
	
	@Override
	public boolean any() {
		Iterator<T> iter = iterator();
		return iter.hasNext();
	}
	
	@Override
	public List<T> toList() {
		ArrayList<T> list = new ArrayList<T>();
		
		for(T item : this)
			list.add(item);
		
		return list;
	}

    @Override
    public long count() {
        Iterator<T> iterator = iterator();
        long cnt = 0;
        while(iterator.hasNext())
        {
            iterator.next();
            cnt++;
        }

        return cnt;
    }

    @SuppressWarnings("unchecked")
    @Override
    public DocumentIterable<T> filter(final Function<T, Boolean> filter) {
        return new PredicateFilteredDocumentIterable<>(filter, this);
    }

    @Override
    public <M> DocumentIterable<M> map(final Function<T, M> mapper) {
        final DocumentIterableBase<T> source = this;
        return new DocumentIterableBase<M>() {
            @Override
            public Iterator<M> iterator() {
                return new Iterator<M>() {
                    private final Iterator<T> iter = source.iterator();

                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public M next() {
                        return mapper.apply(iter.next());
                    }

                    @Override
                    public void remove() {
                        iter.remove();
                    }
                };
            }
        };
    }

    @Override
    public DocumentIterable<Window<T>> window(final int n) {
        return DocumentIterables.wrap(new Iterable<Window<T>>() {

            @Override
            public Iterator<Window<T>> iterator() {
                final LinkedList<T> window = new LinkedList<T>();

                return new Iterator<Window<T>>() {
                    private Iterator<T> iter = DocumentIterableBase.this.iterator();
                    private boolean endFound = false;

                    private boolean moveForward() {
                        if(window.size() == n)
                            return true;
                        else
                        {
                            while(iter.hasNext() && window.size() < n) {
                                window.addLast(iter.next());
                            }

                            if(!endFound && window.size() < n) {
                                endFound = true;
                            }

                            return window.size() == n;
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        return moveForward();
                    }

                    @Override
                    public Window<T> next() {
                        if(window.size() != n)
                            if(!moveForward())
                                throw new NoSuchElementException();

                        Window<T> retval = Window.create(window);
                        window.removeFirst();

                        return retval;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

        });
    }

    @Override
    public DocumentIterable<Window<T>> window(final int n, final T padstart, final T padend) {
        return DocumentIterables.wrap(new Iterable<Window<T>>() {

            @Override
            public Iterator<Window<T>> iterator() {
                final LinkedList<T> window = new LinkedList<T>();

                window.add(padstart);

                return new Iterator<Window<T>>() {
                    private Iterator<T> iter = DocumentIterableBase.this.iterator();
                    private boolean endFound = false;

                    private boolean moveForward() {
                        if(window.size() == n)
                            return true;
                        else
                        {
                            while(iter.hasNext() && window.size() < n) {
                                window.addLast(iter.next());
                            }

                            if(!endFound && window.size() < n) {
                                window.add(padend);
                                endFound = true;
                            }

                            return window.size() == n;
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        return moveForward();
                    }

                    @Override
                    public Window<T> next() {
                        if(window.size() != n)
                            if(!moveForward())
                                throw new NoSuchElementException();

                        Window<T> retval = Window.create(window);
                        window.removeFirst();

                        return retval;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

        });
    }
}

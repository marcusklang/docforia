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
import java.util.function.Function;

/**
 * Predicate filtered document
 */
public class PredicateFilteredDocumentIterable<T> extends DocumentIterableBase {
    private final Function<T,Boolean> predicate;
    private final Iterable<T> source;

    public PredicateFilteredDocumentIterable(Function<T, Boolean> predicate, Iterable<T> source) {
        this.predicate = predicate;
        this.source = source;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private final Iterator<T> iter = source.iterator();
            private T next = null;

            private boolean moveForward() {
                if(next == null) {
                    while(iter.hasNext()) {
                        next = iter.next();
                        if(predicate.apply(next)) {
                            return true;
                        }
                    }

                    next = null;
                    return false;
                }
                else
                    return true;
            }

            @Override
            public boolean hasNext() {
                return moveForward();
            }

            @Override
            public T next() {
                if(next == null) {
                    if(!moveForward())
                        throw new NoSuchElementException();
                }

                T retVal = next;
                next = null;

                return retVal;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}

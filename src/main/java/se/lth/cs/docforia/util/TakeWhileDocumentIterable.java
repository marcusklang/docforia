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
 * Take while iterable
 */
public class TakeWhileDocumentIterable<T> extends DocumentIterableBase<T> {
    private final Function<T,Boolean> predicate;
    private final Iterable<T> iterables;

    public TakeWhileDocumentIterable(Function<T, Boolean> predicate, Iterable<T> iterables) {
        this.predicate = predicate;
        this.iterables = iterables;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            final Iterator<T> iterator = iterables.iterator();
            T next;
            boolean ended = false;

            private boolean moveForward() {
                if(!ended && next == null) {
                    if(iterator.hasNext()) {
                        next = iterator.next();
                        if(!predicate.apply(next)) {
                            ended = true;
                            next = null;
                            return false;
                        }
                        else {
                            return true;
                        }
                    }

                    return false;
                }
                else
                    return !ended;
            }


            @Override
            public boolean hasNext() {
                return moveForward();
            }

            @Override
            public T next() {
                if(!moveForward())
                    throw new NoSuchElementException();

                T retval = next;
                next = null;
                return retval;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}

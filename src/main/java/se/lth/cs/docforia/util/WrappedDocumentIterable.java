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
import java.util.List;

/**
 * Wraps iterables
 */
public class WrappedDocumentIterable<T> extends DocumentIterableBase<T> {
    protected final Iterable<T> base;
    protected final boolean isList;

    public WrappedDocumentIterable(Iterable<T> base) {
        isList = base instanceof List;

        this.base = base;
    }

    @Override
    public List<T> toList() {
        return isList ? (List<T>)base : super.toList();
    }

    @Override
    public Iterator<T> iterator() {
        return base.iterator();
    }
}

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

import java.util.List;
import java.util.function.Function;

/**
 * Iterable abstraction
 * @param <T>
 */
public interface DocumentIterable<T> extends Iterable<T> {
	T first();
	boolean any();
	List<T> toList();
    long count();
    <M> DocumentIterable<M> map(Function<T, M> mapper);
    DocumentIterable<T> filter(Function<T, Boolean> filter);
    DocumentIterable<Window<T>> window(int n, T padstart, T padend);
    DocumentIterable<Window<T>> window(int n);
}

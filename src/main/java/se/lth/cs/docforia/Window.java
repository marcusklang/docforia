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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Window implementation
 * @param <T>
 */
public abstract class Window<T> implements Iterable<T> {

	public abstract T[] asArray();
	public abstract List<T> asList();
	
	public static <T> Window<T> create(Iterable<? extends T> items) {
		return new ListWindow<T>(items);
	}
	
	public static <T> Window<T> create(T...items) {
		return new ArrayWindow<T>(items);
	}
	
	public abstract int size();
	
	protected static class ListWindow<T> extends Window<T> {
		private final ArrayList<T> items = new ArrayList<T>();
		
		public ListWindow(Iterable<? extends T> iter) {
			for(T item : iter) {
				items.add(item);
			}
		}

		@Override
		public int size() {
			return items.size();
		}

		@Override
		public Iterator<T> iterator() {
			return items.iterator();
		}

		@SuppressWarnings("unchecked")
		@Override
		public T[] asArray() {
			return items.toArray((T[])new Object[items.size()]);
		}

		@Override
		public List<T> asList() {
			return items;
		}
	}
	
	protected static class ArrayWindow<T> extends Window<T> {
		private final T[] items;
		
		public ArrayWindow(T... items) {
			this.items = items;
		}

		@Override
		public int size() {
			return items.length;
		}

		@Override
		public Iterator<T> iterator() {
			return Arrays.asList(items).iterator();
		}

		@Override
		public T[] asArray() {
			return items;
		}

		@Override
		public List<T> asList() {
			return Arrays.asList(items);
		}
		
		
	}
	
}

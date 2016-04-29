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

/**
 * General reference
 */
public interface StoreRef {
	/**
	 * Create/Get a string reference that uniquely points to this ref
	 * <b>Remarks: </b> Support depends on underlying storage engine, by default throws UnsupportedOperationException.
	 * @throws UnsupportedOperationException If unique referencing is not supported.
     */
	default String reference() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Checks if this reference is valid i.e. not removed.
	 */
	boolean valid();

	PropertyStore get();
}

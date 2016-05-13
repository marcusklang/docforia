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

/** Node Reference */
public interface NodeRef extends StoreRef, Range {
	/**
	 * Gets/Retrieves the underlying store
     */
	NodeStore get();

	/**
	 * Layer this node belongs to.
     */
	LayerRef layer();

	@Override
	default int length() {
		return getEnd()-getStart();
	}

	@Override
	default int getStart() {
		return get().getStart();
	}

	@Override
	default int getEnd() {
		return get().getEnd();
	}

	@Override
	default float getMidpoint() {
		return (getEnd()-getStart())/2.0f + getStart();
	}
}

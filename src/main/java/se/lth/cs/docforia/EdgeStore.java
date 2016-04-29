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
 * Edge storage abstraction
 * <p>
 * @see se.lth.cs.docforia.memstore.MemoryEdge for a memory based implementation
 **/
public abstract class EdgeStore extends PropertyStore {
    /** Get the head reference or null if it has not been set. */
	public abstract NodeRef getHead();

    /** Get the tail reference or null if it has not been set. */
	public abstract NodeRef getTail();

    /** Set the head */
	public abstract void setHead(NodeRef head);

    /** Set the tail */
	public abstract void setTail(NodeRef tail);

    /** Get the layer */
	public abstract String getLayer();

    /** Get the variant */
	public abstract String getVariant();

    /** Set the variant */
	public abstract void setVariant(String variant);

    /** Connect two nodes by this edge */
	public abstract void connect(NodeRef tail, NodeRef head);

    /** Get the original edge reference */
	public abstract EdgeRef getRef();
}

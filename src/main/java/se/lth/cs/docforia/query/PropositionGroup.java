package se.lth.cs.docforia.query;
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

import se.lth.cs.docforia.Edge;
import se.lth.cs.docforia.Node;

import java.util.*;

/**
 * Group result
 */
public class PropositionGroup implements Iterable<Proposition> {

    private final Proposition key;
    private final List<Proposition> children;

    public PropositionGroup(Proposition key, List<Proposition> children) {
        this.key = key;
        this.children = children;
    }

    public Proposition key() {
        return key;
    }

    public <N extends Node> N key(NodeTVar<N> var) {
        return key.get(var);
    }

    public <E extends Edge> E key(EdgeTVar<E> var) {
        return key.get(var);
    }

    public <N extends Node> N key(NodeVar var) {
        return (N) key.get(var);
    }

    public <E extends Edge> E key(EdgeVar var) {
        return (E) key.get(var);
    }

    public int size() {
        return children.size();
    }

    public Proposition value(int index) {
        return children.get(index);
    }

    public <N extends Node> N value(int index, NodeTVar<N> var) {
        return children.get(index).get(var);
    }

    public <E extends Edge> E value(int index, EdgeTVar<E> var) {
        return children.get(index).get(var);
    }

    private class NodeList<N extends Node> extends AbstractList<N> implements RandomAccess {

        private NodeTVar<N> var;

        public NodeList(NodeTVar<N> var) {
            this.var = var;
        }

        @Override
        public N get(int index) {
            return children.get(index).get(var);
        }

        @Override
        public int size() {
            return children.size();
        }
    }

    private class EdgeList<E extends Edge> extends AbstractList<E> implements RandomAccess {

        private EdgeTVar<E> var;

        public EdgeList(EdgeTVar<E> var) {
            this.var = var;
        }

        @Override
        public E get(int index) {
            return children.get(index).get(var);
        }

        @Override
        public int size() {
            return children.size();
        }
    }

    /**
     * Get a list of a specific node variable.
     * @param var node variable
     * @param <N> node type
     * @return  a lightweight read-only abstraction over internal store
     */
    public <N extends Node> List<N> list(NodeTVar<N> var) {
        return new NodeList<>(var);
    }

    /**
     * Get a list of a specific edge variable.
     * @param var edge variable
     * @param <E> edge type
     * @return  a lightweight read-only abstraction over internal store
     */
    public <E extends Edge> List<E> list(EdgeTVar<E> var) {
        return new EdgeList<>(var);
    }

    public <N extends Node> N value(int index, NodeVar var) {
        return (N)children.get(index).get(var);
    }

    public <E extends Edge> E value(int index, EdgeVar var) {
        return (E)children.get(index).get(var);
    }

    public List<Proposition> values() {
        return children;
    }

    public <N extends Node> Iterable<N> nodes(final NodeTVar<N> var) {
        return new Iterable<N>() {
            @Override
            public Iterator<N> iterator() {
                return new Iterator<N>() {
                    Iterator<Proposition> iter = values().iterator();

                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public N next() {
                        return iter.next().get(var);
                    }

                    @Override
                    public void remove() {
                        iter.remove();
                    }
                };
            }
        };
    }

    public <E extends Edge> Iterable<E> edges(final EdgeTVar<E> var) {
        return new Iterable<E>() {
            @Override
            public Iterator<E> iterator() {
                return new Iterator<E>() {
                    Iterator<Proposition> iter = values().iterator();

                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public E next() {
                        return iter.next().get(var);
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
    public Iterator<Proposition> iterator() {
        return children.iterator();
    }
}

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

import se.lth.cs.docforia.*;

import java.util.Arrays;
import java.util.Objects;

/**
 * Single result
 */
public class Proposition {
    private final Document doc;
    public StoreRef[] proposition;

    private Proposition(Document doc, StoreRef[] proposition) {
        this.proposition = proposition;
        this.doc = doc;
    }
    public Proposition(Document doc, int size) {
        this.proposition = new StoreRef[size];
        this.doc = doc;
    }

    public EdgeRef edgeref(Var var) {
        return (EdgeRef)proposition[var.index];
    }

    public NodeRef noderef(Var var) {
        return (NodeRef)proposition[var.index];
    }

    @SuppressWarnings("unchecked")
    public <T extends Edge> T get(EdgeVar var) {
        return (T)doc.representations().get(edgeref(var));
    }

    @SuppressWarnings("unchecked")
    public <T extends Node> T get(NodeVar var) {
        return (T)doc.representations().get(noderef(var));
    }

    @SuppressWarnings("unchecked")
    public <N extends Node> N get(NodeTVar<N> var) {
        return (N)doc.representations().get(noderef(var));
    }

    @SuppressWarnings("unchecked")
    public <E extends Edge> E get(EdgeTVar<E> var) {
        return (E)doc.representations().get(edgeref(var));
    }

    @SuppressWarnings("unchecked")
    public <T extends PropertyContainer<?>> T get(Var var) {
        StoreRef storeRef = proposition[var.index];

        if(storeRef instanceof NodeRef) {
            return (T)doc.representation((NodeRef) storeRef);
        }
        else if(storeRef instanceof EdgeRef) {
            return (T)doc.representation((EdgeRef)storeRef);
        }
        else
            throw new UnsupportedOperationException("Not supported.");
    }

    public Proposition copy() {
        return new Proposition(doc, Arrays.copyOf(proposition, proposition.length));
    }

    public <T> T get(int index) {
        return (T)proposition[index];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Proposition that = (Proposition) o;
        return  Objects.deepEquals(proposition, that.proposition);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(proposition);
    }
}

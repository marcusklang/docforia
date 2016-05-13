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

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import se.lth.cs.docforia.*;
import se.lth.cs.docforia.exceptions.QueryException;

import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * Single result
 */
public class Proposition {
    /** Contextual data for this proposition (internal) */
    public final PropositionContext context;

    /** The raw data array, indicies are provided by {@code context.indexOf(var)} */
    public final StoreRef[] data;

    private Proposition(PropositionContext context, StoreRef[] data) {
        this.context = context;
        this.data = data;
    }
    public Proposition(PropositionContext context) {
        this.context = context;
        this.data = new StoreRef[context.numVars()];
    }

    public EdgeRef edgeref(Var var) {
        return (EdgeRef)data[context.indexOf(var)];
    }

    public NodeRef noderef(Var var) {
        return (NodeRef)data[context.indexOf(var)];
    }

    @SuppressWarnings("unchecked")
    public <T extends Edge> T get(EdgeVar var) {
        return (T)context.getDocument().edge(edgeref(var));
    }

    @SuppressWarnings("unchecked")
    public <T extends Node> T get(NodeVar var) {
        return (T)context.getDocument().node(noderef(var));
    }

    @SuppressWarnings("unchecked")
    public <N extends Node> N get(NodeTVar<N> var) {
        return (N)context.getDocument().node(noderef(var));
    }

    @SuppressWarnings("unchecked")
    public <E extends Edge> E get(EdgeTVar<E> var) {
        return (E)context.getDocument().edge(edgeref(var));
    }

    @SuppressWarnings("unchecked")
    public <T extends PropertyContainer<?>> T get(Var var) {
        StoreRef storeRef = data[context.indexOf(var)];
        if(storeRef == null)
            throw new NoSuchElementException("No proposition for given var!");

        if(storeRef instanceof NodeRef) {
            return (T)context.getDocument().representation((NodeRef) storeRef);
        }
        else if(storeRef instanceof EdgeRef) {
            return (T)context.getDocument().representation((EdgeRef)storeRef);
        }
        else
            throw new UnsupportedOperationException("Not supported.");
    }

    public Proposition copy() {
        return new Proposition(context, Arrays.copyOf(data, data.length));
    }

    public void set(NodeVar var, StoreRef node) {
        data[context.indexOf(var)] = node;
    }

    public void set(NodeVar var, Node node) {
        data[context.indexOf(var)] = node.getRef();
    }

    public void set(EdgeVar var, Edge edge) {
        data[context.indexOf(var)] = edge.getRef();
    }

    public void set(Var var, StoreRef ref) {
        data[context.indexOf(var)] = ref;
    }

    public Proposition subset(Collection<? extends Var> vars) {
        Reference2IntOpenHashMap<Var> var2idx = new Reference2IntOpenHashMap<>();
        for (Var var : vars) {
            var2idx.put(var, var2idx.size());
        }

        Proposition prop = new Proposition(new PropositionContext(context.getDocument(), var2idx));

        int i = 0;
        for (Var var : vars) {
            prop.data[i++] = data[context.indexOf(var)];
        }

        return prop;
    }

    public Proposition subset(Var...vars) {
        Reference2IntOpenHashMap<Var> var2idx = new Reference2IntOpenHashMap<>();
        for (Var var : vars) {
            var2idx.put(var, var2idx.size());
        }

        Proposition prop = new Proposition(new PropositionContext(context.getDocument(), var2idx));

        int i = 0;
        for (Var var : vars) {
            prop.data[i++] = data[context.indexOf(var)];
        }

        return prop;
    }

    public Proposition subset(PropositionContext outputContext) {
        if(outputContext == context)
            return new Proposition(context, Arrays.copyOf(data,data.length));

        Proposition prop = new Proposition(outputContext);
        for (Reference2IntMap.Entry<Var> varEntry : outputContext.entries()) {
            int varIndex = context.indexOf(varEntry.getKey());
            if(varIndex == -1)
                throw new QueryException("Missing variable in select: " + varEntry.getKey().toString());

            prop.data[varEntry.getIntValue()] = data[varIndex];
        }

        return prop;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Proposition that = (Proposition) o;
        return Arrays.deepEquals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(data);
    }
}

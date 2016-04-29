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

import se.lth.cs.docforia.Node;
import se.lth.cs.docforia.query.NodeTVar;
import se.lth.cs.docforia.query.NodeVar;
import se.lth.cs.docforia.query.Proposition;

import java.util.function.Function;

/**
 * Concrete node mapper
 */
public class GetNode<N extends Node> implements Function<Proposition,N> {
    private final NodeVar var;

    public GetNode(NodeVar var) {
        this.var = var;
    }

    @Override
    public N apply(Proposition in) {
        return in.get(var);
    }

    public static <N extends Node> GetNode<N> of(NodeVar var) {
        return new GetNode<N>(var);
    }

    public static <N extends Node> GetNode<N> of(NodeTVar<N> var) {
        return new GetNode<N>(var);
    }
}

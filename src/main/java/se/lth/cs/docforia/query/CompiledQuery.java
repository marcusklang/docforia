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

import se.lth.cs.docforia.util.DocumentIterable;
import se.lth.cs.docforia.util.DocumentIterableBase;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Represents a compiled query, that can be used to create many streams from.
 */
public class CompiledQuery extends DocumentIterableBase<Proposition> implements DocumentIterable<Proposition> {
    private final QueryContext context;
    private final Set<Var> outputVars;
    private final Predicate[] predicates;

    public CompiledQuery(QueryContext context, Set<Var> outputVars, Predicate[] predicates) {
        this.context = context;
        this.outputVars = outputVars;
        this.predicates = predicates;
    }

    public Stream<Proposition> stream() {
        return StreamSupport.stream(new SpliteratableQuery(context, outputVars, predicates), false);
    }

    @Override
    public Iterator<Proposition> iterator() {
        return stream().iterator();
    }
}

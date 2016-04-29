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

/**
 * Typed edge query variable
 */
public class EdgeTVar<E extends Edge> extends EdgeVar {
    public EdgeTVar(String type) {
        super(type);
    }

    public EdgeTVar(String type, String variant) {
        super(type, variant);
    }

    public EdgeTVar(Class<E> edge) {
        super(edge);
    }

    public EdgeTVar(Class<E> edge, String variant) {
        super(edge, variant);
    }

    public <E extends Edge> EdgeTVar<E> var(Class<E> clazz) {
        return new EdgeTVar<E>(clazz);
    }

    public <E extends Edge> EdgeTVar<E> var(Class<E> clazz, String variant) {
        return new EdgeTVar<E>(clazz, variant);
    }
}

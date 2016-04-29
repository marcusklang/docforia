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
 * Edge query variable
 */
public class EdgeVar extends Var {

    protected final String type;
    protected final String variant;

    public <E extends Edge> EdgeVar(String type) {
        this(type, null);
    }

    public EdgeVar(String type, String variant) {
        this.type = "@" + type;
        this.variant = variant;
    }

    public <E extends Edge> EdgeVar(Class<E> edge) {
        this(edge, null);
    }

    public <E extends Edge> EdgeVar(Class<E> edge, String variant) {
        this.type = edge.getName();
        this.variant = variant;
    }

    @Override
    public String getLayer() {
        return type;
    }

    @Override
    public String getVariant() {
        return variant;
    }

    @Override
    public String toString() {
        return "EdgeVar(type: " + type + ", variant: " + String.valueOf(variant) + ")";
    }
}

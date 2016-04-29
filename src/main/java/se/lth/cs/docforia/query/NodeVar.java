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

import se.lth.cs.docforia.Node;
import se.lth.cs.docforia.NodeRef;

import java.util.Objects;

/**
 * Node variable
 */
public class NodeVar extends Var {

    protected final String type;
    protected final String variant;

    public NodeVar(String type) {
        this(type, null);
    }

    public NodeVar(String type, String variant) {
        this.type = type;
        this.variant = variant;
    }

    public <N extends Node> NodeVar(Class<N> type) {
        this(type, null);
    }

    public <N extends Node> NodeVar(Class<N> type, String variant) {
        this.type = type.getName();
        this.variant = variant;
    }

    public boolean equalsType(Node node) {
        return equalsType(node.getRef());
    }

    public boolean equalsType(NodeRef node) {
        return Objects.equals(node.get().getLayer(), type) && Objects.equals(node.get().getVariant(), variant);
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
        return "NodeVar(type: " + type + ", variant: " + String.valueOf(variant) + ")";
    }
}

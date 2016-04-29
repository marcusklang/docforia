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

/**
 * Typed node query variable
 */
public class NodeTVar<N extends Node> extends NodeVar {
    public NodeTVar(String type) {
        super(type);
    }

    public NodeTVar(String type, String variant) {
        super(type, variant);
    }

    public NodeTVar(Class<N> type) {
        super(type);
    }

    public NodeTVar(Class<N> type, String variant) {
        super(type, variant);
    }

    public <N extends Node> NodeTVar<N> var(Class<N> clazz) {
        return new NodeTVar<N>(clazz);
    }

    public <N extends Node> NodeTVar<N> var(Class<N> clazz, String variant) {
        return new NodeTVar<N>(clazz, variant);
    }
}

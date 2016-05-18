package se.lth.cs.docforia.query.filter;
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

import se.lth.cs.docforia.DocumentEngine;
import se.lth.cs.docforia.EdgeRef;

import java.util.Iterator;

/**
 * Basic edge type filter
 */
public class BasicEdgeFilter extends EdgeFilter {
    private final DocumentEngine engine;
    private final String type;
    private final String variant;

    public BasicEdgeFilter(DocumentEngine engine, String type, String variant) {
        this.engine = engine;
        this.type = type;
        this.variant = variant;
    }

    @Override
    public Iterator<EdgeRef> newIterator() {
        if(type == null && variant == null)
            return engine.edges().iterator();
        else
            return engine.edges(type, variant).iterator();
    }
}

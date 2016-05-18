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
import se.lth.cs.docforia.NodeRef;

import java.util.Iterator;

/**
 * Covered by filter
 */
public class CoveredByFilter extends NodeFilter {

    private final DocumentEngine engine;
    private Iterable<NodeRef> covered;
    private final String type;
    private final String variant;
    private final int from;
    private final int to;

    public CoveredByFilter(DocumentEngine engine, String type, String variant, int from, int to) {
        this.engine = engine;
        this.type = type;
        this.variant = variant;

        this.from = from;
        this.to = to;
        this.covered = engine.coveredAnnotation(type, variant, from, to);
    }

    @Override
    public Iterator<NodeRef> newIterator() {
        return covered.iterator();
    }
}

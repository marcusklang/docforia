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
import se.lth.cs.docforia.DocumentNodeNavigator;
import se.lth.cs.docforia.NodeRef;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Intersection filter
 */
public class IntersectionFilter extends NodeFilter {
    private final DocumentEngine engine;
    private final String layer;
    private final String variant;
    private final int from;
    private final int to;

    public IntersectionFilter(DocumentEngine engine, String layer, String variant, int from, int to) {
        this.engine = engine;
        this.layer = layer;
        if(variant == null) {
            this.variant = engine.store().getDefaultNodeVariants().get(this.layer);
        } else {
            this.variant = variant;
        }
        this.from = from;
        this.to = to;
    }

    @Override
    public Iterator<NodeRef> newIterator() {
        final DocumentNodeNavigator annotations = engine.annotations(layer, variant);
        if(annotations.nextFloor(from)) {
            return new Iterator<NodeRef>() {
                private NodeRef ref = annotations.current();

                @Override
                public boolean hasNext() {
                    if(ref != null)
                        return true;
                    else
                        return false;
                }

                @Override
                public NodeRef next() {
                    if(ref == null)
                        throw new NoSuchElementException();

                    NodeRef retval = ref;

                    if(annotations.next()) {
                        ref = annotations.current();
                        if(ref.get().getStart() >= to) {
                            ref = null;
                        }
                    }
                    else
                        ref = null;

                    return retval;
                }

                @Override
                public void remove() {

                }
            };
        }
        else {
            return Collections.emptyIterator();
        }
    }
}

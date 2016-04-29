package se.lth.cs.docforia.memstore;
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

import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import se.lth.cs.docforia.*;
import se.lth.cs.docforia.util.DocumentIterable;
import se.lth.cs.docforia.util.DocumentIterableBase;
import se.lth.cs.docforia.util.DocumentIterables;
import se.lth.cs.docforia.util.TakeWhileDocumentIterable;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

/**
 * Memory Document Database implementation
 */
public class MemoryDocumentEngine extends DocumentEngine {
    protected MemoryDocumentStore store;

    @SuppressWarnings("unchecked")
    public MemoryDocumentEngine(final MemoryDocumentStore store) {
        this.store = store;
    }

    @Override
    public DocumentStore store() {
        return store;
    }

    @Override
    public LayerRef edgeLayer(String edgeType, String edgeVariant) {
        return store.getEdgeLayerRef(edgeType, edgeVariant);
    }

    @Override
    public LayerRef nodeLayer(String nodeLayer, String nodeVariant) {
        return store.getNodeLayerRef(nodeLayer, nodeVariant);
    }

    @Override
    public DocumentIterable<EdgeRef> edges(boolean onlyDefaultVariant) {
        return onlyDefaultVariant ? DocumentIterables.wrap(store.edges()) : edges();
    }

    @SuppressWarnings("unchecked")
    @Override
    public DocumentIterable<EdgeRef> edges(final String edgeLayer, boolean onlyDefaultVariant) {
        if(onlyDefaultVariant)
            return edges(edgeLayer, store.defaultEdgeVariant.get(edgeLayer));
        else {
            final MemoryNodeCollection.Key key = new MemoryNodeCollection.Key(edgeLayer, null);

            return (DocumentIterable) DocumentIterables.concat(
                    new TakeWhileDocumentIterable<>(
                            (Function<MemoryNodeCollection, Boolean>) in -> in.key.layer.equals(edgeLayer),
                            (Iterable<MemoryNodeCollection>) () -> store.nodes.tailMap(key).values().iterator()
                    )
            );
        }
    }

    @Override
    public DocumentIterable<EdgeRef> edges(final String edgeLayer, final String edgeVariant) {
        final MemoryEdgeCollection.Key key = new MemoryEdgeCollection.Key(edgeLayer, store.defaultEdgeVariant.get(edgeVariant));
        return new DocumentIterableBase<EdgeRef>() {
            @Override
            public Iterator<EdgeRef> iterator() {
                MemoryEdgeCollection edgeRefs = store.edges.get(key);
                if(edgeRefs == null)
                    return Collections.emptyIterator();
                else
                    return edgeRefs.iterator();
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public DocumentIterable<EdgeRef> edges(NodeRef start, Direction dir) {
        switch (dir) {
            case IN:
                return (DocumentIterable)DocumentIterables.wrap(((MemoryNode)start).inlinks);
            case OUT:
                return (DocumentIterable)DocumentIterables.wrap(((MemoryNode)start).outlinks);
            case BOTH:
                DocumentIterables.concat((DocumentIterable)DocumentIterables.wrap(((MemoryNode)start).inlinks),
                                         (DocumentIterable)DocumentIterables.wrap(((MemoryNode)start).outlinks));
        }

        return super.edges(start, dir);
    }

    @Override
    public DocumentIterable<NodeRef> nodes(final String nodeLayer) {
        return new DocumentIterableBase<NodeRef>() {
            @Override
            public Iterator<NodeRef> iterator() {
                MemoryNodeCollection nodeRefs = store.nodes.get(new MemoryNodeCollection.Key(nodeLayer, store.defaultNodeVariant.get(nodeLayer)));
                if(nodeRefs == null)
                    return Collections.emptyIterator();
                else
                    return nodeRefs.iterator();
            }
        };
    }

    @Override
    public DocumentIterable<String> nodeLayers() {
        return new DocumentIterableBase<String>() {
            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    Iterator<MemoryNodeCollection.Key> keys = store.nodes.keySet().iterator();
                    String next;
                    String last = null;

                    private boolean moveForward() {
                        if(next == null) {
                            while(keys.hasNext()) {
                                next = keys.next().layer;
                                if(next.equals(last)) {
                                    next = null;
                                } else {
                                    last = next;
                                    return true;
                                }
                            }

                            return false;
                        }
                        else
                            return true;
                    }

                    @Override
                    public boolean hasNext() {
                        return moveForward();
                    }

                    @Override
                    public String next() {
                        if(!moveForward())
                            throw new NoSuchElementException();

                        String retval = next;
                        next = null;
                        return retval;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override
    public DocumentIterable<String> edgeLayers() {
        return new DocumentIterableBase<String>() {
            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    Iterator<MemoryEdgeCollection.Key> keys = store.edges.keySet().iterator();
                    String next;
                    String last = null;

                    private boolean moveForward() {
                        if(next == null) {
                            while(keys.hasNext()) {
                                next = keys.next().layer;
                                if(next.equals(last)) {
                                    next = null;
                                } else {
                                    last = next;
                                    return true;
                                }
                            }

                            return false;
                        }
                        else
                            return true;
                    }

                    @Override
                    public boolean hasNext() {
                        return moveForward();
                    }

                    @Override
                    public String next() {
                        if(!moveForward())
                            throw new NoSuchElementException();

                        String retval = next;
                        next = null;
                        return retval;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override
    public DocumentIterable<NodeRef> nodes(final String nodeLayer, final String variant) {
        final MemoryNodeCollection.Key key = new MemoryNodeCollection.Key(nodeLayer, variant);
        return new DocumentIterableBase<NodeRef>() {
            @Override
            public Iterator<NodeRef> iterator() {
                MemoryNodeCollection nodeRefs = store.nodes.get(key);
                if(nodeRefs == null)
                    return Collections.emptyIterator();
                else
                    return nodeRefs.iterator();
            }
        };
    }

    @Override
    public String toString(Range range) {
        return store.text.substring(range.getStart(), range.getEnd());
    }

    @Override
    public DocumentNodeNavigator annotations(String nodeLayer, String nodeVariant) {
        MemoryNodeCollection nodeRefs = store.nodes.get(new MemoryNodeCollection.Key(nodeLayer, nodeVariant));
        if(nodeRefs == null || nodeRefs.nodes.isEmpty() || nodeRefs.nodes.lastIntKey() == -1)
            return new DocumentNodeNavigator() {
                @Override
                public NodeRef current() {
                    return null;
                }

                @Override
                public boolean next() {
                    return false;
                }

                @Override
                public boolean nextFloor(int start) {
                    return false;
                }

                @Override
                public boolean hasReachedEnd() {
                    return true;
                }

                @Override
                public boolean prev() {
                    return false;
                }

                @Override
                public void reset() {

                }

                @Override
                public boolean next(int start) {
                    return false;
                }

                @Override
                public int start() {
                    return 0;
                }

                @Override
                public int end() {
                    return 0;
                }
            };
        else {
            return nodeRefs.annotationNavigator();
        }
    }

    @Override
    public DocumentIterable<String> nodeLayerVariants(final String nodeLayer) {
        return new DocumentIterableBase<String>() {
            @Override
            public Iterator<String> iterator() {
                final ObjectBidirectionalIterator<MemoryNodeCollection.Key> iterator = store.nodes.keySet().tailSet(new MemoryNodeCollection.Key(nodeLayer, null)).iterator();
                return new Iterator<String>() {
                    private String next;
                    private boolean valid = false;

                    private boolean moveForward() {
                        if(!valid) {
                            while(iterator.hasNext()) {
                                MemoryNodeCollection.Key nextkey = iterator.next();
                                if(nextkey.layer.equals(nodeLayer)) {
                                    if(store.nodes.get(nextkey).nodes.size() == 0)
                                        continue;

                                    if(nextkey.variant != null) {
                                        next = nextkey.variant;
                                    }
                                    else
                                        continue;

                                    valid = true;
                                }
                                else
                                    valid = false;

                                return valid;
                            }

                            return false;
                        }
                        else
                            return true;
                    }

                    @Override
                    public boolean hasNext() {
                        return moveForward();
                    }

                    @Override
                    public String next() {
                        if(!moveForward())
                            throw new NoSuchElementException();

                        valid = false;
                        return next;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override
    public DocumentIterable<Optional<String>> nodeLayerAllVariants(final String nodeLayer) {
        return new DocumentIterableBase<Optional<String>>() {
            @Override
            public Iterator<Optional<String>> iterator() {
                final ObjectBidirectionalIterator<MemoryNodeCollection.Key> iterator = store.nodes.keySet().tailSet(new MemoryNodeCollection.Key(nodeLayer, null)).iterator();
                return new Iterator<Optional<String>>() {
                    private Optional<String> next;
                    private boolean valid = false;

                    private boolean moveForward() {
                        if(!valid) {
                            while(iterator.hasNext()) {
                                MemoryNodeCollection.Key nextkey = iterator.next();
                                if(nextkey.layer.equals(nodeLayer)) {
                                    if(store.nodes.get(nextkey).nodes.size() == 0)
                                        continue;

                                    if(nextkey.variant == null)
                                        next = Optional.empty();
                                    else
                                        next = Optional.of(nextkey.variant);

                                    valid = true;
                                }
                                else
                                    valid = false;

                                return valid;
                            }

                            return false;
                        }
                        else
                            return true;
                    }

                    @Override
                    public boolean hasNext() {
                        return moveForward();
                    }

                    @Override
                    public Optional<String> next() {
                        if(!moveForward())
                            throw new NoSuchElementException();

                        valid = false;
                        return next;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}

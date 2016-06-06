package se.lth.cs.docforia;
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

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import se.lth.cs.docforia.util.*;

import java.util.*;

/** Document database functions
 *
 * Provides a set of functions used by the query engine to retrieve and find data.
 *
 * <b>Remarks:</b> The implementing storage system should override and provide optimized functions.
 * The default implemention only provides simple O(n) searching algorithms for easier implementation.
 */
public abstract class DocumentEngine {

	/**
	 * Get the underlying store
	 * @return representation of the store
	 */
	public abstract DocumentStore store();

	/**
	 * Get an iterable of all edges of default variant type
	 * @return iterable of edges
	 */
	public final DocumentIterable<EdgeRef> edges() {
		return DocumentIterables.wrap(store().edges());
	}

	/**
	 * Get an iterable of all edges of a certain type, default variant
	 * @param edgeLayer the edge layer
	 * @return iterable of all matching edges
	 */
	public final DocumentIterable<EdgeRef> edges(final String edgeLayer) {
		return edges(edgeLayer, true);
	}

	/**
	 * Get an iterable of all edges of a certain layer
	 * @param layerRef the edge layer
	 * @return iterable of all matching edges
	 */
	public final DocumentIterable<EdgeRef> edges(final LayerRef layerRef) {
		return edges(layerRef.getLayer(), layerRef.getVariant());
	}

	/**
	 * Get an iterable of all edges
	 * @param onlyDefaultVariant select if only the default variants will be selected or not.
	 * @return iterable of all matching edges
	 */
	public DocumentIterable<EdgeRef> edges(boolean onlyDefaultVariant) {
		final DocumentIterable<EdgeRef> edgeIterable = edges();
		if(onlyDefaultVariant)
			return new FilteredDocumentIterable<EdgeRef>(edgeIterable) {
				@Override
				protected boolean accept(EdgeRef current) {
					String currentVariant = current.get().getVariant();
					return Objects.equals(currentVariant, null);
				}
				
				@Override
				public String toString() {
					return "FilteredDocmentIterable(" + edgeIterable.toString() +")";
				}
			};
		else
			return edgeIterable;
	}

	/**
	 * Get an iterable of all edges of a certain type
	 * @param edgeLayer the raw edge layer
	 * @param onlyDefaultVariant select if only the default variants will be selected or not.
	 * @return
	 */
	public DocumentIterable<EdgeRef> edges(final String edgeLayer, boolean onlyDefaultVariant) {
		final DocumentIterable<EdgeRef> edgeIterable = edges(onlyDefaultVariant);
		return new FilteredDocumentIterable<EdgeRef>(edgeIterable) {
			@Override
			protected boolean accept(EdgeRef current) {
				return current.get().getLayer().equals(edgeLayer);
			}
			
			@Override
			public String toString() {
				return "FilteredDocmentIterable<" + edgeLayer + ">(" + edgeIterable.toString() +")";
			}
		};
	}

	/**
	 * Get an iterable of all edges of certain type and variant
	 * @param edgeLayer   the raw layer name
	 * @param edgeVariant the raw layer variant
	 * @return iterable of the edges matching the given criteria
	 */
	public DocumentIterable<EdgeRef> edges(final String edgeLayer, final String edgeVariant) {
		final Iterable<EdgeRef> edgeIterable = store().edges();
		return new FilteredDocumentIterable<EdgeRef>(edgeIterable) {
			@Override
			protected boolean accept(EdgeRef current) {
				EdgeStore store = current.get();
				return store.getLayer().equals(edgeLayer) && stringEqual(store.getVariant(), edgeVariant);
			}
			
			@Override
			public String toString() {
				return "FilteredDocmentIterable<" + edgeLayer + ">(" + edgeIterable.toString() +")";
			}
		};
	}

	/**
	 * Get an iterable of all edges with specified direction and starting position
	 * @param start the node to find edges from
	 * @param dir   the direction of the edge
	 * @return iterable of the edges matching the given criteria
	 */
	public DocumentIterable<EdgeRef> edges(final NodeRef start, Direction dir) {
		if(dir == Direction.BOTH)
			return DocumentIterables.wrap(store().edges(start));
		else if(dir == Direction.IN) {
			return new FilteredDocumentIterable<EdgeRef>(store().inboundEdges(start)) {
				@Override
				protected boolean accept(EdgeRef value) {
					return value.get().getHead() == start;
				}
				
			};
		}
		else if(dir == Direction.OUT) {
			return new FilteredDocumentIterable<EdgeRef>(store().outboundEdges(start)) {
				@Override
				protected boolean accept(EdgeRef value) {
					return value.get().getTail() == start;
				}
				
			};
		}
		else
			throw new UnsupportedOperationException("Not implemented.");
	}

	/**
	 * Get an iterable of all edges of certain type with specified direction that is connected to given node
	 * @param start the node to find edges from
	 * @param layer the raw edge layer name
	 * @param dir the direction of the edge
	 * @return iterable of the edges matching the given criteria
	 */
	public DocumentIterable<EdgeRef> edges(NodeRef start, String layer, Direction dir) {
		return edges(start, layer, null, dir);
	}

	/**
	 * Get an iterable of all edges of a certain type, variant with specified direction that is connected to given node
	 * @param start the node to find edges from
	 * @param layer the raw edge layer name
	 * @param variant the raw edge layer variant
	 * @param dir the direction of the edge
	 * @return iterable of the edges matching the given criteria
	 */
	public DocumentIterable<EdgeRef> edges(NodeRef start, final String layer, final String variant, Direction dir) {
        final LayerRef edgeLayerRef = store().edgeLayer(layer, variant);

        return new FilteredDocumentIterable<EdgeRef>(edges(start, dir)) {
			
			@Override
			protected boolean accept(EdgeRef value) {
                return value.layer().equal(edgeLayerRef);
			}
			
		};
	}

    /**
     * Get an iterable of all edges of a certain type, variant with specified direction that is connected to given node
     * @param start the node to find edges from
     * @param edgeLayerRef the raw edge layer name
     * @param dir the direction of the edge
     * @return iterable of the edges matching the given criteria
     */
    public DocumentIterable<EdgeRef> edges(NodeRef start, final LayerRef edgeLayerRef, Direction dir) {
        return new FilteredDocumentIterable<EdgeRef>(edges(start, dir)) {

            @Override
            protected boolean accept(EdgeRef value) {
                return value.layer().equal(edgeLayerRef);
            }

        };
    }

	public Iterable<EdgeRef> edges(NodeRef tail, final NodeRef head, final String layer, final String variant) {
		return new FilteredDocumentIterable<EdgeRef>(edges(tail, layer, variant, Direction.OUT)) {
			@Override
			protected boolean accept(EdgeRef value) {
				return value.get().getHead().equals(head);
			}
		};
	}

    /**
     * Get all nodes via edges
     * @param start        the starting node
     * @param edgeLayerRef the edge layer to find edges via
     * @param nodeLayerRef the node layer to find nodes in
     * @param dir the directionality (IN and OUT supported, not BOTH)
     */
    public DocumentIterable<NodeRef> edgeNodes(NodeRef start, final LayerRef edgeLayerRef, final LayerRef nodeLayerRef, Direction dir) {
        final DocumentIterable<EdgeRef> edges = edges(start, edgeLayerRef, dir);

        switch (dir) {
            case IN:
                return new FilteredMappedDocumentIterable<NodeRef, EdgeRef>(edges) {
                    @Override
                    protected boolean accept(EdgeRef value) {
                        return value.get().getTail().layer().equal(nodeLayerRef);
                    }

                    @Override
                    protected NodeRef map(EdgeRef value) {
                        return value.get().getTail();
                    }
                };
            case OUT:
                return new FilteredMappedDocumentIterable<NodeRef, EdgeRef>(edges) {
                    @Override
                    protected boolean accept(EdgeRef value) {
                        return value.get().getHead().layer().equal(nodeLayerRef);
                    }

                    @Override
                    protected NodeRef map(EdgeRef value) {
                        return value.get().getHead();
                    }
                };
            default:
                throw new UnsupportedOperationException();
        }
    }

	/**
	 * Project inbound
	 * @param start
	 * @param nodeLayer
	 * @param nodeVariant
	 * @param edgeLayer
	 * @param edgeVariant
     * @return
     */
	public Iterable<NodeRef> projectInbound(NodeRef start, boolean includeStart, final String nodeLayer, final String nodeVariant, final String edgeLayer, final String edgeVariant) {
	    //TODO: Check the use of nodeLayer, and possibly fix!
        ArrayList<NodeRef> nodeRefs = new ArrayList<>();
        ReferenceOpenHashSet<NodeRef> visited = new ReferenceOpenHashSet<>();
        ArrayDeque<Iterator<EdgeRef>> s = new ArrayDeque<>();

        LayerRef edgeLayerRef = store().edgeLayer(edgeLayer, edgeVariant);

        if(includeStart)
            nodeRefs.add(start);

		s.push(edges(start,edgeLayerRef, Direction.IN).iterator());

		while(!s.isEmpty()) {
            Iterator<EdgeRef> peek = s.peek();
            if(peek.hasNext()) {
                EdgeRef next = peek.next();
                NodeRef newstart = next.get().getTail();
                if(!visited.contains(newstart)) {
                    nodeRefs.add(newstart);
                    s.push(edges(newstart, edgeLayerRef, Direction.IN).iterator());
                    visited.add(newstart);
                }
            }
            else
                s.pop();
        }

		return nodeRefs;
	}

    /**
     * Project outbound
     * @param start
     * @param nodeLayer
     * @param nodeVariant
     * @param edgeLayer
     * @param edgeVariant
     * @return
     */
    public Iterable<NodeRef> projectOutbound(NodeRef start, boolean includeStart, final String nodeLayer, final String nodeVariant, final String edgeLayer, final String edgeVariant) {
		//TODO: Check the use of nodeLayer, and possibly fix!
		ArrayList<NodeRef> nodeRefs = new ArrayList<>();
        ReferenceOpenHashSet<NodeRef> visited = new ReferenceOpenHashSet<>();
        ArrayDeque<Iterator<EdgeRef>> s = new ArrayDeque<>();

        LayerRef edgeLayerRef = store().edgeLayer(edgeLayer, edgeVariant);

        if(includeStart)
            nodeRefs.add(start);

        s.push(edges(start, edgeLayerRef, Direction.OUT).iterator());

        while(!s.isEmpty()) {
            Iterator<EdgeRef> peek = s.peek();
            if(peek.hasNext()) {
                EdgeRef next = peek.next();
                NodeRef newstart = next.get().getHead();
                if(!visited.contains(newstart)) {
                    nodeRefs.add(newstart);
                    s.push(edges(newstart, edgeLayerRef, Direction.OUT).iterator());
                    visited.add(newstart);
                }
            }
            else
                s.pop();
        }

        return nodeRefs;
    }

	/**
	 * Get an iterable of all nodes
	 * @return iterable of all nodes
	 */
	public DocumentIterable<NodeRef> nodes() {
		return DocumentIterables.wrap(store().nodes());
	}

	/**
	 * Get nodes of a particular layer
	 * <b>Remarks:</b> This method gets the default variant and then calls nodes(nodeLayer, defaultVariant)
	 * @param nodeLayer
	 * @return iterable of node with matching layer name
	 */
	public DocumentIterable<NodeRef> nodes(final String nodeLayer) {
		return nodes(nodeLayer, null);
	}
	
	private boolean stringEqual(final String a, final String b) {
		return Objects.equals(a,b);
	}
	
	/**
	 * Get all node layers present in the document
	 * @return iterable of all nodes
	 */
	public DocumentIterable<String> nodeLayers() {
		HashSet<String> nodeLayers = new HashSet<String>();
		for(NodeRef ref : store().nodes()) {
			nodeLayers.add(ref.get().getLayer());
		}
		return DocumentIterables.wrap(nodeLayers);
	}

	/**
	 * Get all node variants of a particular layer present in the document
	 * @return iterable of all nodes
	 */
	public DocumentIterable<String> nodeLayerVariants(String layer) {
		HashSet<String> nodeVariants = new HashSet<String>();
		for(NodeRef ref : store().nodes()) {
			if(ref.get().getLayer().equals(layer) && ref.get().getVariant() != null) {
				nodeVariants.add(ref.get().getVariant());
			}
		}
		return DocumentIterables.wrap(nodeVariants);
	}

	/**
	 * Get all node variants of a particular layer present in the document
	 * @return iterable of all nodes
	 */
	public DocumentIterable<Optional<String>> nodeLayerAllVariants(String layer) {
		HashSet<Optional<String>> nodeVariants = new HashSet<Optional<String>>();
		for(NodeRef ref : store().nodes()) {
			if(ref.get().getLayer().equals(layer)) {
				if(ref.get().getVariant() == null)
					nodeVariants.add(Optional.empty());
				else
					nodeVariants.add(Optional.of(ref.get().getVariant()));
			}
		}
		return DocumentIterables.wrap(nodeVariants);
	}

	/**
	 * Get all edge layers present in the document
	 * @return iterable of all nodes
	 */
	public DocumentIterable<String> edgeLayers() {
		HashSet<String> edgeLayers = new HashSet<String>();
		for(EdgeRef ref : store().edges()) {
			edgeLayers.add(ref.get().getVariant());
		}
		return DocumentIterables.wrap(edgeLayers);
	}

	/**
	 * Get all edge variants of a particular layer present in the document
	 * @return iterable of all edge layer variants
	 */
	public DocumentIterable<String> edgeLayerVariants(String layer) {
		HashSet<String> edgeVariants = new HashSet<String>();
		for(EdgeRef ref : store().edges()) {
			if(ref.get().getLayer().equals(layer) && ref.get().getVariant() != null) {
				edgeVariants.add(ref.get().getVariant());
			}
		}
		return DocumentIterables.wrap(edgeVariants);
	}

	/**
	 * Get all edge variants of a particular layer present in the document
	 * @return iterable of all edge layer variants
	 */
	public DocumentIterable<Optional<String>> edgeLayerAllVariants(String layer) {
		HashSet<Optional<String>> edgeVariants = new HashSet<Optional<String>>();
		for(EdgeRef ref : store().edges()) {
			if(ref.get().getLayer().equals(layer)) {
				if(ref.get().getVariant() == null)
					edgeVariants.add(Optional.empty());
				else
					edgeVariants.add(Optional.of(ref.get().getVariant()));
			}
		}
		return DocumentIterables.wrap(edgeVariants);
	}


	/**
	 * Get all node layers present in the document
	 * @return iterable of all nodes
	 */
	public DocumentIterable<LayerRef> nodeLayerRefs() {
		HashSet<LayerRef> nodeLayers = new HashSet<LayerRef>();
		for(NodeRef ref : store().nodes()) {
			nodeLayers.add(ref.layer());
		}
		return DocumentIterables.wrap(nodeLayers);
	}

	/**
	 * Get all edge layers present in the document
	 * @return iterable of all nodes
	 */
	public DocumentIterable<LayerRef> edgeLayerRefs() {
		HashSet<LayerRef> edgeLayers = new HashSet<LayerRef>();
		for(EdgeRef ref : store().edges()) {
			edgeLayers.add(ref.layer());
		}
		return DocumentIterables.wrap(edgeLayers);
	}

	/**
	 * Get all nodes with given node layer
	 * @param layerRef the raw layer name.
	 * @return iterable of all nodes matching layer name and variant
	 */
	public DocumentIterable<NodeRef> nodes(final LayerRef layerRef) {
		return nodes(layerRef.getLayer(), layerRef.getVariant());
	}

	/**
	 * Get all nodes with given node layer and variant
	 * @param nodeLayer the raw layer name.
	 * @param variant the raw layer variant
	 * @return iterable of all nodes matching layer name and variant
	 */
	public DocumentIterable<NodeRef> nodes(final String nodeLayer, final String variant) {
		final Iterable<NodeRef> nodeIterable = store().nodes();

		return new FilteredDocumentIterable<NodeRef>(nodeIterable) {
			@Override
			protected boolean accept(NodeRef current) {
				NodeStore store = current.get();
				return store.getLayer().equals(nodeLayer) && stringEqual(store.getVariant(),variant);
			}
			
			@Override
			public String toString() {
				return "FilteredDocmentIterable<" + nodeLayer + ">(" + nodeIterable.toString() +")";
			}
		};
			
	}

	/**
	 * Get all annotations (nodes with start, end) that is covered by (from, to)
	 * @param nodeLayer raw layer name
	 * @param from cover start
	 * @param to cover end
	 * @return iterable of all nodes covered by range (from, to)
	 */
	public final DocumentIterable<NodeRef> coveredAnnotation(final String nodeLayer, final int from, final int to) {
		return coveredAnnotation(nodeLayer, null, from, to);
	}

	/**
	 * Get all annotations (nodes with start, end) that is covered by (from, to)
	 * @param nodeLayer raw layer name
     * @param nodeVariant variant name
	 * @param from cover start
	 * @param to cover end
	 * @return iterable of all nodes covered by range (from, to)
	 */
	public DocumentIterable<NodeRef> coveredAnnotation(final String nodeLayer, final String nodeVariant, final int from, final int to) {
		final Iterable<NodeRef> nodes = nodes(nodeLayer, nodeVariant);
		return new FilteredDocumentIterable<NodeRef>(nodes) {

			private Range range = new MutableRange(from, to);
			
			@Override
			protected boolean accept(NodeRef value) {
                NodeStore store = value.get();
                return value.get().isAnnotation() && range.getStart() <= store.getStart() && range.getEnd() >= store.getEnd();
            }
			
			@Override
			public String toString() {
				return "FilteredIterable<" + nodeLayer + ">(cover from: " + from + ", cover to: "+ to +", iterable: " + nodes.toString() + ")";
			}
		};
	}

    /**
     * Get all overlapping annotations
     * @param nodeLayer   raw layer name
     * @param nodeVariant variant name
     * @param from overlap start
     * @param to overlap end
     * @return
     */
    public DocumentIterable<NodeRef> overlappingAnnotations(final String nodeLayer, final String nodeVariant, final int from, final int to) {
        final Iterable<NodeRef> nodes = nodes(nodeLayer, nodeVariant);
        return new FilteredDocumentIterable<NodeRef>(nodes) {

            private Range range = new MutableRange(from, to);

            @Override
            protected boolean accept(NodeRef value) {
                NodeStore store = value.get();
                return value.get().isAnnotation() && store.getEnd() > range.getStart() && store.getStart() < range.getEnd();
            }

            @Override
            public String toString() {
                return "FilteredIterable<" + nodeLayer + ">(overlap from: " + from + ", overlap to: "+ to +", iterable: " + nodes.toString() + ")";
            }
        };
    }

	/**
	 * Get all annotations (nodes with start, end) that is covering (from, to)
	 * @param nodeLayer   raw layer name
	 * @param nodeVariant raw layer variant
	 * @param from range start
	 * @param to   range end
	 * @return
	 */
	public DocumentIterable<NodeRef> coveringAnnotation(final String nodeLayer, final String nodeVariant, final int from, final int to) {
        final Iterable<NodeRef> nodes = overlappingAnnotations(nodeLayer, nodeVariant, from, to);
        return new FilteredDocumentIterable<NodeRef>(nodes) {

            private Range range = new MutableRange(from, to);

            @Override
            protected boolean accept(NodeRef value) {
                NodeStore store = value.get();
                return value.get().isAnnotation() && value.get().getStart() <= from && value.get().getEnd() >= to;
            }

            @Override
            public String toString() {
                return "FilteredIterable<" + nodeLayer + ">(overlap from: " + from + ", overlap to: "+ to +", iterable: " + nodes.toString() + ")";
            }
        };
	}

	/**
	 * Remove all nodes of a particular type, default variant
	 * @param nodeLayer raw layer name
	 */
	public final void removeAllNodes(String nodeLayer)  {
		removeAllNodes(nodeLayer, null);
	}

	/**
	 * Remove all nodes of a particular type, variant
	 * @param nodeLayer   raw layer name
	 * @param nodeVariant raw layer variant
	 */
	public void removeAllNodes(String nodeLayer, String nodeVariant)  {
		ArrayList<NodeRef> nodes = new ArrayList<NodeRef>();
		for(NodeRef node : nodes(nodeLayer, nodeVariant)) {
			nodes.add(node);
		}
		
		for(NodeRef node : nodes)
			store().remove(node);
	}

	/**
	 * Remove all nodes of a particular layer
	 * @param layerRef    raw layer
	 */
	public void removeAllNodes(LayerRef layerRef)  {
		removeAllNodes(layerRef.getLayer(), layerRef.getVariant());
	}

	/**
	 * Remove all edges of a particular type
	 * @param edgeType raw layer name
	 */
	public final void removeAllEdges(String edgeType)  {
		removeAllEdges(edgeType, null);
	}

	/**
	 * Remove all edges of a particular type, variant
	 * @param edgeType raw layer name
	 * @param edgeVariant raw layer variant
	 */
	public void removeAllEdges(String edgeType, String edgeVariant)  {
		ArrayList<EdgeRef> edges = new ArrayList<EdgeRef>();
		for(EdgeRef edge : edges(edgeType, edgeVariant)) {
			edges.add(edge);
		}
		
		for(EdgeRef edge : edges)
			store().remove(edge);
	}

	/**
	 * Remove all edges of a particular layer
	 * @param layerRef    raw layer
	 */
	public void removeAllEdges(LayerRef layerRef)  {
		removeAllEdges(layerRef.getLayer(), layerRef.getVariant());
	}

	/**
	 * Retain only the nodes in the @param(layerRefs) collection, remove all other
	 * @param layerRefs the collection of nodes to retain
     */
	public void retainOnlyNodes(Collection<LayerRef> layerRefs) {
		HashSet<LayerRef> refs = new HashSet<>(layerRefs);
		ArrayList<LayerRef> remove = new ArrayList<>();
		for (LayerRef layerRef : nodeLayerRefs()) {
			if(!refs.contains(layerRef)) {
				remove.add(layerRef);
			}
		}

		for (LayerRef layerRef : remove) {
			removeAllNodes(layerRef);
		}
	}

	/**
	 * Retain only the edges in the @param(layerRefs) collection, remove all other
	 * @param layerRefs the collection of nodes to retain
	 */
	public void retainOnlyEdges(Collection<LayerRef> layerRefs) {
		HashSet<LayerRef> refs = new HashSet<>(layerRefs);
		ArrayList<LayerRef> remove = new ArrayList<>();
		for (LayerRef layerRef : edgeLayerRefs()) {
			if(!refs.contains(layerRef)) {
				remove.add(layerRef);
			}
		}

		for (LayerRef layerRef : remove) {
			removeAllEdges(layerRef);
		}
	}

	/**
	 * Get string of document in range
	 * @param range the range
	 * @return text of range
	 */
	public String toString(Range range) {
		return store().getText().substring(range.getStart(), range.getEnd());
	}


	/**
	 * Get all nodes that has given property with key, value
	 */
	public DocumentIterable<NodeRef> nodesWithProperty(String key, String value) {
		return nodesWithProperty(store().nodes(), key, value);
	}

	/**
	 * Get all nodes of specific type that has given property with key, value
	 */
	public DocumentIterable<NodeRef> nodesWithProperty(String nodeLayer, String key, String value) {
		return nodesWithProperty(nodes(nodeLayer), key, value);
	}

	/**
	 * Get all nodes of specific type, variant that has given property with key, value
	 */
	public DocumentIterable<NodeRef> nodesWithProperty(String nodeLayer, String nodeVariant, String key, String value) {
		return nodesWithProperty(nodes(nodeLayer, nodeVariant), key, value);
	}

	private DocumentIterable<NodeRef> nodesWithProperty(final Iterable<NodeRef> nodes, final String key, final String value) {
		return new FilteredDocumentIterable<NodeRef>(nodes) {

			@Override
			protected boolean accept(NodeRef input) {
				return value.equals(input.get().getProperty(key));
			}
			
			@Override
			public String toString() {
				return "FilteredDocumentIterable<NodeStore>(property(" + key + ") == '" + value + "', nodes: " + nodes +")";
			}
		};
	}

    /**
     * Returns a navigator that is forwarded to the position of given node ref
     * @param ref the node ref to get a navigator around, must be an annotation.
     */
    public AnnotationNavigator<NodeRef> annotations(NodeRef ref) {
        if(!ref.get().isAnnotation())
            throw new IllegalArgumentException("ref is not an annotation!");

        AnnotationNavigator<NodeRef> navigator = annotations(ref.layer().getLayer(), ref.layer().getVariant());
        while(navigator.next()) {
            if(navigator.current().equals(ref))
                return navigator;
        }

        return navigator;
    }

    /**
     * Get a navigator for a layer
     * @param nodeLayer   the node layer
     * @param nodeVariant the variant or Optional.empty() if default
     */
    public AnnotationNavigator<NodeRef> annotations(final String nodeLayer, final Optional<String> nodeVariant) {
        return annotations(nodeLayer, nodeVariant.isPresent() ? nodeVariant.get() : null);
    }

    /**
     * Get a navigator for a layer
     * @param nodeLayer   the node layer
     * @param nodeVariant the variant or null if default
     */
	public AnnotationNavigator<NodeRef> annotations(final String nodeLayer, final String nodeVariant) {
		return new AnnotationNavigator<NodeRef>() {
			private Iterator<NodeRef> iter = nodes(nodeLayer, nodeVariant).iterator();
			private NodeRef current = null;
			private boolean reachedEnd = false;

			@Override
			public NodeRef current() {
				return current;
			}

			@Override
			public boolean next() {
				while(iter.hasNext()) {
					current = iter.next();
					if(current.get().isAnnotation())
						return true;
				}

				reachedEnd = true;
				return false;
			}

			@Override
			public boolean hasReachedEnd() {
				return reachedEnd;
			}

			@Override
			public void reset() {
				iter = nodes(nodeLayer, nodeVariant).iterator();
			}

            @Override
            public boolean prev() {
                throw new UnsupportedOperationException();
            }

            @Override
			public boolean next(int start) {
				if(reachedEnd)
					return false;

				while(iter.hasNext()) {
					current = iter.next();
					if(current.get().isAnnotation()) {
						if(current.get().getStart() >= start)
							return true;
					}
				}

				reachedEnd = true;
				return false;
			}

			@Override
			public boolean nextFloor(int start) {
				if(reachedEnd)
					return false;

				while(iter.hasNext()) {
					current = iter.next();
					if(current.get().isAnnotation()) {
						if(current.get().getEnd() > start)
							return true;
					}
				}

				reachedEnd = true;
				return false;
			}

			@Override
			public int start() {
				return current.get().getStart();
			}

			@Override
			public int end() {
				return current.get().getEnd();
			}
		};
	}

	/**
	 * Get all edges that has given property with key, value
	 */
	public DocumentIterable<EdgeRef> edgesWithProperty(String key, String value) {
		return edgesWithProperty(store().edges(), key, value);
	}

	/**
	 * Get all edges of specific type that has given property with key, value
	 */
	public DocumentIterable<EdgeRef> edgesWithProperty(String edgeLayer, String key, String value) {
		return edgesWithProperty(edges(edgeLayer), key, value);
	}

	/**
	 * Get all edges of specific type, variant that has given property with key, value
	 */
	public DocumentIterable<EdgeRef> edgesWithProperty(String edgeLayer, String edgeVariant, String key, String value) {
		return edgesWithProperty(edges(edgeLayer, edgeVariant), key, value);
	}
	
	private DocumentIterable<EdgeRef> edgesWithProperty(final Iterable<EdgeRef> edges, final String key, final String value) {
		return new FilteredDocumentIterable<EdgeRef>(edges) {

			@Override
			protected boolean accept(EdgeRef current) {
				return value.equals(current.get().getProperty(key));
			}
			
			@Override
			public String toString() {
				return "FilteredIterable<EdgeStore>(property(" + key + ") == '" + value + "', nodes: " + edges +")";
			}
		};
	}

}

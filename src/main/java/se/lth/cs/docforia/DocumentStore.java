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

import se.lth.cs.docforia.data.DataRef;
import se.lth.cs.docforia.util.Iterables;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Document storage abastract, is subclassed by the implementing storage layer.
 */
@SuppressWarnings("unchecked")
public abstract class DocumentStore extends PropertyStore {

    public DocumentStore() {

    }

    public abstract Document getDocument();

    /**
	 * Get an iterator of all nodes
	 * <p>
	 * <b>Remarks:</b> All variants and not just defaults are returned
	 * @return iterable of all nodes
	 */
	public abstract Iterable<NodeRef> nodes();

    /**
     * Get an iterable of all edges
     * <p>
     * <b>Remarks:</b> All variants and not just defaults are returned
     * @return iterable of all edges
     */
	public abstract Iterable<EdgeRef> edges();

    /**
     * Inbound and outbound edges to and form a node.
     * @param node the node ref
     * @return Iterable of inbound and outbound edges
     */
	public Iterable<EdgeRef> edges(NodeRef node) {
		return Iterables.concat(inboundEdges(node), outboundEdges(node));
	}

    /**
     * Inbound edges to a node.
     * @param node the node reference
     * @return Iterable of inbound edges
     */
	public abstract Iterable<EdgeRef> inboundEdges(NodeRef node);

    /**
     * Outbound edges from a node.
     * @param node the node reference
     * @return Iterable of outbound edges
     */
	public abstract Iterable<EdgeRef> outboundEdges(NodeRef node);


	protected void markNodeAsRemoved(Node node) {
		node.store = null;
	}
	protected void markEdgeAsRemoved(Edge edge) {
		edge.store = null;
	}

	public void migrateNodesToVariant(String nodeLayer, String prevVariant, String newVariant) { throw new UnsupportedOperationException(); }
	public void migrateEdgesToVariant(String edgeLayer, String prevVariant, String newVariant) { throw new UnsupportedOperationException(); }

/*
	protected static class NodeLayerRef implements LayerRef {
		private String nodeLayer;
		private String variant;

		public NodeLayerRef(String nodeLayer, String variant) {
			this.nodeLayer = nodeLayer;
			this.variant = variant;
		}

        public NodeLayerRef(LayerRef ref) {
            this.nodeLayer = ref.getLayer();
            this.variant = ref.getVariant();
        }

		@Override
		public boolean equal(LayerRef ref) {
			return ref.getLayer().equals(nodeLayer) && Objects.equals(variant, ref.getVariant());
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			NodeLayerRef that = (NodeLayerRef) o;

			if (!nodeLayer.equals(that.nodeLayer)) return false;
			return variant != null ? variant.equals(that.variant) : that.variant == null;
		}

		@Override
		public int hashCode() {
			int result = nodeLayer.hashCode();
			result = 31 * result + (variant != null ? variant.hashCode() : 0);
			return result;
		}

		@Override
		public String getLayer() {
			return nodeLayer;
		}

		@Override
		public String getVariant() {
			return variant;
		}
	}

	protected static class EdgeLayerRef implements LayerRef {
		private String edgeLayer;
		private String variant;

		public EdgeLayerRef(String edgeLayer, String variant) {
			this.edgeLayer = edgeLayer;
			this.variant = variant;
		}

        public EdgeLayerRef(LayerRef ref) {
            this.edgeLayer = ref.getLayer();
            this.variant = ref.getVariant();
        }

		@Override
		public boolean equal(LayerRef ref) {
			return ref.getLayer().equals(edgeLayer) && Objects.equals(variant, ref.getVariant());
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			EdgeLayerRef that = (EdgeLayerRef) o;

			if (!edgeLayer.equals(that.edgeLayer)) return false;
			return variant != null ? variant.equals(that.variant) : that.variant == null;

		}

		@Override
		public int hashCode() {
			int result = edgeLayer.hashCode();
			result = 31 * result + (variant != null ? variant.hashCode() : 0);
			return result;
		}

		@Override
		public String getLayer() {
			return edgeLayer;
		}

		@Override
		public String getVariant() {
			return variant;
		}
	}

	public LayerRef getNodeLayerRef(final String nodeLayer, final String variant) {
		return new NodeLayerRef(nodeLayer, variant);
	}

	public LayerRef getEdgeLayerRef(final String edgeLayer, final String variant) {
		return new EdgeLayerRef(edgeLayer, variant);
	}
	*/

	public abstract void remove(NodeRef nodeId);
	public abstract void remove(EdgeRef edgeId);

    public void remove(DocumentNodeLayer layer) {
        List<NodeRef> nodes = StreamSupport.stream(layer.spliterator(), false).collect(Collectors.toList());
        nodes.forEach(this::remove);
    }

    public void remove(DocumentEdgeLayer layer) {
        List<EdgeRef> edges = StreamSupport.stream(layer.spliterator(), false).collect(Collectors.toList());
        edges.forEach(this::remove);
    }

	public abstract EdgeRef getEdge(String uniqueRef);
	public abstract NodeRef getNode(String uniqueRef);

	public abstract String getText();
	public abstract void setText(String text);

    /**
     * Creates an edge.
     * <p>
     * <b>Remarks:</b>
     * When creating many edges use: {@link #edgeLayer(String)} and then {@link DocumentEdgeLayer#create()}
     * or {@link DocumentEdgeLayer#create(NodeRef, NodeRef)}
     */
	public abstract EdgeRef createEdge(String edgeLayer);

    /**
     * Creates a node.
     * <p>
     * <b>Remarks:</b>
     * When creating many nodes use: {@link #nodeLayer(String)} and then {@link DocumentNodeLayer#create()}}
     * or {@link DocumentNodeLayer#create(int, int)}
     */
	public abstract NodeRef createNode(String nodeLayer);

    /**
     * Get node layer
     * @param nodeLayer the node layer
     */
    public DocumentNodeLayer nodeLayer(String nodeLayer) {
        return nodeLayer(nodeLayer);
    }

    /**
     * Get a node layer representation for faster creation of nodes
     * @param nodeLayer   node layer type
     * @param nodeVariant node variant
     */
    public DocumentNodeLayer nodeLayer(String nodeLayer, String nodeVariant) {
        return new DocumentNodeLayer() {
            private String realNodeLayer = nodeLayer;
            private String realNodeVariant = nodeVariant;

            @Override
            public String getLayer() {
                return realNodeLayer;
            }

            @Override
            public String getVariant() {
                return realNodeVariant;
            }

            @Override
            public NodeRef create() {
                return DocumentStore.this.createNode(realNodeLayer, realNodeVariant);
            }

            @Override
            public NodeRef create(int start, int end) {
                NodeRef node = DocumentStore.this.createNode(realNodeLayer, realNodeVariant);
                node.get().setRanges(start, end);
                return node;
            }

            @Override
            public void migrate(String newLayer) {
                migrate(newLayer, realNodeVariant);
            }

            @Override
            public void migrate(String newLayer, String variant) {
                DocumentNodeLayer newNodeLayer = nodeLayer(newLayer, variant);
                for (NodeRef nodeRef : this) {
                    NodeStore nodeStore = nodeRef.get();
                    NodeStore newNodeStore;
                    if(nodeStore.isAnnotation()) {
                        newNodeStore = newNodeLayer.create(nodeStore.getStart(), nodeStore.getEnd()).get();
                    } else {
                        newNodeStore = newNodeLayer.create().get();
                    }

                    for (Map.Entry<String, DataRef> entry : nodeStore.properties()) {
                        newNodeStore.putProperty(entry.getKey(), entry.getValue());
                    }

                    DocumentEngine engine = getDocument().engine();
                    for (EdgeRef inbound : engine.edges(nodeStore, Direction.IN)) {
                        inbound.get().connect(inbound.get().getTail(), newNodeStore);
                    }

                    for (EdgeRef outbound : engine.edges(nodeStore, Direction.OUT)) {
                        outbound.get().connect(newNodeStore, outbound.get().getHead());
                    }
                }

                DocumentStore.this.remove(this);

                this.realNodeLayer = newLayer;
                this.realNodeVariant = variant;
            }

            @Override
            public void remove(NodeRef ref) {
                if(!ref.layer().equal(this)) {
                    throw new IllegalArgumentException("The given node does not belong to this layer!");
                }

                DocumentStore.this.remove(ref);
            }

            @Override
            public int size() {
                return (int)StreamSupport.stream(this.spliterator(), false).count();
            }

            @Override
            public Iterator<NodeRef> iterator() {
                return getDocument().engine().nodes(realNodeLayer, realNodeVariant).iterator();
            }
        };
    }

    /**
     * Get low-level access to the edge layer
     * @param edgeLayer edge layer type
     */
    public DocumentEdgeLayer edgeLayer(String edgeLayer) {
        return edgeLayer(edgeLayer, null);
    }

    /**
     * Get low-level access to the edge layer
     * @param edgeLayer   edge layer type
     * @param edgeVariant edge variant
     */
    public DocumentEdgeLayer edgeLayer(String edgeLayer, String edgeVariant) {
        //This is a slow default implementation, that should be overridden for optimal performance
        return new DocumentEdgeLayer() {
            private String realEdgeLayer = edgeLayer;
            private String realEdgeVariant = edgeVariant;

            @Override
            public String getLayer() {
                return realEdgeLayer;
            }

            @Override
            public String getVariant() {
                return realEdgeVariant;
            }

            @Override
            public EdgeRef create() {
                return DocumentStore.this.createEdge(realEdgeLayer, realEdgeVariant);
            }

            @Override
            public EdgeRef create(NodeRef tail, NodeRef head) {
                EdgeRef e = DocumentStore.this.createEdge(realEdgeLayer, realEdgeVariant);
                e.get().connect(tail, head);
                return e;
            }

			@Override
			public void migrate(String newLayer) {
                migrate(newLayer, realEdgeVariant);
			}

            @Override
            public void migrate(String newLayer, String variant) {
                DocumentEdgeLayer newLayerType = edgeLayer(newLayer, realEdgeVariant);

                //Create new edges
                for (EdgeRef edgeRef : this) {
                    EdgeStore edgeStore = edgeRef.get();
                    EdgeRef newEdge = newLayerType.create(edgeStore.getTail(), edgeStore.getHead());
                    EdgeStore newEdgeStore = newEdge.get();

                    for (Map.Entry<String, DataRef> entry : edgeStore.properties()) {
                        newEdgeStore.putProperty(entry.getKey(), entry.getValue());
                    }
                }

                //Delete old ones
                DocumentStore.this.remove(this);

                this.realEdgeLayer = newLayer;
                this.realEdgeVariant = variant;
            }

            @Override
            public void remove(EdgeRef ref) {
                if(!ref.layer().equal(this)) {
                    throw new IllegalArgumentException("The given edge does not belong to this layer!");
                }

                DocumentStore.this.remove(ref);
            }

            @Override
            public int size() {
                return (int)StreamSupport.stream(this.spliterator(), false).count();
            }

            @Override
            public Iterator<EdgeRef> iterator() {
                return getDocument().engine().edges(realEdgeLayer, realEdgeVariant).iterator();
            }
        };
    }

	public EdgeRef createEdge(String edgeLayer, String edgeVariant) {
		EdgeRef edgeRef = createEdge(edgeLayer);
		if(edgeVariant != null)
			edgeRef.get().putProperty("@variant", edgeVariant);
		
		return edgeRef;
	}
	
	public NodeRef createNode(String nodeLayer, String nodeVariant) {
		NodeRef nodeRef = createNode(nodeLayer);
		if(nodeVariant != null)
			nodeRef.get().putProperty("@variant", nodeVariant);
		
		return nodeRef;
	}

    /**
     * Appends a document's store content to this one
     *
     * <b>Remarks:</b> Default variants are NOT merged, all variants are added.
     * The source document store will not be modified.
     * @param store the source document store to append
     */
    public void append(DocumentStore store, StringBuilder sb) {
        int offset = sb.length();
        sb.append(store.getText());

        IdentityHashMap<NodeRef,NodeRef> nodes = new IdentityHashMap<NodeRef, NodeRef>();

        //1. Add all nodes and annotations and adjust all ranges with an offset
        for(NodeRef source : store.nodes()) {
            NodeStore sourceStore = source.get();

            NodeRef target = createNode(sourceStore.getLayer(), sourceStore.getVariant());
            NodeStore targetStore = target.get();

			if(sourceStore.isAnnotation()) {
				targetStore.setRanges(sourceStore.getStart() + offset, sourceStore.getEnd() + offset);
			}

            for(Map.Entry<String,DataRef> entry : sourceStore.properties()) {
                targetStore.putProperty(entry.getKey(), entry.getValue());
            }

            nodes.put(source, target);
        }

        //2. Add all edges
        for(EdgeRef source : store.edges()) {
            EdgeStore sourceStore = source.get();
            NodeRef sourceHead = sourceStore.getHead();
            NodeRef sourceTail = sourceStore.getTail();

            EdgeRef target = createEdge(sourceStore.getLayer(), sourceStore.getVariant());
            EdgeStore targetStore = target.get();

            targetStore.connect(nodes.get(sourceTail), nodes.get(sourceHead));

            for(Map.Entry<String,DataRef> entry : sourceStore.properties()) {
                targetStore.putProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    /** Indicate if this store is read only */
	public boolean isReadOnly() {
		return false;
	}
}

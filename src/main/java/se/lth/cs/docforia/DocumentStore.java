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

import java.util.*;
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
	
	public Map<String,String> getDefaultEdgeVariants() { return Collections.emptyMap(); }
	public Map<String,String> getDefaultNodeVariants() { return Collections.emptyMap(); }
	
	public void setDefaultNodeVariant(Map<String,String> defaultVariants)
	{
		for(Map.Entry<String, String> entry : defaultVariants.entrySet())
			setDefaultNodeVariant(entry.getKey(), entry.getValue());
	}
	
	public void setDefaultEdgeVariant(Map<String,String> defaultVariants)
	{
		for(Map.Entry<String, String> entry : defaultVariants.entrySet())
			setDefaultEdgeVariant(entry.getKey(), entry.getValue());
	}

	protected void markNodeAsRemoved(Node node) {
		node.store = null;
	}
	protected void markEdgeAsRemoved(Edge edge) {
		edge.store = null;
	}

	public void setDefaultNodeVariant(String nodeLayer, String variant) { throw new UnsupportedOperationException(); }
	public void setDefaultEdgeVariant(String edgeLayer, String variant) { throw new UnsupportedOperationException(); }
	
	public void migrateNodesToVariant(String nodeLayer, String prevVariant, String newVariant) { throw new UnsupportedOperationException(); }
	public void migrateEdgesToVariant(String edgeLayer, String prevVariant, String newVariant) { throw new UnsupportedOperationException(); }

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

	public abstract void remove(NodeRef nodeId);
	public abstract void remove(EdgeRef edgeId);
	
	public abstract EdgeRef getEdge(String uniqueRef);
	public abstract NodeRef getNode(String uniqueRef);

	public abstract String getText();
	public abstract void setText(String text);

	//TODO: Remove all variant metadata stuff.
	public abstract String getVariantMetadata(String variant, String key);
	public abstract boolean hasVariantMetadata(String variant, String key);
	public abstract void putVariantMetadata(String variant, String key, String value);
	public abstract void removeVariantMetadata(String variant);
	public abstract void removeVariantMetadata(String variant, String key);

	public abstract Iterable<Map.Entry<String,String>> variantMetadata(String variant);
	public abstract Iterable<String> variantsWithMetadata();

	public abstract EdgeRef createEdge(String edgeLayer);
	public abstract NodeRef createNode(String nodeLayer);

    /**
     * Get a node layer representation for faster creation of nodes
     * @param nodeLayer   node layer type
     * @param nodeVariant node variant
     */
    public DocumentNodeLayer nodeLayer(String nodeLayer, String nodeVariant) {
        return new DocumentNodeLayer() {
            @Override
            public LayerRef layer() {
                return getNodeLayerRef(nodeLayer, nodeVariant);
            }

            @Override
            public NodeRef create() {
                return DocumentStore.this.createNode(nodeLayer, nodeVariant);
            }

            @Override
            public NodeRef create(int start, int end) {
                NodeRef node = DocumentStore.this.createNode(nodeLayer, nodeVariant);
                node.get().setRanges(start, end);
                return node;
            }

            @Override
            public int size() {
                return (int)StreamSupport.stream(this.spliterator(), false).count();
            }

            @Override
            public Iterator<NodeRef> iterator() {
                return getDocument().engine().nodes(nodeLayer, nodeVariant).iterator();
            }
        };
    }

    /**
     * Get a edge layer representation for faster creation of nodes
     * @param edgeLayer   node layer type
     * @param edgeVariant node variant
     */
    public DocumentEdgeLayer edgeLayer(String edgeLayer, String edgeVariant) {
        return new DocumentEdgeLayer() {
            @Override
            public LayerRef layer() {
                return getEdgeLayerRef(edgeLayer, edgeVariant);
            }

            @Override
            public EdgeRef create() {
                return DocumentStore.this.createEdge(edgeLayer, edgeVariant);
            }

            @Override
            public EdgeRef create(NodeRef tail, NodeRef head) {
                EdgeRef e = DocumentStore.this.createEdge(edgeLayer, edgeVariant);
                e.get().connect(tail, head);
                return e;
            }

            @Override
            public int size() {
                return (int)StreamSupport.stream(this.spliterator(), false).count();
            }

            @Override
            public Iterator<EdgeRef> iterator() {
                return getDocument().engine().edges(edgeLayer, edgeVariant).iterator();
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

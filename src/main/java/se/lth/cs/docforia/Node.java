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
import se.lth.cs.docforia.util.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.IntStream;

/**
 * Node abstraction
 * @param <T> The inherting class type, is used to allow chaining with type checking.
 */
@SuppressWarnings("unchecked")
public abstract class Node<T extends Node<T>>
        implements Iterable<Map.Entry<String,DataRef>>,
        PropertyStoreProxy<T>, CharSequence, ComparableRange
{
	protected DocumentProxy doc;
	protected NodeStore store;
	protected HashMap<String,Object> tags;

	public Node() {

	}

	/**
	 * Create a new node in selected document, using class type as layer
	 * @param doc the document to add this node to
	 */
	public Node(DocumentProxy doc) {
		doc.add(this);
	}

    /** Called by the storage implementation, when this representation has been initialized. */
	protected void initialized() {

	}

	public T setRange(Range range) {
		return setRange(range.getStart(), range.getEnd());
	}

	public T setRange(int start, int end) {
		if(doc instanceof View) {
			if (doc.inverseTransform(start) < ((View) doc).getParent().getStart() ||
				doc.inverseTransform(end) > ((View) doc).getParent().getEnd())
				throw new IllegalArgumentException("start or end, the condition: start >= 0 && end >= 0 in orignal document must hold.");

			if (end < start)
				throw new IllegalArgumentException("end < start, negative length, this is illegal!");

			if (doc.inverseTransform(start) > ((View) doc).getParent().length())
				throw new IllegalArgumentException("annotation is larger than document!");
		}
		else {
			if (start < 0 || end < 0)
				throw new IllegalArgumentException("start or end, the condition: start >= 0 && end >= 0 must hold.");

			if (end < start)
				throw new IllegalArgumentException("end < start, negative length, this is illegal!");

			if (start > doc.length())
				throw new IllegalArgumentException("annotation (" + start + "-" + end + ") is larger than document (" + doc.length() + ")!");
		}

		MutableRange range = new MutableRange(start, end);
		doc.inverseTransform(range);

		this.store.setRanges(range.getStart(), range.getEnd());
		return (T)this;
	}

    /**
     * Set the variant of this node.
     * @param variant the variant name
     * @return this representation
     */
	@SuppressWarnings("unchecked")
	public T setVariant(String variant) {
		if(variant == null)
			throw new NullPointerException("variant");

		store.setVariant(variant);
		return (T)this;
	}

	@SuppressWarnings("unchecked")
	public T setVariant(Optional<String> variant) {
		store.setVariant(variant.isPresent() ? variant.get() : null);
		return (T)this;
	}

	/**
	 * Check if nodes have identical ranges
	 * @param node node to compare against
	 * @return true fi ranges are equal
	 */
	public boolean rangeEquals(Node node) {
		if(node.isAnnotation()) {
			return node.getStart() == this.getStart() && node.getEnd() == this.getEnd();
		}
		else
			return false;
	}

    /**
     * Get the parent document this node is associated with
     * @return parent document
     */
    public final DocumentProxy getProxy() {
        return doc;
    }

	/**
	 * Get the parent document this node is associated with
	 * @return parent document
	 */
	public final Document getDocument() {
		if(doc instanceof DocumentTransaction)
			return ((DocumentTransaction) doc).doc;
		else
			return (Document)doc;
	}

    //========================================================================================================

	private abstract static class EdgePointSelector {
		public Class<?> getEndpointType(final Node source, final Edge e) {
			return getEndpoint(source, e).getClass();
		}
		
		public abstract Node getEndpoint(final Node source, final Edge e);
	}
	
	private static class InboundSelector extends EdgePointSelector {
		@Override
		public final Node getEndpoint(final Node source, final Edge e) {
			return e.getTail();
		}
	}
	
	private static class OutboundSelector extends EdgePointSelector {
		@Override
		public final Node getEndpoint(final Node source, final Edge e) {
			return e.getHead();
		}
	}
	
	private static class ConnectedSelector extends EdgePointSelector {
		@Override
		public final Node getEndpoint(final Node source, final Edge e) {
			if(e.getHead() == source)
				return e.getTail();
			else
				return e.getHead();
		}
	}

	//Singletons, thread-safe instances of methods above. They are emulated function values.
	private static final InboundSelector inboundSelector = new InboundSelector();
	private static final OutboundSelector outboundSelector = new OutboundSelector();
	private static final ConnectedSelector connectedSelector = new ConnectedSelector();

    //========================================================================================================


	/**
     * Project all nodes from this node via edges (using edges with a head to this node)
     * @param nodeType the node to find from this node
     * @param edgeType the edges to go via
     * @param <N> Node tyoe
     * @param <E> Edge type
     * @return iterable
     */
    public <N extends Node, E extends Edge> List<N> projectInbound(Class<N> nodeType, Class<E> edgeType) {
        return projectInbound(nodeType, edgeType, true);
    }

	/**
	 * Project all nodes from this node via edges (using edges with a head to this node)
	 * @param nodeType the node to find from this node
	 * @param edgeType the edges to go via
     * @param includeStart include the node you start from
	 * @param <N> Node type
	 * @param <E> Edge type
     * @return iterable
     */
	public <N extends Node, E extends Edge> List<N> projectInbound(Class<N> nodeType, Class<E> edgeType, boolean includeStart) {
        //TODO: Check the use of nodeType, and possibly fix!
		LayerRef layer = this.getRef().layer();
		ArrayList<N> nodes = new ArrayList<>();
		for (NodeRef nodeRef : getDocument().engine().projectInbound(
				this.getRef(),
                includeStart,
				layer.getLayer(),
				layer.getVariant(),
				edgeType.getName(),
				null))
		{
			nodes.add((N)doc.representation(nodeRef));
		}
		return nodes;
	}

    /*
    //TODO: Future implementation
    public <N extends Node, E extends Edge> List<N> shortestPathTo(N node, Class<E> viaEdges) {

    }

    public <N extends Node, E extends Edge> List<N> shortestWeightedPathTo(N node, Function<E,Double> weightfn, Class<E> viaEdges) {

    }
    */

    /**
     * Project all nodes from this node via edges (using edges with a tail from this node)
     * @param nodeType the node to find from this node
     * @param edgeType the edges to go via
     * @param <N> Node type
     * @param <E> Edge type
     * @return iterable
     */
    public <N extends Node, E extends Edge> List<N> projectOutbound(Class<N> nodeType, Class<E> edgeType) {
        return projectOutbound(nodeType, edgeType, true);
    }


    /**
	 * Project all nodes from this node via edges (using edges with a tail from this node)
	 * @param nodeType the node to find from this node
	 * @param edgeType the edges to go via
	 * @param <N> Node tye
	 * @param <E> Edge type
	 * @return iterable
	 */
	public <N extends Node, E extends Edge> List<N> projectOutbound(Class<N> nodeType, Class<E> edgeType, boolean includeStart) {
		//TODO: Check the use of nodeType, and possibly fix!
		LayerRef layer = this.getRef().layer();
		ArrayList<N> nodes = new ArrayList<>();
		DocumentRepresentations documentRepresentations = getDocument().representations();
		for (NodeRef nodeRef : getDocument().engine().projectOutbound(
				this.getRef(),
                includeStart,
				layer.getLayer(),
				layer.getVariant(),
				edgeType.getName(),
				null))
		{
			nodes.add((N) documentRepresentations.get(nodeRef));
		}
		return nodes;
	}

    @Override
    public PropertyStore store() {
        return store;
    }
    //========================================================================================================

    /**
     * Add an edge from this node to another
     * @param head the head of the edge
     * @param edge the edge (might be unitialized, will then be initialized)
     * @param <E> type of edge
     * @return edge
     */
	public <E extends Edge> E connect(Node head, E edge) {
        if(edge.store == null)
            edge = doc.add(edge);

		edge.connect(this, head);
		return edge;
	}

    //========================================================================================================

	private <N extends Node> DocumentIterable<N> getEdgeNodeIterable(
			final Class<N> type,
			final EdgePointSelector selector, 
			final Iterable<Edge> edges
	) {
		return new FilteredMappedDocumentIterable<N, Edge>(edges) {

			@Override
			protected boolean accept(Edge current) {
				return selector.getEndpointType(Node.this, current).equals(type);
			}

			@SuppressWarnings("unchecked")
			@Override
			protected N map(Edge value) {
				return (N)selector.getEndpoint(Node.this, value);
			}
		};
	}

    private <N extends Node> DocumentIterable<N> getEdgeNodeIterable(
            final String dynamicType,
            final EdgePointSelector selector,
            final Iterable<Edge> edges
    ) {
        final String type = "@" + dynamicType;
        return new FilteredMappedDocumentIterable<N, Edge>(edges) {

            @Override
            protected boolean accept(Edge current) {
                return selector.getEndpoint(Node.this, current).store.getLayer().equals(type);
            }

            @SuppressWarnings("unchecked")
            @Override
            protected N map(Edge value) {
                return (N)selector.getEndpoint(Node.this, value);
            }
        };
    }

    //========================================================================================================

	private <E extends Edge, N extends Node> DocumentIterable<E> edgeIterableFilter(
			final Class<E> edgeType,
			final Class<N> nodeType,
			final EdgePointSelector selector,
			final Iterable<E> edges
	) {
		return new FilteredDocumentIterable<E>(edges) {

			@Override
			protected boolean accept(E current) {
				return selector.getEndpointType(Node.this, current).equals(nodeType) ;
			}
		};
	}

    private <N extends Node> DocumentIterable<Edge> getEdgeIterable(
            final Class<N> nodeType,
            final EdgePointSelector selector,
            final Iterable<Edge> edges
    ) {
        return new FilteredDocumentIterable<Edge>(edges) {

            @Override
            protected boolean accept(Edge current) {
                return selector.getEndpointType(Node.this, current).equals(nodeType);
            }
        };
    }

    private <E extends Edge> DocumentIterable<E> getEdgeIterable(
            final Class<E> edgeType,
            final String dynamicNodeType,
            final EdgePointSelector selector,
            final Iterable<E> edges
    ) {
        final String nodeType = "@" + dynamicNodeType;
        return new FilteredDocumentIterable<E>(edges) {

            @Override
            protected boolean accept(E current) {
                if (current.store.getLayer().equals(edgeType))
                    return selector.getEndpoint(Node.this, current).store.getLayer().equals(nodeType);
                else
                    return false;
            }
        };
    }

    private DocumentIterable<Edge> getEdgeIterable(
            final String dynamicNodeType,
            final EdgePointSelector selector,
            final Iterable<Edge> edges
    ) {
        final String nodeType = "@" + dynamicNodeType;
        return new FilteredDocumentIterable<Edge>(edges) {

            @Override
            protected boolean accept(Edge current) {
                return selector.getEndpoint(Node.this, current).store.getLayer().equals(nodeType);
            }
        };
    }

    //========================================================================================================
	
	private <N extends Node, E extends Edge> DocumentIterable<NodeEdge<N,E>> getFilteredEdgeNodeIteratable(
            final Class<E> edgeType,
            final Class<N> nodeType,
			final EdgePointSelector selector, 
			final Iterable<E> edges
	) {
		return new FilteredMappedDocumentIterable<NodeEdge<N,E>, E>(edges) {

			@Override
			protected boolean accept(E current) {
				return selector.getEndpointType(Node.this, current).equals(nodeType);
			}

			@SuppressWarnings("unchecked")
			@Override
			protected NodeEdge<N, E> map(E current) {
				return new NodeEdge<N, E>((N)selector.getEndpoint(Node.this, current), current);
			}
			
		};
	}
	
	private <N extends Node, E extends Edge> DocumentIterable<N> getFilteredNodeIteratable(
            final Class<E> edgeType,
			final Class<N> nodeType, 
			final EdgePointSelector selector, 
			final Iterable<E> edges
	) {
		return new FilteredMappedDocumentIterable<N, E>(edges) {

			@Override
			protected boolean accept(E current) {
				return selector.getEndpointType(Node.this, current).equals(nodeType);
			}

			@SuppressWarnings("unchecked")
			@Override
			protected N map(E current) {
				return (N)selector.getEndpoint(Node.this, current);
			}
			
		};
	}

    private <N extends Node> DocumentIterable<N> getFilteredNodeIteratable(
            final String dynamicNodeType,
            final EdgePointSelector selector,
            final Iterable<Edge> edges
    ) {
        final String nodeType = "@" + dynamicNodeType;
        return new FilteredMappedDocumentIterable<N, Edge>(edges) {

            @Override
            protected boolean accept(Edge current) {
                return selector.getEndpoint(Node.this, current).store.getLayer().equals(nodeType);
            }

            @SuppressWarnings("unchecked")
            @Override
            protected N map(Edge current) {
                return (N)selector.getEndpoint(Node.this, current);
            }

        };
    }

    private <N extends Node, E extends Edge> DocumentIterable<N> getFilteredNodeIteratable(
            final Class<E> edgeType,
            final String dynamicNodeType,
            final EdgePointSelector selector,
            final Iterable<E> edges
    ) {
        final String nodeType = "@" + dynamicNodeType;
        return new FilteredMappedDocumentIterable<N, E>(edges) {

            @Override
            protected boolean accept(E current) {
                return selector.getEndpoint(Node.this, current).store.getLayer().equals(nodeType);
            }

            @SuppressWarnings("unchecked")
            @Override
            protected N map(E current) {
                return (N)selector.getEndpoint(Node.this, current);
            }

        };
    }

    //========================================================================================================
	
	private DocumentIterable<Node> getNodesIterable(final EdgePointSelector selector, final Iterable<Edge> edges) {
		return new MappedDocumentIterable<Node, Edge>(edges) {

			@Override
			protected Node map(Edge edge) {
				return selector.getEndpoint(Node.this, edge);
			}
			
		};
	}
	
	private DocumentIterable<NodeEdge<Node,Edge>> getEdgeNodeIterable(final EdgePointSelector selector, final Iterable<Edge> edges) {
		return new MappedDocumentIterable<NodeEdge<Node,Edge>, Edge>(edges) {

			@Override
			protected NodeEdge<Node,Edge> map(Edge edge) {
				return new NodeEdge<Node, Edge>(selector.getEndpoint(Node.this, edge), edge);
			}
			
		};
	}

    //========================================================================================================

	public DocumentIterable<Node> inboundNodes() {
		return getNodesIterable(inboundSelector, getDocument().edges(this,Direction.IN));
	}
	
	public DocumentIterable<Node> connectedNodes() {
		return getNodesIterable(connectedSelector, getDocument().edges(this,Direction.BOTH));
	}
	
	public DocumentIterable<Node> outboundNodes() {
		return getNodesIterable(outboundSelector, getDocument().edges(this,Direction.OUT));
	}

    //========================================================================================================

	public DocumentIterable<NodeEdge<Node,Edge>> inboundNodeEdges() {
		return getEdgeNodeIterable(inboundSelector, getDocument().edges(this,Direction.IN));
	}
	
	public DocumentIterable<NodeEdge<Node,Edge>> connectedNodeEdges() {
		return getEdgeNodeIterable(connectedSelector, getDocument().edges(this,Direction.BOTH));
	}
	
	public DocumentIterable<NodeEdge<Node,Edge>> outboundNodeEdges() {
		return getEdgeNodeIterable(outboundSelector, getDocument().edges(this,Direction.OUT));
	}

    //========================================================================================================
	
 	public <N extends Node> DocumentIterable<N> inboundNodes(final Class<N> type) {
 		return getEdgeNodeIterable(type, inboundSelector, getDocument().edges(this, Direction.IN));
	}
 	
 	public <N extends Node> DocumentIterable<N> outboundNodes(final Class<N> type) {
 		return getEdgeNodeIterable(type, outboundSelector, getDocument().edges(this, Direction.OUT));
	}
 	
 	public <N extends Node> DocumentIterable<N> connectedNodes(final Class<N> type) {
 		return getEdgeNodeIterable(type, connectedSelector, getDocument().edges(this, Direction.BOTH));
	}

    //========================================================================================================

    public <N extends Node> DocumentIterable<N> inboundNodes(final String dynamicNodeType) {
        return getEdgeNodeIterable(dynamicNodeType, inboundSelector, getDocument().edges(this, Direction.IN));
    }

    public <N extends Node> DocumentIterable<N> outboundNodes(final String dynamicNodeType) {
        return getEdgeNodeIterable(dynamicNodeType, outboundSelector, getDocument().edges(this, Direction.OUT));
    }

    public <N extends Node> DocumentIterable<N> connectedNodes(final String dynamicNodeType) {
        return getEdgeNodeIterable(dynamicNodeType, connectedSelector, getDocument().edges(this, Direction.BOTH));
    }

    //========================================================================================================

 	public <N extends Node, E extends Edge> DocumentIterable<N> outboundNodes(
 			final Class<E> edgeType, 
 			final Class<N> nodeType
 	) {
 		return getFilteredNodeIteratable(edgeType, nodeType, outboundSelector, getDocument().edges(this, edgeType, Direction.OUT));
 	}
 	
 	public <N extends Node, E extends Edge> DocumentIterable<N> inboundNodes(
 			final Class<E> edgeType, 
 			final Class<N> nodeType
 	) {
 		return getFilteredNodeIteratable(edgeType, nodeType, inboundSelector, getDocument().edges(this, edgeType, Direction.IN));
 	}
 	
 	public <N extends Node, E extends Edge> DocumentIterable<N> connectedNodes(
 			final Class<E> edgeType, 
 			final Class<N> nodeType
 	) {
 		return getFilteredNodeIteratable(edgeType, nodeType, connectedSelector, getDocument().edges(this, edgeType, Direction.BOTH));
 	}

    //========================================================================================================

    public <N extends Node> DocumentIterable<N> outboundNodes(
            final String dynamicEdgeType,
            final Class<N> nodeType
    ) {
        return getFilteredNodeIteratable(Edge.class, nodeType, outboundSelector, getDocument().edges(this, dynamicEdgeType, Direction.OUT));
    }

    public <N extends Node> DocumentIterable<N> inboundNodes(
            final String dynamicEdgeType,
            final Class<N> nodeType
    ) {
        return getFilteredNodeIteratable(Edge.class, nodeType, inboundSelector, getDocument().edges(this, dynamicEdgeType, Direction.IN));
    }

    public <N extends Node> DocumentIterable<N> connectedNodes(
            final String dynamicEdgeType,
            final Class<N> nodeType
    ) {
        return getFilteredNodeIteratable(Edge.class, nodeType, connectedSelector, getDocument().edges(this, dynamicEdgeType, Direction.BOTH));
    }

    //========================================================================================================

    public DocumentIterable<Node> outboundNodes(
            final String dynamicEdgeType,
            final String dynamicNodeType
    ) {
        return getFilteredNodeIteratable(dynamicNodeType, outboundSelector, getDocument().edges(this, dynamicEdgeType, Direction.OUT));
    }

    public DocumentIterable<Node> inboundNodes(
            final String dynamicEdgeType,
            final String dynamicNodeType
    ) {
        return getFilteredNodeIteratable(dynamicNodeType, inboundSelector, getDocument().edges(this, dynamicEdgeType, Direction.IN));
    }

    public DocumentIterable<Node> connectedNodes(
            final String dynamicEdgeType,
            final String dynamicNodeType
    ) {
        return getFilteredNodeIteratable(dynamicNodeType, connectedSelector, getDocument().edges(this, dynamicEdgeType, Direction.BOTH));
    }

    //========================================================================================================

    public <E extends Edge> DocumentIterable<Node> outboundNodes(
            final Class<E> edgeType,
            final String dynamicNodeType
    ) {
        return getFilteredNodeIteratable(edgeType, dynamicNodeType, outboundSelector, getDocument().edges(this, edgeType, Direction.OUT));
    }

    public <E extends Edge> DocumentIterable<Node> inboundNodes(
            final Class<E> edgeType,
            final String dynamicNodeType
    ) {
        return getFilteredNodeIteratable(edgeType,dynamicNodeType, inboundSelector, getDocument().edges(this, edgeType, Direction.IN));
    }

    public <E extends Edge> DocumentIterable<Node> connectedNodes(
            final Class<E> edgeType,
            final String dynamicNodeType
    ) {
        return getFilteredNodeIteratable(edgeType, dynamicNodeType, connectedSelector, getDocument().edges(this, edgeType, Direction.BOTH));
    }

    //========================================================================================================

 	public <N extends Node, E extends Edge> DocumentIterable<NodeEdge<N,E>> outboundNodeEdges(
 			final Class<E> edgeType, 
 			final Class<N> nodeType
 	) {
 		return getFilteredEdgeNodeIteratable(edgeType, nodeType, outboundSelector, getDocument().edges(this, edgeType, Direction.OUT));
 	}
 	
 	public <N extends Node, E extends Edge> DocumentIterable<NodeEdge<N,E>> inboundNodeEdges(
 			final Class<E> edgeType, 
 			final Class<N> nodeType
 	) {
 		return getFilteredEdgeNodeIteratable(edgeType, nodeType, inboundSelector, getDocument().edges(this, edgeType, Direction.IN));
 	}
 	
 	public <N extends Node, E extends Edge> DocumentIterable<NodeEdge<N,E>> connectedNodeEdges(
 			final Class<E> edgeType, 
 			final Class<N> nodeType
 	) {
 		return getFilteredEdgeNodeIteratable(edgeType, nodeType, connectedSelector, getDocument().edges(this, edgeType, Direction.BOTH));
 	}

    //========================================================================================================

	public <E extends Edge> DocumentIterable<E> inboundEdges(final Class<E> type) {
		return getDocument().edges(this, type, Direction.IN);
	}
	
	public <E extends Edge> DocumentIterable<E> outboundEdges(final Class<E> type) {
		return getDocument().edges(this, type, Direction.OUT);
	}
	
	public <E extends Edge> DocumentIterable<E> connectedEdges(final Class<E> type) {
		return getDocument().edges(this, type, Direction.BOTH);
	}

    //========================================================================================================

    public <E extends Edge> DocumentIterable<E> inboundEdges(final String dynamicType) {
        return getDocument().edges(this, dynamicType, Direction.IN);
    }

    public <E extends Edge> DocumentIterable<E> outboundEdges(final String dynamicType) {
        return getDocument().edges(this, dynamicType, Direction.OUT);
    }

    public <E extends Edge> DocumentIterable<E> connectedEdges(final String dynamicType) {
        return getDocument().edges(this, dynamicType, Direction.BOTH);
    }

    //========================================================================================================
	
	public DocumentIterable<Edge> inboundEdges() {
		return DocumentIterables.wrap(getDocument().edges(this, Direction.IN));
	}
	
	public DocumentIterable<Edge> outboundEdges() {
		return DocumentIterables.wrap(getDocument().edges(this, Direction.OUT));
	}
	
	public DocumentIterable<Edge> connectedEdges() {
		return DocumentIterables.wrap(getDocument().edges(this, Direction.BOTH));
	}

    //========================================================================================================
	
	public  <N extends Node, E extends Edge> DocumentIterable<E> inboundEdges(final Class<E> edgeType, final Class<N> nodeType) {
		return edgeIterableFilter(edgeType, nodeType, inboundSelector, inboundEdges(edgeType));
	}
	
	public  <N extends Node, E extends Edge> DocumentIterable<E> outboundEdges(final Class<E> edgeType, final Class<N> nodeType) {
		return edgeIterableFilter(edgeType, nodeType, outboundSelector, outboundEdges(edgeType));
	}
	
	public  <N extends Node, E extends Edge> DocumentIterable<E> connectedEdges(final Class<E> edgeType, final Class<N> nodeType) {
		return edgeIterableFilter(edgeType, nodeType, connectedSelector, connectedEdges(edgeType));
	}

    //========================================================================================================

    public  <N extends Node> DocumentIterable<Edge> inboundEdges(final String edgeType, final Class<N> nodeType) {
        return getEdgeIterable(nodeType, inboundSelector, inboundEdges("@" + edgeType));
    }

    public  <N extends Node> DocumentIterable<Edge> outboundEdges(final String edgeType, final Class<N> nodeType) {
        return getEdgeIterable(nodeType, outboundSelector, outboundEdges("@" + edgeType));
    }

    public  <N extends Node> DocumentIterable<Edge> connectedEdges(final String edgeType, final Class<N> nodeType) {
        return getEdgeIterable(nodeType, connectedSelector, connectedEdges("@" + edgeType));
    }

    //========================================================================================================

    public  <N extends Node, E extends Edge> DocumentIterable<E> inboundEdges(final Class<E> edgeType, final String nodeType) {
        return getEdgeIterable(edgeType, nodeType, inboundSelector, inboundEdges(edgeType));
    }

    public  <N extends Node, E extends Edge> DocumentIterable<E> outboundEdges(final Class<E> edgeType, final String nodeType) {
        return getEdgeIterable(edgeType, nodeType, outboundSelector, outboundEdges(edgeType));
    }

    public  <N extends Node, E extends Edge> DocumentIterable<E> connectedEdges(final Class<E> edgeType, final String nodeType) {
        return getEdgeIterable(edgeType, nodeType, connectedSelector, connectedEdges(edgeType));
    }

    //========================================================================================================

    public DocumentIterable<Edge> inboundEdges(final String edgeType, final String nodeType) {
        return getEdgeIterable(nodeType, inboundSelector, inboundEdges("@" + edgeType));
    }

    public DocumentIterable<Edge> outboundEdges(final String edgeType, final String nodeType) {
        return getEdgeIterable(nodeType, outboundSelector, outboundEdges("@" + edgeType));
    }

    public DocumentIterable<Edge> connectedEdges(final String edgeType, final String nodeType) {
        return getEdgeIterable(nodeType, connectedSelector, connectedEdges("@" + edgeType));
    }

    //========================================================================================================
	
	protected final Map<String, Object> getTags() {
		if(tags == null)
			this.tags = new HashMap<String, Object>();
			
		return this.tags;
	}
	
	/**
	 * Check if a runtime induced tag exists
	 * @param tag
	 * @return
	 */
	public boolean hasTag(String tag) {
		return getTags().containsKey(tag);
	}
	
	/**
	 * Gets a runtime induced tag
	 * @param tag
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T getTag(String tag) {
		return (T)getTags().get(tag);
	}
	
	public String getLayer() {
		if(store.getLayer().charAt(0) == '@')
			return store.getLayer().substring(1);
		else
			return store.getLayer();
	}
	
	public Optional<String> getVariant() {
		return store.getVariant() == null ? Optional.empty() : Optional.of(store.getVariant());
	}
	
	/**
	 * Puts a tag for runtime use only, the value is NOT stored.
	 * @param tag
	 * @param value
	 */
	public void putTag(String tag, Object value) {
		getTags().put(tag, value);
	}
	
	public void removeTag(String tag) {
		if(tags != null)
			tags.remove(tag);
	}
	
	public final void clearTags() {
		if(tags != null) {
			tags = null;
		}
	}

    public Iterable<Map.Entry<String,DataRef>> properties() {
        return new Iterable<Entry<String, DataRef>>() {
            @Override
            public Iterator<Entry<String, DataRef>> iterator() {
                return Node.this.iterator();
            }
        };
    }
	
	@Override
	public Iterator<Entry<String, DataRef>> iterator() {
		return new Iterator<Map.Entry<String,DataRef>>() {
			
			final Iterator<Map.Entry<String,DataRef>> iter = store.properties().iterator();// doc.propertyHook.iterator(doc, Node.this).iterator();
			Map.Entry<String,DataRef> current;
			
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
			private boolean moveForward() {
				while(iter.hasNext()) {
					current = iter.next();
					if(current.getKey().charAt(0) != '_')
						return true;
				}
				
				current = null;
				return false;
			}
			
			@Override
			public Entry<String, DataRef> next() {
				if(current == null)
					moveForward();
				
				Map.Entry<String, DataRef> retVal = current;
				current = null;
				return retVal;
			}
			
			@Override
			public boolean hasNext() {
				if(current == null)
					return moveForward();
				else
					return true;
			}
		};
	}

	@Override
	public final int getStart() {
		return doc.transform(store.getStart());
	}

	@Override
	public final int getEnd() {
		return doc.transform(store.getEnd());
	}

	@Override
	public int length() {
		return getEnd()-getStart();
	}

	@Override
	public float getMidpoint() {
		return (getEnd() - getStart()) / 2.0f + getStart();
	}

	public NodeRef getRef() {
		return store;
	}

	@Override
	public int compareTo(Range o) {
		return Float.compare(getMidpoint(), o.getMidpoint());
	}

	public String text() {
		return doc.text(this.getStart(),this.getEnd());
	}

    @Override
    public char charAt(int index) {
        return text().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return text().subSequence(start,end);
    }

    @Override
    public IntStream chars() {
        return text().chars();
    }

    @Override
    public IntStream codePoints() {
        return text().codePoints();
    }

    public String textContext(int max_size, boolean pad) {
		if(getEnd()-getStart() >= max_size)
			return text();

		int remaining = max_size - (getEnd()-getStart());
		int left = remaining / 2;
		int right = remaining - left;

		if(pad) {
			left = getStart()-left;
			right = getEnd()+right;

			StringBuilder sb = new StringBuilder();

			if(left < 0) {
				for(int i = 0; i < Math.abs(left); i++) {
					sb.append(' ');
				}
				left = 0;
			}

			if(right > doc.length()) {
				sb.append(doc.text(left,doc.length()));
				for(int i = doc.length(); i < right; i++) {
					sb.append(' ');
				}
			} else {
				sb.append(doc.text(left,right));
			}
			return sb.toString();
		} else {
			left = Math.max(getStart()-left, 0);
			right = Math.min(doc.length(), getEnd()+right);
			return doc.text(left, right);
		}
	}

	public boolean intersects(Range range) {
		return getStart() < range.getEnd() && getEnd() > range.getStart();
	}

	public boolean covers(Range range) {
		return getStart() <= range.getStart() && getEnd() >= range.getEnd();
	}

	public boolean coveredBy(Range range) {
		return range.getStart() <= getStart() && range.getEnd() >= getEnd();
	}

	public boolean isAnnotation() {
		return store.isAnnotation();
	}

	public boolean hasDynamicLayer() {
		return false;
	}

	public boolean valid() {
		return this.store != null;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node entries = (Node) o;

        if (!store.equals(entries.store)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return store.hashCode();
    }

    @Override
	public String toString() {
        if(!valid())
            throw new IllegalStateException("Node has been removed!");
        else if(!isAnnotation())
            return "";
        else
            return text();
	}
}

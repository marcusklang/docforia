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

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import se.lth.cs.docforia.data.DataRef;
import se.lth.cs.docforia.exceptions.RepresentationException;
import se.lth.cs.docforia.util.DocumentIterable;
import se.lth.cs.docforia.util.MappedDocumentIterable;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Document representation cache for nodes and edges
 */
public class DocumentRepresentations {
	private final Document doc;
	private boolean throwOnFactoryFailure = false;

	public DocumentRepresentations(Document doc) {
        this(doc, true);
	}

	protected DocumentRepresentations(Document doc, boolean initializeMaps) {
		this.doc = doc;
        if(initializeMaps) {
            indexNodeRef = new Reference2ObjectOpenHashMap<NodeRef,Node>();
            indexEdgeRef = new Reference2ObjectOpenHashMap<EdgeRef,Edge>();
        }
	}

	protected Reference2ObjectOpenHashMap<NodeRef,Node> indexNodeRef;
	protected Reference2ObjectOpenHashMap<EdgeRef,Edge> indexEdgeRef;

    protected void resetRepresentations() {
        for (Map.Entry<EdgeRef, Edge> entry : indexEdgeRef.entrySet()) {
            Edge edge = entry.getValue();
            edge.doc = null;
            edge.store = null;
        }

        for (Map.Entry<NodeRef, Node> entry : indexNodeRef.entrySet()) {
            Node<?> node = entry.getValue();
            node.doc = null;
            node.store = null;
        }

        indexEdgeRef.clear();
        indexEdgeRef.trim();
        indexNodeRef.clear();
        indexNodeRef.trim();
    }


    /** Get the boolean indicating if this instance will throw an exception if a
     * node/edge factory cannot find the concrete type needed
     * <p>
     * <em>Default behaviour will replace representation with DynamicNode/Edge.</em>
     * */
    public boolean throwOnFactoryFailure() {
        return throwOnFactoryFailure;
    }

    /** Set the boolean indicating if this instance will throw an exception if a
     * node/edge factory cannot find the concrete type needed
     * <p>
     * <em>Default behaviour will replace representation with DynamicNode/Edge.</em> */
    public void setThrowOnFactoryFailure(boolean throwOnFactoryFailure) {
        this.throwOnFactoryFailure = throwOnFactoryFailure;
    }

    /**
	 * Create representation without adding to cache
     * <b>Remarks:</b> This representation is not bound to a document.
     *
	 * @param ref the reference to get a representation for
	 * @return instanced edge representation
	 */
	public Edge create(EdgeRef ref) {
		Edge e;
		if(ref.layer().getLayer().startsWith("@")) {
			e = new DynamicEdge();
		}
		else {
            try {
                e = getEdgeFactory(ref.layer().getLayer()).newInstance();
            } catch (Exception ex) {
                if(throwOnFactoryFailure)
                    throw new RepresentationException(ex);
                else {
                    edgeFactories.put(ref.layer().getLayer(), new EdgeFactory() {
                        @Override
                        public Edge newInstance() {
                            return new DynamicEdge();
                        }
                    });
                    return new DynamicEdge();
                }
            }
        }

		return e;
	}

	/**
	 * Create representation without adding to cache
     * <b>Remarks:</b> This representation is not bound to a document.
     *
     * @param ref the reference to get a representation for
     * @return instanced edge representation
	 */
	public Node create(NodeRef ref) {
		Node n;
		if(ref.layer().getLayer().startsWith("@")) {
			n = new DynamicNode();
		} else {
            try {
			n = getNodeFactory(ref.layer().getLayer()).newInstance();
            } catch (Exception e) {
                if(throwOnFactoryFailure)
                    throw new RepresentationException(e);
                else {
                    nodeFactories.put(ref.layer().getLayer(), new NodeFactory() {
                        @Override
                        public Node newInstance() {
                            return new DynamicNode();
                        }
                    });

                    return new DynamicNode();
                }
            }
		}

		return n;
	}

	/**
	 * Get or create a edge representation
	 * @param ref the edge reference to get a representation for
	 * @return instanced edge representation
	 */
	public Edge get(EdgeRef ref) {
		Edge cached = indexEdgeRef.get(ref);
		if(cached == null) {
            Edge e;
            e = create(ref);
            e.store = ref.get();
            e.doc = doc;
            e.initialized();

			indexEdgeRef.put(ref, e);
			return e;
		}
		else
		{
			return cached;
		}
	}

    /**
     * Get or create a node representation
     * @param ref the node reference to get a representation for
     * @return instanced edge representation
     */
	public Node get(NodeRef ref) {
		Node cached = indexNodeRef.get(ref);
		if(cached == null) {
			Node n;
            n = create(ref);
            n.store = ref.get();
            n.doc = doc;
            n.initialized();

			indexNodeRef.put(ref, n);
			return n;
		}
		else
		{
			return cached;
		}
	}
	
	public <E extends Edge> DocumentIterable<E> wrapEdges(DocumentIterable<EdgeRef> edgeRefs) {
		return new MappedDocumentIterable<E, EdgeRef>(edgeRefs) {

			@SuppressWarnings("unchecked")
			@Override
			protected E map(EdgeRef value) {
				return (E)get(value);
			}
		};
	}
	
	public <N extends Node> DocumentIterable<N> wrapNodes(DocumentIterable<NodeRef> nodeRefs) {
		return new MappedDocumentIterable<N, NodeRef>(nodeRefs) {

			@SuppressWarnings("unchecked")
			@Override
			protected N map(NodeRef value) {
				return (N)get(value);
			}
		};
	}

    protected void initialize(Node node, NodeRef ref) {
        node.store = ref.get();
        node.doc = doc;
        node.initialized();
    }

    protected void initialize(Edge edge, EdgeRef ref) {
        edge.store = ref.get();
        edge.doc = doc;
        edge.initialized();
    }

	public void register(Node node, NodeRef ref) {
        indexNodeRef.put(ref, node);
        initialize(node, ref);
	}
	
	public void register(Edge edge, EdgeRef ref) {
        indexEdgeRef.put(ref, edge);
        initialize(edge, ref);
	}
	
	public Node node(String uniqueRef) {
		return get(doc.store().getNode(uniqueRef));
	}
	
	public Edge edge(String uniqueRef) {
		return get(doc.store().getEdge(uniqueRef));
	}

	private NodeStore importExistingNode(Node node) {
        NodeRef ref = doc.store().createNode(node.getClass().getName());
        NodeStore nodestore = ref.get();
        for(Map.Entry<String, DataRef> prop : node.store.properties()) {
            nodestore.putProperty(prop.getKey(), prop.getValue());
        }

        if(node.isAnnotation())
            nodestore.setRanges(node.getStart(), node.getEnd());
        else
            nodestore.setNoRanges();

        return nodestore;
    }

    private NodeStore importExistingNode(Node node, int start, int end) {
        NodeRef ref = doc.store().createNode(node.getClass().getName());
        NodeStore nodestore = ref.get();
        for(Map.Entry<String, DataRef> prop : node.store.properties()) {
            nodestore.putProperty(prop.getKey(), prop.getValue());
        }

        nodestore.setRanges(start, end);
        return nodestore;
    }

    protected Node importNode(Node node) {
		NodeStore store = importExistingNode(node);
		return this.get(store.getRef());
	}
	
	/**
	 * Import a node with its type and all properties, no edges
	 * @param node the node
	 * @return the new node that belongs to this document
	 */
	protected Node importNode(Node node, int start, int end) {
		NodeStore store = importExistingNode(node, start, end);
		return this.get(store.getRef());
	}
	
	private static ConcurrentHashMap<String, NodeFactory> nodeFactories = new ConcurrentHashMap<String, NodeFactory>();
	private static ConcurrentHashMap<String, EdgeFactory> edgeFactories = new ConcurrentHashMap<String, EdgeFactory>();
	private static ConcurrentHashMap<Class<?>,NodeFactory> nodeFactoriesByClass = new ConcurrentHashMap<Class<?>, NodeFactory>();
	private static ConcurrentHashMap<Class<?>,EdgeFactory> edgeFactoriesByClass = new ConcurrentHashMap<Class<?>, EdgeFactory>();
	
	protected static <N extends Node> NodeFactory getNodeFactory(Class<N> clazz) {
		NodeFactory factory = nodeFactoriesByClass.get(clazz);
		if(factory == null)
		{
			factory = new ReflectiveNodeFactory(clazz);
			nodeFactories.put(clazz.getName(), factory);
			nodeFactoriesByClass.put(clazz, factory);
			return factory;
		}
		else
			return factory;
	}
	
	protected static <E extends Edge> EdgeFactory getEdgeFactory(Class<E> clazz) {
		EdgeFactory factory = edgeFactoriesByClass.get(clazz);
		if(factory == null)
		{
			factory = new ReflectiveEdgeFactory(clazz);
			edgeFactories.put(clazz.getName(), factory);
			edgeFactoriesByClass.put(clazz, factory);
			return factory;
		}
		else
			return factory;
	}
	
	
	protected static NodeFactory getNodeFactory(String className) {
		NodeFactory factory = nodeFactories.get(className);
		if(factory == null) {
			try {
				Class<?> clazz = Class.forName(className);
				factory = new ReflectiveNodeFactory(clazz);
				nodeFactories.put(className, factory);
				nodeFactoriesByClass.put(clazz, factory);
				return factory;
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Could not find concrete type " + className);
			}
		}
		else
			return factory;
	}
	
	protected static EdgeFactory getEdgeFactory(String className) {
		EdgeFactory factory = edgeFactories.get(className);
		if(factory == null) {
			try {
				Class<?> clazz = Class.forName(className);
				factory = new ReflectiveEdgeFactory(clazz);
				edgeFactories.put(className, factory);
				edgeFactoriesByClass.put(clazz, factory);
				return factory;
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Could not find concrete type " + className);
			}
			
		}
		else
			return factory;
	}

    protected static abstract class EdgeFactory {
        public abstract Edge newInstance();
    }

    protected static abstract class NodeFactory {
        public abstract Node newInstance();
    }

	protected static class ReflectiveEdgeFactory extends EdgeFactory {
		private final Constructor<?> ctr;
		private final Class<?> type;
		
		public ReflectiveEdgeFactory(Class<?> clazz) {
			this.type = clazz;
			try {
				this.ctr = clazz.getConstructor();
			} catch (NoSuchMethodException e) {
				throw new RuntimeException("Missing public default constructor for type " + clazz.getCanonicalName(), e);
			} catch (SecurityException e) {
				throw new RuntimeException("Securty exeception when trying to find constructor for type " + clazz.getCanonicalName(), e);
			}
		}

        public Edge newInstance() {
            try {
                return (Edge)type.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Failed to construct type " + type.getName(), e);
            }
        }
	}
	
	protected static class ReflectiveNodeFactory extends NodeFactory {
		private final Constructor<?> ctr;
		private final Class<?> type;
		
		public ReflectiveNodeFactory(Class<?> clazz) {
			this.type = clazz;
			try {
				this.ctr = clazz.getConstructor();
			} catch (NoSuchMethodException e) {
				throw new RuntimeException("Missing public default constructor for type " + clazz.getCanonicalName(), e);
			} catch (SecurityException e) {
				throw new RuntimeException("Securty exeception when trying to find constructor for type " + clazz.getCanonicalName(), e);
			}
		}

        public Node newInstance() {
            try {
                return (Node)type.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Failed to construct type " + type.getName(), e);
            }
        }
	}
}

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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import se.lth.cs.docforia.data.*;
import se.lth.cs.docforia.query.Var;
import se.lth.cs.docforia.query.dsl.CommonClause;
import se.lth.cs.docforia.query.dsl.QueryClause;
import se.lth.cs.docforia.util.AnnotationNavigator;
import se.lth.cs.docforia.util.DocumentIterable;
import se.lth.cs.docforia.util.DocumentIterables;
import se.lth.cs.docforia.util.FilteredMappedDocumentIterable;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Document, text with properties, nodes, annotations and edges
 * <p>
 * Example usage:
 * <pre>
 * {@code
        Document doc = new MemoryDocument("Greetings from Lund, Sweden!");
        //                                 01234567890123456789012345678

        Token Greetings   = new Token(doc).setRange(0,  9);
        Token from        = new Token(doc).setRange(10, 14);
        Token Lund        = new Token(doc).setRange(15, 19);
        Token comma       = new Token(doc).setRange(19, 20);
        Token Sweden      = new Token(doc).setRange(21, 27);
        Token exclamation = new Token(doc).setRange(27, 28);

        Sentence grettingsSentence = new Sentence(doc).setRange(0, 28);

        NamedEntity lundSwedenEntity
                = new NamedEntity(doc).setRange(Lund.getStart(), Sweden.getEnd())
                                      .setLabel("Location");

        NodeTVar<Token> T = Token.var();
        NodeTVar<NamedEntity> NE = NamedEntity.var();

        List<Token> lundLocation = doc.select(T, NE)
                                      .where(T).coveredBy(NE)
                                      .stream()
                                      .sorted(StreamUtils.orderBy(T))
                                      .map(StreamUtils.toNode(T))
                                      .collect(Collectors.toList());

        assert lundLocation.size() == 3;
        for (Token token : lundLocation) {
            System.out.println(token);
        }

        Optional<PropositionGroup> group = doc.select(T, NE)
                                              .where(T).coveredBy(NE)
                                              .stream()
                                              .collect(QueryCollectors.groupBy(doc, NE).orderByValue(T).collector())
                                              .stream()
                                              .findFirst();

        assertTrue(group.isPresent());

        NamedEntity ne = group.get().key(NE);
        System.out.println(ne);

        assert group.get().list(T).size() == 3;
        for (Token token : group.get().list(T)) {
            System.out.println(token);
        }
    }
    </pre>
    <b>Remarks:</b> This document can be used as a CharSequence key, hashCode and equals uses the text as input
 */
@SuppressWarnings("unchecked")
public abstract class Document implements CharSequence, Range, DocumentProxy, PropertyContainer<Document>, Comparable<Document> {

    /** Id property key */
    public static final String PROP_ID = "__id__";

    /** Uri property key */
    public static final String PROP_URI = "__uri__";

    /** Title property key */
    public static final String PROP_TITLE = "__title__";

    /** Language property key */
    public static final String PROP_LANG = "__lang__";

    /** Type property key */
    public static final String PROP_TYPE = "__type__";

	//protected Record record;
	protected HashMap<String,Object> tags;

	/**
	 * Get the document instances object
	 * @return document instances
	 */
	public abstract DocumentRepresentations representations();

	/**
	 * The virtual length of the document: -1 if same as text,
	 * is used to make addition of ranges without any text possible.
	 */
	protected int length = -1;

	/**
	 * Make a full deep copy of this document
     */
	public Document copy() {
		return convert(factory(), this);
	}

	/**
	 * Concat the given documents into a new document
	 * @param id   id of the new document
	 * @param docs array of documents to concat
     * @return new document
     */
	public Document concat(String id, Document...docs) {
		Document doc = newInstance(id, "");
		doc.append(docs);
		return doc;
	}

    /**
     * Concat the given documents into a new document
     * @param id   id of the new document
     * @param docs array of documents to concat
     * @return new document
     */
    public static Document concat(DocumentFactory factory, String id, Document...docs) {
        Document doc = factory.create();
        doc.setId(id);
        doc.append(docs);
        return doc;
    }

	/**
	 * Concat the given documents into a new document
	 * @param id   id of the new document
	 * @param docs iterable of documents to concat
     */
	public Document concat(String id, Iterable<Document> docs) {
		Document doc = newInstance(id, "");
		doc.append(docs);
		return doc;
	}

    /**
     * Concat the given documents into a new document
     * @param id   id of the new document
     * @param docs array of documents to concat
     * @return new document
     */
    public static Document concat(DocumentFactory factory, String id, Iterable<Document> docs) {
        Document doc = factory.create();
        doc.setId(id);
        doc.append(docs);
        return doc;
    }


    public DocumentTransaction begin() {
		return new DocumentTransaction(this);
	}

	/**
	 * Add node
	 * @param node
	 * @param <N>
     * @return
     */
	public <N extends Node> N add(N node) {
		if(node.hasDynamicLayer())
			throw new IllegalArgumentException("Incorrect add method for dynamic nodes, use add(node, layer, [variant])");

		representations().register(node, store().createNode(nodeLayer(node.getClass())));
		return node;
	}

	/**
	 * Add node with a dynamic layer type
	 * @param node the dynamic node
	 * @param layer the dynamic layer
     * @return node
     */
	public DynamicNode add(DynamicNode node, String layer) {
		representations().register(node, store().createNode(nodeLayer(layer)));
		return node;
	}

	/**
	 * Add edge
	 * @param edge
	 * @param <E>
     * @return
     */
	public <E extends Edge> E add(E edge) {
		representations().register(edge, store().createEdge(edgeLayer(edge.getClass())));
		return edge;
	}

	/**
	 * Add edge
	 * @param edge
	 * @param <E>
	 * @return
	 */
	public <E extends Edge> E add(E edge, Node tail, Node head) {
		representations().register(edge, store().createEdge(edgeLayer(edge.getClass())));
        edge.store.connect(tail.store.getRef(), head.store.getRef());
		return edge;
	}

	@Override
	public DynamicEdge add(DynamicEdge edge, String layer) {
		representations().register(edge, store().createEdge(edgeLayer(layer)));
		return edge;
	}

	@Override
	public DynamicEdge add(DynamicEdge edge, String layer, Node tail, Node head) {
		representations().register(edge, store().createEdge(edgeLayer(layer)));
        edge.store.connect(tail.store.getRef(), head.store.getRef());
		return edge;
	}

	/**
	 * Get the lower level name of a dynamic layer
	 * @param layer dynamic layer
	 * @return name of layer for use with engine()
     */
	public static String nodeLayer(final String layer) {
		return "@" + layer;
	}

	/**
	 * Get the lower level name of a concrete node layer
	 * @param layer layer type
	 * @return name of layer for use with engine()
	 */
	public static <N extends Node> String nodeLayer(final Class<N> layer) {
		return layer.getName();
	}

	/**
	 * Get the lower level name of a dynamic edge layer
	 * @param layer dynamic layer
	 * @return name of layer for use with engine()
	 */
	public static String edgeLayer(final String layer) {
		return "@" + layer;
	}

	/**
	 * Get the lower level name of a concrete edge layer
	 * @param layer layer type
	 * @return name of layer for use with engine()
	 */
	public static <E extends Edge> String edgeLayer(final Class<E> layer) {
		return layer.getName();
	}

	/**
	 * Get representation of NodeRef
	 * @param ref the reference
	 * @return representation of Node
	 */
	public Node representation(NodeRef ref) {
		return representations().get(ref);
	}

	/**
	 * Get representation of EdgeRef
	 * @param ref the reference
	 * @return representation of Edge
	 */
	public Edge representation(EdgeRef ref) {
		return representations().get(ref);
	}

	/**
	 * Get an iterable of map entries in the runtime tags
	 */
    public Iterable<Map.Entry<String,Object>> tags() {
        if(tags == null)
            return Collections.<String,Object>emptyMap().entrySet();
        else
            return tags.entrySet();
    }

	/**
	 * Get runtime tag
	 *
	 * <b>Remarks:</b> the runtime tags are not persisted, only exists on this representation, not any created views or copies.
	 */
    public <T> T getTag(String key) {
        if(tags == null)
            return null;
        else
            return (T)tags.get(key);
    }

	/**
	 * Check if runtime tag exists
	 *
	 * <b>Remarks:</b> the runtime tags are not persisted, only exists in this instance, not any created views or copies.
	 * @param key the runtime tags
	 */
	public boolean hasTag(String key) {
		return tags != null && tags.containsKey(key);
	}

	/**
	 * Remove tag
	 */
    public void removeTag(String key) {
        if(tags != null)
            tags.remove(key);
    }

	/**
	 * Put tag
	 * <p>
	 * <b>Remarks:</b> the runtime tags are not persisted, only exists in this instance, not any created views or copies.
	 */
    public void putTag(String key, Object value) {
        if(tags == null)
            tags = new HashMap<>();

        tags.put(key, value);
    }

	/**
	 * Get the underlying store abstraction
	 * <p>
	 * <b>Remarks:</b> Low-level API, be careful, if not used properly things will break in spectacular ways!
	 */
	public abstract DocumentStore store();

	/**
	 * Get document id
     * @return null if not defined.
	 */
    public String id() {
        return getProperty(PROP_ID);
    }

	/**
	 * Set the document id
	 * <p>
	 * <b>Remarks: </b> If bound to a record, affects the record aswell, might throw exception if name is taken.
	 * @param id the id
     */
    public Document setId(String id) {
        putProperty(PROP_ID, id);
        return this;
    }

	/**
	 * Get primary document uri
	 * @return id if no uri.
	 */
	public String uri() {
		return !hasProperty(PROP_URI) ? id() : getStringArrayProperty(PROP_URI)[0];
	}

    /**
     * Get document uri and its aliases.
     */
    public String[] uris() {
        return !hasProperty(PROP_URI) ? new String[] {id()} : getStringArrayProperty(PROP_URI);
    }

    /**
     * Get document uri, read from the parent record
     * @param prefix filter by checking prefix of uri.
     * @return id if no attached record.
     */
    public String uri(String prefix) {
        if(!hasProperty(PROP_URI))
            return id();

        String[] uris = getStringArrayProperty(PROP_URI);

        for (String s : uris) {
            if(s.startsWith(prefix))
                return s;
        }

        return uris[0];
    }

    /** Add an URI alias to this document, if none exists: set as primary */
    public Document addUriAlias(String uri) {
        if(hasProperty(PROP_URI)) {
            String[] input = getStringArrayProperty(PROP_URI);
            String[] output = Arrays.copyOf(input, input.length+1);
            output[input.length] = uri;
            putProperty(PROP_URI, output);
        } else {
            putProperty(PROP_URI, new String[] {uri});
        }
        return this;
    }

    /** Add an URI alias to this document, if none exists: set as primary */
    public Document addUriAlises(String...uris) {
        if(hasProperty(PROP_URI)) {
            String[] input = getStringArrayProperty(PROP_URI);
            String[] output = Arrays.copyOf(uris, uris.length+uris.length);
            System.arraycopy(uris, input.length - input.length, output, input.length, input.length + uris.length - input.length);
            putProperty(PROP_URI, output);
        }
        return this;
    }

    /** Set URI, replacing any existing URI with given uri */
    public Document setUri(String uri) {
        putProperty(PROP_URI, new String[]{uri});
        return this;
    }

    /** Set URI and aliases, replacing any existing URIs with given URI array */
    public Document setUris(String[] uri) {
        putProperty(PROP_URI, uri);
        return this;
    }

	/**
	 * Get the document text
	 */
	public String getText() {
		return store().getText();
	}

	/**
	 * Set document text
	 * <b>Remarks:</b> it is up to the user to make sure that the set text is not less than current length.
	 * This method resets setLength
	 */
	public void setText(String text) {
		store().setText(text);
		if(text.length() >= length) {
			length = -1;
		}
	}

	/**
	 * Get the language
	 * @return language or "mul" if no language has been specified
	 */
	public String language() {
        String lang = getProperty(Document.PROP_LANG);
        return lang != null ? lang : "mul";
	}


    /**
     * Set the language
     * @param lang ISO 639 language code
     */
    public Document setLanguage(String lang) {
        putProperty(PROP_LANG, lang);
        return this;
    }

	/**
	 * Get document type, such as text/plain, text/json, text/x-wiki, ...
     * @return type or "text/plain" if no format has been specified
	 */
    public String type() {
        String type = getProperty(Document.PROP_TYPE);
        return type != null ? type : "text/plain";
    }

	/**
	 * Get document type, such as text/plain, text/json, text/x-wiki, ...
	 */
    public Document setType(String mime) {
        putProperty(Document.PROP_TYPE, mime);
        return this;
    }

    /**
     * Get document property
     * @return null if property does not exist
     */
    @Override
    public String getProperty(String key) {
        return store().getProperty(key);
    }

    @Override
	public <T> T getProperty(String key, Decoder<T> decoder) {
		return store().getProperty(key, decoder);
	}

	@Override
	public <T> T getProperty(String key, T reuse, Decoder<T> decoder) {
        return store().getProperty(key, reuse, decoder);
	}

	@Override
	public <T extends DataRef> T getRefProperty(String key) {
        return store().getRefProperty(key);
	}

	@Override
	public <T extends DataRef> T getRefProperty(String key, Class<T> type) {
        return store().getRefProperty(key, type);
	}

	@Override
	public char getCharProperty(String key) {
        return store().getCharProperty(key);
	}

	@Override
	public int getIntProperty(String key) {
        return store().getIntProperty(key);
	}

	@Override
	public long getLongProperty(String key) {
        return store().getLongProperty(key);
	}

	@Override
	public float getFloatProperty(String key) {
        return store().getFloatProperty(key);
	}

	@Override
	public double getDoubleProperty(String key) {
        return store().getDoubleProperty(key);
	}

	@Override
	public boolean getBooleanProperty(String key) {
        return store().getBooleanProperty(key);
	}

	@Override
	public byte[] getBinaryProperty(String key) {
        return store().getBinaryProperty(key);
	}

	@Override
	public int[] getIntArrayProperty(String key) {
        return store().getIntArrayProperty(key);
	}

	@Override
	public long[] getLongArrayProperty(String key) {
		return store().getLongArrayProperty(key);
	}

	@Override
	public float[] getFloatArrayProperty(String key) {
		return store().getFloatArrayProperty(key);
	}

	@Override
	public double[] getDoubleArrayProperty(String key) {
		return store().getDoubleArrayProperty(key);
	}

	@Override
	public String[] getStringArrayProperty(String key) {
		return store().getStringArrayProperty(key);
	}

    @Override
    public PropertyMap getPropertyMapProperty(String key) {
        return store().getPropertyMapProperty(key);
    }

    @Override
    public Document getDocumentProperty(String key) {
        return store().getDocumentProperty(key);
    }

    @Override
    public Document[] getDocumentArrayProperty(String key) {
        return store().getDocumentArrayProperty(key);
    }

    @Override
	public Document putProperty(String key, DataRef value) {
		store().putProperty(key,value);
        return this;
	}

    @Override
    public Document putProperty(String key, String value) {
        store().putProperty(key, value);
        return this;
    }

	@Override
	public Document putProperty(String key, char ch) {
        store().putProperty(key,ch);
        return this;
	}

	@Override
	public Document putProperty(String key, int value) {
        store().putProperty(key,value);
        return this;
	}

	@Override
	public Document putProperty(String key, long value) {
        store().putProperty(key,value);
        return this;
	}

	@Override
	public Document putProperty(String key, boolean value) {
        store().putProperty(key,value);
        return this;
	}

	@Override
	public Document putProperty(String key, float value) {
        store().putProperty(key,value);
        return this;
	}

	@Override
	public Document putProperty(String key, double value) {
        store().putProperty(key,value);
        return this;
	}

	@Override
	public Document putProperty(String key, byte[] value) {
        store().putProperty(key,value);
        return this;
	}

	@Override
	public Document putProperty(String key, int[] value) {
        store().putProperty(key,value);
        return this;
	}

	@Override
	public Document putProperty(String key, long[] value) {
        store().putProperty(key,value);
        return this;
	}

	@Override
	public Document putProperty(String key, float[] value) {
        store().putProperty(key,value);
        return this;
	}

	@Override
	public Document putProperty(String key, double[] value) {
        store().putProperty(key,value);
        return this;
	}

	@Override
	public Document putProperty(String key, boolean[] value) {
        store().putProperty(key,value);
        return this;
	}

	@Override
	public Document putProperty(String key, String[] value) {
        store().putProperty(key,value);
        return this;
	}

    @Override
    public Document putProperty(String key, PropertyMap value) {
        store().putProperty(key,value);
        return this;
    }

    @Override
    public Document putProperty(String key, Document value) {
        store().putProperty(key,value);
        return this;
    }

    @Override
    public Document putProperty(String key, Document[] value) {
        store().putProperty(key,value);
        return this;
    }

    @Override
	public <T> Document putProperty(String key, T value, Encoder<T> encoder) {
        store().putProperty(key,value, encoder);
        return this;
	}

    /**
	 * Check if document has property
	 */
	public boolean hasProperty(String key) {
		return store().hasProperty(key);
	}

	/**
	 * Remove document property if it exists.
	 */
    public void removeProperty(String key) {
        store().removeProperty(key);
    }

	/**
	 * Get an iterable of all properties
	 */
	public Iterable<Map.Entry<String,DataRef>> properties() {
		return new Iterable<Map.Entry<String, DataRef>>() {
			@Override
			public Iterator<Map.Entry<String, DataRef>> iterator() {
				return new Iterator<Map.Entry<String,DataRef>>() {

					final Iterator<Map.Entry<String,DataRef>> iter = store().properties().iterator();
					Map.Entry<String,DataRef> current;

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}

					private boolean moveForward() {
						while(iter.hasNext()) {
							current = iter.next();
							if(!current.getKey().startsWith("__"))
								return true;
						}

						current = null;
						return false;
					}

					@Override
					public Map.Entry<String, DataRef> next() {
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
		};
	}

	/**
	 * Do a select query agains the document model
	 * @param vars the vars that must appear in output
	 * @return DSL node so that the proper query can be created
	 */
	public CommonClause select(Var...vars) {
		return new QueryClause(this, vars);
	}

	/**
	 * Get the default node variant for a specific type
	 * @param type the node type
	 * @return null if no variants
	 */
	public <T extends Node> String getDefaultNodeVariant(Class<T> type) {
		return store().getDefaultNodeVariants().get(nodeLayer(type));
	}

	/**
	 * Get the default edge variant for a specific type
	 * @param type the edge type
	 * @return null if no variants
	 */
	public <T extends Edge> String getDefaultEdgeVariant(Class<T> type) {
		return store().getDefaultEdgeVariants().get(edgeLayer(type));
	}

	/**
	 * Get the default node variant for a specific type
	 * @param dynamicType the node type
	 * @return null if no variants
	 */
	public <T extends Node> String getDefaultNodeVariant(String dynamicType) {
		return store().getDefaultNodeVariants().get(nodeLayer(dynamicType));
	}

	/**
	 * Get the default node variant for a specific type
	 * @param dynamicType the node type
	 * @return null if no variants
	 */
	public <T extends Edge> String getDefaultEdgeVariant(String dynamicType) {
		return store().getDefaultEdgeVariants().get(edgeLayer(dynamicType));
	}

	/**
	 * Set the default node variant
	 * @param type the node type
	 * @param variant the node variant
	 */
	public <T extends Node> void setDefaultNodeVariant(Class<T> type, String variant) {
		store().setDefaultNodeVariant(nodeLayer(type), variant);
	}

	/**
	 * Set the default edge variant
	 * @param type the edge type
	 * @param variant the edge variant
	 */
	public <T extends Edge> void setDefaultEdgeVariant(Class<T> type, String variant) {
		store().setDefaultEdgeVariant(edgeLayer(type), variant);
	}

	/**
	 * Set default node variant for a dynamic type
	 * @param dynamicType dynamic node type
	 * @param variant the variant
	 */
	public <T extends Node> void setDefaultNodeVariant(String dynamicType, String variant) {
		store().setDefaultNodeVariant(nodeLayer(dynamicType), variant);
	}

	/**
	 * Set default edge variant for a dynamic type
	 * @param dynamicType dynamic edge type
	 * @param variant the variant
	 */
	public <T extends Edge> void setDefaultEdgeVariant(String dynamicType, String variant) {
		store().setDefaultEdgeVariant(edgeLayer(dynamicType), variant);
	}

	/**
	 * Move all nodes of a particular variation from one to another
	 * @param type the node type
	 * @param fromVariant from this variant
	 * @param fromVariant to this variant
	 */
	public <T extends Node> void migrateNodeVariants(Class<T> type, String fromVariant, String toVariant) {
		store().migrateNodesToVariant(nodeLayer(type), fromVariant, toVariant);
	}

	/**
	 * Move all edges of a particular variation from one to another
	 * @param type the edge type
	 * @param fromVariant from this variant
	 * @param fromVariant to this variant
	 */
	public <T extends Edge> void migrateEdgeVariants(Class<T> type, String fromVariant, String toVariant) {
		store().migrateEdgesToVariant(edgeLayer(type), fromVariant, toVariant);
	}

	/**
	 * Move all dynamically typed nodes of a particular variation from one to another
	 * @param dynamicType the node type
	 * @param fromVariant from this variant
	 * @param fromVariant to this variant
	 */
	public <T extends Node> void migrateNodeVariants(String dynamicType, String fromVariant, String toVariant) {
		store().migrateNodesToVariant(nodeLayer(dynamicType), fromVariant, toVariant);
	}

	/**
	 * Move all dynamically typed nodes of a particular variation from one to another
	 * @param dynamicType the node type
	 * @param fromVariant from this variant
	 * @param fromVariant to this variant
	 */
	public <T extends Edge> void migrateEdgeVariants(String dynamicType, String fromVariant, String toVariant) {
		store().migrateEdgesToVariant(edgeLayer(dynamicType), fromVariant, toVariant);
	}

	/**
	 * Create an iterable of all edges
     */
	public DocumentIterable<Edge> edges() {
		return representations().wrapEdges(engine().edges(true));
	}

	/**
	 * Create an iterable of all edges in all variants
	 */
	public DocumentIterable<Edge> edges(boolean allVariants) {
		return representations().wrapEdges(engine().edges(!allVariants));
	}

	/**
	 * Create an iterable of all edges of specific type
	 */
	public <E extends Edge> DocumentIterable<E> edges(final Class<E> edgeType) {
		return representations().wrapEdges(engine().edges(edgeLayer(edgeType)));
	}

	/**
	 * Create an iterable of all edges of specific type and variant
	 */
	public <E extends Edge> DocumentIterable<E> edges(final Class<E> edgeType, String variant) {
		return representations().wrapEdges(engine().edges(edgeLayer(edgeType), variant));
	}

	/**
	 * Create an iterable of all edges of dynamic type
	 */
	public <E extends Edge> DocumentIterable<E> edges(final String dynamicType) {
		return representations().wrapEdges(engine().edges(edgeLayer(dynamicType)));
	}

	/**
	 * Create an iterable of all edges of dynamic type and variant
     */
	public <E extends Edge> DocumentIterable<E> edges(final String dynamicType, final String variant) {
		return representations().wrapEdges(engine().edges(edgeLayer(dynamicType), variant));
	}

	/**
	 * Create an iterable of all edges that is connected to node start dependening on direction
	 */
	public <T extends Edge> DocumentIterable<T> edges(Node start, Direction dir) {
		return representations().wrapEdges(engine().edges(start.store.getRef(), dir));
	}

	/**
	 * Create an iterable of all edges of specific type that is connected to node start dependening on direction
	 */
	public <T extends Edge> DocumentIterable<T> edges(Node start, Class<T> edgeType, Direction dir) {
		return representations().wrapEdges(engine().edges(start.store.getRef(), edgeLayer(edgeType), dir));
	}

	/**
	 * Create an iterable of all edges of dynamic type that is connected to node start dependening on direction
	 */
	public <T extends Edge> DocumentIterable<T> edges(Node start, String dynamicType, Direction dir) {
		return representations().wrapEdges(engine().edges(start.store.getRef(), edgeLayer(dynamicType), dir));
	}

	/**
	 * Create an iterable of all edges of dynamic type and variant that is connected to node start dependening on direction
	 */
	public <T extends Edge> DocumentIterable<T> edges(Node start,  String dynamicType, String variant, Direction dir) {
		return representations().wrapEdges(engine().edges(start.store.getRef(), edgeLayer(dynamicType), variant, dir));
	}

	/**
	 * Create an iterable of all edges of specific type and variant that is connected to node start dependening on direction
	 */
	public <T extends Edge> DocumentIterable<T> edges(Node start, Class<T> edgeType, String variant, Direction dir) {
		return representations().wrapEdges(engine().edges(start.store.getRef(), edgeLayer(edgeType), variant, dir));
	}

	/**
	 * Create an iterable of all edges with property
	 * @param key   the property key
	 * @param value the property value
     */
	public DocumentIterable<Edge> edgesWithProperty(String key, String value) {
		return representations().wrapEdges(engine().edgesWithProperty(key, value));
	}

	/**
	 * Create an iterable of all nodes
     */
	public DocumentIterable<Node> nodes() {
		return representations().wrapNodes(DocumentIterables.wrap(store().nodes()));
	}

	/**
	 * Create an iterable of all nodes with specific property
	 * @param key the property key
	 * @param value the property value
	 */
	public DocumentIterable<Node> nodesWithProperty(String key, String value) {
		return representations().wrapNodes(engine().nodesWithProperty(key, value));
	}

	/**
	 * Create an iterable of all nodes with specific type and property
	 * @param key the property key
	 * @param value the property value
	 */
	public <N extends Node> DocumentIterable<N> nodesWithProperty(Class<N> nodeType, String key, String value) {
		return representations().wrapNodes(engine().nodesWithProperty(nodeLayer(nodeType), key, value));
	}

	/**
	 * Create an iterable of all nodes with specific type, variant and property
	 * @param key the property key
	 * @param value the property value
	 */
	public <N extends Node> DocumentIterable<N> nodesWithProperty(Class<N> nodeType, String nodeVariant, String key, String value) {
		return representations().wrapNodes(engine().nodesWithProperty(nodeLayer(nodeType), nodeVariant, key, value));
	}

	/**
	 * Create an iterable of all nodes with dynamic type and property
	 * @param key the property key
	 * @param value the property value
	 */
	public <N extends Node> DocumentIterable<N> nodesWithProperty(String dynamicType, String key, String value) {
		return representations().wrapNodes(engine().nodesWithProperty(nodeLayer(dynamicType), key, value));
	}

	/**
	 * Create an iterable of all nodes with dynamic type, variant and property
	 * @param key the property key
	 * @param value the property value
	 */
	public <N extends Node> DocumentIterable<N> nodesWithProperty(String dynamicType, String nodeVariant, String key, String value) {
		return representations().wrapNodes(engine().nodesWithProperty(nodeLayer(dynamicType), nodeVariant, key, value));
	}

	/**
	 * Create an iterable of all nodes with specific type
	 */
	public <N extends Node> DocumentIterable<N> nodes(Class<N> nodeType) {
		return representations().wrapNodes(engine().nodes(nodeLayer(nodeType)));
	}

    /**
     * Get a node Stream
     */
    public <T extends Node> Stream<T> nodeStream(Class<T> layer) {
        return StreamSupport.stream(nodes(layer).spliterator(), false);
    }

    /**
	 * Create an iterable of all nodes with specific type and variant
	 */
	public <N extends Node> DocumentIterable<N> nodes(Class<N> nodeType, String variant) {
		return representations().wrapNodes(engine().nodes(nodeLayer(nodeType), variant));
	}

    /**
     * Get a node Stream
     */
    public <T extends Node> Stream<T> nodeStream(Class<T> layer, String variant) {
        return StreamSupport.stream(nodes(layer, variant).spliterator(), false);
    }

	/**
	 * Create an iterable of all nodes with dynamic type
	 */
	public <N extends Node> DocumentIterable<N> nodes(String dynamicType) {
		return representations().wrapNodes(engine().nodes(nodeLayer(dynamicType)));
	}

	/**
	 * Create an iterable of all nodes with dynamic type and variant
	 */
	public <N extends Node> DocumentIterable<N> nodes(String dynamicType, String variant) {
		return representations().wrapNodes(engine().nodes(nodeLayer(dynamicType), variant));
	}

	/**
	 * Create an iterable of all annotations of specific type covered by <code>range</code>
     */
	public <N extends Node> DocumentIterable<N> annotations(final Class<N> nodeType, Range range) {
		return annotations(nodeType, range.getStart(), range.getEnd());
	}

	/**
	 * Create an iterable of all annotations of specific type covered by range from [<code>from</code>, <code>to</code>)
	 */
	public <N extends Node> DocumentIterable<N> annotations(final Class<N> nodeType) {
		return representations().wrapNodes(engine().coveredAnnotation(nodeLayer(nodeType), 0, length()));
	}

	/**
	 * Create an iterable of all annotations of specific type covered by range from [<code>from</code>, <code>to</code>)
	 */
	public <N extends Node> DocumentIterable<N> annotations(final Class<N> nodeType, final int from, final int to) {
		return representations().wrapNodes(engine().coveredAnnotation(nodeLayer(nodeType), from, to));
	}

	/**
	 * Create an iterable of all annotations of specific type and variant covered by <code>range</code>
	 */
	public <N extends Node> DocumentIterable<N> annotations(final Class<N> nodeType, final String variant) {
		return annotations(nodeType, variant, 0, length());
	}

	/**
	 * Create an iterable of all annotations of specific type and variant covered by <code>range</code>
	 */
	public <N extends Node> DocumentIterable<N> annotations(final Class<N> nodeType, final String variant, Range range) {
		return annotations(nodeType, variant, range.getStart(), range.getEnd());
	}

	/**
	 * Create an iterable of all annotations of specific type and variant covered by range from [<code>from</code>, <code>to</code>)
	 */
	public <N extends Node> DocumentIterable<N> annotations(final Class<N> nodeType, final String variant, final int from, final int to) {
		return representations().wrapNodes(engine().coveredAnnotation(nodeLayer(nodeType), variant, from, to));
	}

	/**
	 * Create an iterable of all annotations of dynamic type covered by range from [<code>from</code>, <code>to</code>)
	 */
	public <N extends Node> DocumentIterable<N> annotations(final String dynamicType, final int from, final int to) {
		return representations().wrapNodes(engine().coveredAnnotation(nodeLayer(dynamicType), from, to));
	}

	/**
	 * Create an iterable of all annotations of dynamic type covered by range from [<code>from</code>, <code>to</code>)
	 */
	public <N extends Node> DocumentIterable<N> annotations(final String dynamicType) {
		return representations().wrapNodes(engine().coveredAnnotation(nodeLayer(dynamicType), 0, length()));
	}

	/**
	 * Create an iterable of all annotations of dynamic type and variant covered by range from [<code>from</code>, <code>to</code>)
	 */
	public <N extends Node> DocumentIterable<N> annotations(final String dynamicType, final String variant, final int from, final int to) {
		return representations().wrapNodes(engine().coveredAnnotation(nodeLayer(dynamicType), variant, from, to));
	}

	/**
	 * Create an iterable of all annotations of dynamic type and variant covered by <code>range</code>
	 */
	public <N extends Node> DocumentIterable<N> annotations(final String dynamicType, final String variant) {
		return annotations(dynamicType, variant, 0, length());
	}

	/**
	 * Create an iterable of all annotations of dynamic type and variant covered by <code>range</code>
	 */
	public <N extends Node> DocumentIterable<N> annotations(final String dynamicType, final String variant, Range range) {
		return annotations(dynamicType, variant, range.getStart(), range.getEnd());
	}

	/**
	 * Create an iterable of all annotations of dynamic type covered by <code>range</code>
	 */
	public <N extends Node> DocumentIterable<N> annotations(final String dynamicType, Range range) {
		return annotations(dynamicType, range.getStart(), range.getEnd());
	}

	/**
	 * Remove specific node
	 * @param node the node to remove
     */
	public void remove(Node node) {
		store().remove(node.store.getRef());
	}

	/**
	 * Remove specific edge
	 * @param edge the edge to remove
     */
	public void remove(Edge edge) {
		store().remove(edge.store.getRef());
	}

	/**
	 * Append an existing document to this one
	 */
    public Document append(Document doc) {
		if(doc == null)
			throw new NullPointerException("doc");

        StringBuilder sb = new StringBuilder(this.getText());
        store().append(doc.store(), sb);
        store().setText(sb.toString());
        return this;
    }

	/**
	 * Append a collection of existing documents to this one (optimized)
	 */
	public Document append(Document...docs) {
		return append(Arrays.asList(docs));
	}

	/**
	 * Append an iterable of existing documents to this one (optimized)
	 */
    public Document append(Iterable<Document> docs) {
		if(docs == null)
			throw new NullPointerException("docs");

        StringBuilder sb = new StringBuilder(this.getText());
        for(Document doc : docs) {
            store().append(doc.store(), sb);
        }
        store().setText(sb.toString());
        return this;
    }

    /**
     * Extract a part of this document
     * @param range the range to subdocument
     * @return new document
     * <b>Remarks:</b> default settings are to not include non referenced nodes and not to copy document properties
     */
    public Document subDocument(Range range) {
        return subDocument(range.getStart(), range.getEnd(), false, false);
    }

	/**
	 * Extract a part of this document
	 * @param start start coordinate (inclusive)
	 * @param end end coordinate (exclusive)
	 * @return new document
	 * <b>Remarks:</b> default settings are to not include non referenced nodes and not to copy document properties
	 */
	public Document subDocument(int start, int end) {
		return subDocument(start, end, false, false);
	}


    /**
     * Extract a part of this document
     * @param range the range to subdocument
     * @param includeNonReferencedNodes includes all non referenced nodes from annotations if set to true.
     * @return new document
     * <b>Remarks:</b> default settings are to not copy document properties
     */
    public Document subDocument(Range range, boolean includeNonReferencedNodes) {
        return subDocument(range.getStart(), range.getEnd(), includeNonReferencedNodes, false);
    }

	/**
	 * Extract a part of this document
	 * @param start start coordinate (inclusive)
	 * @param end end coordinate (exclusive)
	 * @param includeNonReferencedNodes includes all non referenced nodes from annotations if set to true.
	 * @return new document
	 * <b>Remarks:</b> default settings are to not copy document properties
	 */
	public Document subDocument(int start, int end, boolean includeNonReferencedNodes) {
		return subDocument(start, end, includeNonReferencedNodes, false);
	}

    /**
     * Extract a part of this document
     * @param range the range to subdocument
     * @param includeNonReferencedNodes includes all non referenced nodes from annotations if set to true.
     * @param copyDocProperties copy doc properties
     * @return new document
     */
    public Document subDocument(Range range, boolean includeNonReferencedNodes, boolean copyDocProperties) {
        return subDocument(range.getStart(), range.getEnd(), includeNonReferencedNodes, copyDocProperties);
    }

    /**
     * Extract a filtered part of this document
     * @param range the range to subdocument
     * @param nodePred the node predicate, that controls which nodes to copy
     * @param edgePred the edge predicate, that controls which edges to copy
     * @param includeNonReferencedNodes includes all non referenced nodes from annotations if set to true.
     * @param copyDocProperties copy doc properties
     * @return
     */
    public Document filteredSubDocument(
            Range range,
            Function<Node,Boolean> nodePred,
            Function<Edge,Boolean> edgePred,
            boolean includeNonReferencedNodes,
            boolean copyDocProperties)
    {
        return filteredSubDocument(range.getStart(), range.getStart(), nodePred, edgePred, includeNonReferencedNodes, copyDocProperties);
    }

    /**
     * Extract a filtered part of this document
     * @param start start coordinate (inclusive)
     * @param end end coordinate (exclusive)
     * @param nodePred the node predicate, that controls which nodes to copy
     * @param edgePred the edge predicate, that controls which edges to copy
     * @param includeNonReferencedNodes includes all non referenced nodes from annotations if set to true.
     * @param copyDocProperties copy doc properties
     * @return
     */
    public Document filteredSubDocument(
            final int start,
            final int end,
            final Function<Node,Boolean> nodePred,
            final Function<Edge,Boolean> edgePred,
            final boolean includeNonReferencedNodes,
            final boolean copyDocProperties)
    {
		//TODO: Optimize this code using the engine system to avoid the overhead of managing concrete instances.
        MutableRange range = new MutableRange(start, end);
        Document doc = newInstance(id(), this.text(start, end));

        ArrayList<Node> nodesToCopy = new ArrayList<Node>();
        IdentityHashMap<Node, Integer> ids = new IdentityHashMap<Node, Integer>();

        //Get all nodes in range
        for(Node node : nodes()) {
            if(!nodePred.apply(node))
                continue;

            if(node.isAnnotation()) {
                if(node.coveredBy(range)) {
                    ids.put(node, nodesToCopy.size());
                    nodesToCopy.add(node);
                }
            }
            else if(includeNonReferencedNodes) {
                ids.put(node, nodesToCopy.size());
                nodesToCopy.add(node);
            }
        }

        Set<Edge> edgesToCopy = Collections.newSetFromMap(new IdentityHashMap<Edge, Boolean>());

        Set<Node> visisted = Collections.newSetFromMap(new IdentityHashMap<Node, Boolean>());
        Deque<Node> nodequeue = new ArrayDeque<Node>(ids.keySet());

        //Depth-First-Search
        while(!nodequeue.isEmpty()) {
            Node<?> node = nodequeue.pop();
            if(visisted.contains(node))
                continue;

            visisted.add(node);

            if(!nodePred.apply(node))
                continue;

            if(!ids.containsKey(node)) {
                ids.put(node, nodesToCopy.size());
                nodesToCopy.add(node);
            }

            for(Edge edge : node.connectedEdges()) {
                if(!edgePred.apply(edge))
                    continue;

                Node head = edge.getHead();
                Node tail = edge.getTail();

                if(!nodePred.apply(head) || !nodePred.apply(tail))
                    continue;

                boolean part1 = false;
                boolean part2 = false;

                if(head.isAnnotation()) {
                    part1 = head.coveredBy(range);
                }
                else if(includeNonReferencedNodes)
                    part1 = true;

                if(tail.isAnnotation()) {
                    part2 = tail.coveredBy(range);
                }
                else if(includeNonReferencedNodes)
                    part2 = true;

                if(part1 && part2) {
                    Node opposite = edge.getOpposite(node);
                    if(!nodePred.apply(opposite))
                        continue;

                    edgesToCopy.add(edge);
                    if(!visisted.contains(opposite) && !ids.containsKey(opposite)) {
                        nodequeue.push(opposite);
                    }
                }
            }
        }

        IdentityHashMap<Integer, Node> newids = new IdentityHashMap<Integer, Node>();

        //1. Add all nodes
        for(Node node : nodesToCopy) {
            Node newnode;

            //Update ranges
            if(node.isAnnotation()) {
                newnode = doc.importNode(node, node.getStart() - start, node.getEnd() - start);
            }
            else
                newnode = doc.importNode(node);

            newids.put(ids.get(node), newnode);
        }

        //2. Connect all nodes with the edges to copy
        for(Edge edge : edgesToCopy) {
            Node newhead = newids.get(ids.get(edge.getHead()));
            Node newtail = newids.get(ids.get(edge.getTail()));
            doc.importEdge(edge, newhead, newtail);
        }

        //3. Copy document properties
        if(copyDocProperties) {
            for(Map.Entry<String,DataRef> attribute : properties()) {
                doc.putProperty(attribute.getKey(), attribute.getValue().copy());
            }
        }

        return doc;
    }

	/**
	 * Identity (always true) node filtering node predicate
	 */
    public final static Function<Node,Boolean> IdentityNodePred = in -> true;

	/**
	 * Identity (always true) edge filtering node predicate
	 */
    public final static Function<Edge,Boolean> IdentityEdgePred = in -> true;

	/**
	 * Extract a part of this document
	 * @param start start coordinate (inclusive)
	 * @param end end coordinate (exclusive)
	 * @param includeNonReferencedNodes includes alla non referenced nodes from annoations if set to true.
	 * @param copyDocProperties copy the document properties
	 * @return new document
	 */
	public Document subDocument(int start, int end, boolean includeNonReferencedNodes, boolean copyDocProperties) {
        return filteredSubDocument(start, end, IdentityNodePred, IdentityEdgePred, includeNonReferencedNodes, copyDocProperties);
	}

	/**
	 * Set the title, uses property "title"
	 */
	public void setTitle(String title) {
		store().putProperty(PROP_TITLE, title);
	}

	/**
	 * Get the title, uses property "title"
     */
	public String getTitle() {
		return store().getProperty(PROP_TITLE);
	}

	/**
	 * Return the text of the document
     */
	@Override
	public String text() {
		return store().getText();
	}

	/**
	 * Convert a range to a string
	 * @param start the start range
	 * @param end the end range
	 * @return string of the given range
	 */
	public String text(int start, int end) {
		String text = store().getText();

		//Truncate start, end to be within allowable range.
		start = Math.min(start, text.length());
		end = Math.min(end, text.length());

		if(start == end)
			return "";
		else
			return text.substring(start, end);
	}

	/**
	 * Convert a range to a string
	 * @param range the range to convert
	 * @return string of the given range
	 */
	public final String text(Range range) {
		return text(range.getStart(), range.getEnd());
	}

	/**
	 * Convert a range to a string
	 * @param range the range to convert
	 * @return string of the given range
     */
	public final String toString(Range range) {
		return text(range.getStart(), range.getEnd());
	}

    @Override
    public String toString() {
        return text();
    }

    /**
	 * Import a edge with its type and all properties, and setting the 
	 * head and tail that belongs to this document.
	 * @param edge the foreign edge
	 * @param importedhead imported edge (belongs to this document)
	 * @param importedtail imported tail (belongs to this document)
	 * @return edge that belongs to this document
	 */
	@SuppressWarnings("unchecked")
	protected <E extends Edge> E importEdge(E edge, Node importedhead, Node importedtail) {
		if(importedhead.doc != this)
			throw new IllegalArgumentException("Imported head does not belong to this document");

		if(importedtail.doc != this)
			throw new IllegalArgumentException("Imported edge does not belong to this document");

		LayerRef layer = edge.getRef().layer();
		EdgeRef ref = store().createEdge(layer.getLayer(), layer.getVariant());
		EdgeStore edgeStore = ref.get();
		for(Map.Entry<String, DataRef> prop : edge.store.properties()) {
			edgeStore.putProperty(prop.getKey(), prop.getValue());
		}

		edgeStore.connect(importedtail.store.getRef(), importedhead.store.getRef());
		return (E) representations().get(ref);
	}

	/**
	 * Check if this document is actually a view
	 * @return true if it is.
	 */
	public boolean isView() {
		return false;
	}

	/**
	 * Import a node
	 */
	@SuppressWarnings("unchecked")
	public <N extends Node> N importNode(N node) {
		LayerRef layer = node.getRef().layer();
		NodeRef ref = store().createNode(layer.getLayer(), layer.getVariant());
		NodeStore store = ref.get();
		for(Map.Entry<String, DataRef> entry : node.store.properties()) {
			store.putProperty(entry.getKey(), entry.getValue());
		}
		
		return (N) representations().get(ref);
	}

	/**
	 * Import a node with a new range (start, end)
	 */
	@SuppressWarnings("unchecked")
	public <N extends Node> N importNode(N node, int start, int end) {
		LayerRef layer = node.getRef().layer();
		NodeRef ref = store().createNode(layer.getLayer(), layer.getVariant());
		NodeStore store = ref.get();
		store.setRanges(start, end);
		for(Map.Entry<String, DataRef> entry : node.store.properties()) {
			store.putProperty(entry.getKey(), entry.getValue());
		}
		
		return (N) representations().get(ref);
	}

	/**
	 * Import nodes from another document and possible another storage format
	 * @param doc         the source document
	 * @param targetStart the start position in this document (offset to imported annotations)
     */
    public Document importNodes(Document doc, int targetStart) {
        return importNodes(doc, targetStart, IdentityNodePred, IdentityEdgePred);
    }

	/**
	 * Import nodes from another document and possible another storage format
	 * @param doc         the source document
	 * @param targetStart the start position in this document (offset to imported annotations)
	 * @param nodePred     filtering function that allows you to select which nodes to import
	 * @param edgePred     filtering function that allows you to select which edges to import
     */
    public Document importNodes(Document doc, int targetStart, Function<Node,Boolean> nodePred, Function<Edge,Boolean> edgePred) {
        return importNodes(doc, targetStart, nodePred, edgePred, false, false);
    }

	/**
	 * Import nodes from another document and possible another storage format
	 * @param source       the source document
	 * @param targetStart  the start position in this document (offset to imported annotations)
	 * @param nodePred     filtering function that allows you to select which nodes to import
	 * @param edgePred     filtering function that allows you to select which edges to import
	 * @param copyDocProperties copy document properties from the source
	 * @param includeNonReferencedNodes copy non referenced pure nodes from the source
     */
    public Document importNodes(
			Document source,
			int targetStart,
			Function<Node,Boolean> nodePred,
			Function<Edge,Boolean> edgePred,
			boolean copyDocProperties,
			boolean includeNonReferencedNodes)
	{
        ArrayList<Node> nodesToCopy = new ArrayList<Node>();
        IdentityHashMap<Node, Integer> ids = new IdentityHashMap<Node, Integer>();

        //Get all nodes in range
        for(Node node : source.nodes()) {
            if(!nodePred.apply(node))
                continue;

            if(node.isAnnotation()) {
                ids.put(node, nodesToCopy.size());
                nodesToCopy.add(node);
            }
            else if(includeNonReferencedNodes) {
                ids.put(node, nodesToCopy.size());
                nodesToCopy.add(node);
            }
        }

        Set<Edge> edgesToCopy = Collections.newSetFromMap(new IdentityHashMap<Edge, Boolean>());

        Set<Node> visisted = Collections.newSetFromMap(new IdentityHashMap<Node, Boolean>());
        Deque<Node> nodequeue = new ArrayDeque<Node>(ids.keySet());

        //Depth-First-Search
        while(!nodequeue.isEmpty()) {
            Node<?> node = nodequeue.pop();
            if(visisted.contains(node))
                continue;

            visisted.add(node);

            if(!nodePred.apply(node))
                continue;

            if(!ids.containsKey(node)) {
                ids.put(node, nodesToCopy.size());
                nodesToCopy.add(node);
            }

            for(Edge edge : node.connectedEdges()) {
                if(!edgePred.apply(edge))
                    continue;

                Node head = edge.getHead();
                Node tail = edge.getTail();

                if(!nodePred.apply(head) || !nodePred.apply(tail))
                    continue;

                boolean part1 = false;
                boolean part2 = false;

                if(head.isAnnotation()) {
                    part1 = true;
                }
                else if(includeNonReferencedNodes)
                    part1 = true;

                if(tail.isAnnotation()) {
                    part2 = true;
                }
                else if(includeNonReferencedNodes)
                    part2 = true;

                if(part1 && part2) {
                    Node opposite = edge.getOpposite(node);
                    if(!nodePred.apply(opposite))
                        continue;

                    edgesToCopy.add(edge);
                    if(!visisted.contains(opposite) && !ids.containsKey(opposite)) {
                        nodequeue.push(opposite);
                    }
                }
            }
        }

        IdentityHashMap<Integer, Node> newids = new IdentityHashMap<Integer, Node>();

        //1. Add all nodes
        for(Node node : nodesToCopy) {
            Node newnode;

            //Update ranges
            if(node.isAnnotation()) {
                newnode = importNode(node, node.getStart() + targetStart, node.getEnd() + targetStart);
            }
            else
                newnode = importNode(node);

            newids.put(ids.get(node), newnode);
        }

        //2. Connect all nodes with the edges to copy
        for(Edge edge : edgesToCopy) {
            Node newhead = newids.get(ids.get(edge.getHead()));
            Node newtail = newids.get(ids.get(edge.getTail()));
            importEdge(edge, newhead, newtail);
        }

        //3. Copy document properties
        if(copyDocProperties) {
            for(Map.Entry<String,DataRef> attribute : source.properties()) {
                putProperty(attribute.getKey(), attribute.getValue());
            }
        }

		return this;
    }

	/**
	 * Construct a new document with the same type of underlying storage as this document.
	 */
	public Document newInstance(String id, String text) {
		return factory().createFragment(id, text);
	}

	/**
	 * Get a reference to the engine
	 */
	public abstract DocumentEngine engine();

	/**
	 * Remove all nodes of a specific type
	 * @param nodeType the type of nodes
	 * @param <N> Concrete node type
     */
	public <N extends Node> Document removeAllNodes(Class<N> nodeType)  {
		engine().removeAllNodes(nodeLayer(nodeType));
		return this;
	}

	/**
	 * Remove all nodes of a specific type and variant
	 * @param nodeType    the type of nodes
	 * @param nodeVariant the node variant
	 * @param <N> Concrete node type
     */
	public <N extends Node> Document removeAllNodes(Class<N> nodeType, String nodeVariant)  {
		engine().removeAllNodes(nodeLayer(nodeType), nodeVariant);
		return this;
	}

	/**
	 * Remove all dynamic nodes of a specific type
	 * @param dynamicNodeType the type of nodes
	 * @param <N> Concrete node type
	 */
	public <N extends Node> Document removeAllNodes(String dynamicNodeType)  {
		engine().removeAllNodes(nodeLayer(dynamicNodeType));
		return this;
	}

	/**
	 * Remove all dynamic nodes of a specific type and variant
	 * @param dynamicNodeType    the type of nodes
	 * @param nodeVariant the node variant
	 * @param <N> Concrete node type
	 */
	public <N extends Node> Document removeAllNodes(String dynamicNodeType, String nodeVariant)  {
		engine().removeAllNodes(nodeLayer(dynamicNodeType), nodeVariant);
		return this;
	}

	/**
	 * Remove all edges of a specific type
	 * @param edgeType the type of edges
	 * @param <E> Concerete edge type
     */
	public <E extends Edge> Document removeAllEdges(Class<E> edgeType)  {
		engine().removeAllEdges(edgeLayer(edgeType));
		return this;
	}

	/**
	 * Remove all edges of a specific type and variant
	 * @param edgeType     the type of edges
	 * @param edgeVariant  the edge variant
	 * @param <E>          concrete edge type
     */
	public <E extends Edge> Document removeAllEdges(Class<E> edgeType, String edgeVariant)  {
		engine().removeAllEdges(edgeLayer(edgeType), edgeVariant);
		return this;
	}

	/**
	 * Remove all edges of a specific dynamic type
	 * @param dynamicType the dynamic edge type
     */
	public Document removeAllEdges(String dynamicType) {
		engine().removeAllEdges(edgeLayer(dynamicType));
		return this;
	}

	/**
	 * Remove all edges of a specific dynamic type and variant
	 * @param dynamicType the dynamic edge type
	 * @param edgeVariant the dynamic edge variant
	 */
	public Document removeAllEdges(String dynamicType, String edgeVariant) {
		engine().removeAllEdges(edgeLayer(dynamicType), edgeVariant);
		return this;
	}

	/**
	 * The start pos of this document
	 * <b>Remarks:</b> mostly applicable to inheriting documents such as views
	 */
	@Override
	public int getStart() {
		return 0;
	}

	/**
	 * The end pos of this document
	 * <b>Remarks:</b> mostly applicable to inheriting documents such as views
	 */
	@Override
	public int getEnd() {
		if(length == -1)
			return store().getText().length();
		else
			return length;
	}
	
	@Override
	public float getMidpoint() {
		return getEnd() / 2.0f;
	}

	/**
	 * Advanced: Override length with a value to allow insertion of annotations that exceeds set text.
	 * @param length the new length
	 * <b>Remarks:</b> Remember to set the text at the end otherwise all those annotations will have "" as text.
	 */
	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * Compute the length of the document.
     */
	@Override
	public int length() {
		return getEnd() - getStart();
	}

	/**
	 * Get variant metadata
	 * @param variant the variant
	 * @param key the key
     */
	public String getVariantMetadata(String variant, String key) {
		return store().getVariantMetadata(variant, key);
	}

	/**
	 * Put variant metadata
	 * @param variant the variant
	 * @param key the key
	 * @param value the values to store
     */
	public void putVariantMetadata(String variant, String key, String value) {
		store().putVariantMetadata(variant, key, value);
	}

	/**
	 * Remove all variant metadata
	 * @param variant the variant to remove all values from
     */
	public void removeVariantMetadata(String variant) {
		store().removeVariantMetadata(variant);
	}

	/**
	 * Get all variants that has metadata
     */
	public Iterable<String> variantsWithMetadata() {
		return store().variantsWithMetadata();
	}

	/**
	 * Get all metadata for a specific metadata
	 * @param variant the variant
	 * @return iterable of all values
     */
	public Iterable<Map.Entry<String, String>> variantMetadata(String variant) {
		return store().variantMetadata(variant);
	}

	/**
	 * Convert a NodeRef into a representation
	 * @param ref Only used to determine return type, not actually used to createFragment type.
	 * @param ref the node ref
	 * @param <N> Should be same as representation, otherwise a CastException will be thrown
     */
	@SuppressWarnings("unchecked")
	public <N extends Node> N node(NodeRef ref) {
		return (N) representations().get(ref);
	}

	/**
	 * Convert a EdgeRef into a representation
	 * @param ref Only used to determine return type, not actually used to createFragment type.
	 * @param ref the node ref
	 * @param <E> Should be same as representation, otherwise a CastException will be thrown
	 */
	@SuppressWarnings("unchecked")
	public <E extends Edge> E edge(EdgeRef ref) {
		return (E) representations().get(ref);
	}

	/**
	 * Create a view of this document
	 * @param start start positon
	 * @param end   end position
	 * @return view of the document that acts as a thin annotatinos translation layer
	 * <b>Remarks:</b> proxy objects are different due to having a different transformation.
	 */
    public View view(int start, int end) {
        return new View(this, start, end);
    }

    /**
     * Create a view of this document
     * @param range the node to get a view of
     * @return view of the document that acts as a thin annotatinos translation layer
     * <b>Remarks:</b> proxy objects are different due to having a different transformation.
     */
    public View view(Node range) {
        return new View(this, range.getStart(), range.getEnd());
    }

	/**
	 * Replace all matching with provided string and reflect coordinate
	 * changes on all annotations.
	 * @param regex matcher regex
	 * @param with  replacement text
	 * <b>Remarks:</b> removal of resulting anchors is true by default.
	 */
	public void replace(String regex, String with) {
		replace(Pattern.compile(regex), with, true);
	}

	/**
	 * Replace all matching with provided string and reflect coordinate
	 * changes on all annotations.
	 * @param regex matcher regex
	 * @param with  replacement text
	 * @param removeResultingAnchors set to true if you want to remove annotations
	 *                               that results in 0 length after replacement.
	 */
	public void replace(String regex, String with, boolean removeResultingAnchors) {
		replace(Pattern.compile(regex), with, removeResultingAnchors);
	}

	/**
	 * Replace all matching with provided string and reflect coordinate
	 * changes on all annotations.
	 * @param regex matcher regex
	 * @param with  replacement text
	 * <b>Remarks:</b> removal of resulting anchors is true by default.
	 */
	public void replace(Pattern regex, String with) {
		replace(regex, with, true);
	}

	/**
	 * Replace all matching with provided string and reflect coordinate
	 * changes on all annotations.
	 * @param regex matcher regex
	 * @param with  replacement text
	 * @param removeResultingAnchors set to true if you want to remove annotations
	 *                               that results in 0 length after replacement.
	 */
	public void replace(Pattern regex, String with, boolean removeResultingAnchors) {
		Matcher matcher = regex.matcher(text());
		IntArrayList startValues = new IntArrayList();
		IntArrayList endValues = new IntArrayList();

		StringBuilder sb = new StringBuilder();

		int delta = 0;

		int last = 0;
		while(matcher.find()) {
			startValues.add(matcher.start());
			endValues.add(matcher.end());

			delta += Math.abs((matcher.end()-matcher.start()) - with.length());

			int prevStart = last;
			int prevEnd = matcher.start();

			if(prevStart != prevEnd) {
				sb.append(store().getText(), prevStart, prevEnd);
			}

			sb.append(with);
			last = matcher.end();
		}

		if(last != text().length())
		{
			sb.append(text(), last, text().length());
		}

		store().setText(sb.toString());

		if(delta > 0) {
			//Go through each layer, and correct the start,end positions
			DocumentEngine engine = engine();
			List<NodeRef> removalList = new ArrayList<>();

			DocumentTransaction trans = begin();

			for (String layer : engine().nodeLayers().toList()) {
				for (Optional<String> variant : engine().nodeLayerAllVariants(layer).toList()) {
					AnnotationNavigator<NodeRef> annotations = engine().annotations(layer, variant);
					List<NodeRef> annotationList = new ArrayList<>();

					int deltaGlobal = 0;

					while(annotations.next()) {
						annotationList.add(annotations.current());
					}

					if(annotationList.size() > 0) {
						int k = 0;
						int h = 0;

						NodeStore nodeStore = annotationList.get(k).get();
						int newStart = nodeStore.getStart();
						int newEnd = nodeStore.getEnd();

						while( k < annotationList.size() && h < startValues.size() ) {
							if(nodeStore == null) {
								nodeStore = annotationList.get(k).get();
								newStart = nodeStore.getStart()+deltaGlobal;
								newEnd = nodeStore.getEnd()+deltaGlobal;
							}

							int startValue = startValues.get(h);
							int endValue = endValues.get(h);

							if(endValue <= nodeStore.getStart()) { //replace is before annotation
								deltaGlobal += with.length() - (endValue - startValue);
								newStart += with.length() - (endValue - startValue);
								newEnd += with.length() - (endValue - startValue);
								h++;
							} else if(nodeStore.getEnd() <= startValue) { //annotation is before replace
								nodeStore.setRanges(newStart,newEnd);
								nodeStore = null;

								k++;
							} else if(nodeStore.getStart() < endValue && nodeStore.getEnd() > startValue) { //replace intersects annotation
								int deltaLocal = with.length() - (endValue - startValue);

								if(deltaLocal < 0) {
									if(endValue + deltaLocal < nodeStore.getStart())
										newStart = endValue + deltaLocal + deltaGlobal;

									if(endValue <= nodeStore.getEnd())
										newEnd += deltaLocal;
									else if(endValue + deltaLocal <= nodeStore.getEnd())
										newEnd = endValue + deltaLocal + deltaGlobal;
									else
										System.out.println("Not handled.");
								}
								else {
									if(endValue <= nodeStore.getEnd()) {
										newEnd += deltaLocal;
									}
								}

								assert newStart <= newEnd;

								if(endValue >= nodeStore.getEnd()) {
									if(newStart == newEnd && nodeStore.getStart() != nodeStore.getEnd() && removeResultingAnchors) {
										removalList.add(nodeStore.getRef());
									}

									nodeStore.setRanges(newStart,newEnd);
									nodeStore = null;
									k++; //replace is larger than current annotation
								} else {
									deltaGlobal += deltaLocal;
									h++; //replace is within current annotation
								}
							}
							else {
								throw new UnsupportedOperationException("Should not happen");
							}
						}

						if(nodeStore != null) {
							nodeStore.setRanges(newStart,newEnd);
							nodeStore = null;
							k++;
						}

						while(k < annotationList.size()) {
							nodeStore = annotationList.get(k).get();
							nodeStore.setRanges(nodeStore.getStart()+deltaGlobal,nodeStore.getEnd()+deltaGlobal);

							k++;
						}
					}
				}
			}

			for (NodeRef nodeRef : removalList) {
				this.store().remove(nodeRef);
			}
		}
	}

	/**
	 * Split the document into bits by matching regex
	 * @param regex the regex to split by
	 * @param limit the number of splits, must be &gt;= 0, 0 == infinite
	 * @return iterable of views symbolising the splits
	 */
	public Iterable<View> split(final Pattern regex, final int limit) {
		if(limit < 0)
			throw new IllegalArgumentException("max must be >= 0");

		if(limit == 0) {
			//infinite number
			return new Iterable<View>() {
				@Override
				public Iterator<View> iterator() {

					return new Iterator<View>() {
						Matcher matcher = regex.matcher(text());
						boolean read;
						int last = 0;
						int nextStart;
						int nextEnd;

						public boolean moveForward() {
							if(read)
								return nextStart != -1 && nextEnd != -1;
							else
							{
								if(matcher.find()) {
									nextStart = last;
									nextEnd = matcher.start();
									last = matcher.end();
									read = true;
									return true;
								}
								else if(last != -1) {
									read = true;
									nextStart = last;
									nextEnd = length();
									last = -1;
									return true;
								}
								else {
									read = true;
									last = -1;
									nextStart = -1;
									nextEnd = -1;
									return false;
								}
							}
						}

						@Override
						public boolean hasNext() {
							return moveForward();
						}

						@Override
						public View next() {
							if(!moveForward())
								throw new NoSuchElementException();

							read = false;
							return view(nextStart, nextEnd);
						}

						@Override
						public void remove() {
							throw new UnsupportedOperationException();
						}
					};
				}
			};
		}
		else if(limit == 1) {
			return new Iterable<View>() {
				@Override
				public Iterator<View> iterator() {
					return Arrays.asList(view(0,getEnd())).iterator();
				}
			};
		}
		else {
			return new Iterable<View>() {
				@Override
				public Iterator<View> iterator() {

					return new Iterator<View>() {
						Matcher matcher = regex.matcher(text());
						boolean read;
						int last = 0;
						int nextStart;
						int nextEnd;
						int numRead;

						public boolean moveForward() {
							if(read)
								return nextStart != -1 && nextEnd != -1;
							else
							{
								if(numRead != limit - 1) {
									if(matcher.find()) {
										nextStart = last;
										nextEnd = matcher.start();
										last = matcher.end();
										read = true;
										numRead++;
										return true;
									}
									else if(last != -1) {
										read = true;
										nextStart = last;
										nextEnd = length();
										last = -1;
										numRead++;
										return true;
									}
									else
										return false;
								}
								else if(last != -1) {
									read = true;
									nextStart = last;
									nextEnd = length();
									last = -1;
									return true;
								}
								else
									return false;
							}
						}

						@Override
						public boolean hasNext() {
							return moveForward();
						}

						@Override
						public View next() {
							if(!moveForward())
								throw new NoSuchElementException();

							read = false;
							return view(nextStart, nextEnd);
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

	/**
	 * Do a regex search and return a view for each match.
	 * @param regex the regex
	 * @return iterable of views for each match
	 * <b>Remarks:</b> compiles an regex with default flags.
	 */
	public DocumentIterable<View> find(final String regex) {
		return find(Pattern.compile(regex));
	}

	/**
	 * Do a regex search and return a view for each match.
	 * @param regex the compiled regex pattern
	 * @return iterable of views for each match
	 */
	public DocumentIterable<View> find(final Pattern regex) {
		return DocumentIterables.wrap(new Iterable<View>() {
			@Override
			public Iterator<View> iterator() {

				return new Iterator<View>() {
					Matcher matcher = regex.matcher(text());
					boolean read;
					int nextStart;
					int nextEnd;

					public boolean moveForward() {
						if(read)
							return nextStart != -1 && nextEnd != -1;
						else
						{
							if(matcher.find()) {
								nextStart = matcher.start();
								nextEnd = matcher.end();
								read = true;
								return true;
							}
							else
							{
								read = true;
								nextStart = -1;
								nextEnd = -1;
								return false;
							}
						}
					}

					@Override
					public boolean hasNext() {
						return moveForward();
					}

					@Override
					public View next() {
						if(!moveForward())
							throw new NoSuchElementException();

						read = false;
						return view(nextStart, nextEnd);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		});
	}

	/**
	 * Do a regex search and return a view for each match.
	 * @param regex the compiled regex pattern
	 * @param group the group to derive name from.
	 * @return iterable of views for each match
	 */
	public DocumentIterable<View> find(final Pattern regex, final int group) {
		return DocumentIterables.wrap(new Iterable<View>() {
			@Override
			public Iterator<View> iterator() {

				return new Iterator<View>() {
					Matcher matcher = regex.matcher(text());
					boolean read;
					int nextStart;
					int nextEnd;

					public boolean moveForward() {
						if(read)
							return nextStart != -1 && nextEnd != -1;
						else
						{
							if(matcher.find()) {
								nextStart = matcher.start(group);
								nextEnd = matcher.end(group);
								read = true;
								return true;
							}
							else
							{
								read = true;
								nextStart = -1;
								nextEnd = -1;
								return false;
							}
						}
					}

					@Override
					public boolean hasNext() {
						return moveForward();
					}

					@Override
					public View next() {
						if(!moveForward())
							throw new NoSuchElementException();

						read = false;
						return view(nextStart, nextEnd);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		});
	}

    /**
     * Transforms location to target document coordinates.
	 * @param pos the position
     * @return range
     */
    public int transform(int pos) {
        return pos;
    }

	/**
	 * Transforms location to target document coordinates by modifying input parameter.
	 */
	public void transform(MutableRange seq) {}

	/**
	 * Transforms location to target document coordinates.
	 * @return new AnnotationRange of range with the new coordinates
	 */
	public Range transform(Range range) {
		return range;
	}

	/**
	 * Transforms location to source document coordinates.
	 * @param pos the position
	 * @return range
	 */
    public int inverseTransform(int pos) {
        return pos;
    }

	/**
	 * Transforms location to source document coordinates by modifying input parameter.
	 */
	public void inverseTransform(MutableRange seq) {}

	/**
	 * Transforms location to source document coordinates.
	 * @return range
	 */
    public Range inverseTransform(Range range) {
        return range;
    }

	/**
	 * Get Document factory that allows you to createFragment other records that has the same storage layer as this one.
	 * @return factory
	 */
    public abstract DocumentFactory factory();

    /**
     * <b>UNSAFE! </b> Will remove all cached instances of
     * representations to let the GC collect and reclaim that space.
     *
     * All referenced and saved (non-refs such as Token) will be void and invalid.
     * This also removes any tags attached to all nodes and edges.
     */
    public void unsafeResetRepresentations() {
        representations().resetRepresentations();
    }

    public Iterable<Map.Entry<String,Document>> documentProperties() {
        return new FilteredMappedDocumentIterable<Map.Entry<String, Document>,Map.Entry<String,DataRef>>(properties()) {
            @Override
            protected boolean accept(Map.Entry<String, DataRef> value) {
                return value.getValue() instanceof DocRef;
            }

            @Override
            protected Map.Entry<String, Document> map(Map.Entry<String, DataRef> value) {
                return new Map.Entry<String, Document>() {
                    @Override
                    public String getKey() {
                        return value.getKey();
                    }

                    @Override
                    public Document getValue() {
                        return ((DocRef)value.getValue()).documentValue();
                    }

                    @Override
                    public Document setValue(Document value) {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

	/**
	 * Convert a document from one storage layer to another
	 * @param factory the new storage layer factory
	 * @param source the old document
     * @return copy of source into a new representation
     */
	public static Document convert(DocumentFactory factory, Document source) {
		Document target = factory.createFragment(source.id(), source.text());
		target.setType(source.type());

		//Copy properties
		for (Map.Entry<String, DataRef> entry : source.store().properties()) {
			target.putProperty(entry.getKey(), entry.getValue().copy());
		}

		//Copy variant info
		for (Map.Entry<String, String> edgeEntry : source.store().getDefaultEdgeVariants().entrySet()) {
			target.store().setDefaultEdgeVariant(edgeEntry.getKey(),edgeEntry.getValue());
		}

		for (Map.Entry<String, String> nodeEntry : source.store().getDefaultNodeVariants().entrySet()) {
			target.store().setDefaultNodeVariant(nodeEntry.getKey(), nodeEntry.getValue());
		}

		for (String variant : source.store().variantsWithMetadata()) {
			for (Map.Entry<String, String> entry : source.store().variantMetadata(variant)) {
				target.putVariantMetadata(variant, entry.getKey(), entry.getValue());
			}
		}

		//Copy nodes
		Reference2ReferenceOpenHashMap<NodeRef,NodeRef> nodeRefs = new Reference2ReferenceOpenHashMap<>();

		DocumentStore sourceStore = source.store();
		DocumentStore targetStore = target.store();

		for (NodeRef sourceNodeRef : sourceStore.nodes()) {
			NodeStore sourceNodeStore = sourceNodeRef.get();

			NodeRef targetNodeRef = targetStore.createNode(sourceNodeStore.getLayer(), sourceNodeStore.getVariant());
			NodeStore targetNodeStore = targetNodeRef.get();

			for (Map.Entry<String, DataRef> entry : sourceNodeStore.properties()) {
				targetNodeStore.putProperty(entry.getKey(), entry.getValue().copy());
			}

			if(!sourceNodeStore.isAnnotation()) {
				targetNodeStore.setNoRanges();
			} else {
				targetNodeStore.setRanges(sourceNodeStore.getStart(), sourceNodeStore.getEnd());
			}

			nodeRefs.put(sourceNodeRef, targetNodeRef);
		}

		//Copy edges
		for (EdgeRef edgeRef : sourceStore.edges()) {
			EdgeStore sourceEdgeStore = edgeRef.get();

			EdgeRef targetEdgeRef = targetStore.createEdge(sourceEdgeStore.getLayer(), sourceEdgeStore.getVariant());
			EdgeStore targetEdgeStore = targetEdgeRef.get();

			for (Map.Entry<String, DataRef> entry : sourceEdgeStore.properties()) {
				targetEdgeStore.putProperty(entry.getKey(), entry.getValue().copy());
			}

			targetEdgeStore.connect(nodeRefs.get(sourceEdgeStore.getTail()), nodeRefs.get(sourceEdgeStore.getHead()));
		}

		return target;
	}

	/**
	 * Apply a document function
	 * @param function some function that modifies this document.
     */
	public Document apply(DocumentFunction function) {
		function.apply(this);
		return this;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(o == null) return false;

        if (o instanceof Document) {
            Document document = (Document) o;
            return document.text().equals(this.text());
        } else
            return o instanceof CharSequence && this.text().equals(o.toString());
    }

    @Override
    public int compareTo(Document o) {
        return text().compareTo(o.text());
    }

    /** Deep equals, by comparing containing layer and document properties */
    public boolean deepEquals(Document doc) {
		if(!doc.text().equals(doc.text()))
			return false;

        if(doc.store().numProperties() != store().numProperties())
            return false;

        for (Map.Entry<String, DataRef> entry : store().properties()) {
            if(!doc.hasProperty(entry.getKey()))
                return false;

            if(!doc.getRefProperty(entry.getKey()).equals(entry.getValue()))
                return false;
        }

        if(nodes().count() != doc.nodes().count())
            return false;

        if(edges().count() != doc.edges().count())
            return false;

        HashSet<LayerRef> currentNoderefs
                = StreamSupport.stream(engine().nodeLayerRefs().spliterator(),false)
                               .map(DocumentStore.NodeLayerRef::new)
                               .collect(Collectors.toCollection(HashSet::new));

        HashSet<LayerRef> currentEdgerefs
                = StreamSupport.stream(engine().edgeLayerRefs().spliterator(),false)
                               .map(DocumentStore.EdgeLayerRef::new)
                               .collect(Collectors.toCollection(HashSet::new));

        HashSet<LayerRef> compareNoderefs
                = StreamSupport.stream(doc.engine().nodeLayerRefs().spliterator(),false)
                                               .map(DocumentStore.NodeLayerRef::new)
                                               .collect(Collectors.toCollection(HashSet::new));

        HashSet<LayerRef> compareEdgerefs
                = StreamSupport.stream(doc.engine().edgeLayerRefs().spliterator(),false)
                               .map(DocumentStore.EdgeLayerRef::new)
                               .collect(Collectors.toCollection(HashSet::new));

        if(!currentNoderefs.equals(compareNoderefs))
            return false;

        if(!currentEdgerefs.equals(compareEdgerefs))
            return false;

        //TODO: Implement a full node and edge comparision.
        return true;
    }

    @Override
    public int hashCode() {
        return text().hashCode();
    }

    /**
     * To bytes with level 2 compression.
     */
    public byte[] toBytes() {
        return toBytes(DocumentStorageLevel.LEVEL_2);
    }

	/**
	 * Convert to bytes
     */
	public byte[] toBytes(DocumentStorageLevel opt) {
        return factory().io().toBytes(this, opt);
    }

	/**
	 * Convert this document into json
     * <b>Remarks: </b> Uses Baseline optimization, for readability.
     */
	public String toJson() {
		return toJson(DocumentStorageLevel.LEVEL_0);
	}

    /**
     * Convert this document into json
     */
    public String toJson(DocumentStorageLevel opt) {
        return factory().io().toJson(this, opt);
    }

    @Override
    public char charAt(int index) {
        return text().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return text(start, end);
    }

    @Override
    public IntStream chars() {
        return text().chars();
    }

    @Override
    public IntStream codePoints() {
        return text().codePoints();
    }
}

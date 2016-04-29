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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import se.lth.cs.docforia.EdgeRef;
import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.data.CoreRef;
import se.lth.cs.docforia.data.CoreRefType;
import se.lth.cs.docforia.data.DataRef;

import java.io.IOError;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * Memory Document JSON Level 0 codec
 */
public class MemoryJsonLevel0Codec extends MemoryJsonCodec {

    public static final MemoryJsonLevel0Codec INSTANCE = new MemoryJsonLevel0Codec();

    protected static void writeProperties(Object2ObjectOpenHashMap<String,DataRef> props, JsonGenerator writer) {
        try {
            writer.writeStartObject();
            for (Object2ObjectMap.Entry<String, DataRef> entry : props.object2ObjectEntrySet()) {
                writer.writeFieldName(entry.getKey());
                if(entry.getValue() instanceof CoreRef) {
                    CoreRef prop =  (CoreRef)(entry.getValue());
                    prop.write(writer);
                }
                else
                    throw new UnsupportedOperationException("Only CoreRefs are supported for encoding.");
            }

            writer.writeEndObject();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    protected static int writeNodes(int idcounter, MemoryNodeCollection collection, JsonGenerator writer, Reference2IntOpenHashMap<NodeRef> refs) {
        try {
            int numPureNodes = 0;

            writer.writeStartObject();

            writer.writeArrayFieldStart("ranges");
            for (NodeRef nodeRef : collection) {
                MemoryNode nodeStore = (MemoryNode) nodeRef.get();
                if (!nodeStore.isAnnotation()) {
                    numPureNodes++;
                } else {
                    writer.writeNumber(nodeStore.start);
                    writer.writeNumber(nodeStore.end);
                }

                refs.put(nodeStore, idcounter++);
            }
            writer.writeEndArray();

            writer.writeObjectField("numBare", numPureNodes);

            writer.writeArrayFieldStart("properties");
            for (NodeRef nodeRef : collection) {
                MemoryNode nodeStore = (MemoryNode) nodeRef.get();
                writeProperties(nodeStore.properties, writer);
            }
            writer.writeEndArray();

            writer.writeEndObject();

            return idcounter;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    protected static int writeNodeLayer(int idcounter, JsonGenerator writer, ArrayList<MemoryNodeCollection> layerGroup, Reference2IntOpenHashMap<NodeRef> refs) {
        try {
            writer.writeStartObject();
            writer.writeObjectField("layer", layerGroup.get(0).getKey().layer);
            writer.writeArrayFieldStart("variants");
            for (MemoryNodeCollection collection : layerGroup) {
                writer.writeString(collection.key.getVariant() == null ? "" : collection.key.getVariant());
            }
            writer.writeEndArray();

            writer.writeFieldName("nodes");
            writer.writeStartArray();
            for (MemoryNodeCollection collection : layerGroup) {
                idcounter = writeNodes(idcounter, collection, writer, refs);
            }
            writer.writeEndArray();

            writer.writeEndObject();
        } catch (IOException e) {
            throw new IOError(e);
        }
        return idcounter;
    }

    public static void readNodes(JsonNode node, MemoryNodeCollection collection, Int2ReferenceOpenHashMap<NodeRef> nodeRefs) {
        JsonNode ranges = node.path("ranges");
        JsonNode properties = node.path("properties");
        int numBare = node.path("numBare").asInt();
        int numNodes = ranges.size()/2;

        Iterator<JsonNode> propIter = properties.elements();

        for(int i = 0; i < numBare; i++) {
            MemoryNode memoryNode;
            nodeRefs.put(nodeRefs.size(), memoryNode = collection.create());

            memoryNode.properties = readProperties(propIter.next());
        }

        for(int i = 0; i < numNodes; i++) {
            int start = ranges.get(i*2).asInt();
            int end = ranges.get(i*2+1).asInt();

            MemoryNode memoryNode;
            nodeRefs.put(nodeRefs.size(), memoryNode = collection.create(start, end));

            memoryNode.properties = readProperties(propIter.next());
        }
    }

    public static void readNodeLayers(JsonNode node, MemoryDocumentStore store, Int2ReferenceOpenHashMap<NodeRef> nodeRefs) {
        Iterator<JsonNode> layerIter = node.elements();
        while(layerIter.hasNext()) {
            JsonNode layerNode = layerIter.next();
            String layer = layerNode.path("layer").asText();

            Iterator<JsonNode> variantsIter = layerNode.path("variants").elements();
            Iterator<JsonNode> nodesIter = layerNode.path("nodes").elements();
            while (variantsIter.hasNext() && nodesIter.hasNext()) {
                JsonNode nodes = nodesIter.next();
                String variant = variantsIter.next().asText();
                readNodes(nodes, store.getNodeCollection(layer, variant.length() == 0 ? null : variant), nodeRefs);
            }
        }
    }

    public static void readEdges(JsonNode node, MemoryEdgeCollection collection, Int2ReferenceOpenHashMap<NodeRef> nodeRefs) {
        JsonNode connections = node.path("connections");
        JsonNode properties = node.path("properties");
        int numEdges = connections.size()/2;

        Iterator<JsonNode> propIter = properties.elements();

        for(int i = 0; i < numEdges; i++) {
            int head = connections.get(i*2).intValue();
            int tail = connections.get(i*2+1).intValue();

            MemoryEdge memoryEdge = collection.create();
            memoryEdge.connect(nodeRefs.get(tail), nodeRefs.get(head));

            memoryEdge.properties = readProperties(propIter.next());
        }
    }

    public static void readEdgeLayers(JsonNode node, MemoryDocumentStore store, Int2ReferenceOpenHashMap<NodeRef> nodeRefs) {
        Iterator<JsonNode> layerIter = node.elements();
        while(layerIter.hasNext()) {
            JsonNode layerNode = layerIter.next();
            String layer = layerNode.path("layer").asText();

            Iterator<JsonNode> variantsIter = layerNode.path("variants").elements();
            Iterator<JsonNode> nodesIter = layerNode.path("edges").elements();
            while (variantsIter.hasNext() && nodesIter.hasNext()) {
                JsonNode nodes = nodesIter.next();
                String variant = variantsIter.next().asText();
                readEdges(nodes, store.getEdgeCollection(layer, variant.length() == 0 ? null : variant), nodeRefs);
            }
        }
    }

    protected static void writeEdges(MemoryEdgeCollection collection, JsonGenerator writer, Reference2IntOpenHashMap<NodeRef> refs) {
        try {
            writer.writeStartObject();
            writer.writeArrayFieldStart("connections");
            for (EdgeRef edgeRef : collection) {
                MemoryEdge edge = (MemoryEdge)edgeRef.get();

                writer.writeNumber(refs.getInt(edge.head));
                writer.writeNumber(refs.getInt(edge.tail));
            }
            writer.writeEndArray();

            writer.writeArrayFieldStart("properties");

            for (EdgeRef edgeRef : collection) {
                MemoryEdge edge = (MemoryEdge) edgeRef.get();
                writeProperties(edge.properties, writer);
            }
            writer.writeEndArray();
            writer.writeEndObject();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    protected static void writeEdgeLayer(JsonGenerator writer, ArrayList<MemoryEdgeCollection> layerGroup, Reference2IntOpenHashMap<NodeRef> refs) {
        try {
            writer.writeStartObject();
            writer.writeObjectField("layer", layerGroup.get(0).getKey().layer);
            writer.writeArrayFieldStart("variants");
            for (MemoryEdgeCollection collection : layerGroup) {
                writer.writeString(collection.key.getVariant() == null ? "" : collection.key.getVariant());
            }
            writer.writeEndArray();

            writer.writeFieldName("edges");
            writer.writeStartArray();
            for (MemoryEdgeCollection collection : layerGroup) {
                writeEdges(collection, writer, refs);
            }
            writer.writeEndArray();

            writer.writeEndObject();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    protected static void writeEdgeLayers(MemoryDocument doc, JsonGenerator writer, Reference2IntOpenHashMap<NodeRef> refs) {
        String lastLayer = "";
        ArrayList<MemoryEdgeCollection> layerCollections = new ArrayList<>();

        for (Object2ReferenceMap.Entry<MemoryEdgeCollection.Key, MemoryEdgeCollection> entry : doc.store.edges.object2ReferenceEntrySet()) {
            if(lastLayer.equals("")) {
                layerCollections.add(entry.getValue());
                lastLayer = entry.getKey().layer;
            }
            else if(lastLayer.equals(entry.getKey().layer)) {
                layerCollections.add(entry.getValue());
                lastLayer = entry.getKey().layer;
            }
            else {
                writeEdgeLayer(writer, layerCollections, refs);
                layerCollections.clear();
                lastLayer = entry.getKey().layer;
                layerCollections.add(entry.getValue());
            }
        }

        if(!layerCollections.isEmpty()) {
            writeEdgeLayer(writer, layerCollections, refs);
        }
    }

    protected static int writeNodeLayers(MemoryDocument doc, JsonGenerator writer, Reference2IntOpenHashMap<NodeRef> refs) {
        String lastLayer = "";
        ArrayList<MemoryNodeCollection> layerCollections = new ArrayList<>();
        int idcounter = 0;

        for (Object2ReferenceMap.Entry<MemoryNodeCollection.Key, MemoryNodeCollection> entry : doc.store.nodes.object2ReferenceEntrySet()) {
            if(lastLayer.equals("")) {
                layerCollections.add(entry.getValue());
                lastLayer = entry.getKey().layer;
            }
            else if(lastLayer.equals(entry.getKey().layer)) {
                layerCollections.add(entry.getValue());
                lastLayer = entry.getKey().layer;
            }
            else {
                idcounter = writeNodeLayer(idcounter, writer, layerCollections, refs);
                layerCollections.clear();
                lastLayer = entry.getKey().layer;
                layerCollections.add(entry.getValue());
            }
        }

        if(!layerCollections.isEmpty()) {
            idcounter = writeNodeLayer(idcounter, writer, layerCollections, refs);
        }

        return idcounter;
    }


    public String encode(MemoryDocument doc) {
        try {
            StringWriter writer = new StringWriter();
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator generator = jsonFactory.createGenerator(writer);
            encode(doc, generator);
            generator.flush();
            return writer.toString();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }


    public void encode(MemoryDocument doc, JsonGenerator jsonWriter) {
        try {
            Reference2IntOpenHashMap<NodeRef> refs = new Reference2IntOpenHashMap<>();

            jsonWriter.writeStartObject();
            jsonWriter.writeObjectFieldStart("DM10");

            //Write properties
            jsonWriter.writeObjectFieldStart("properties");
            for (String s : doc.store.properties.keySet()) {
                jsonWriter.writeFieldName(s);

                DataRef ref = doc.store.properties.get(s);
                if(ref instanceof CoreRef) {
                    ((CoreRef) ref).write(jsonWriter);
                } else {
                    throw new UnsupportedOperationException("Only core properties are supported with PropertyMap!");
                }
            }
            jsonWriter.writeEndObject();

            jsonWriter.writeObjectField("text", doc.store.text);

            //Write nodes
            jsonWriter.writeArrayFieldStart("nodes");
            writeNodeLayers(doc, jsonWriter, refs);
            jsonWriter.writeEndArray();

            //Write edges
            jsonWriter.writeArrayFieldStart("edges");
            writeEdgeLayers(doc, jsonWriter, refs);
            jsonWriter.writeEndArray();

            jsonWriter.writeEndObject();
            jsonWriter.writeEndObject();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Object2ObjectOpenHashMap<String,DataRef> readProperties(JsonNode propNode) {
        Object2ObjectOpenHashMap<String,DataRef> map = new Object2ObjectOpenHashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fieldIter = propNode.fields();
        while(fieldIter.hasNext()) {
            Map.Entry<String, JsonNode> entry = fieldIter.next();
            map.put(entry.getKey(), CoreRefType.fromJsonValue(entry.getValue()).readJson(entry.getValue()));
        }

        return map;
    }

    @Override
    public MemoryDocument decode(JsonNode jsonNode) {
        if(!jsonNode.isObject()) {
            String json = jsonNode.toString();
            throw new IllegalArgumentException("This is not a document model JSON, first 100 chars: " + json.substring(0, Math.min(json.length(),100)));
        }

        Iterator<String> fields = jsonNode.fieldNames();
        if(!fields.hasNext()) {
            String json = jsonNode.toString();
            throw new IllegalArgumentException("This is not a document model JSON, first 100 chars: " + json.substring(0, Math.min(json.length(),100)));
        }

        String version = fields.next();
        assert version.equals("DM10");

        MemoryDocumentStore store = new MemoryDocumentStore();

        JsonNode docData = jsonNode.path("DM10");

        JsonNode properties = docData.path("properties");
        store.properties = readProperties(properties);

        store.text = docData.get("text").asText();

        Int2ReferenceOpenHashMap<NodeRef> nodeRefs = new Int2ReferenceOpenHashMap<>();

        JsonNode nodes = docData.path("nodes");
        readNodeLayers(nodes, store, nodeRefs);

        JsonNode edges = docData.path("edges");
        readEdgeLayers(edges, store, nodeRefs);

        return new MemoryDocument(store);
    }
}

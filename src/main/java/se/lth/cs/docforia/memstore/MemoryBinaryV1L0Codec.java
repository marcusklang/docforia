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
import se.lth.cs.docforia.io.mem.Input;
import se.lth.cs.docforia.io.mem.Output;

import java.util.ArrayList;

/**
 * Memory Binary Codec v1 level 0
 */
public class MemoryBinaryV1L0Codec extends MemoryBinaryCodec {
    public static final MemoryBinaryV1L0Codec INSTANCE = new MemoryBinaryV1L0Codec();

    private void writeMagic(Output writer) {
        writer.writeByte((byte)'D');
        writer.writeByte((byte)'M');
        writer.writeByte((byte)'1');
        writer.writeByte((byte)'0');
    }

    private void writeProperties(Object2ObjectOpenHashMap<String,DataRef> props, Output writer) {
        writer.writeInt(props.size());
        for (Object2ObjectMap.Entry<String, DataRef> entry : props.object2ObjectEntrySet()) {
            writer.writeString(entry.getKey());
            if(entry.getValue() instanceof CoreRef) {
                CoreRef prop =  (CoreRef)(entry.getValue());
                writer.writeByte(prop.id().value);
                prop.write(writer);
            }
            else
                throw new UnsupportedOperationException("Only CoreRefs are supported for encoding.");
        }
    }

    private int writeNodes(int idcounter, MemoryNodeCollection collection, Output writer, Reference2IntOpenHashMap<NodeRef> refs) {
        int numPureNodesPos = writer.reserve(4);
        writer.reserve(4);

        int numPureNodes = 0;
        int numRangeNodes = 0;

        for (NodeRef nodeRef : collection) {
            MemoryNode nodeStore = (MemoryNode) nodeRef.get();
            if (!nodeStore.isAnnotation()) {
                numPureNodes++;
            } else {
                writer.writeInt(nodeStore.start);
                writer.writeInt(nodeStore.end);
                numRangeNodes++;
            }

            writeProperties(nodeStore.properties, writer);
            refs.put(nodeStore, idcounter++);
        }

        int currentPos = writer.position();

        writer.setPosition(numPureNodesPos);
        writer.writeInt(numPureNodes);
        writer.writeInt(numRangeNodes);
        writer.setPosition(currentPos);

        return idcounter;
    }

    private int writeNodeLayer(int idcounter, MemoryDocument doc, Output writer, ArrayList<MemoryNodeCollection> layerGroup, Reference2IntOpenHashMap<NodeRef> refs) {
        int id = MemoryCoreNodeLayer.fromLayerName(layerGroup.get(0).getKey().layer).id;
        if(id == -1) {
            writer.writeByte(0x7F);
            writer.writeString(layerGroup.get(0).getKey().layer);
        } else {
            writer.writeByte(id);
        }

        int startPos = writer.reserve(4);

        for (MemoryNodeCollection collection : layerGroup) {
            writer.writeString(collection.key.getVariant() == null ? "" : collection.key.getVariant());
            idcounter = writeNodes(idcounter, collection, writer, refs);
        }

        int currentPos = writer.position();
        writer.setPosition(startPos);
        writer.writeInt(currentPos-startPos-4);
        writer.setPosition(currentPos);
        return idcounter;
    }

    private void writeEdges(MemoryEdgeCollection collection, Output writer, Reference2IntOpenHashMap<NodeRef> refs) {
        writer.writeInt(collection.edges.size());

        for (EdgeRef edgeRef : collection) {
            MemoryEdge edge = (MemoryEdge)edgeRef.get();

            writer.writeInt(refs.getInt(edge.head));
            writer.writeInt(refs.getInt(edge.tail));
            writeProperties(edge.properties, writer);
        }
    }

    private void writeEdgeLayer(MemoryDocument doc, Output writer, ArrayList<MemoryEdgeCollection> layerGroup, Reference2IntOpenHashMap<NodeRef> refs) {
        int id = MemoryCoreEdgeLayer.fromLayerName(layerGroup.get(0).getKey().layer).id;
        if(id == -1) {
            writer.writeByte(0xFF);
            writer.writeString(layerGroup.get(0).getKey().layer);
        } else {
            writer.writeByte(id | 0x80);
        }

        int startPos = writer.reserve(4);

        for (MemoryEdgeCollection collection : layerGroup) {
            writer.writeString(collection.key.getVariant() == null ? "" : collection.key.getVariant());
            writeEdges(collection, writer, refs);
        }

        int currentPos = writer.position();
        writer.setPosition(startPos);
        writer.writeInt(currentPos-startPos-4);
        writer.setPosition(currentPos);
    }

    private void writeEdgeLayers(MemoryDocument doc, Output writer, Reference2IntOpenHashMap<NodeRef> refs) {
        String lastLayer = "";
        ArrayList<MemoryEdgeCollection> layerCollections = new ArrayList<>();
        int idcounter = 0;

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
                writeEdgeLayer(doc, writer, layerCollections, refs);
                layerCollections.clear();
                lastLayer = entry.getKey().layer;
                layerCollections.add(entry.getValue());
            }
        }

        if(!layerCollections.isEmpty()) {
            writeEdgeLayer(doc, writer, layerCollections, refs);
        }
    }

    private int writeNodeLayers(MemoryDocument doc, Output writer, Reference2IntOpenHashMap<NodeRef> refs) {
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
                idcounter = writeNodeLayer(idcounter, doc, writer, layerCollections, refs);
                layerCollections.clear();
                lastLayer = entry.getKey().layer;
                layerCollections.add(entry.getValue());
            }
        }

        if(!layerCollections.isEmpty()) {
            idcounter = writeNodeLayer(idcounter, doc, writer, layerCollections, refs);
        }

        return idcounter;
    }

    @Override
    public void encode(MemoryDocument doc, Output writer, MemoryBinary.DocumentIndex index) {
        writeMagic(writer);
        writeProperties(doc.store.properties, writer);

        writer.writeString(doc.store.text != null ? doc.store.text : "");

        Reference2IntOpenHashMap<NodeRef> refs = new Reference2IntOpenHashMap<>();

        int layerStart = writer.reserve(4);
        writeNodeLayers(doc, writer, refs);
        writeEdgeLayers(doc, writer, refs);
        int currentPos = writer.position();

        writer.setPosition(layerStart);
        writer.writeInt(currentPos-layerStart-4);
        writer.setPosition(currentPos);
    }

    private Object2ObjectOpenHashMap<String,DataRef> readProperties(Input reader) {
        Object2ObjectOpenHashMap<String,DataRef> props = new Object2ObjectOpenHashMap<>();
        int numProperties = reader.readInt();
        for(int i = 0; i < numProperties; i++) {
            String key = reader.readString();

            DataRef value = CoreRefType.fromByteValue(reader.readByte()).read(reader);
            props.put(key, value);
        }

        return props;
    }

    private void readNodes(MemoryNodeCollection collection, Input reader, Int2ReferenceOpenHashMap<NodeRef> nodeRefs)
    {
        int numPureNodes = reader.readInt();
        int numRangeNodes = reader.readInt();

        for(int i = 0; i < numPureNodes; i++) {
            MemoryNode node = collection.create();
            node.properties = readProperties(reader);
            nodeRefs.put(nodeRefs.size(), node);
        }

        for(int i = 0; i < numRangeNodes; i++) {
            int start = reader.readInt();
            int end = reader.readInt();

            MemoryNode node = collection.create(start, end);
            node.properties = readProperties(reader);
            nodeRefs.put(nodeRefs.size(), node);
        }
    }

    private void readNodeLayer(int id, Input reader, MemoryDocumentStore store, Int2ReferenceOpenHashMap<NodeRef> nodeRefs) {
        String layer = MemoryCoreNodeLayer.fromId(id).layer;
        if(layer == null) {
            layer = reader.readString();
        }

        int endPos = reader.readInt();
        endPos += reader.position();

        while(reader.position() < endPos) {
            String variant = reader.readString();
            if(variant.equals(""))
                variant = null;

            readNodes(store.getNodeCollection(layer, variant), reader, nodeRefs);
        }
    }

    private void readEdges(Input reader, MemoryEdgeCollection store, Int2ReferenceOpenHashMap<NodeRef> nodeRefs) {
        int edgeSize = reader.readInt();

        for(int i = 0; i < edgeSize; i++) {
            int head = reader.readInt();
            int tail = reader.readInt();

            MemoryEdge edge = store.create();

            edge.connect(nodeRefs.get(tail), nodeRefs.get(head));
            edge.properties = readProperties(reader);
        }
    }

    private void readEdgeLayer(int id, Input reader, MemoryDocumentStore store, Int2ReferenceOpenHashMap<NodeRef> nodeRefs) {
        String layer = MemoryCoreEdgeLayer.fromId(id).layer;
        if(layer == null) {
            layer = reader.readString();
        }

        int endPos = reader.readInt();
        endPos += reader.position();

        while(reader.position() < endPos) {
            String variant = reader.readString();
            if(variant.equals(""))
                variant = null;

            readEdges(reader, store.getEdgeCollection(layer, variant), nodeRefs);
        }
    }

    @Override
    public MemoryDocument decode(Input reader) {
        MemoryDocumentStore store = new MemoryDocumentStore();
        store.properties = readProperties(reader);
        store.text = reader.readString();

        Int2ReferenceOpenHashMap<NodeRef> nodeRefs = new Int2ReferenceOpenHashMap<>();

        int layerEnd = reader.readInt();
        layerEnd += reader.position();

        while(reader.position() < layerEnd) {
            int id = Byte.toUnsignedInt(reader.readByte());
            if((id & 0x80) == 0) {
                readNodeLayer(id, reader, store, nodeRefs);
            } else {
                readEdgeLayer(id & ~0x80, reader, store, nodeRefs);
            }
        }

        return new MemoryDocument(store);
    }
}

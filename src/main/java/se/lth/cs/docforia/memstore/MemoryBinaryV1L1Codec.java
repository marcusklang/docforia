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
import it.unimi.dsi.fastutil.objects.*;
import se.lth.cs.docforia.EdgeRef;
import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.PropertyStore;
import se.lth.cs.docforia.StoreRef;
import se.lth.cs.docforia.data.CoreRef;
import se.lth.cs.docforia.data.CoreRefType;
import se.lth.cs.docforia.data.DataRef;
import se.lth.cs.docforia.data.StringRef;
import se.lth.cs.docforia.io.mem.Input;
import se.lth.cs.docforia.io.mem.Output;

import java.util.ArrayList;
import java.util.Map;

/**
 * Memory Binary Codec v1 level 1
 */
public class MemoryBinaryV1L1Codec extends MemoryBinaryCodec {
    public static final MemoryBinaryV1L1Codec INSTANCE = new MemoryBinaryV1L1Codec();

    private static class PropertyKey {
        private String key;
        private CoreRefType type;

        public PropertyKey(String key, CoreRefType type) {
            this.key = key;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PropertyKey that = (PropertyKey) o;

            return type == that.type && key.equals(that.key);
        }

        @Override
        public int hashCode() {
            int result = key.hashCode();
            result = 31 * result + type.value;
            return result;
        }

        @Override
        public String toString() {
            return "'" + key + "', " + type.toString();
        }
    }

    private void writeMagic(Output writer) {
        writer.writeByte((byte)'D');
        writer.writeByte((byte)'M');
        writer.writeByte((byte)'1');
        writer.writeByte((byte)'1');
    }

    private void writeProperties(Object2ObjectOpenHashMap<String,DataRef> props, Output writer) {
        writer.writeVarInt(props.size(), true);
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

    private void writeEscapedProperty(Output output, CoreRef ref) {
        Output data = new Output(32,2<<29);
        switch (ref.id()) {
            case STRING:
                StringRef stringRef = (StringRef)ref;
                data.writeString(stringRef.stringValue());
                break;
            default:
                ref.write(data);
                break;
        }

        if(data.getBuffer()[0] == 0x00) {
            output.writeByte((byte)0xFF);
        }
        else if(data.getBuffer()[0] == 0xFF) {
            output.writeByte((byte)0xFF);
        }

        data.writeTo(output);
    }

    private void writeProperties(Object2IntLinkedOpenHashMap<PropertyKey> propertyKeys, Object2ObjectOpenHashMap<String,DataRef> props, Output writer) {
        //0x00 == skip, 0xFF == escaped byte follows
        for (Object2IntMap.Entry<PropertyKey> entry : propertyKeys.object2IntEntrySet()) {
            if(props.containsKey(entry.getKey().key)) {
                CoreRef prop = (CoreRef)props.get(entry.getKey().key);
                if(prop.id() == entry.getKey().type) {
                    writeEscapedProperty(writer, prop);
                } else {
                    writer.writeByte(0);
                }
            } else {
                writer.writeByte(0);
            }
        }
    }

    private void writeAllProperties(Iterable<? extends StoreRef> properties, Output writer) {
        Object2IntLinkedOpenHashMap<PropertyKey> propertyKeys = new Object2IntLinkedOpenHashMap<>();
        for (StoreRef entry : properties) {
            for (Map.Entry<String, DataRef> propEntry : entry.get().properties()) {
                if(propEntry.getValue() instanceof CoreRef) {
                    CoreRef prop = (CoreRef)propEntry.getValue();
                    PropertyKey pkey = new PropertyKey(propEntry.getKey(), prop.id());
                    if(!propertyKeys.containsKey(pkey)) {
                        propertyKeys.put(pkey, propertyKeys.size());
                    }
                }
                else
                    throw new UnsupportedOperationException("Only CoreRefs are supported for encoding.");
            }
        }

        writer.writeVarInt(propertyKeys.size(),true);
        for (Object2IntMap.Entry<PropertyKey> entry : propertyKeys.object2IntEntrySet()) {
            writer.writeByte(entry.getKey().type.value);
            writer.writeString(entry.getKey().key);
        }

        for (StoreRef entry : properties) {
            if(entry instanceof MemoryNode) {
                writeProperties(propertyKeys, ((MemoryNode)entry).properties, writer);
            }
            else if(entry instanceof MemoryEdge) {
                writeProperties(propertyKeys, ((MemoryEdge)entry).properties, writer);
            }
            else
                throw new RuntimeException("BUG!");
        }
    }

    private DataRef readProperty(PropertyKey pkey, Input reader) {
        switch (pkey.type) {
            case STRING:
                return new StringRef(reader.readString());
            default:
                return pkey.type.read(reader);
        }
    }

    private void readAllProperties(Iterable<? extends StoreRef> entries, Input reader) {
        final int numPropertyKeys = reader.readVarInt(true);
        PropertyKey[] pkeys = new PropertyKey[numPropertyKeys];

        for (int i = 0; i < numPropertyKeys; i++) {
            byte id = reader.readByte();
            String pkey = reader.readString();
            pkeys[i] = new PropertyKey(pkey, CoreRefType.fromByteValue(id));
        }

        for (StoreRef entry : entries) {
            PropertyStore store = entry.get();
            for (PropertyKey pkey : pkeys) {
                int firstByte = Byte.toUnsignedInt(reader.readByte());
                if(firstByte == 0)
                    continue;
                else if(firstByte != 0xFF) {
                    reader.setPosition(reader.position()-1);
                }

                store.putProperty(pkey.key, readProperty(pkey, reader));
            }
        }
    }

    private int writeNodes(int idcounter, MemoryNodeCollection collection, Output writer, Reference2IntOpenHashMap<NodeRef> refs) {
        int numPureNodes = 0;
        int numRangeNodes = 0;

        Output nodeDataWriter = new Output(512,2<<29);

        int lastRange = 0;
        for (NodeRef nodeRef : collection) {
            MemoryNode nodeStore = (MemoryNode) nodeRef.get();
            if (!nodeStore.isAnnotation()) {
                numPureNodes++;
            } else {
                nodeDataWriter.writeVarInt(nodeStore.start-lastRange, true);
                nodeDataWriter.writeVarInt(nodeStore.end-nodeStore.start, true);
                lastRange = nodeStore.end;
                numRangeNodes++;
            }

            //writeProperties(nodeStore.properties, nodeDataWriter);
            refs.put(nodeStore, idcounter++);
        }

        writer.writeVarInt(numPureNodes, true);
        writer.writeVarInt(numRangeNodes, true);
        nodeDataWriter.writeTo(writer);

        writeAllProperties(collection, writer);
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

        writer.writeVarInt(layerGroup.size(), true);

        for (MemoryNodeCollection collection : layerGroup) {
            writer.writeString(collection.key.getVariant() == null ? "" : collection.key.getVariant());
            idcounter = writeNodes(idcounter, collection, writer, refs);
        }

        return idcounter;
    }

    private void writeEdges(MemoryEdgeCollection collection, Output writer, Reference2IntOpenHashMap<NodeRef> refs) {
        writer.writeVarInt(collection.edges.size(),true);

        for (EdgeRef edgeRef : collection) {
            MemoryEdge edge = (MemoryEdge)edgeRef.get();

            writer.writeVarInt(refs.getInt(edge.head),true);
            writer.writeVarInt(refs.getInt(edge.tail),true);
        }

        writeAllProperties(collection, writer);
    }

    private void writeEdgeLayer(MemoryDocument doc, Output writer, ArrayList<MemoryEdgeCollection> layerGroup, Reference2IntOpenHashMap<NodeRef> refs) {
        int id = MemoryCoreEdgeLayer.fromLayerName(layerGroup.get(0).getKey().layer).id;
        if(id == -1) {
            writer.writeByte(0xFF);
            writer.writeString(layerGroup.get(0).getKey().layer);
        } else {
            writer.writeByte(id | 0x80);
        }

        writer.writeVarInt(layerGroup.size(),true);
        for (MemoryEdgeCollection collection : layerGroup) {
            writer.writeString(collection.key.getVariant() == null ? "" : collection.key.getVariant());
            writeEdges(collection, writer, refs);
        }
    }

    private void writeEdgeLayers(MemoryDocument doc, Output writer, Reference2IntOpenHashMap<NodeRef> refs) {
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
        int numProperties = reader.readVarInt(true);
        for(int i = 0; i < numProperties; i++) {
            String key = reader.readString();

            DataRef value = CoreRefType.fromByteValue(reader.readByte()).read(reader);
            props.put(key, value);
        }

        return props;
    }

    private void readNodes(MemoryNodeCollection collection, Input reader, Int2ReferenceOpenHashMap<NodeRef> nodeRefs)
    {
        int numPureNodes = reader.readVarInt(true);
        int numRangeNodes = reader.readVarInt(true);

        for(int i = 0; i < numPureNodes; i++) {
            MemoryNode node = collection.create();
            node.properties = readProperties(reader);
            nodeRefs.put(nodeRefs.size(), node);
        }

        int last = 0;

        ArrayList<MemoryNode> memoryNodes = new ArrayList<>(numPureNodes+numRangeNodes);
        for(int i = 0; i < numRangeNodes; i++) {
            int start = reader.readVarInt(true)+last;
            int end = reader.readVarInt(true)+start;
            last = end;

            MemoryNode node = collection.create(start, end);
            nodeRefs.put(nodeRefs.size(), node);
            memoryNodes.add(node);
        }

        readAllProperties(memoryNodes, reader);
    }

    private void readNodeLayer(int id, Input reader, MemoryDocumentStore store, Int2ReferenceOpenHashMap<NodeRef> nodeRefs) {
        String layer = MemoryCoreNodeLayer.fromId(id).layer;
        if(layer == null) {
            layer = reader.readString();
        }

        int numVariants = reader.readVarInt(true);

        for(int i = 0; i < numVariants; i++) {
            String variant = reader.readString();
            if(variant.equals(""))
                variant = null;

            readNodes(store.getNodeCollection(layer, variant), reader, nodeRefs);
        }
    }

    private void readEdges(Input reader, MemoryEdgeCollection store, Int2ReferenceOpenHashMap<NodeRef> nodeRefs) {
        int edgeSize = reader.readVarInt(true);

        ArrayList<MemoryEdge> memoryEdges = new ArrayList<>(edgeSize);
        for(int i = 0; i < edgeSize; i++) {
            int head = reader.readVarInt(true);
            int tail = reader.readVarInt(true);

            MemoryEdge edge = store.create();

            edge.connect(nodeRefs.get(tail), nodeRefs.get(head));
            memoryEdges.add(edge);
        }

        readAllProperties(memoryEdges, reader);
    }

    private void readEdgeLayer(int id, Input reader, MemoryDocumentStore store, Int2ReferenceOpenHashMap<NodeRef> nodeRefs) {
        String layer = MemoryCoreEdgeLayer.fromId(id).layer;
        if(layer == null) {
            layer = reader.readString();
        }

        int numVariants = reader.readVarInt(true);
        for (int i = 0; i < numVariants; i++) {
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

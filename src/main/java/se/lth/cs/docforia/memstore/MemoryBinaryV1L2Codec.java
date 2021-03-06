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

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.*;
import se.lth.cs.docforia.*;
import se.lth.cs.docforia.data.*;
import se.lth.cs.docforia.io.mem.Input;
import se.lth.cs.docforia.io.mem.Output;
import se.lth.cs.docforia.memstore.encoders.StringFieldDecoder;
import se.lth.cs.docforia.memstore.encoders.StringFieldEncoder;
import se.lth.cs.docforia.util.Iterables;
import se.lth.cs.docforia.util.StringTable;

import java.io.IOError;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.StreamSupport;

/**
 * Memory Binary Codec v1 level 2
 */
public class MemoryBinaryV1L2Codec extends MemoryBinaryCodec {
    public static final MemoryBinaryV1L2Codec INSTANCE = new MemoryBinaryV1L2Codec();

    public static class Reporter {
        private Node root;

        private static class Node {
            public Node(String name, int start) {
                this.name = name;
                this.start = start;
            }

            private String name;
            private int start;
            private int end;

            private ArrayList<Node> children = new ArrayList<>();

            public Node add(Node node) {
                children.add(node);
                return node;
            }

            public void report(int level, int total, StringTable table) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < level * 2; i++) {
                    sb.append(' ');
                }

                sb.append(name);

                table.row(sb.toString(), end - start, ((end - start) / (double) total) * 100.0, start, end);

                for (Node child : children) {
                    child.report(level + 1, total, table);
                }
            }
        }

        private Output writer;
        private ArrayDeque<Node> tree = new ArrayDeque<>();

        public Reporter(Output writer) {
            this.writer = writer;
            this.root = new Node("ROOT", writer.position());
            tree.push(root);
        }

        public void begin(String post) {
            Node parent = tree.peek();
            tree.push(parent.add(new Node(post, writer.position())));
        }

        public void end() {
            tree.pop().end = writer.position();
        }

        public void report() {
            end();

            StringTable table = new StringTable("Item", "Size", "%", "start", "end");
            NumberFormat nf = NumberFormat.getIntegerInstance();
            nf.setGroupingUsed(true);

            NumberFormat df = NumberFormat.getNumberInstance();
            df.setMinimumFractionDigits(2);
            df.setMaximumFractionDigits(2);

            table.setFormatter(1, o -> nf.format((Integer) o));
            table.setFormatter(2, o -> df.format((Double) o));

            table.setFormatter(3, o -> nf.format((Integer) o));
            table.setFormatter(4, o -> nf.format((Integer) o));

            table.alignRight(1);

            root.report(0, root.end - root.start, table);
            table.print(System.out);
        }
    }

    public static class NullReporter extends Reporter {
        public NullReporter(Output writer) {
            super(writer);
        }

        @Override
        public void begin(String post) {

        }

        @Override
        public void end() {

        }

        @Override
        public void report() {

        }
    }

    public static class Writer {
        private Output writer;
        private BinaryCoreWriter propwriter;
        private MemoryDocument doc;

        public Writer(Output writer, MemoryDocument doc) {
            this.writer = writer;
            this.propwriter = new BinaryCoreWriter(writer);
            this.doc = doc;
        }

        private void writeMagic() {
            writer.writeByte((byte)'D');
            writer.writeByte((byte)'M');
            writer.writeByte((byte)'1');
            writer.writeByte((byte)'2');
        }

        protected void beginReport(String name, Object...args) {

        }

        protected boolean isReporting() {
            return false;
        }

        protected void endReport() {

        }

        protected void done() {

        }

        private void writeProperties(Object2ObjectOpenHashMap<String,DataRef> props) {
            BinaryCoreWriter propwriter = new BinaryCoreWriter(writer);
            beginReport("doc properties");
            writer.writeVarInt(props.size(), true);
            for (Object2ObjectMap.Entry<String, DataRef> entry : props.object2ObjectEntrySet()) {
                beginReport("doc prop %s", entry.getKey());
                writer.writeString(entry.getKey());
                if(entry.getValue() instanceof CoreRef) {
                    CoreRef prop =  (CoreRef)(entry.getValue());
                    writer.writeByte(prop.id().value);
                    prop.write(propwriter);
                }
                else
                    throw new UnsupportedOperationException("Only CoreRefs are supported for encoding.");
                endReport();
            }
            endReport();
        }

        private void encodePropertyField(String text, boolean node, Iterable<? extends StoreRef> entries, PropertyKey key) {
            BinaryCoreWriter propwriter = new BinaryCoreWriter(writer);
            beginReport("prop %s (N = %d)", key.key, isReporting() ? StreamSupport.stream(entries.spliterator(),false).count() : 0);

            switch (key.type) {
                case STRING:
                    StringFieldEncoder encoder = new StringFieldEncoder();
                    if(node)
                        encoder.encodeNodeProperties(writer, key.key, text, entries);
                    else
                        encoder.encodeEdgeProperties(writer, key.key, text, entries);

                    break;
                default:
                    for (StoreRef storeRef : entries) {
                        DataRef property = storeRef.get().getRefProperty(key.key);
                        CoreRef cref = (CoreRef)property;
                        cref.write(propwriter);
                    }
                    break;
            }

            endReport();
        }


        private int writeNodes(String text, int idcounter, MemoryNodeCollection collection, Reference2IntOpenHashMap<NodeRef> refs) {
            PropertyLayerData<NodeRef> propertyLayerData = new PropertyLayerData<>(collection, true);

            if(isReporting())
                beginReport("variant %s data", Objects.toString(collection.getKey().variant));

            beginReport("prop types and keys");

            //1. Encode types and then keys
            {
                writer.writeVarInt(propertyLayerData.propertyKeyId.size()-1, true); //Do not write property key == 0 (pure node identifier)

                Output typeIds = new Output(propertyLayerData.propertyKeyId.size()-1);
                Output keys = new Output(32,2<<29);

                for (Object2IntMap.Entry<PropertyKey> propertyKeyEntry : propertyLayerData.propertyKeyId.object2IntEntrySet()) {
                    if(propertyKeyEntry.getKey().type != CoreRefType.NULL) {
                        typeIds.writeByte(propertyKeyEntry.getKey().type.value);
                    }
                }

                for (Object2IntMap.Entry<PropertyKey> propertyKeyEntry : propertyLayerData.propertyKeyId.object2IntEntrySet()) {
                    if(propertyKeyEntry.getKey().type != CoreRefType.NULL)
                    {
                        keys.writeString(propertyKeyEntry.getKey().key);
                    }
                }

                typeIds.writeTo(writer);
                keys.writeTo(writer);
            }

            endReport();

            beginReport("propsets");

            //2. Encode property sets
            writer.writeVarInt(propertyLayerData.propertySets.size(), true);
            for (Object2IntMap.Entry<PropertySet> entry : propertyLayerData.propertySets.object2IntEntrySet()) {
                PropertyKey[] pkeys = entry.getKey().keys;
                writer.writeVarInt(pkeys.length, true);
                for (PropertyKey pkey : pkeys) {
                    writer.writeVarInt(propertyLayerData.propertyKeyId.getInt(pkey), true);
                }
            }

            endReport();

            beginReport("node ranges");

            //3. Encode ranges in order of property sets
            for (Object2IntMap.Entry<PropertySet> entry : propertyLayerData.propertySets.object2IntEntrySet()) {

                if(isReporting()) {
                    beginReport("propset %d ( N = %d )",  entry.getIntValue(), propertyLayerData.propertyNodes.get(propertyLayerData.propertySets.getInt(entry.getKey())).size());
                }

                if(entry.getKey().keys.length > 0 && entry.getKey().keys[0].type == CoreRefType.NULL) {
                    writer.writeVarInt(propertyLayerData.propertyNodes.get(propertyLayerData.propertySets.getInt(entry.getKey())).size(), true);
                    for (NodeRef nodeRef : propertyLayerData.propertyNodes.get(propertyLayerData.propertySets.getInt(entry.getKey()))) {
                        MemoryNode nodeStore = (MemoryNode) nodeRef.get();
                        refs.put(nodeStore, idcounter++);
                    }
                } else {
                    writer.writeVarInt(propertyLayerData.propertyNodes.get(propertyLayerData.propertySets.getInt(entry.getKey())).size(), true);
                    int lastRange = 0;
                    for (NodeRef nodeRef : propertyLayerData.propertyNodes.get(propertyLayerData.propertySets.getInt(entry.getKey()))) {
                        MemoryNode nodeStore = (MemoryNode) nodeRef.get();
                        if (!nodeStore.isAnnotation()) {
                            throw new RuntimeException("BUG!");
                        } else {
                            writer.writeVarInt(nodeStore.start - lastRange, true);
                            writer.writeVarInt(nodeStore.end - nodeStore.start, true);
                            lastRange = nodeStore.end;
                        }

                        refs.put(nodeStore, idcounter++);
                    }
                }
                endReport();
            }

            endReport();

            beginReport("node propfields");

            //4. Encode fields in order
            for (Object2IntMap.Entry<PropertyKey> entry : propertyLayerData.propertyKeyId.object2IntEntrySet()) {
                if(entry.getKey().type != CoreRefType.NULL) {
                    encodePropertyField(text, true, propertyLayerData.getPropertyKeyEntries(entry.getIntValue()), entry.getKey());
                }
            }

            endReport();

            endReport();
            return idcounter;
        }

        private int writeNodeLayer(int idcounter, ArrayList<MemoryNodeCollection> layerGroup, Reference2IntOpenHashMap<NodeRef> refs) {
            if(isReporting()) {
                beginReport("Node layer %s", layerGroup.get(0).key.getLayer());
            }

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
                idcounter = writeNodes(doc.store.text, idcounter, collection, refs);
            }

            endReport();
            return idcounter;
        }

        private void writeEdges(MemoryEdgeCollection collection, Reference2IntOpenHashMap<NodeRef> refs) {
            PropertyLayerData<EdgeRef> propertyLayerData = new PropertyLayerData<>(collection, false);

            beginReport("variant " + Objects.toString(collection.getKey().variant) + " data");

            beginReport("prop types and keys");

            //1. Encode types and then keys
            {
                writer.writeVarInt(propertyLayerData.propertyKeyId.size(), true);

                Output typeIds = new Output(propertyLayerData.propertyKeyId.size());
                Output keys = new Output(32,2<<29);

                for (Object2IntMap.Entry<PropertyKey> propertyKeyEntry : propertyLayerData.propertyKeyId.object2IntEntrySet()) {
                    typeIds.writeByte(propertyKeyEntry.getKey().type.value);
                }

                for (Object2IntMap.Entry<PropertyKey> propertyKeyEntry : propertyLayerData.propertyKeyId.object2IntEntrySet()) {
                    keys.writeString(propertyKeyEntry.getKey().key);
                }

                typeIds.writeTo(writer);
                keys.writeTo(writer);
            }

            endReport();

            beginReport("propsets");

            //2. Encode property sets
            writer.writeVarInt(propertyLayerData.propertySets.size(), true);
            for (Object2IntMap.Entry<PropertySet> entry : propertyLayerData.propertySets.object2IntEntrySet()) {
                PropertyKey[] pkeys = entry.getKey().keys;
                writer.writeVarInt(pkeys.length, true);
                for (PropertyKey pkey : pkeys) {
                    writer.writeVarInt(propertyLayerData.propertyKeyId.getInt(pkey), true);
                }
            }

            endReport();

            beginReport("edge connections");

            for (ArrayList<EdgeRef> edgeRefs : propertyLayerData.propertyNodes.values()) {
                writer.writeVarInt(edgeRefs.size(), true);

                for (EdgeRef edgeRef : edgeRefs) {
                    MemoryEdge edge = (MemoryEdge) edgeRef.get();

                    int headId = refs.getInt(edge.head);
                    int tailId = refs.getInt(edge.tail);

                    writer.writeVarInt(headId, true);
                    writer.writeVarInt(tailId, true);
                }
            }

            endReport();

            beginReport("edge propfields");

            //4. Encode fields in order
            for (Object2IntMap.Entry<PropertyKey> entry : propertyLayerData.propertyKeyId.object2IntEntrySet()) {
                encodePropertyField(null, false, propertyLayerData.getPropertyKeyEntries(entry.getIntValue()), entry.getKey());
            }

            endReport();

            endReport();
        }

        private void writeEdgeLayer(ArrayList<MemoryEdgeCollection> layerGroup, Reference2IntOpenHashMap<NodeRef> refs) {
            if(isReporting()) {
                beginReport("Edge layer %s", layerGroup.get(0).key.getLayer());
            }

            int id = MemoryCoreEdgeLayer.fromLayerName(layerGroup.get(0).getKey().layer).id;
            if(id == -1) {
                writer.writeByte(0xFF);
                writer.writeString(layerGroup.get(0).getKey().layer);
            } else {
                writer.writeByte((byte)(id | 0x80));
            }

            writer.writeVarInt(layerGroup.size(),true);
            for (MemoryEdgeCollection collection : layerGroup) {
                writer.writeString(collection.key.getVariant() == null ? "" : collection.key.getVariant());
                writeEdges(collection, refs);
            }

            endReport();
        }

        private void writeEdgeLayers(Reference2IntOpenHashMap<NodeRef> refs) {
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
                } else {
                    writeEdgeLayer(layerCollections, refs);
                    layerCollections.clear();
                    lastLayer = entry.getKey().layer;
                    layerCollections.add(entry.getValue());
                }
            }

            if(!layerCollections.isEmpty()) {
                writeEdgeLayer(layerCollections, refs);
            }
        }

        private int writeNodeLayers(Reference2IntOpenHashMap<NodeRef> refs) {
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
                    idcounter = writeNodeLayer(idcounter, layerCollections, refs);
                    layerCollections.clear();
                    lastLayer = entry.getKey().layer;
                    layerCollections.add(entry.getValue());
                }
            }

            if(!layerCollections.isEmpty()) {
                idcounter = writeNodeLayer(idcounter, layerCollections, refs);
            }

            return idcounter;
        }

    }


    private static class Reader {
        private Input reader;
        private BinaryCoreReader propreader;

        public Reader(Input reader) {
            this.reader = reader;
            this.propreader = new BinaryCoreReader(reader);
        }

        private Object2ObjectOpenHashMap<String,DataRef> readProperties() {
            Object2ObjectOpenHashMap<String,DataRef> props = new Object2ObjectOpenHashMap<>();
            int numProperties = reader.readVarInt(true);
            for(int i = 0; i < numProperties; i++) {
                String key = reader.readString();

                DataRef value = propreader.read();
                props.put(key, value);
            }

            return props;
        }

        private PropertyKey[] readNodePropertyKeys() {
            int count = reader.readVarInt(true);
            byte[] types = new byte[count+1];

            //Read types
            if(count > 0)
                reader.readBytes(types, 1, types.length-1);

            PropertyKey[] propertyKeys = new PropertyKey[count+1];
            propertyKeys[0] = new PropertyKey("", CoreRefType.NULL);

            //Read strings
            for(int i = 0; i < count; i++) {
                propertyKeys[i+1] = new PropertyKey(reader.readString(), BinaryCoreReader.fromByteValue(types[i+1]));
            }

            return propertyKeys;
        }

        private PropertyKey[] readEdgePropertyKeys() {
            int count = reader.readVarInt(true);
            byte[] types = new byte[count];

            //Read types
            reader.readBytes(types);

            PropertyKey[] propertyKeys = new PropertyKey[count];

            //Read strings
            for(int i = 0; i < count; i++) {
                propertyKeys[i] = new PropertyKey(reader.readString(), BinaryCoreReader.fromByteValue(types[i]));
            }

            return propertyKeys;
        }


        private IntArrayList getPkey2IdList(Int2ObjectOpenHashMap<IntArrayList> pkeyid2setid, int id) {
            IntArrayList list = pkeyid2setid.get(id);
            if(list == null)
            {
                pkeyid2setid.put(id, list = new IntArrayList());
                return list;
            }
            else
                return list;
        }

        private PropertySet[] readPropertySets(PropertyKey[] keys, Int2ObjectOpenHashMap<IntArrayList> pkeyid2setid) {
            int numPropertySets = reader.readVarInt(true);
            if(numPropertySets < 0)
                throw new IOError(new IOException("Failed to read property sets, count is negative: " + numPropertySets));

            PropertySet[] propertySets = new PropertySet[numPropertySets];

            for(int i = 0; i < numPropertySets; i++) {
                int numKeys = reader.readVarInt(true);
                PropertyKey[] propertySet = new PropertyKey[numKeys];
                for(int k = 0; k < numKeys; k++) {
                    int pkey;
                    propertySet[k] = keys[pkey = reader.readVarInt(true)];
                    getPkey2IdList(pkeyid2setid, pkey).add(i);
                }

                propertySets[i] = new PropertySet(propertySet);
            }

            return propertySets;
        }

        private void decodePropertyField(
                String text,
                PropertyKey propertyKey,
                int[] propertySetMapping,
                IntArrayList psets,
                Int2ReferenceOpenHashMap<? extends StoreRef> refs)
        {
            int count = 0;
            IntListIterator iter = psets.iterator();
            while(iter.hasNext()) {
                int psetid = iter.nextInt();
                count += propertySetMapping[psetid+1]-propertySetMapping[psetid];
            }

            switch (propertyKey.type) {
                case STRING: {
                    StringFieldDecoder.decode(reader, text, propertyKey.key, count, propertySetMapping, psets, refs);
                    break;
                }
                default:
                {
                    IntListIterator noderange = psets.iterator();
                    while (noderange.hasNext()) {
                        int psetid = noderange.nextInt();
                        final int start = propertySetMapping[psetid];
                        final int end = propertySetMapping[psetid + 1];

                        for (int i = start; i < end; i++) {
                            refs.get(i).get().putProperty(propertyKey.key, propreader.read(propertyKey.type));
                        }
                    }
                    break;
                }
            }
        }

        private void readNodes(String text, MemoryNodeCollection collection, Int2ReferenceOpenHashMap<NodeRef> nodeRefs)
        {
            Int2ObjectOpenHashMap<IntArrayList> pkey2pset = new Int2ObjectOpenHashMap<>();
            PropertyKey[] propertyKeys = readNodePropertyKeys();
            PropertySet[] propertySets = readPropertySets(propertyKeys, pkey2pset);
            int[] propertySetNodeMapping = new int[propertySets.length+1];

            //Read ranges
            for (int k = 0; k < propertySets.length; k++) {
                propertySetNodeMapping[k] = nodeRefs.size();
                int numNodes = reader.readVarInt(true);

                if(propertySets[k].keys.length > 0 && propertySets[k].keys[0].type == CoreRefType.NULL) {
                    for(int i = 0; i < numNodes; i++) {
                        MemoryNode node = collection.create();
                        nodeRefs.put(nodeRefs.size(), node);
                    }
                } else {
                    int last = 0;

                    for(int i = 0; i < numNodes; i++) {
                        int start = reader.readVarInt(true)+last;
                        int end = reader.readVarInt(true)+start;
                        last = end;

                        MemoryNode node = collection.create(start, end);
                        nodeRefs.put(nodeRefs.size(), node);
                    }
                }
            }

            propertySetNodeMapping[propertySets.length] = nodeRefs.size();

            //Read properties
            for (int i = 1; i < propertyKeys.length; i++) {
                IntArrayList psets = pkey2pset.get(i);
                if(psets != null) {
                    decodePropertyField(text, propertyKeys[i], propertySetNodeMapping, psets, nodeRefs);
                }
            }
        }

        private void readNodeLayer(int id, String text, MemoryDocumentStore store, Int2ReferenceOpenHashMap<NodeRef> nodeRefs) {
            String layer = MemoryCoreNodeLayer.fromId(id).layer;
            if(layer == null) {
                layer = reader.readString();
            }

            int numVariants = reader.readVarInt(true);

            for(int i = 0; i < numVariants; i++) {
                String variant = reader.readString();
                if(variant.equals(""))
                    variant = null;

                readNodes(text, store.getNodeCollection(layer, variant), nodeRefs);
            }
        }

        private void readEdges(Input reader, MemoryEdgeCollection collection, Int2ReferenceOpenHashMap<NodeRef> nodeRefs) {
            Int2ObjectOpenHashMap<IntArrayList> pkey2pset = new Int2ObjectOpenHashMap<>();
            PropertyKey[] propertyKeys = readEdgePropertyKeys();
            PropertySet[] propertySets = readPropertySets(propertyKeys, pkey2pset);
            int[] propertySetNodeMapping = new int[propertySets.length+1];

            Int2ReferenceOpenHashMap<EdgeRef> edgeRefs = new Int2ReferenceOpenHashMap<>();

            for(int i = 0; i < propertySets.length; i++) {
                propertySetNodeMapping[i] = edgeRefs.size();
                int edgeSize = reader.readVarInt(true);
                for(int k = 0; k < edgeSize; k++) {
                    int head = reader.readVarInt(true);
                    int tail = reader.readVarInt(true);

                    MemoryEdge edge = collection.create();
                    edge.connect(nodeRefs.get(tail), nodeRefs.get(head));

                    edgeRefs.put(edgeRefs.size(), edge);
                }
            }

            propertySetNodeMapping[propertyKeys.length] = edgeRefs.size();

            //Read properties
            for (int i = 0; i < propertyKeys.length; i++) {
                IntArrayList psets = pkey2pset.get(i);
                if(psets != null) {
                    decodePropertyField("", propertyKeys[i], propertySetNodeMapping, psets, edgeRefs);
                }
            }
        }

        private void readEdgeLayer(int id, MemoryDocumentStore store, Int2ReferenceOpenHashMap<NodeRef> nodeRefs) {
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
    }

    private static class PropertySet {
        public PropertyKey[] keys;
        private int cachedHashCode;

        public PropertySet(PropertyStore store) {
            int i = 0;
            if(store instanceof NodeStore && !((NodeStore) store).isAnnotation()) {
                keys = new PropertyKey[store.numProperties()+1];
                keys[0] = new PropertyKey("", CoreRefType.NULL);
                i++;
            } else {
                keys = new PropertyKey[store.numProperties()];
            }

            for (Map.Entry<String, DataRef> entry : store.properties()) {
                String key = entry.getKey();
                CoreRefType type;
                if(entry.getValue() instanceof CoreRef) {
                    type = ((CoreRef) entry.getValue()).id();
                } else {
                    throw new UnsupportedOperationException("Only CoreRefs are supported for encoding.");
                }

                keys[i++] = new PropertyKey(key, type);
            }

            cachedHashCode = Arrays.hashCode(keys);
        }

        public PropertySet(PropertyKey[] keys) {
            this.keys = keys;
            cachedHashCode = Arrays.hashCode(keys);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PropertySet that = (PropertySet) o;

            return Arrays.equals(keys, that.keys);

        }

        @Override
        public int hashCode() {
            return cachedHashCode;
        }
    }

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

    private static class PropertyLayerData<S extends StoreRef> {
        Object2IntLinkedOpenHashMap<PropertySet> propertySets = new Object2IntLinkedOpenHashMap<>();
        ArrayList<PropertySet> propertySetsIdx = new ArrayList<>();

        Object2IntLinkedOpenHashMap<PropertyKey> propertyKeyId = new Object2IntLinkedOpenHashMap<>();
        Int2ObjectLinkedOpenHashMap<ArrayList<S>> propertyNodes = new Int2ObjectLinkedOpenHashMap<>();
        IntArrayList[] propertyKey2setIds;

        public PropertyLayerData(Iterable<S> collection, boolean addNull) {
            if(addNull)
                propertyKeyId.put(new PropertyKey("", CoreRefType.NULL), 0);

            //Find all property sets
            for (S storeRef : collection) {
                PropertyStore store = storeRef.get();

                //Create property set for nodeRef.
                PropertySet propertySet = new PropertySet(store);

                if(!propertySets.containsKey(propertySet)) {
                    ArrayList<S> refs = new ArrayList<>();
                    refs.add(storeRef);
                    propertyNodes.put(propertySets.size(), refs);
                    propertySets.put(propertySet, propertySets.size());
                    propertySetsIdx.add(propertySet);

                    for (PropertyKey key : propertySet.keys) {
                        if (!propertyKeyId.containsKey(key)) {
                            propertyKeyId.put(key, propertyKeyId.size());
                        }
                    }
                } else {
                    int propsetId = propertySets.getInt(propertySet);
                    propertyNodes.get(propsetId).add(storeRef);
                }
            }

            propertyKey2setIds = new IntArrayList[propertyKeyId.size()];
            for (int i = 0; i < propertyKey2setIds.length; i++) {
                propertyKey2setIds[i] = new IntArrayList();
            }

            for (Object2IntMap.Entry<PropertySet> entry : propertySets.object2IntEntrySet()) {
                for (PropertyKey key : entry.getKey().keys) {
                    propertyKey2setIds[propertyKeyId.getInt(key)].add(entry.getIntValue());
                }
            }
        }

        public Iterable<S> getPropertyKeyEntries(int propertyKey) {
            IntArrayList list = propertyKey2setIds[propertyKey];
            Iterable<S>[] storeRefs = new Iterable[list.size()];
            for (int i = 0; i < list.size(); i++) {
                storeRefs[i] = propertyNodes.get(list.get(i));
            }

            return Iterables.concat(storeRefs);
        }
    }

    @Override
    public void encode(MemoryDocument doc, Output output, MemoryBinary.DocumentIndex index) {
        Writer writer = new Writer(output, doc);
        writer.writeMagic();
        writer.writeProperties(doc.store.properties);

        output.writeString(doc.store.text != null ? doc.store.text : "");

        Reference2IntOpenHashMap<NodeRef> refs = new Reference2IntOpenHashMap<>();

        int layerStart = output.reserve(4);
        writer.writeNodeLayers(refs);
        writer.writeEdgeLayers(refs);
        int currentPos = output.position();

        output.setPosition(layerStart);
        output.writeInt(currentPos-layerStart-4);
        output.setPosition(currentPos);
        writer.done();
    }

    @Override
    public MemoryDocument decode(Input input) {
        Reader reader = new Reader(input);
        MemoryDocumentStore store = new MemoryDocumentStore();
        store.properties = reader.readProperties();
        store.text = input.readString();

        Int2ReferenceOpenHashMap<NodeRef> nodeRefs = new Int2ReferenceOpenHashMap<>();

        int layerEnd = input.readInt();
        layerEnd += input.position();

        while(input.position() < layerEnd) {
            int id = Byte.toUnsignedInt(input.readByte());
            if((id & 0x80) == 0) {
                reader.readNodeLayer(id, store.text, store, nodeRefs);
            } else {
                reader.readEdgeLayer(id & ~0x80, store, nodeRefs);
            }
        }

        return new MemoryDocument(store);
    }
}

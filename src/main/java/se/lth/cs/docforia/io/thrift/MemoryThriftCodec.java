package se.lth.cs.docforia.io.thrift;
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
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.AutoExpandingBufferWriteTransport;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TMemoryInputTransport;
import se.lth.cs.docforia.*;
import se.lth.cs.docforia.data.*;
import se.lth.cs.docforia.io.mem.ByteBufferOutputStream;
import se.lth.cs.docforia.memstore.MemoryCoreEdgeLayer;
import se.lth.cs.docforia.memstore.MemoryCoreNodeLayer;
import se.lth.cs.docforia.memstore.MemoryDocument;

import java.io.IOError;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class MemoryThriftCodec {
    public static MemoryDocument decode(byte[] data) {
        try {
            TCompactProtocol protocol = new TCompactProtocol(new TMemoryInputTransport(data));
            TDocument doc = new TDocument();
            doc.read(protocol);

            return new Decoder(doc).decode();
        } catch (TException e) {
            throw new IOError(e);
        }
    }

    public static byte[] encode(MemoryDocument document) {
        TDocument doc = new Encoder(document).encode();
        ByteBufferOutputStream bbos = new ByteBufferOutputStream();
        AutoExpandingBufferWriteTransport output = new AutoExpandingBufferWriteTransport(2048, 2.0);
        TCompactProtocol protocol = new TCompactProtocol(output);
        try {
            doc.write(protocol);
        } catch (TException e) {
            throw new IOError(e);
        }

        return Arrays.copyOfRange(output.getBuf().array(), 0, output.getPos());
    }

    public static ByteBuffer encodeByteBuffer(MemoryDocument document) {
        TDocument doc = new Encoder(document).encode();
        ByteBufferOutputStream bbos = new ByteBufferOutputStream();
        TCompactProtocol protocol = new TCompactProtocol(new TIOStreamTransport(bbos));
        try {
            doc.write(protocol);
        } catch (TException e) {
            throw new IOError(e);
        }

        ByteBuffer flipped = bbos.buffer();
        flipped.flip();

        return flipped;
    }

    private static class Encoder {
        public TDocument docBuilder;
        public MemoryDocument doc;

        private Object2IntLinkedOpenHashMap<LayerRef> nodeLayer2id = new Object2IntLinkedOpenHashMap<>();
        private Object2IntLinkedOpenHashMap<LayerRef> edgeLayer2id = new Object2IntLinkedOpenHashMap<>();
        private Int2IntOpenHashMap coordinateMapping = new Int2IntOpenHashMap();

        public Encoder(MemoryDocument doc) {
            this.docBuilder = new TDocument();
            this.doc = doc;
        }

        public Int2IntOpenHashMap generateTextFragments(ArrayList<String> outTextFragments) {
            Int2IntOpenHashMap mapping = new Int2IntOpenHashMap();
            IntRBTreeSet coordinates = new IntRBTreeSet();

            coordinates.add(0);

            for (NodeRef nodeRef : doc.engine().nodes()) {
                NodeStore nodeStore = nodeRef.get();
                if(nodeStore.isAnnotation()) {
                    coordinates.add(nodeStore.getStart());
                    coordinates.add(nodeStore.getEnd());
                }
            }

            coordinates.add(doc.length());

            IntBidirectionalIterator iter = coordinates.iterator();
            int i = 0;
            int last = 0;

            if(!coordinates.isEmpty()) {
                int pos = iter.nextInt();
                if(pos != 0) {
                    outTextFragments.add(doc.text(last,pos));
                    last = pos;
                }

                mapping.put(pos, i++);

                while(iter.hasNext()) {
                    pos = iter.nextInt();
                    outTextFragments.add(doc.text(last,pos));
                    last = pos;
                    mapping.put(pos, i++);
                }
            }

            return mapping;
        }

        public void encodeDocumentProperties() {
            ArrayList<TPropertyKey> docpropkeys = new ArrayList<>();
            ArrayList<TPropertyValue> docpropvalues = new ArrayList<>();
            PropertyMapEncoder pmap = new PropertyMapEncoder(docpropkeys, docpropvalues, typefinder);

            for (Map.Entry<String, DataRef> prop : doc.store().properties()) {
                if(se.lth.cs.docforia.Document.PROP_ALL.contains(prop.getKey())) {
                    //Special!
                    switch (prop.getKey()) {
                        case se.lth.cs.docforia.Document.PROP_ID:
                            docpropkeys.add(new TPropertyKey().setSpecial(TSpecialKey.DOC_ID).setType(TDataType.D_STRING));

                            docpropvalues.add(new TPropertyValue().setStringValues(Collections.singletonList(prop.getValue().stringValue())));
                            break;
                        case se.lth.cs.docforia.Document.PROP_LANG:
                            docpropkeys.add(new TPropertyKey().setSpecial(TSpecialKey.DOC_LANG)
                                                                  .setType(TDataType.D_STRING));

                            docpropvalues.add(new TPropertyValue().setStringValues(Collections.singletonList(prop.getValue().stringValue())));
                            break;
                        case se.lth.cs.docforia.Document.PROP_TITLE:
                            docpropkeys.add(new TPropertyKey().setSpecial(TSpecialKey.DOC_TITLE)
                                                                  .setType(prop.getValue() instanceof DocRef ? TDataType.D_DOCUMENT : TDataType.D_STRING));

                            if(prop.getValue() instanceof DocRef) {
                                docpropvalues.add(new TPropertyValue().setDocValues(
                                        Collections.singletonList(new Encoder((MemoryDocument)((DocRef)prop.getValue()).documentValue()).encode())));
                            } else {
                                docpropvalues.add(new TPropertyValue().setStringValues(Collections.singletonList(prop.getValue().stringValue())));
                            }
                            break;
                        case se.lth.cs.docforia.Document.PROP_TYPE:
                            docpropkeys.add(new TPropertyKey().setSpecial(TSpecialKey.DOC_TYPE)
                                                                  .setType(TDataType.D_STRING));

                            docpropvalues.add(new TPropertyValue().setStringValues(Collections.singletonList(prop.getValue().stringValue())));
                            break;
                        case se.lth.cs.docforia.Document.PROP_URI:
                            docpropkeys.add(new TPropertyKey().setSpecial(TSpecialKey.DOC_URI)
                                                                  .setType(TDataType.D_STRING_ARRAY));

                            docpropvalues.add(new TPropertyValue().setStringValues(Arrays.asList(((StringArrayRef)prop.getValue()).arrayValue())));
                            break;
                    }
                } else {
                    if(!(prop.getValue() instanceof CoreRef))
                        throw new UnsupportedOperationException("Only CoreRef types are supported!");

                    CoreRef coreValue = (CoreRef)prop.getValue();
                    coreValue.write(typefinder);
                    pmap.pkeys.add(new TPropertyKey().setName(prop.getKey()).setType(typefinder.type));
                    pmap.pvalues = docpropvalues;
                    coreValue.write(pmap);
                }
            }

            //Add text property, find the minimum coordinate spaces.
            ArrayList<String> textFragments = new ArrayList<>();
            this.coordinateMapping = generateTextFragments(textFragments);
            pmap.pkeys.add(new TPropertyKey().setSpecial(TSpecialKey.DOC_TEXT).setType(TDataType.D_STRING_ARRAY));
            pmap.pvalues.add(new TPropertyValue().setStringValues(textFragments));

            docBuilder.setPropmap(new TPropertyMap().setPropkeys(docpropkeys).setPropvalues(docpropvalues));
        }

        private void produceNodeEdgeIds() {
            //Produce ids for nodes and edge layers
            for (String nodeLayer : doc.engine().nodeLayers()) {
                for (Optional<String> variant : doc.engine().nodeLayerAllVariants(nodeLayer)) {
                    nodeLayer2id.put(doc.store().nodeLayer(nodeLayer, variant.orElse(null)), nodeLayer2id.size());

                }
            }

            for (String edgeLayer : doc.engine().edgeLayers()) {
                for (Optional<String> variant : doc.engine().edgeLayerAllVariants(edgeLayer)) {
                    edgeLayer2id.put(doc.store().edgeLayer(edgeLayer, variant.orElse(null)), edgeLayer2id.size());
                }
            }
        }

        /** Internal object to handle property keys */
        private static class PropertyKey {
            private TSpecialKey specialKey;
            private String key;

            private TDataType type;
            private int layerid;

            public PropertyKey() {
                this.specialKey = null;
                this.key = null;
                this.type = null;
                this.layerid = -1;
            }

            public PropertyKey(TSpecialKey specialKey, TDataType type) {
                this.specialKey = specialKey;
                this.type = type;
                this.layerid = -1;
            }

            public PropertyKey(TSpecialKey specialKey, int layerid) {
                this.specialKey = specialKey;
                this.layerid = layerid;
            }

            public PropertyKey(String key, TDataType type) {
                this.key = key;
                this.type = type;
                this.layerid = -1;
            }

            private PropertyKey(TSpecialKey specialKey, String key, TDataType type, int layerid) {
                this.specialKey = specialKey;
                this.key = key;
                this.type = type;
                this.layerid = layerid;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                PropertyKey that = (PropertyKey) o;

                if (layerid != that.layerid) return false;
                if (specialKey != that.specialKey) return false;
                if (key != null ? !key.equals(that.key) : that.key != null) return false;
                return type == that.type;

            }

            @Override
            public int hashCode() {
                int result = specialKey != null ? specialKey.hashCode() : 0;
                result = 31 * result + (key != null ? key.hashCode() : 0);
                result = 31 * result + (type != null ? type.hashCode() : 0);
                result = 31 * result + layerid;
                return result;
            }

            public TSpecialKey getSpecialKey() {
                return specialKey;
            }

            public String getKey() {
                return key;
            }

            public TDataType getType() {
                return type;
            }

            public int getLayerid() {
                return layerid;
            }

            public void set(TSpecialKey specialKey,TDataType type) {
                this.specialKey = specialKey;
                this.type = type;
                this.layerid = -1;
            }

            public void set(TSpecialKey specialKey, int layerid) {
                this.specialKey = specialKey;
                this.layerid = layerid;
            }

            public void set(String key, TDataType type) {
                this.key = key;
                this.type = type;
                this.layerid = -1;
            }

            public PropertyKey copy() {
                return new PropertyKey(specialKey, key, type, layerid);
            }
        }

        private static class TypeFinder implements CoreRefWriter {
            public TDataType type;

            @Override
            public void write(byte[] binary) {
                type = TDataType.D_BINARY;
            }

            @Override
            public void write(String string) {
                type = TDataType.D_STRING;
            }

            @Override
            public void write(boolean boolValue) {
                type = TDataType.D_BOOLEAN;
            }

            @Override
            public void write(int intValue) {
                type = TDataType.D_INT;
            }

            @Override
            public void write(long longValue) {
                type = TDataType.D_LONG;
            }

            @Override
            public void write(float floatValue) {
                type = TDataType.D_FLOAT;
            }

            @Override
            public void write(double doubleValue) {
                type = TDataType.D_DOUBLE;
            }

            @Override
            public void write(MemoryDocument doc) {
                type = TDataType.D_DOCUMENT;
            }

            @Override
            public void write(PropertyMap propertyMap) {
                type = TDataType.D_PROPERTY_MAP;
            }

            @Override
            public void writeBooleanArray(boolean[] boolValues) {
                type = TDataType.D_BOOLEAN_ARARY;
            }

            @Override
            public void writeIntArray(int[] intValues) {
                type = TDataType.D_INT_ARRAY;
            }

            @Override
            public void writeLongArray(long[] longValues) {
                type = TDataType.D_LONG_ARRAY;
            }

            @Override
            public void writeFloatArray(float[] floatValues) {
                type = TDataType.D_FLOAT_ARRAY;
            }

            @Override
            public void writeDoubleArray(double[] doubleValues) {
                type = TDataType.D_DOUBLE_ARRAY;
            }

            @Override
            public void writeDocumentArray(MemoryDocument[] docValues) {
                type = TDataType.D_DOCUMENT_ARRAY;
            }

            @Override
            public void writeStringArray(String[] stringValues) {
                type = TDataType.D_STRING_ARRAY;
            }
        }

        private static class PropertyMapEncoder implements CoreRefWriter {
            private ArrayList<TPropertyKey> pkeys;
            private ArrayList<TPropertyValue> pvalues;
            private TypeFinder finder;

            public PropertyMapEncoder(ArrayList<TPropertyKey> pkeys, ArrayList<TPropertyValue> pvalues, TypeFinder finder) {
                this.pkeys = pkeys;
                this.pvalues = pvalues;
                this.finder = finder;
            }

            public PropertyMapEncoder() {
                this.pkeys = new ArrayList<>();
                this.pvalues = new ArrayList<>();
                this.finder = new TypeFinder();
            }

            public static TPropertyMap encode(PropertyMap pmap) {
                PropertyMapEncoder encoder = new PropertyMapEncoder();
                for (Map.Entry<String, DataRef> entry : pmap.properties()) {
                    if(!(entry.getValue() instanceof CoreRef)) {
                        throw new UnsupportedOperationException("Only CoreRefs are supported!");
                    }

                    CoreRef value = ((CoreRef)entry.getValue());
                    value.write(encoder.finder);

                    encoder.pkeys.add(new TPropertyKey().setName(entry.getKey()).setType(encoder.finder.type));
                    value.write(encoder);
                }

                return new TPropertyMap().setPropkeys(encoder.pkeys).setPropvalues(encoder.pvalues);
            }

            @Override
            public void write(byte[] binary) {
                pvalues.add(new TPropertyValue().setBinaryValue(Collections.singletonList(ByteBuffer.wrap(binary))));
            }

            @Override
            public void write(String string) {
                pvalues.add(new TPropertyValue().setStringValues(Collections.singletonList(string)));
            }

            @Override
            public void write(boolean boolValue) {
                pvalues.add(new TPropertyValue().setBoolValues(Collections.singletonList(boolValue)));
            }

            @Override
            public void write(int intValue) {
                pvalues.add(new TPropertyValue().setIntValues(Collections.singletonList(intValue)));
            }

            @Override
            public void write(long longValue) {
                pvalues.add(new TPropertyValue().setLongValues(Collections.singletonList(longValue)));
            }

            @Override
            public void write(float floatValue) {
                pvalues.add(new TPropertyValue().setDoubleValues(Collections.singletonList(Double.valueOf(floatValue))));
            }

            @Override
            public void write(double doubleValue) {
                pvalues.add(new TPropertyValue().setDoubleValues(Collections.singletonList(doubleValue)));
            }

            @Override
            public void write(MemoryDocument doc) {
                pvalues.add(new TPropertyValue().setDocValues(Collections.singletonList(new Encoder(doc).encode())));
            }

            @Override
            public void write(PropertyMap propertyMap) {
                pvalues.add(new TPropertyValue().setPropValues(Collections.singletonList(PropertyMapEncoder.encode(propertyMap))));
            }

            @Override
            public void writeBooleanArray(boolean[] boolValues) {
                TPropertyValue builder = new TPropertyValue();
                for (boolean boolValue : boolValues) {
                    builder.addToBoolValues(boolValue);
                }
                pvalues.add(builder);
            }

            @Override
            public void writeIntArray(int[] intValues) {
                TPropertyValue builder = new TPropertyValue();
                for (int intValue : intValues) {
                    builder.addToIntValues(intValue);
                }
                builder.addToLengthInfo(intValues.length);
                pvalues.add(builder);
            }

            @Override
            public void writeLongArray(long[] longValues) {
                TPropertyValue builder = new TPropertyValue();
                for (long longValue : longValues) {
                    builder.addToLongValues(longValue);
                }
                builder.addToLengthInfo(longValues.length);
                pvalues.add(builder);
            }

            @Override
            public void writeFloatArray(float[] floatValues) {
                TPropertyValue builder = new TPropertyValue();
                for (float floatValue : floatValues) {
                    builder.addToDoubleValues(floatValue);
                }
                builder.addToLengthInfo(floatValues.length);
                pvalues.add(builder);
            }

            @Override
            public void writeDoubleArray(double[] doubleValues) {
                TPropertyValue builder = new TPropertyValue();
                for (double doubleValue : doubleValues) {
                    builder.addToDoubleValues(doubleValue);
                }
                builder.addToLengthInfo(doubleValues.length);
                pvalues.add(builder);
            }

            @Override
            public void writeDocumentArray(MemoryDocument[] docValues) {
                TPropertyValue builder = new TPropertyValue();
                for (MemoryDocument docValue : docValues) {
                    builder.addToDocValues(new Encoder(docValue).encode());
                }
                builder.addToLengthInfo(docValues.length);
                pvalues.add(builder);
            }

            @Override
            public void writeStringArray(String[] stringValues) {
                TPropertyValue builder = new TPropertyValue();
                for (String stringValue : stringValues) {
                    builder.addToStringValues(stringValue);
                }
                builder.addToLengthInfo(stringValues.length);
                pvalues.add(builder);
            }
        }

        private static class PropertyStreamWriter implements CoreRefWriter {
            private ArrayList<TColumn> nodeBuilders;
            private int pkeyid;

            @Override
            public void write(byte[] binary) {
                nodeBuilders.get(pkeyid).addToBinaryValues(ByteBuffer.wrap(binary));
            }

            @Override
            public void write(String string) {
                nodeBuilders.get(pkeyid).addToStringValues(string);
            }

            @Override
            public void write(boolean boolValue) {
                nodeBuilders.get(pkeyid).addToBoolValues(boolValue);
            }

            @Override
            public void write(int intValue) {
                nodeBuilders.get(pkeyid).addToIntValues(intValue);
            }

            @Override
            public void write(long longValue) {
                nodeBuilders.get(pkeyid).addToLongValues(longValue);
            }

            @Override
            public void write(float floatValue) {
                nodeBuilders.get(pkeyid).addToDoubleValues(floatValue);
            }

            @Override
            public void write(double doubleValue) {
                nodeBuilders.get(pkeyid).addToDoubleValues(doubleValue);
            }

            @Override
            public void write(MemoryDocument doc) {
                nodeBuilders.get(pkeyid).addToDocValues(new Encoder(doc).encode());
            }

            @Override
            public void write(PropertyMap propertyMap) {
                nodeBuilders.get(pkeyid).addToPropmapValues(PropertyMapEncoder.encode(propertyMap));
            }

            @Override
            public void writeBooleanArray(boolean[] boolValues) {
                TColumn builder = nodeBuilders.get(pkeyid);
                for (boolean boolValue : boolValues) {
                    builder.addToBoolValues(boolValue);
                }
                builder.addToLengthInfo(boolValues.length);
            }

            @Override
            public void writeIntArray(int[] intValues) {
                TColumn builder = nodeBuilders.get(pkeyid);
                for (int intValue : intValues) {
                    builder.addToIntValues(intValue);
                }
                builder.addToLengthInfo(intValues.length);
            }

            @Override
            public void writeLongArray(long[] longValues) {
                TColumn builder = nodeBuilders.get(pkeyid);
                for (long longValue : longValues) {
                    builder.addToLongValues(longValue);
                }
                builder.addToLengthInfo(longValues.length);
            }

            @Override
            public void writeFloatArray(float[] floatValues) {
                TColumn builder = nodeBuilders.get(pkeyid);
                for (float floatValue : floatValues) {
                    builder.addToDoubleValues(floatValue);
                }
                builder.addToLengthInfo(floatValues.length);
            }

            @Override
            public void writeDoubleArray(double[] doubleValues) {
                TColumn builder = nodeBuilders.get(pkeyid);
                for (double doubleValue : doubleValues) {
                    builder.addToDoubleValues(doubleValue);
                }
                builder.addToLengthInfo(doubleValues.length);
            }

            @Override
            public void writeDocumentArray(MemoryDocument[] docValues) {
                TColumn builder = nodeBuilders.get(pkeyid);
                for (MemoryDocument docValue : docValues) {
                    builder.addToDocValues(new Encoder(docValue).encode());
                }
                builder.addToLengthInfo(docValues.length);
            }

            @Override
            public void writeStringArray(String[] stringValues) {
                TColumn builder = nodeBuilders.get(pkeyid);
                for (String stringValue : stringValues) {
                    builder.addToStringValues(stringValue);
                }
                builder.addToLengthInfo(stringValues.length);
            }
        }

        private TypeFinder typefinder = new TypeFinder();

        private HashSet<PropertyKey> getPropertySetsFromNodeEdge(StoreRef ref) {
            HashSet<PropertyKey> pset = new HashSet<>();
            for (Map.Entry<String, DataRef> prop : ref.get().properties()) {
                if(!(prop.getValue() instanceof CoreRef)) {
                    throw new UnsupportedOperationException("Only CoreRef properties are supported by this encoder!");
                }

                CoreRef value = (CoreRef)prop.getValue();
                value.write(typefinder);
                PropertyKey pkey = new PropertyKey(prop.getKey(), typefinder.type);
                pset.add(pkey);
            }
            return pset;
        }

        private HashSet<PropertyKey> getPropertySet(NodeRef ref) {
            HashSet<PropertyKey> pset = getPropertySetsFromNodeEdge(ref);

            IntRBTreeSet edgeLayersOut = new IntRBTreeSet();

            doc.engine().edges(ref, Direction.OUT).stream().forEach(e -> {
                edgeLayersOut.add(edgeLayer2id.get(e.layer()));
            });

            IntIterator iter = edgeLayersOut.iterator();
            while(iter.hasNext()) {
                pset.add(new PropertyKey(TSpecialKey.NODE_NUM_EDGE_OUT, iter.nextInt()));
            }

            if(ref.get().isAnnotation()) {
                pset.add(new PropertyKey(TSpecialKey.NODE_RANGE_START_END, TDataType.D_INT));
            }

            return pset;
        }

        private HashSet<PropertyKey> getPropertySet(EdgeRef ref) {
            HashSet<PropertyKey> pset = getPropertySetsFromNodeEdge(ref);
            pset.add(new PropertyKey(TSpecialKey.EDGE_HEAD, nodeLayer2id.getInt(ref.get().getHead().layer())));

            return pset;
        }

        private void findNodePropertySet(String nodeLayer,
                                         Object2IntLinkedOpenHashMap<HashSet<PropertyKey>> pset2id,
                                         Object2IntLinkedOpenHashMap<PropertyKey> pkey2id) {
            Object2IntOpenHashMap<HashSet<PropertyKey>> pset2count = new Object2IntOpenHashMap<>();
            Object2IntOpenHashMap<PropertyKey> pkeys2count = new Object2IntOpenHashMap<>();

            for (Optional<String> variant : doc.engine().nodeLayerAllVariants(nodeLayer)) {
                for (NodeRef nodeRef : doc.store().nodeLayer(nodeLayer, variant.orElse(null))) {
                    HashSet<PropertyKey> pset = getPropertySet(nodeRef);
                    pset2count.put(pset, pset2count.getInt(pset)+1);
                    for (PropertyKey propertyKey : pset) {
                        pkeys2count.put(propertyKey, pkeys2count.getInt(propertyKey)+1);
                    }
                }
            }

            Iterator<HashSet<PropertyKey>> iter = pset2count.object2IntEntrySet()
                                                            .stream()
                                                            .sorted((x, y) -> -Integer.compare(x.getIntValue(), y.getIntValue()))
                                                            .map(Map.Entry::getKey)
                                                            .iterator();

            int i = 0;

            while(iter.hasNext()) {
                pset2id.put(iter.next(), i++);
            }

            Iterator<PropertyKey> iter2 = pkeys2count.object2IntEntrySet()
                                                     .stream()
                                                     .sorted((x, y) -> -Integer.compare(x.getIntValue(), y.getIntValue()))
                                                     .map(Map.Entry::getKey)
                                                     .iterator();

            i = 0;
            while(iter2.hasNext()) {
                pkey2id.put(iter2.next(), i++);
            }
        }

        private void findEdgePropertySet(String edgeLayer,
                                         Object2IntLinkedOpenHashMap<HashSet<PropertyKey>> pset2id,
                                         Object2IntLinkedOpenHashMap<PropertyKey> pkey2id) {
            Object2IntOpenHashMap<HashSet<PropertyKey>> pset2count = new Object2IntOpenHashMap<>();
            Object2IntOpenHashMap<PropertyKey> pkeys2count = new Object2IntOpenHashMap<>();

            for (Optional<String> variant : doc.engine().edgeLayerAllVariants(edgeLayer)) {
                for (EdgeRef edgeRef : doc.store().edgeLayer(edgeLayer, variant.orElse(null))) {
                    HashSet<PropertyKey> pset = getPropertySet(edgeRef);
                    pset2count.put(pset, pset2count.getInt(pset)+1);
                    for (PropertyKey propertyKey : pset) {
                        pkeys2count.put(propertyKey, pkeys2count.getInt(propertyKey)+1);
                    }
                }
            }

            Iterator<HashSet<PropertyKey>> iter = pset2count.object2IntEntrySet()
                                                            .stream()
                                                            .sorted((x, y) -> -Integer.compare(x.getIntValue(), y.getIntValue()))
                                                            .map(Map.Entry::getKey)
                                                            .iterator();

            int i = 0;

            while(iter.hasNext()) {
                pset2id.put(iter.next(), i++);
            }

            Iterator<PropertyKey> iter2 = pkeys2count.object2IntEntrySet()
                                                     .stream()
                                                     .sorted((x, y) -> -Integer.compare(x.getIntValue(), y.getIntValue()))
                                                     .map(Map.Entry::getKey)
                                                     .iterator();

            i = 0;
            while(iter2.hasNext()) {
                pkey2id.put(iter2.next(), i++);
            }
        }

        private static final PropertyKey NODE_RANGE_PKEY = new PropertyKey(TSpecialKey.NODE_RANGE_START_END, TDataType.D_INT);

        private Reference2IntOpenHashMap<NodeRef> node2id = new Reference2IntOpenHashMap<>();
        private Int2ObjectOpenHashMap<ArrayList<EdgeRef>> edgelayer2list = new Int2ObjectOpenHashMap<>();

        private ArrayList<EdgeRef> getEdgelayer(int layerid) {
            ArrayList<EdgeRef> edgeRefs = edgelayer2list.get(layerid);
            if(edgeRefs == null ){
                edgeRefs = new ArrayList<>();
                edgelayer2list.put(layerid, edgeRefs);
            }

            return edgeRefs;
        }

        private TNodes encodeNodeLayer(String nodeLayer) {
            TNodes nodeBuilder = new TNodes();
            ArrayList<TColumn> columnBuilders = new ArrayList<>();
            TTypeStream nodeStream = new TTypeStream(new ArrayList<>());

            //1. Find and save all property sets
            Object2IntLinkedOpenHashMap<PropertyKey> pkeys = new Object2IntLinkedOpenHashMap<>();
            Object2IntLinkedOpenHashMap<HashSet<PropertyKey>> psets = new Object2IntLinkedOpenHashMap<>();
            findNodePropertySet(nodeLayer, psets, pkeys);

            MemoryCoreNodeLayer possibleCoreLayer = MemoryCoreNodeLayer.fromLayerName(nodeLayer);
            if(possibleCoreLayer == MemoryCoreNodeLayer.UNKNOWN) {
                nodeBuilder.setName(nodeLayer);
            } else {
                nodeBuilder.setBuiltin(TNodeTypes.findByValue(possibleCoreLayer.id));
            }

            //2. Create all property streams
            pkeys.object2IntEntrySet().forEach(e -> {
                PropertyKey pkey = e.getKey();
                if(pkey.specialKey != null) {
                    switch(pkey.specialKey) {
                        case NODE_NUM_EDGE_OUT:
                            columnBuilders.add(new TColumn());
                            nodeBuilder.addToKeys(new TPropertyKey().setSpecial(pkey.specialKey)
                                                                      .setLayerid(pkey.layerid));
                            break;
                        default:
                            columnBuilders.add(new TColumn());
                            nodeBuilder.addToKeys(new TPropertyKey().setSpecial(pkey.specialKey)
                                                                      .setType(pkey.type));
                            break;
                    }
                }
                else {
                    columnBuilders.add(new TColumn());
                    nodeBuilder.addToKeys(new TPropertyKey().setName(pkey.key).setType(pkey.type));
                }
            });

            //3. Create all propertysets.
            psets.object2IntEntrySet().forEach(e -> {
                TPropertySet psetbuilder = new TPropertySet();
                for (PropertyKey propertyKey : e.getKey()) {
                    psetbuilder.addToKeys(pkeys.getInt(propertyKey));
                }
                nodeBuilder.addToSets(psetbuilder);
            });

            PropertyStreamWriter propwriter = new PropertyStreamWriter();
            propwriter.nodeBuilders = columnBuilders;

            int laststream = 0;

            //4. Encode a node at a time per variant, add propertyset used to stream (delta-code)
            for (Optional<String> variant : doc.engine().nodeLayerAllVariants(nodeLayer)) {
                int count = 0;
                int lastpos = 0;

                for (NodeRef nodeRef : doc.store().nodeLayer(nodeLayer, variant.orElse(null))) {
                    HashSet<PropertyKey> pset = new HashSet<>();
                    NodeStore store = nodeRef.get();
                    for (Map.Entry<String, DataRef> prop : store.properties()) {
                        if(!(prop.getValue() instanceof CoreRef)) {
                            throw new UnsupportedOperationException("Only CoreRef properties are supported by this encoder!");
                        }

                        CoreRef value = (CoreRef)prop.getValue();
                        value.write(typefinder);
                        PropertyKey pkey = new PropertyKey(prop.getKey(), typefinder.type);
                        pset.add(pkey);

                        propwriter.pkeyid = pkeys.getInt(pkey);
                        value.write(propwriter);
                    }

                    if(store.isAnnotation()) {
                        pset.add(NODE_RANGE_PKEY);
                        int idx = pkeys.getInt(NODE_RANGE_PKEY);
                        TColumn rangeCol = columnBuilders.get(idx);

                        int pos = coordinateMapping.get(store.getStart());
                        rangeCol.addToIntValues(pos-lastpos);
                        lastpos = pos;
                        pos = coordinateMapping.get(store.getEnd());
                        rangeCol.addToIntValues(pos-lastpos);
                        lastpos = pos;
                    }

                    Int2IntRBTreeMap edgeCounts = new Int2IntRBTreeMap();

                    doc.engine().edges(nodeRef, Direction.OUT).stream().forEach(e -> {
                        int edgeLayerId = edgeLayer2id.get(e.layer());
                        getEdgelayer(edgeLayerId).add(e);
                        edgeCounts.put(edgeLayerId, edgeCounts.get(edgeLayerId)+1);
                    });

                    for (Int2IntMap.Entry entry : edgeCounts.int2IntEntrySet()) {
                        PropertyKey pkey = new PropertyKey(TSpecialKey.NODE_NUM_EDGE_OUT, entry.getIntKey());
                        pset.add(pkey);
                        int idx = pkeys.getInt(pkey);
                        columnBuilders.get(idx).addToIntValues(entry.getIntValue());
                    }

                    if(psets.size() != 1) {
                        int psetid = psets.getInt(pset);
                        nodeStream.addToStream(psetid-laststream);
                        laststream = psetid;
                    }

                    node2id.put(nodeRef, count);
                    count++;
                }

                nodeBuilder.addToVariants(variant.orElse(""));
                nodeBuilder.addToNumentries(count);
            }

            //Save columns in order.
            columnBuilders.forEach(nodeBuilder::addToColumns);

            docBuilder.addToNodestreams(nodeStream);

            return nodeBuilder;
        }

        private void encodeNodes() {
            for (String nodeLayer : doc.engine().nodeLayers()) {
                TNodes nodes = encodeNodeLayer(nodeLayer);
                docBuilder.addToNodes(nodes);
            }
        }

        private TEdges encodeEdgeLayer(String edgeLayer) {
            TEdges edgeBuilder = new TEdges();
            ArrayList<TColumn> columnBuilders = new ArrayList<>();
            TTypeStream edgeStream = new TTypeStream();

            MemoryCoreEdgeLayer possibleCoreLayer = MemoryCoreEdgeLayer.fromLayerName(edgeLayer);
            if(possibleCoreLayer == MemoryCoreEdgeLayer.UNKNOWN) {
                edgeBuilder.setName(edgeLayer);
            } else {
                edgeBuilder.setBuiltin(TEdgeTypes.findByValue(possibleCoreLayer.id));
            }

            //1. Find and save all property sets
            Object2IntLinkedOpenHashMap<PropertyKey> pkeys = new Object2IntLinkedOpenHashMap<>();
            Object2IntLinkedOpenHashMap<HashSet<PropertyKey>> psets = new Object2IntLinkedOpenHashMap<>();
            findEdgePropertySet(edgeLayer, psets, pkeys);

            //2. Create all property streams
            pkeys.object2IntEntrySet().forEach(e -> {
                PropertyKey pkey = e.getKey();
                if(pkey.specialKey != null) {
                    switch(pkey.specialKey) {
                        case EDGE_HEAD:
                            columnBuilders.add(new TColumn());
                            edgeBuilder.addToKeys(new TPropertyKey().setSpecial(pkey.specialKey)
                                                                      .setLayerid(pkey.layerid));
                            break;
                        default:
                            columnBuilders.add(new TColumn());
                            edgeBuilder.addToKeys(new TPropertyKey().setSpecial(pkey.specialKey)
                                                                      .setType(pkey.type));
                            break;
                    }
                }
                else {
                    columnBuilders.add(new TColumn());
                    edgeBuilder.addToKeys(new TPropertyKey().setName(pkey.key).setType(pkey.type));
                }
            });

            //3. Create all propertysets.
            psets.object2IntEntrySet().forEach(e -> {
                TPropertySet psetbuilder = new TPropertySet();
                for (PropertyKey propertyKey : e.getKey()) {
                    psetbuilder.addToKeys(pkeys.getInt(propertyKey));
                }
                edgeBuilder.addToSets(psetbuilder);
            });

            PropertyStreamWriter propwriter = new PropertyStreamWriter();
            propwriter.nodeBuilders = columnBuilders;

            int laststream = 0;

            for (Optional<String> variant : doc.engine().edgeLayerAllVariants(edgeLayer)) {
                DocumentEdgeLayer edgeCollection = doc.store().edgeLayer(edgeLayer, variant.orElse(null));

                ArrayList<EdgeRef> edges = getEdgelayer(edgeLayer2id.get(edgeCollection));
                for (EdgeRef edgeRef : edges) {
                    HashSet<PropertyKey> pset = new HashSet<>();
                    EdgeStore store = edgeRef.get();
                    for (Map.Entry<String, DataRef> prop : store.properties()) {
                        if (!(prop.getValue() instanceof CoreRef)) {
                            throw new UnsupportedOperationException("Only CoreRef properties are supported by this encoder!");
                        }

                        CoreRef value = (CoreRef) prop.getValue();
                        value.write(typefinder);
                        PropertyKey pkey = new PropertyKey(prop.getKey(), typefinder.type);
                        pset.add(pkey);

                        propwriter.pkeyid = pkeys.getInt(pkey);
                        value.write(propwriter);
                    }

                    NodeRef head = edgeRef.get().getHead();

                    PropertyKey pkey = new PropertyKey(TSpecialKey.EDGE_HEAD, nodeLayer2id.getInt(head.layer()));

                    pset.add(pkey);
                    columnBuilders.get(pkeys.getInt(pkey)).addToIntValues(node2id.getInt(head));

                    if(psets.size() != 1) {
                        int psetid = psets.getInt(pset);
                        edgeStream.addToStream(psetid-laststream);
                        laststream = psetid;
                    }
                }

                edgeBuilder.addToVariants(variant.orElse(""));
                edgeBuilder.addToNumentries(edges.size());
            }

            //Save columns in order.
            columnBuilders.forEach(edgeBuilder::addToColumns);

            docBuilder.addToEdgestreams(edgeStream);

            return edgeBuilder;
        }

        private void encodeEdges() {
            for (String edgeLayer : doc.engine().edgeLayers()) {
                TEdges edges = encodeEdgeLayer(edgeLayer);
                docBuilder.addToEdges(edges);
            }
        }

        public TDocument encode() {
            encodeDocumentProperties();
            produceNodeEdgeIds();
            encodeNodes();
            encodeEdges();

            return docBuilder;
        }
    }

    private static class Decoder {
        private TDocument inputdoc;
        private IntArrayList offsetmap = new IntArrayList();
        private MemoryDocument doc = new MemoryDocument();

        private Int2ObjectOpenHashMap<DocumentNodeLayer> nodelayerid2layer = new Int2ObjectOpenHashMap<>();
        private Int2ObjectOpenHashMap<DocumentEdgeLayer> edgelayerid2layer = new Int2ObjectOpenHashMap<>();
        private Int2ObjectOpenHashMap<ArrayList<NodeRef>> edgelayerTails = new Int2ObjectOpenHashMap<>();
        private Int2ObjectOpenHashMap<ArrayList<NodeRef>> layer2id2nodes = new Int2ObjectOpenHashMap<>();

        private ArrayList<NodeRef> getTailArray(int edgelayer) {
            ArrayList<NodeRef> nodeRefs = edgelayerTails.get(edgelayer);
            if(nodeRefs == null) {
                nodeRefs = new ArrayList<>();
                edgelayerTails.put(edgelayer, nodeRefs);
            }

            return nodeRefs;
        }

        private void addNode(int layerid, NodeRef node) {
            ArrayList<NodeRef> nodeRefs = layer2id2nodes.get(layerid);
            if(nodeRefs == null) {
                nodeRefs = new ArrayList<>();
                layer2id2nodes.put(layerid, nodeRefs);
            }

            nodeRefs.add(node);
        }

        public Decoder(TDocument inputdoc) {
            this.inputdoc = inputdoc;
        }

        private static class PropertyReader implements CoreRefReader {
            protected TPropertyValue pval;

            public CoreRef read(TDataType type) {
                switch (type) {
                    case D_BINARY: return readBinary();
                    case D_BOOLEAN: return readBoolean();
                    case D_BOOLEAN_ARARY: return readBooleanArray();
                    case D_INT: return readInt();
                    case D_INT_ARRAY: return readIntArray();
                    case D_LONG: return readLong();
                    case D_LONG_ARRAY: return readLongArray();
                    case D_FLOAT: return readFloat();
                    case D_FLOAT_ARRAY: return readFloatArray();
                    case D_DOUBLE: return readDouble();
                    case D_DOUBLE_ARRAY: return readDoubleArray();
                    case D_STRING: return readString();
                    case D_STRING_ARRAY: return readStringArray();
                    case D_DOCUMENT: return readDocument();
                    case D_DOCUMENT_ARRAY: return readDocumentArray();
                    case D_PROPERTY_MAP: return readPropertyMap();
                    default:
                        throw new UnsupportedOperationException("Unsupported format found.");
                }
            }

            @Override
            public StringRef readString() {
                return new StringRef(pval.getStringValues().get(0));
            }

            @Override
            public StringArrayRef readStringArray() {
                return new StringArrayRef(pval.getStringValues().toArray(new String[pval.getStringValuesSize()]));
            }

            @Override
            public PropertyMapRef readPropertyMap() {
                return PropertyMapDecoder.decode(pval.getPropValues().get(0));
            }

            @Override
            public BooleanRef readBoolean() {
                return pval.getBoolValues().get(0) ? BooleanRef.TRUE : BooleanRef.FALSE;
            }

            @Override
            public IntRef readInt() {
                return new IntRef(pval.getIntValues().get(0));
            }

            @Override
            public LongRef readLong() {
                return new LongRef(pval.getLongValues().get(0));
            }

            @Override
            public FloatRef readFloat() {
                return new FloatRef(pval.getDoubleValues().get(0).floatValue());
            }

            @Override
            public DoubleRef readDouble() {
                return new DoubleRef(pval.getDoubleValues().get(0));
            }

            @Override
            public BinaryRef readBinary() {
                ByteBuffer byteBuffer = pval.getBinaryValue().get(0);
                byte[] buffer = new byte[byteBuffer.remaining()];
                byteBuffer.get(buffer);
                return new BinaryRef(buffer);
            }

            @Override
            public DocRef readDocument() {
                return new DocRef(new Decoder(pval.getDocValues().get(0)).decode());
            }

            @Override
            public BooleanArrayRef readBooleanArray() {
                boolean[] boolValues = new boolean[pval.getBoolValuesSize()];
                final List<Boolean> list = pval.getBoolValues();
                for (int i = 0; i < boolValues.length; i++) {
                    boolValues[i] = list.get(i);
                }
                return new BooleanArrayRef(boolValues);
            }

            @Override
            public IntArrayRef readIntArray() {
                int[] intValues = new int[pval.getIntValuesSize()];
                final List<Integer> list = pval.getIntValues();
                for (int i = 0; i < intValues.length; i++) {
                    intValues[i] = list.get(i);
                }
                return new IntArrayRef(intValues);
            }

            @Override
            public LongArrayRef readLongArray() {
                long[] longValues = new long[pval.getLongValuesSize()];
                final List<Long> list = pval.getLongValues();
                for (int i = 0; i < longValues.length; i++) {
                    longValues[i] = list.get(i);
                }
                return new LongArrayRef(longValues);
            }

            @Override
            public FloatArrayRef readFloatArray() {
                float[] floatValues = new float[pval.getDoubleValuesSize()];
                final List<Double> list = pval.getDoubleValues();
                for (int i = 0; i < floatValues.length; i++) {
                    floatValues[i] = list.get(i).floatValue();
                }
                return new FloatArrayRef(floatValues);
            }

            @Override
            public DoubleArrayRef readDoubleArray() {
                double[] doubleValues = new double[pval.getDoubleValuesSize()];
                final List<Double> list = pval.getDoubleValues();
                for (int i = 0; i < doubleValues.length; i++) {
                    doubleValues[i] = list.get(i);
                }
                return new DoubleArrayRef(doubleValues);
            }

            @Override
            public DocArrayRef readDocumentArray() {
                MemoryDocument[] docValues = new MemoryDocument[pval.getDocValuesSize()];
                final List<TDocument> list = pval.getDocValues();
                for(int i = 0; i < docValues.length; i++) {
                    docValues[i] = new Decoder(list.get(i)).decode();
                }
                return new DocArrayRef(docValues);
            }
        }

        private static class ColumnReader implements CoreRefReader {
            protected TColumn pval;
            protected int index = 0;
            protected int arrcountidx = 0;
            protected TDataType type;

            public ColumnReader(TColumn pval) {
                this.pval = pval;
            }

            public CoreRef read() {
                switch (type) {
                    case D_BINARY: return readBinary();
                    case D_BOOLEAN: return readBoolean();
                    case D_BOOLEAN_ARARY: return readBooleanArray();
                    case D_INT: return readInt();
                    case D_INT_ARRAY: return readIntArray();
                    case D_LONG: return readLong();
                    case D_LONG_ARRAY: return readLongArray();
                    case D_FLOAT: return readFloat();
                    case D_FLOAT_ARRAY: return readFloatArray();
                    case D_DOUBLE: return readDouble();
                    case D_DOUBLE_ARRAY: return readDoubleArray();
                    case D_STRING: return readString();
                    case D_STRING_ARRAY: return readStringArray();
                    case D_DOCUMENT: return readDocument();
                    case D_DOCUMENT_ARRAY: return readDocumentArray();
                    case D_PROPERTY_MAP: return readPropertyMap();
                    default:
                        throw new UnsupportedOperationException("Unsupported format found.");
                }
            }

            @Override
            public StringRef readString() {
                return new StringRef(pval.getStringValues().get(index++));
            }

            private int readArrayCount() {
                return pval.getLengthInfo().get(arrcountidx++);
            }

            @Override
            public StringArrayRef readStringArray() {
                int num = readArrayCount();
                int idx = index;
                index += num;

                return new StringArrayRef(pval.getStringValues().subList(idx,idx+num).toArray(new String[num]));
            }

            @Override
            public PropertyMapRef readPropertyMap() {
                return PropertyMapDecoder.decode(pval.getPropmapValues().get(index++));
            }

            @Override
            public BooleanRef readBoolean() {
                return pval.getBoolValues().get(index++) ? BooleanRef.TRUE : BooleanRef.FALSE;
            }

            @Override
            public IntRef readInt() {
                return new IntRef(pval.getIntValues().get(index++));
            }

            @Override
            public LongRef readLong() {
                return new LongRef(pval.getLongValues().get(index++));
            }

            @Override
            public FloatRef readFloat() {
                return new FloatRef(pval.getDoubleValues().get(index++).floatValue());
            }

            @Override
            public DoubleRef readDouble() {
                return new DoubleRef(pval.getDoubleValues().get(index++));
            }

            @Override
            public BinaryRef readBinary() {
                ByteBuffer byteBuffer = pval.getBinaryValues().get(index++);
                byte[] buffer = new byte[byteBuffer.remaining()];
                byteBuffer.get(buffer);
                return new BinaryRef(buffer);
            }

            @Override
            public DocRef readDocument() {
                return new DocRef(new Decoder(pval.getDocValues().get(index++)).decode());
            }

            @Override
            public BooleanArrayRef readBooleanArray() {
                boolean[] boolValues = new boolean[readArrayCount()];
                final List<Boolean> list = pval.getBoolValues();
                for (int i = 0; i < boolValues.length; i++) {
                    boolValues[i] = list.get(i+index);
                }
                index += boolValues.length;
                return new BooleanArrayRef(boolValues);
            }

            @Override
            public IntArrayRef readIntArray() {
                int[] intValues = new int[readArrayCount()];
                final List<Integer> list = pval.getIntValues();
                for (int i = 0; i < intValues.length; i++) {
                    intValues[i] = list.get(i+index);
                }
                index += intValues.length;
                return new IntArrayRef(intValues);
            }

            @Override
            public LongArrayRef readLongArray() {
                long[] longValues = new long[readArrayCount()];
                final List<Long> list = pval.getLongValues();
                for (int i = 0; i < longValues.length; i++) {
                    longValues[i] = list.get(i+index);
                }
                index += longValues.length;
                return new LongArrayRef(longValues);
            }

            @Override
            public FloatArrayRef readFloatArray() {
                float[] floatValues = new float[readArrayCount()];
                final List<Double> list = pval.getDoubleValues();
                for (int i = 0; i < floatValues.length; i++) {
                    floatValues[i] = list.get(i+index).floatValue();
                }
                index += floatValues.length;
                return new FloatArrayRef(floatValues);
            }

            @Override
            public DoubleArrayRef readDoubleArray() {
                double[] doubleValues = new double[readArrayCount()];
                List<Double> list = pval.getDoubleValues();
                for (int i = 0; i < doubleValues.length; i++) {
                    doubleValues[i] = list.get(i+index);
                }
                index += doubleValues.length;
                return new DoubleArrayRef(doubleValues);
            }

            @Override
            public DocArrayRef readDocumentArray() {
                MemoryDocument[] docValues = new MemoryDocument[pval.getDocValuesSize()];
                final List<TDocument> list = pval.getDocValues();
                for(int i = 0; i < docValues.length; i++) {
                    docValues[i] = new Decoder(list.get(i+index)).decode();
                }
                index += docValues.length;
                return new DocArrayRef(docValues);
            }
        }

        private static class PropertyMapDecoder extends PropertyReader {
            private List<TPropertyKey> keys;
            private List<TPropertyValue> values;
            private PropertyMap target;
            private TPropertyValue pval;

            private PropertyMapDecoder(TPropertyMap propmap) {
                this.keys = propmap.getPropkeys();
                this.values = propmap.getPropvalues();
                this.target = new PropertyMap();
            }

            public static PropertyMapRef decode(TPropertyMap propmap) {
                return new PropertyMapDecoder(propmap).decode();
            }

            public PropertyMapRef decode() {
                for (int i = 0; i < keys.size(); i++) {
                    TPropertyKey pkey = keys.get(i);
                    this.pval = values.get(i);
                    String key = pkey.getName();
                    target.putProperty(key, read(pkey.getType()));
                }

                return new PropertyMapRef(target);
            }
        }

        private void decodeDocProperties() {
            List<TPropertyKey> propkeys = inputdoc.getPropmap().getPropkeys();
            List<TPropertyValue> propvals = inputdoc.getPropmap().getPropvalues();

            PropertyReader propreader = new PropertyReader();

            for (int i = 0; i < propkeys.size(); i++) {
                TPropertyKey pkey = propkeys.get(i);
                if(!pkey.isSetName()) {
                    switch(pkey.getSpecial()) {
                        case DOC_ID:
                            doc.putProperty(se.lth.cs.docforia.Document.PROP_ID, propvals.get(i).getStringValues().get(0));
                            break;
                        case DOC_LANG:
                            doc.putProperty(se.lth.cs.docforia.Document.PROP_LANG, propvals.get(i).getStringValues().get(0));
                            break;
                        case DOC_TEXT: {
                            List<String> texts = propvals.get(i).getStringValues();
                            StringBuilder sb = new StringBuilder();
                            int last = 0;
                            for (int k = 0; k < texts.size(); k++) {
                                offsetmap.add(last);
                                sb.append(texts.get(k));
                                last = sb.length();
                            }

                            offsetmap.add(last);
                            doc.setText(sb.toString());
                            break;
                        }
                        case DOC_TYPE:
                            doc.putProperty(se.lth.cs.docforia.Document.PROP_LANG, propvals.get(i).getStringValues().get(0));
                            break;
                        case DOC_URI: {
                            List<String> uri = propvals.get(i).getStringValues();
                            doc.putProperty(Document.PROP_URI, uri.toArray(new String[uri.size()]));
                            break;
                        }
                        case DOC_TITLE:
                            if(pkey.getType() == TDataType.D_STRING) {
                                doc.putProperty(Document.PROP_TITLE, propvals.get(i).getStringValues().get(0));
                            }
                            else if(pkey.getType() == TDataType.D_DOCUMENT) {
                                doc.putProperty(Document.PROP_TITLE, new Decoder(propvals.get(i).getDocValues().get(0)).decode());
                            }
                            break;
                    }
                } else {
                    propreader.pval = propvals.get(i);
                    doc.putProperty(pkey.getName(), propreader.read(pkey.getType()));
                }
            }
        }

        private int decodeNodeLayer(int layerid, final TNodes nodelayer, final TTypeStream stream) {
            final List<String> variants = nodelayer.getVariants();
            final List<Integer> numentries = nodelayer.getNumentries();

            String layerName = "";

            if(!nodelayer.isSetName()) {
                layerName = MemoryCoreNodeLayer.fromId(nodelayer.getBuiltin().getValue()).layer;
            } else {
                layerName = nodelayer.getName();
            }

            int[][] columnItems = new int[nodelayer.getKeysSize()][];
            ColumnReader[] columnReaders = new ColumnReader[nodelayer.getColumnsSize()];
            List<TPropertyKey> keys = nodelayer.getKeys();

            for (int i = 0; i < nodelayer.getSetsSize(); i++) {
                TPropertySet set = nodelayer.getSets().get(i);
                columnItems[i] = new int[set.getKeysSize()];
                for (int k = 0; k < set.getKeysSize(); k++) {
                    columnItems[i][k] = set.getKeys().get(k);
                }
            }

            for (int i = 0; i < keys.size(); i++) {
                TPropertyKey propertyKey = keys.get(i);
                columnReaders[i] = new ColumnReader(nodelayer.getColumns().get(i));
                if(!propertyKey.isSetName()) {
                    //Special property
                    switch (propertyKey.getSpecial()) {
                        case NODE_NUM_EDGE_OUT:
                            columnReaders[i].type = TDataType.D_INT;
                            break;
                        case NODE_RANGE_START_END:
                            columnReaders[i].type = TDataType.D_INT;
                            break;
                        default:
                            System.err.println("Unknown property " + propertyKey.toString() + " found.");
                            break;
                    }
                } else {
                    //Normal property
                    columnReaders[i].type = propertyKey.getType();
                }
            }

            int k = 0;
            int lastStream = 0;
            final int numTotalNodes = stream.getStreamSize() == 0 ? numentries.stream().mapToInt(Integer::intValue).sum() : stream.getStreamSize() ;
            final boolean useDefaultAlways = stream.getStreamSize() == 0;

            for (int i = 0; i < variants.size(); i++) {
                int lastRange = 0;

                String variantName = nodelayer.getVariants().get(i);
                DocumentNodeLayer variantLayer = doc.store().nodeLayer(layerName, variantName.isEmpty() ? null : variantName);
                nodelayerid2layer.put(layerid, variantLayer);

                final int numNodes = numentries.get(i);
                ArrayList<NodeRef> nodeList = new ArrayList<>(numNodes);
                int j = 0;
                List<Integer> streamlist = stream.getStream();
                while(k < numTotalNodes && j < numNodes) {
                    int set = 0;
                    if(!useDefaultAlways)
                        set = streamlist.get(k)+lastStream;

                    lastStream = set;

                    int start = Integer.MIN_VALUE, end = Integer.MIN_VALUE;
                    ObjectArrayList<String> propKeys = new ObjectArrayList<>(columnItems[set].length);
                    ObjectArrayList<CoreRef> propValues = new ObjectArrayList<>(columnItems[set].length);
                    IntArrayList edgeData = new IntArrayList();

                    int[] columns = columnItems[set];
                    for (int pk : columns) {
                        final TPropertyKey propertyKey = keys.get(pk);
                        final ColumnReader columnReader = columnReaders[pk];

                        if (!propertyKey.isSetName()) {
                            //Special property
                            switch (propertyKey.getSpecial()) {
                                case NODE_NUM_EDGE_OUT:
                                    edgeData.add(propertyKey.getLayerid());
                                    edgeData.add(columnReader.pval.getIntValues().get(columnReader.index++));
                                    break;
                                case NODE_RANGE_START_END:
                                    int startpos = columnReader.pval.getIntValues().get(columnReader.index++) + lastRange;
                                    int endpos = columnReader.pval.getIntValues().get(columnReader.index++) + startpos;
                                    start = offsetmap.getInt(startpos);
                                    end = offsetmap.getInt(endpos);
                                    lastRange = endpos;
                                    break;
                                default:
                                    System.err.println("Unknown property " + propertyKey.toString() + " found.");
                                    break;
                            }
                        } else {
                            //Normal property
                            propKeys.add(propertyKey.getName());
                            propValues.add(columnReader.read());
                        }
                    }

                    NodeRef nodeRef;
                    if(start != Integer.MIN_VALUE) {
                        nodeRef = variantLayer.create(start,end);
                    }
                    else {
                        nodeRef = variantLayer.create();
                    }

                    for (int h = 0; h < propKeys.size(); h++) {
                        nodeRef.get().putProperty(propKeys.get(h), propValues.get(h));
                    }

                    for (int h = 0; h < edgeData.size(); h += 2) {
                        int elayerid = edgeData.get(h);
                        int num = edgeData.get(h+1);
                        ArrayList<NodeRef> nodes = getTailArray(elayerid);
                        for (int t = 0; t < num; t++) {
                            nodes.add(nodeRef);
                        }
                    }

                    nodeList.add(nodeRef);

                    j += 1;
                    k += 1;
                }

                layer2id2nodes.put(layerid, nodeList);
                layerid++;
            }
            return layerid;
        }

        private void decodeNodes() {
            if(inputdoc.getNodesSize() > 0) {
                List<TNodes> nodesList = inputdoc.getNodes();
                List<TTypeStream> nodestreamsList = inputdoc.getNodestreams();
                int layerid = 0;
                for (int i = 0; i < nodesList.size(); i++) {
                    layerid = decodeNodeLayer(layerid, nodesList.get(i), nodestreamsList.get(i));
                }
            }
        }

        private int decodeEdges(int layerid, TEdges edgelayer, TTypeStream stream) {
            final List<String> variants = edgelayer.getVariants();
            final List<Integer> numentries = edgelayer.getNumentries();

            String layerName = "";

            if(!edgelayer.isSetName()) {
                layerName = MemoryCoreEdgeLayer.fromId(edgelayer.getBuiltin().getValue()).layer;
            } else {
                layerName = edgelayer.getName();
            }

            int[][] columnItems = new int[edgelayer.getKeysSize()][];
            ColumnReader[] columnReaders = new ColumnReader[edgelayer.getColumnsSize()];
            List<TPropertyKey> keys = edgelayer.getKeys();

            for (int i = 0; i < edgelayer.getSetsSize(); i++) {
                TPropertySet set = edgelayer.getSets().get(i);
                columnItems[i] = new int[set.getKeysSize()];
                for (int k = 0; k < set.getKeysSize(); k++) {
                    columnItems[i][k] = set.getKeys().get(k);
                }
            }

            for (int i = 0; i < keys.size(); i++) {
                TPropertyKey propertyKey = keys.get(i);
                columnReaders[i] = new ColumnReader(edgelayer.getColumns().get(i));
                if(!propertyKey.isSetName()) {
                    //Special property
                    switch (propertyKey.getSpecial()) {
                        case EDGE_HEAD:
                            columnReaders[i].type = TDataType.D_INT;
                            break;
                        default:
                            System.err.println("Unknown property " + propertyKey.toString() + " found.");
                            break;
                    }
                } else {
                    //Normal property
                    columnReaders[i].type = propertyKey.getType();
                }
            }

            int k = 0;
            int lastStream = 0;
            final int numTotalNodes = stream.getStreamSize() == 0 ? numentries.stream().mapToInt(Integer::intValue).sum() : stream.getStreamSize() ;
            final boolean useDefaultAlways = stream.getStreamSize() == 0;

            for (int i = 0; i < variants.size(); i++) {
                String variantName = edgelayer.getVariants().get(i);
                DocumentEdgeLayer variantLayer = doc.store().edgeLayer(layerName, variantName.isEmpty() ? null : variantName);
                edgelayerid2layer.put(layerid, variantLayer);

                final int numEdges = numentries.get(i);
                int j = 0;
                ArrayList<NodeRef> tailArray = getTailArray(layerid);
                List<Integer> streamlist = stream.getStream();
                while(k < numTotalNodes && j < numEdges) {
                    int set = 0;
                    if(!useDefaultAlways)
                        set = streamlist.get(k)+lastStream;

                    lastStream = set;

                    ObjectArrayList<String> propKeys = new ObjectArrayList<>(columnItems[set].length);
                    ObjectArrayList<CoreRef> propValues = new ObjectArrayList<>(columnItems[set].length);

                    NodeRef tail=tailArray.get(j);
                    NodeRef head=null;

                    int[] columns = columnItems[set];
                    for (int pk : columns) {
                        final TPropertyKey propertyKey = keys.get(pk);
                        final ColumnReader columnReader = columnReaders[pk];

                        if (!propertyKey.isSetName()) {
                            //Special property
                            switch (propertyKey.getSpecial()) {
                                case EDGE_HEAD:
                                    int targetLayer = propertyKey.getLayerid();
                                    int targetNode = columnReaders[pk].pval.getIntValues().get(columnReaders[pk].index++);
                                    head = layer2id2nodes.get(targetLayer).get(targetNode);
                                    break;
                                default:
                                    System.err.println("Unknown property " + propertyKey.toString() + " found.");
                                    break;
                            }
                        } else {
                            //Normal property
                            propKeys.add(propertyKey.getName());
                            propValues.add(columnReader.read());
                        }
                    }

                    if(head == null)
                        throw new IOError(new IOException("Invalid head, head = null!"));

                    EdgeRef edgeRef = variantLayer.create(tail, head);
                    EdgeStore edgeStore = edgeRef.get();

                    for (int h = 0; h < propKeys.size(); h++) {
                        edgeStore.putProperty(propKeys.get(h), propValues.get(h));
                    }

                    j += 1;
                    k += 1;
                }

                layerid++;
            }
            return layerid;
        }

        private void decodeEdges() {
            if(inputdoc.getEdgesSize() > 0) {
                List<TEdges> edgeslist = inputdoc.getEdges();
                List<TTypeStream> edgestreamlist = inputdoc.getEdgestreams();
                int layerid = 0;
                for (int i = 0; i < edgeslist.size(); i++) {
                    layerid = decodeEdges(layerid, edgeslist.get(i), edgestreamlist.get(i));
                }
            }
        }

        public MemoryDocument decode() {
            decodeDocProperties();
            decodeNodes();
            decodeEdges();
            return doc;
        }
    }
}

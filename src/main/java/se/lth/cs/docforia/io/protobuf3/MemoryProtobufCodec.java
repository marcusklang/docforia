package se.lth.cs.docforia.io.protobuf3;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import se.lth.cs.docforia.*;
import se.lth.cs.docforia.data.*;
import se.lth.cs.docforia.io.singlepart.Singlepart;
import se.lth.cs.docforia.memstore.MemoryCoreEdgeLayer;
import se.lth.cs.docforia.memstore.MemoryCoreNodeLayer;
import se.lth.cs.docforia.memstore.MemoryDocument;

import java.io.IOError;
import java.io.IOException;
import java.util.*;

public class MemoryProtobufCodec {
    public static MemoryDocument decode(byte[] data) {
        try {
            return new Decoder(Singlepart.Document.parseFrom(data)).decode();
        } catch (InvalidProtocolBufferException e) {
            throw new IOError(e);
        }
    }

    public static byte[] encode(MemoryDocument document) {
        return new Encoder(document).encode().toByteArray();
    }

    private static class Encoder {
        public Singlepart.Document.Builder docBuilder;
        public MemoryDocument doc;

        private Object2IntLinkedOpenHashMap<LayerRef> nodeLayer2id = new Object2IntLinkedOpenHashMap<>();
        private Object2IntLinkedOpenHashMap<LayerRef> edgeLayer2id = new Object2IntLinkedOpenHashMap<>();
        private Int2IntOpenHashMap coordinateMapping = new Int2IntOpenHashMap();

        public Encoder(MemoryDocument doc) {
            this.docBuilder = Singlepart.Document.newBuilder();
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
            ArrayList<Singlepart.PropertyKey> docpropkeys = new ArrayList<>();
            ArrayList<Singlepart.PropertyValue> docpropvalues = new ArrayList<>();
            PropertyMapEncoder pmap = new PropertyMapEncoder(docpropkeys, docpropvalues, typefinder);

            for (Map.Entry<String, DataRef> prop : doc.store().properties()) {
                if(Document.PROP_ALL.contains(prop.getKey())) {
                    //Special!
                    switch (prop.getKey()) {
                        case Document.PROP_ID:
                            docpropkeys.add(Singlepart.PropertyKey.newBuilder()
                                    .setSpecial(Singlepart.PropertyKey.SpecialKey.DOC_ID)
                                    .setType(Singlepart.PropertyKey.DataType.D_STRING)
                                    .build());

                            docpropvalues.add(Singlepart.PropertyValue.newBuilder()
                                    .addStringValues(prop.getValue().stringValue())
                                    .build());
                            break;
                        case Document.PROP_LANG:
                            docpropkeys.add(Singlepart.PropertyKey.newBuilder()
                                    .setSpecial(Singlepart.PropertyKey.SpecialKey.DOC_LANG)
                                    .setType(Singlepart.PropertyKey.DataType.D_STRING)
                                    .build());

                            docpropvalues.add(Singlepart.PropertyValue.newBuilder()
                                    .addStringValues(prop.getValue().stringValue())
                                    .build());
                            break;
                        case Document.PROP_TITLE:
                            docpropkeys.add(Singlepart.PropertyKey.newBuilder()
                                    .setSpecial(Singlepart.PropertyKey.SpecialKey.DOC_TITLE)
                                    .setType(prop.getValue() instanceof DocRef ? Singlepart.PropertyKey.DataType.D_DOCUMENT : Singlepart.PropertyKey.DataType.D_STRING)
                                    .build());

                            if(prop.getValue() instanceof DocRef) {
                                docpropvalues.add(Singlepart.PropertyValue.newBuilder()
                                        .addDocValues(new Encoder((MemoryDocument)((DocRef)prop.getValue()).documentValue()).encode() )
                                        .build());
                            } else {
                                docpropvalues.add(Singlepart.PropertyValue.newBuilder()
                                        .addStringValues(prop.getValue().stringValue())
                                        .build());
                            }
                            break;
                        case Document.PROP_TYPE:
                            docpropkeys.add(Singlepart.PropertyKey.newBuilder()
                                    .setSpecial(Singlepart.PropertyKey.SpecialKey.DOC_TYPE)
                                    .setType(Singlepart.PropertyKey.DataType.D_STRING)
                                    .build());

                            docpropvalues.add(Singlepart.PropertyValue.newBuilder()
                                    .addStringValues(prop.getValue().stringValue())
                                    .build());
                            break;
                        case Document.PROP_URI:
                            docpropkeys.add(Singlepart.PropertyKey.newBuilder()
                                    .setSpecial(Singlepart.PropertyKey.SpecialKey.DOC_URI)
                                    .setType(Singlepart.PropertyKey.DataType.D_STRING_ARRAY)
                                    .build());

                            docpropvalues.add(Singlepart.PropertyValue.newBuilder()
                                    .addAllStringValues(Arrays.asList(((StringArrayRef)prop.getValue()).arrayValue()))
                                    .build());
                            break;
                    }
                } else {
                    if(!(prop.getValue() instanceof CoreRef))
                        throw new UnsupportedOperationException("Only CoreRef types are supported!");

                    CoreRef coreValue = (CoreRef)prop.getValue();
                    coreValue.write(typefinder);
                    pmap.pkeys.add(Singlepart.PropertyKey.newBuilder().setName(prop.getKey()).setType(typefinder.type).build());
                    pmap.pvalues = docpropvalues;
                    coreValue.write(pmap);
                }
            }

            //Add text property, find the minimum coordinate spaces.
            ArrayList<String> textFragments = new ArrayList<>();
            this.coordinateMapping = generateTextFragments(textFragments);
            pmap.pkeys.add(Singlepart.PropertyKey.newBuilder().setSpecial(Singlepart.PropertyKey.SpecialKey.DOC_TEXT).setType(Singlepart.PropertyKey.DataType.D_STRING_ARRAY).build());
            pmap.pvalues.add(Singlepart.PropertyValue.newBuilder().addAllStringValues(textFragments).build());

            docBuilder.setPropmap(Singlepart.PropertyMap.newBuilder().addAllPropkeys(docpropkeys).addAllPropvalues(docpropvalues).build());
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
            private Singlepart.PropertyKey.SpecialKey specialKey;
            private String key;

            private Singlepart.PropertyKey.DataType type;
            private int layerid;

            public PropertyKey() {
                this.specialKey = null;
                this.key = null;
                this.type = null;
                this.layerid = -1;
            }

            public PropertyKey(Singlepart.PropertyKey.SpecialKey specialKey, Singlepart.PropertyKey.DataType type) {
                this.specialKey = specialKey;
                this.type = type;
                this.layerid = -1;
            }

            public PropertyKey(Singlepart.PropertyKey.SpecialKey specialKey, int layerid) {
                this.specialKey = specialKey;
                this.layerid = layerid;
            }

            public PropertyKey(String key, Singlepart.PropertyKey.DataType type) {
                this.key = key;
                this.type = type;
                this.layerid = -1;
            }

            private PropertyKey(Singlepart.PropertyKey.SpecialKey specialKey, String key, Singlepart.PropertyKey.DataType type, int layerid) {
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

            public Singlepart.PropertyKey.SpecialKey getSpecialKey() {
                return specialKey;
            }

            public String getKey() {
                return key;
            }

            public Singlepart.PropertyKey.DataType getType() {
                return type;
            }

            public int getLayerid() {
                return layerid;
            }

            public void set(Singlepart.PropertyKey.SpecialKey specialKey, Singlepart.PropertyKey.DataType type) {
                this.specialKey = specialKey;
                this.type = type;
                this.layerid = -1;
            }

            public void set(Singlepart.PropertyKey.SpecialKey specialKey, int layerid) {
                this.specialKey = specialKey;
                this.layerid = layerid;
            }

            public void set(String key, Singlepart.PropertyKey.DataType type) {
                this.key = key;
                this.type = type;
                this.layerid = -1;
            }

            public PropertyKey copy() {
                return new PropertyKey(specialKey, key, type, layerid);
            }
        }

        private static class TypeFinder implements CoreRefWriter {
            public Singlepart.PropertyKey.DataType type;

            @Override
            public void write(byte[] binary) {
                type = Singlepart.PropertyKey.DataType.D_BINARY;
            }

            @Override
            public void write(String string) {
                type = Singlepart.PropertyKey.DataType.D_STRING;
            }

            @Override
            public void write(boolean boolValue) {
                type = Singlepart.PropertyKey.DataType.D_BOOLEAN;
            }

            @Override
            public void write(int intValue) {
                type = Singlepart.PropertyKey.DataType.D_INT;
            }

            @Override
            public void write(long longValue) {
                type = Singlepart.PropertyKey.DataType.D_LONG;
            }

            @Override
            public void write(float floatValue) {
                type = Singlepart.PropertyKey.DataType.D_FLOAT;
            }

            @Override
            public void write(double doubleValue) {
                type = Singlepart.PropertyKey.DataType.D_DOUBLE;
            }

            @Override
            public void write(MemoryDocument doc) {
                type = Singlepart.PropertyKey.DataType.D_DOCUMENT;
            }

            @Override
            public void write(PropertyMap propertyMap) {
                type = Singlepart.PropertyKey.DataType.D_PROPERTY_MAP;
            }

            @Override
            public void writeBooleanArray(boolean[] boolValues) {
                type = Singlepart.PropertyKey.DataType.D_BOOLEAN_ARARY;
            }

            @Override
            public void writeIntArray(int[] intValues) {
                type = Singlepart.PropertyKey.DataType.D_INT_ARRAY;
            }

            @Override
            public void writeLongArray(long[] longValues) {
                type = Singlepart.PropertyKey.DataType.D_LONG_ARRAY;
            }

            @Override
            public void writeFloatArray(float[] floatValues) {
                type = Singlepart.PropertyKey.DataType.D_FLOAT_ARRAY;
            }

            @Override
            public void writeDoubleArray(double[] doubleValues) {
                type = Singlepart.PropertyKey.DataType.D_DOUBLE_ARRAY;
            }

            @Override
            public void writeDocumentArray(MemoryDocument[] docValues) {
                type = Singlepart.PropertyKey.DataType.D_DOCUMENT_ARRAY;
            }

            @Override
            public void writeStringArray(String[] stringValues) {
                type = Singlepart.PropertyKey.DataType.D_STRING_ARRAY;
            }
        }

        private static class PropertyMapEncoder implements CoreRefWriter {
            private ArrayList<Singlepart.PropertyKey> pkeys;
            private ArrayList<Singlepart.PropertyValue> pvalues;
            private TypeFinder finder;

            public PropertyMapEncoder(ArrayList<Singlepart.PropertyKey> pkeys, ArrayList<Singlepart.PropertyValue> pvalues, TypeFinder finder) {
                this.pkeys = pkeys;
                this.pvalues = pvalues;
                this.finder = finder;
            }

            public PropertyMapEncoder() {
                this.pkeys = new ArrayList<>();
                this.pvalues = new ArrayList<>();
                this.finder = new TypeFinder();
            }

            public static Singlepart.PropertyMap encode(PropertyMap pmap) {
                PropertyMapEncoder encoder = new PropertyMapEncoder();
                for (Map.Entry<String, DataRef> entry : pmap.properties()) {
                    if(!(entry.getValue() instanceof CoreRef)) {
                        throw new UnsupportedOperationException("Only CoreRefs are supported!");
                    }

                    CoreRef value = ((CoreRef)entry.getValue());
                    value.write(encoder.finder);

                    encoder.pkeys.add(Singlepart.PropertyKey.newBuilder().setName(entry.getKey()).setType(encoder.finder.type).build());
                    value.write(encoder);
                }

                return Singlepart.PropertyMap.newBuilder().addAllPropkeys(encoder.pkeys).addAllPropvalues(encoder.pvalues).build();
            }

            @Override
            public void write(byte[] binary) {
                pvalues.add(Singlepart.PropertyValue.newBuilder().setBinaryValue(ByteString.copyFrom(binary)).build());
            }

            @Override
            public void write(String string) {
                pvalues.add(Singlepart.PropertyValue.newBuilder().addStringValues(string).build());
            }

            @Override
            public void write(boolean boolValue) {
                pvalues.add(Singlepart.PropertyValue.newBuilder().addBoolValues(boolValue).build());
            }

            @Override
            public void write(int intValue) {
                pvalues.add(Singlepart.PropertyValue.newBuilder().addIntValues(intValue).build());
            }

            @Override
            public void write(long longValue) {
                pvalues.add(Singlepart.PropertyValue.newBuilder().addLongValues(longValue).build());
            }

            @Override
            public void write(float floatValue) {
                pvalues.add(Singlepart.PropertyValue.newBuilder().addFloatValues(floatValue).build());
            }

            @Override
            public void write(double doubleValue) {
                pvalues.add(Singlepart.PropertyValue.newBuilder().addDoubleValues(doubleValue).build());
            }

            @Override
            public void write(MemoryDocument doc) {
                pvalues.add(Singlepart.PropertyValue.newBuilder().addDocValues(new Encoder(doc).encode()).build());
            }

            @Override
            public void write(PropertyMap propertyMap) {
                pvalues.add(Singlepart.PropertyValue.newBuilder().addPropValues(PropertyMapEncoder.encode(propertyMap)).build());
            }

            @Override
            public void writeBooleanArray(boolean[] boolValues) {
                Singlepart.PropertyValue.Builder builder = Singlepart.PropertyValue.newBuilder();
                for (boolean boolValue : boolValues) {
                    builder.addBoolValues(boolValue);
                }
                pvalues.add(builder.build());
            }

            @Override
            public void writeIntArray(int[] intValues) {
                Singlepart.PropertyValue.Builder builder = Singlepart.PropertyValue.newBuilder();
                for (int intValue : intValues) {
                    builder.addIntValues(intValue);
                }
                builder.addLengthInfo(intValues.length);
                pvalues.add(builder.build());
            }

            @Override
            public void writeLongArray(long[] longValues) {
                Singlepart.PropertyValue.Builder builder = Singlepart.PropertyValue.newBuilder();
                for (long longValue : longValues) {
                    builder.addLongValues(longValue);
                }
                builder.addLengthInfo(longValues.length);
                pvalues.add(builder.build());
            }

            @Override
            public void writeFloatArray(float[] floatValues) {
                Singlepart.PropertyValue.Builder builder = Singlepart.PropertyValue.newBuilder();
                for (float floatValue : floatValues) {
                    builder.addFloatValues(floatValue);
                }
                builder.addLengthInfo(floatValues.length);
                pvalues.add(builder.build());
            }

            @Override
            public void writeDoubleArray(double[] doubleValues) {
                Singlepart.PropertyValue.Builder builder = Singlepart.PropertyValue.newBuilder();
                for (double doubleValue : doubleValues) {
                    builder.addDoubleValues(doubleValue);
                }
                builder.addLengthInfo(doubleValues.length);
                pvalues.add(builder.build());
            }

            @Override
            public void writeDocumentArray(MemoryDocument[] docValues) {
                Singlepart.PropertyValue.Builder builder = Singlepart.PropertyValue.newBuilder();
                for (MemoryDocument docValue : docValues) {
                    builder.addDocValues(new Encoder(docValue).encode());
                }
                builder.addLengthInfo(docValues.length);
                pvalues.add(builder.build());
            }

            @Override
            public void writeStringArray(String[] stringValues) {
                Singlepart.PropertyValue.Builder builder = Singlepart.PropertyValue.newBuilder();
                for (String stringValue : stringValues) {
                    builder.addStringValues(stringValue);
                }
                builder.addLengthInfo(stringValues.length);
                pvalues.add(builder.build());
            }
        }

        private static class PropertyStreamWriter implements CoreRefWriter {
            private ArrayList<Singlepart.Column.Builder> nodeBuilders;
            private int pkeyid;

            @Override
            public void write(byte[] binary) {
                nodeBuilders.get(pkeyid).addBinaryValues(ByteString.copyFrom(binary));
            }

            @Override
            public void write(String string) {
                nodeBuilders.get(pkeyid).addStringValues(string);
            }

            @Override
            public void write(boolean boolValue) {
                nodeBuilders.get(pkeyid).addBoolValues(boolValue);
            }

            @Override
            public void write(int intValue) {
                nodeBuilders.get(pkeyid).addIntValues(intValue);
            }

            @Override
            public void write(long longValue) {
                nodeBuilders.get(pkeyid).addLongValues(longValue);
            }

            @Override
            public void write(float floatValue) {
                nodeBuilders.get(pkeyid).addFloatValues(floatValue);
            }

            @Override
            public void write(double doubleValue) {
                nodeBuilders.get(pkeyid).addDoubleValues(doubleValue);
            }

            @Override
            public void write(MemoryDocument doc) {
                nodeBuilders.get(pkeyid).addDocValues(new Encoder(doc).encode());
            }

            @Override
            public void write(PropertyMap propertyMap) {
                nodeBuilders.get(pkeyid).addPropmapValues(PropertyMapEncoder.encode(propertyMap));
            }

            @Override
            public void writeBooleanArray(boolean[] boolValues) {
                Singlepart.Column.Builder builder = nodeBuilders.get(pkeyid);
                for (boolean boolValue : boolValues) {
                    builder.addBoolValues(boolValue);
                }
                builder.addLengthInfo(boolValues.length);
            }

            @Override
            public void writeIntArray(int[] intValues) {
                Singlepart.Column.Builder builder = nodeBuilders.get(pkeyid);
                for (int intValue : intValues) {
                    builder.addIntValues(intValue);
                }
                builder.addLengthInfo(intValues.length);
            }

            @Override
            public void writeLongArray(long[] longValues) {
                Singlepart.Column.Builder builder = nodeBuilders.get(pkeyid);
                for (long longValue : longValues) {
                    builder.addLongValues(longValue);
                }
                builder.addLengthInfo(longValues.length);
            }

            @Override
            public void writeFloatArray(float[] floatValues) {
                Singlepart.Column.Builder builder = nodeBuilders.get(pkeyid);
                for (float floatValue : floatValues) {
                    builder.addFloatValues(floatValue);
                }
                builder.addLengthInfo(floatValues.length);
            }

            @Override
            public void writeDoubleArray(double[] doubleValues) {
                Singlepart.Column.Builder builder = nodeBuilders.get(pkeyid);
                for (double doubleValue : doubleValues) {
                    builder.addDoubleValues(doubleValue);
                }
                builder.addLengthInfo(doubleValues.length);
            }

            @Override
            public void writeDocumentArray(MemoryDocument[] docValues) {
                Singlepart.Column.Builder builder = nodeBuilders.get(pkeyid);
                for (MemoryDocument docValue : docValues) {
                    builder.addDocValues(new Encoder(docValue).encode());
                }
                builder.addLengthInfo(docValues.length);
            }

            @Override
            public void writeStringArray(String[] stringValues) {
                Singlepart.Column.Builder builder = nodeBuilders.get(pkeyid);
                for (String stringValue : stringValues) {
                    builder.addStringValues(stringValue);
                }
                builder.addLengthInfo(stringValues.length);
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
                pset.add(new PropertyKey(Singlepart.PropertyKey.SpecialKey.NODE_NUM_EDGE_OUT, iter.nextInt()));
            }

            if(ref.get().isAnnotation()) {
                pset.add(new PropertyKey(Singlepart.PropertyKey.SpecialKey.NODE_RANGE_START_END, Singlepart.PropertyKey.DataType.D_INT));
            }

            return pset;
        }

        private HashSet<PropertyKey> getPropertySet(EdgeRef ref) {
            HashSet<PropertyKey> pset = getPropertySetsFromNodeEdge(ref);
            pset.add(new PropertyKey(Singlepart.PropertyKey.SpecialKey.EDGE_HEAD, nodeLayer2id.getInt(ref.get().getHead().layer())));

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

        private static final PropertyKey NODE_RANGE_PKEY = new PropertyKey(Singlepart.PropertyKey.SpecialKey.NODE_RANGE_START_END, Singlepart.PropertyKey.DataType.D_INT);

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

        private Singlepart.Nodes encodeNodeLayer(String nodeLayer) {
            Singlepart.Nodes.Builder nodeBuilder = Singlepart.Nodes.newBuilder();
            ArrayList<Singlepart.Column.Builder> columnBuilders = new ArrayList<>();
            Singlepart.TypeStream.Builder nodeStream = Singlepart.TypeStream.newBuilder();

            //1. Find and save all property sets
            Object2IntLinkedOpenHashMap<PropertyKey> pkeys = new Object2IntLinkedOpenHashMap<>();
            Object2IntLinkedOpenHashMap<HashSet<PropertyKey>> psets = new Object2IntLinkedOpenHashMap<>();
            findNodePropertySet(nodeLayer, psets, pkeys);

            MemoryCoreNodeLayer possibleCoreLayer = MemoryCoreNodeLayer.fromLayerName(nodeLayer);
            if(possibleCoreLayer == MemoryCoreNodeLayer.UNKNOWN) {
                nodeBuilder.setName(nodeLayer);
            } else {
                nodeBuilder.setBuiltinValue(possibleCoreLayer.id);
            }

            //2. Create all property streams
            pkeys.object2IntEntrySet().forEach(e -> {
                PropertyKey pkey = e.getKey();
                if(pkey.specialKey != null) {
                    switch(pkey.specialKey) {
                        case NODE_NUM_EDGE_OUT:
                            columnBuilders.add(Singlepart.Column.newBuilder());
                            nodeBuilder.addKeys(Singlepart.PropertyKey.newBuilder()
                                    .setSpecial(pkey.specialKey)
                                    .setLayerid(pkey.layerid)
                                    .build());
                            break;
                        default:
                            columnBuilders.add(Singlepart.Column.newBuilder());
                            nodeBuilder.addKeys(Singlepart.PropertyKey.newBuilder()
                                    .setSpecial(pkey.specialKey)
                                    .setType(pkey.type)
                                    .build());
                            break;
                    }
                }
                else {
                    columnBuilders.add(Singlepart.Column.newBuilder());
                    nodeBuilder.addKeys(Singlepart.PropertyKey.newBuilder()
                            .setName(pkey.key).setType(pkey.type)
                            .build());
                }
            });

            //3. Create all propertysets.
            psets.object2IntEntrySet().forEach(e -> {
                Singlepart.PropertySet.Builder psetbuilder = Singlepart.PropertySet.newBuilder();
                for (PropertyKey propertyKey : e.getKey()) {
                    psetbuilder.addKeys(pkeys.getInt(propertyKey));
                }
                nodeBuilder.addSets(psetbuilder);
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
                        Singlepart.Column.Builder rangeCol = columnBuilders.get(idx);

                        int pos = coordinateMapping.get(store.getStart());
                        rangeCol.addIntValues(pos-lastpos);
                        lastpos = pos;
                        pos = coordinateMapping.get(store.getEnd());
                        rangeCol.addIntValues(pos-lastpos);
                        lastpos = pos;
                    }

                    Int2IntRBTreeMap edgeCounts = new Int2IntRBTreeMap();

                    doc.engine().edges(nodeRef, Direction.OUT).stream().forEach(e -> {
                        int edgeLayerId = edgeLayer2id.get(e.layer());
                        getEdgelayer(edgeLayerId).add(e);
                        edgeCounts.put(edgeLayerId, edgeCounts.get(edgeLayerId)+1);
                    });

                    for (Int2IntMap.Entry entry : edgeCounts.int2IntEntrySet()) {
                        PropertyKey pkey = new PropertyKey(Singlepart.PropertyKey.SpecialKey.NODE_NUM_EDGE_OUT, entry.getIntKey());
                        pset.add(pkey);
                        int idx = pkeys.getInt(pkey);
                        columnBuilders.get(idx).addIntValues(entry.getIntValue());
                    }

                    if(psets.size() != 1) {
                        int psetid = psets.getInt(pset);
                        nodeStream.addStream(psetid-laststream);
                        laststream = psetid;
                    }

                    node2id.put(nodeRef, count);
                    count++;
                }

                nodeBuilder.addVariants(variant.orElse(""));
                nodeBuilder.addNumentries(count);
            }

            //Save columns in order.
            columnBuilders.forEach(nodeBuilder::addColumns);

            docBuilder.addNodestreams(nodeStream);

            return nodeBuilder.build();
        }

        private void encodeNodes() {
            for (String nodeLayer : doc.engine().nodeLayers()) {
                Singlepart.Nodes nodes = encodeNodeLayer(nodeLayer);
                docBuilder.addNodes(nodes);
            }
        }

        private Singlepart.Edges encodeEdgeLayer(String edgeLayer) {
            Singlepart.Edges.Builder edgeBuilder = Singlepart.Edges.newBuilder();
            ArrayList<Singlepart.Column.Builder> columnBuilders = new ArrayList<>();
            Singlepart.TypeStream.Builder edgeStream = Singlepart.TypeStream.newBuilder();

            MemoryCoreEdgeLayer possibleCoreLayer = MemoryCoreEdgeLayer.fromLayerName(edgeLayer);
            if(possibleCoreLayer == MemoryCoreEdgeLayer.UNKNOWN) {
                edgeBuilder.setName(edgeLayer);
            } else {
                edgeBuilder.setBuiltinValue(possibleCoreLayer.id);
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
                            columnBuilders.add(Singlepart.Column.newBuilder());
                            edgeBuilder.addKeys(Singlepart.PropertyKey.newBuilder()
                                    .setSpecial(pkey.specialKey)
                                    .setLayerid(pkey.layerid)
                                    .build());
                            break;
                        default:
                            columnBuilders.add(Singlepart.Column.newBuilder());
                            edgeBuilder.addKeys(Singlepart.PropertyKey.newBuilder()
                                    .setSpecial(pkey.specialKey)
                                    .setType(pkey.type)
                                    .build());
                            break;
                    }
                }
                else {
                    columnBuilders.add(Singlepart.Column.newBuilder());
                    edgeBuilder.addKeys(Singlepart.PropertyKey.newBuilder()
                            .setName(pkey.key).setType(pkey.type)
                            .build());
                }
            });

            //3. Create all propertysets.
            psets.object2IntEntrySet().forEach(e -> {
                Singlepart.PropertySet.Builder psetbuilder = Singlepart.PropertySet.newBuilder();
                for (PropertyKey propertyKey : e.getKey()) {
                    psetbuilder.addKeys(pkeys.getInt(propertyKey));
                }
                edgeBuilder.addSets(psetbuilder);
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

                    PropertyKey pkey = new PropertyKey(Singlepart.PropertyKey.SpecialKey.EDGE_HEAD, nodeLayer2id.getInt(head.layer()));

                    pset.add(pkey);
                    columnBuilders.get(pkeys.getInt(pkey)).addIntValues(node2id.getInt(head));

                    if(psets.size() != 1) {
                        int psetid = psets.getInt(pset);
                        edgeStream.addStream(psetid-laststream);
                        laststream = psetid;
                    }
                }

                edgeBuilder.addVariants(variant.orElse(""));
                edgeBuilder.addNumentries(edges.size());
            }

            //Save columns in order.
            columnBuilders.forEach(edgeBuilder::addColumns);

            docBuilder.addEdgestreams(edgeStream);

            return edgeBuilder.build();
        }

        private void encodeEdges() {
            for (String edgeLayer : doc.engine().edgeLayers()) {
                Singlepart.Edges edges = encodeEdgeLayer(edgeLayer);
                docBuilder.addEdges(edges);
            }
        }

        public Singlepart.Document encode() {
            encodeDocumentProperties();
            produceNodeEdgeIds();
            encodeNodes();
            encodeEdges();

            return docBuilder.build();
        }
    }

    private static class Decoder {
        private Singlepart.Document inputdoc;
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

        public Decoder(Singlepart.Document inputdoc) {
            this.inputdoc = inputdoc;
        }

        private static class PropertyReader implements CoreRefReader {
            protected Singlepart.PropertyValue pval;

            public CoreRef read(Singlepart.PropertyKey.DataType type) {
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
                return new StringRef(pval.getStringValues(0));
            }

            @Override
            public StringArrayRef readStringArray() {
                return new StringArrayRef(pval.getStringValuesList().toArray(new String[pval.getStringValuesCount()]));
            }

            @Override
            public PropertyMapRef readPropertyMap() {
                return PropertyMapDecoder.decode(pval.getPropValues(0));
            }

            @Override
            public BooleanRef readBoolean() {
                return pval.getBoolValues(0) ? BooleanRef.TRUE : BooleanRef.FALSE;
            }

            @Override
            public IntRef readInt() {
                return new IntRef(pval.getIntValues(0));
            }

            @Override
            public LongRef readLong() {
                return new LongRef(pval.getLongValues(0));
            }

            @Override
            public FloatRef readFloat() {
                return new FloatRef(pval.getFloatValues(0));
            }

            @Override
            public DoubleRef readDouble() {
                return new DoubleRef(pval.getDoubleValues(0));
            }

            @Override
            public BinaryRef readBinary() {
                return new BinaryRef(pval.getBinaryValue().toByteArray());
            }

            @Override
            public DocRef readDocument() {
                return new DocRef(new Decoder(pval.getDocValues(0)).decode());
            }

            @Override
            public BooleanArrayRef readBooleanArray() {
                boolean[] boolValues = new boolean[pval.getBoolValuesCount()];
                for (int i = 0; i < boolValues.length; i++) {
                    boolValues[i] = pval.getBoolValues(i);
                }
                return new BooleanArrayRef(boolValues);
            }

            @Override
            public IntArrayRef readIntArray() {
                int[] intValues = new int[pval.getIntValuesCount()];
                for (int i = 0; i < intValues.length; i++) {
                    intValues[i] = pval.getIntValues(i);
                }
                return new IntArrayRef(intValues);
            }

            @Override
            public LongArrayRef readLongArray() {
                long[] longValues = new long[pval.getLongValuesCount()];
                for (int i = 0; i < longValues.length; i++) {
                    longValues[i] = pval.getLongValues(i);
                }
                return new LongArrayRef(longValues);
            }

            @Override
            public FloatArrayRef readFloatArray() {
                float[] floatValues = new float[pval.getFloatValuesCount()];
                for (int i = 0; i < floatValues.length; i++) {
                    floatValues[i] = pval.getFloatValues(i);
                }
                return new FloatArrayRef(floatValues);
            }

            @Override
            public DoubleArrayRef readDoubleArray() {
                double[] doubleValues = new double[pval.getDoubleValuesCount()];
                for (int i = 0; i < doubleValues.length; i++) {
                    doubleValues[i] = pval.getDoubleValues(i);
                }
                return new DoubleArrayRef(doubleValues);
            }

            @Override
            public DocArrayRef readDocumentArray() {
                MemoryDocument[] docValues = new MemoryDocument[pval.getDocValuesCount()];
                for(int i = 0; i < docValues.length; i++) {
                    docValues[i] = new Decoder(pval.getDocValues(i)).decode();
                }
                return new DocArrayRef(docValues);
            }
        }

        private static class ColumnReader implements CoreRefReader {
            protected Singlepart.Column pval;
            protected int index = 0;
            protected int arrcountidx = 0;
            protected Singlepart.PropertyKey.DataType type;

            public ColumnReader(Singlepart.Column pval) {
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
                return new StringRef(pval.getStringValues(index++));
            }

            private int readArrayCount() {
                return pval.getLengthInfo(arrcountidx++);
            }

            @Override
            public StringArrayRef readStringArray() {
                int num = readArrayCount();
                int idx = index;
                index += num;

                return new StringArrayRef(pval.getStringValuesList().subList(idx,idx+num).toArray(new String[num]));
            }

            @Override
            public PropertyMapRef readPropertyMap() {
                return PropertyMapDecoder.decode(pval.getPropmapValues(index++));
            }

            @Override
            public BooleanRef readBoolean() {
                return pval.getBoolValues(index++) ? BooleanRef.TRUE : BooleanRef.FALSE;
            }

            @Override
            public IntRef readInt() {
                return new IntRef(pval.getIntValues(index++));
            }

            @Override
            public LongRef readLong() {
                return new LongRef(pval.getLongValues(index++));
            }

            @Override
            public FloatRef readFloat() {
                return new FloatRef(pval.getFloatValues(index++));
            }

            @Override
            public DoubleRef readDouble() {
                return new DoubleRef(pval.getDoubleValues(index++));
            }

            @Override
            public BinaryRef readBinary() {
                return new BinaryRef(pval.getBinaryValues(index++).toByteArray());
            }

            @Override
            public DocRef readDocument() {
                return new DocRef(new Decoder(pval.getDocValues(index++)).decode());
            }

            @Override
            public BooleanArrayRef readBooleanArray() {
                boolean[] boolValues = new boolean[readArrayCount()];
                for (int i = 0; i < boolValues.length; i++) {
                    boolValues[i] = pval.getBoolValues(i+index);
                }
                index += boolValues.length;
                return new BooleanArrayRef(boolValues);
            }

            @Override
            public IntArrayRef readIntArray() {
                int[] intValues = new int[readArrayCount()];
                for (int i = 0; i < intValues.length; i++) {
                    intValues[i] = pval.getIntValues(i+index);
                }
                index += intValues.length;
                return new IntArrayRef(intValues);
            }

            @Override
            public LongArrayRef readLongArray() {
                long[] longValues = new long[readArrayCount()];
                for (int i = 0; i < longValues.length; i++) {
                    longValues[i] = pval.getLongValues(i+index);
                }
                index += longValues.length;
                return new LongArrayRef(longValues);
            }

            @Override
            public FloatArrayRef readFloatArray() {
                float[] floatValues = new float[readArrayCount()];
                for (int i = 0; i < floatValues.length; i++) {
                    floatValues[i] = pval.getFloatValues(i+index);
                }
                index += floatValues.length;
                return new FloatArrayRef(floatValues);
            }

            @Override
            public DoubleArrayRef readDoubleArray() {
                double[] doubleValues = new double[readArrayCount()];
                for (int i = 0; i < doubleValues.length; i++) {
                    doubleValues[i] = pval.getDoubleValues(i+index);
                }
                index += doubleValues.length;
                return new DoubleArrayRef(doubleValues);
            }

            @Override
            public DocArrayRef readDocumentArray() {
                MemoryDocument[] docValues = new MemoryDocument[pval.getDocValuesCount()];
                for(int i = 0; i < docValues.length; i++) {
                    docValues[i] = new Decoder(pval.getDocValues(i+index)).decode();
                }
                index += docValues.length;
                return new DocArrayRef(docValues);
            }
        }

        private static class PropertyMapDecoder extends PropertyReader {
            private List<Singlepart.PropertyKey> keys;
            private List<Singlepart.PropertyValue> values;
            private PropertyMap target;
            private Singlepart.PropertyValue pval;

            private PropertyMapDecoder(Singlepart.PropertyMap propmap) {
                this.keys = propmap.getPropkeysList();
                this.values = propmap.getPropvaluesList();
                this.target = new PropertyMap();
            }

            public static PropertyMapRef decode(Singlepart.PropertyMap propmap) {
                return new PropertyMapDecoder(propmap).decode();
            }

            public PropertyMapRef decode() {
                for (int i = 0; i < keys.size(); i++) {
                    Singlepart.PropertyKey pkey = keys.get(i);
                    this.pval = values.get(i);
                    String key = pkey.getName();
                    target.putProperty(key, read(pkey.getType()));
                }

                return new PropertyMapRef(target);
            }
        }

        private void decodeDocProperties() {
            List<Singlepart.PropertyKey> propkeys = inputdoc.getPropmap().getPropkeysList();
            List<Singlepart.PropertyValue> propvals = inputdoc.getPropmap().getPropvaluesList();

            PropertyReader propreader = new PropertyReader();

            for (int i = 0; i < propkeys.size(); i++) {
                Singlepart.PropertyKey pkey = propkeys.get(i);
                if(pkey.getName().isEmpty()) {
                    switch(pkey.getSpecial()) {
                        case DOC_ID:
                            doc.putProperty(Document.PROP_ID, propvals.get(i).getStringValues(0));
                            break;
                        case DOC_LANG:
                            doc.putProperty(Document.PROP_LANG, propvals.get(i).getStringValues(0));
                            break;
                        case DOC_TEXT: {
                            List<String> uri = propvals.get(i).getStringValuesList();
                            StringBuilder sb = new StringBuilder();
                            int last = 0;
                            for (int k = 0; k < uri.size(); k++) {
                                offsetmap.add(last);
                                sb.append(uri.get(k));
                                last = sb.length();
                            }

                            offsetmap.add(last);
                            doc.setText(sb.toString());
                            break;
                        }
                        case DOC_TYPE:
                            doc.putProperty(Document.PROP_LANG, propvals.get(i).getStringValues(0));
                            break;
                        case DOC_URI: {
                            List<String> uri = propvals.get(i).getStringValuesList();
                            doc.putProperty(Document.PROP_URI, uri.toArray(new String[uri.size()]));
                            break;
                        }
                        case DOC_TITLE:
                            if(pkey.getType() == Singlepart.PropertyKey.DataType.D_STRING) {
                                doc.putProperty(Document.PROP_TITLE, propvals.get(i).getStringValues(0));
                            }
                            else if(pkey.getType() == Singlepart.PropertyKey.DataType.D_DOCUMENT) {
                                doc.putProperty(Document.PROP_TITLE, new Decoder(propvals.get(i).getDocValues(0)).decode());
                            }
                            break;
                    }
                } else {
                    propreader.pval = propvals.get(i);
                    doc.putProperty(pkey.getName(), propreader.read(pkey.getType()));
                }
            }
        }

        private int decodeNodeLayer(int layerid, final Singlepart.Nodes nodelayer, final Singlepart.TypeStream stream) {
            final List<String> variants = nodelayer.getVariantsList();
            final List<Integer> numentries = nodelayer.getNumentriesList();

            String layerName = "";

            if(nodelayer.getName().isEmpty()) {
                layerName = MemoryCoreNodeLayer.fromId(nodelayer.getBuiltinValue()).layer;
            } else {
                layerName = nodelayer.getName();
            }

            int[][] columnItems = new int[nodelayer.getKeysCount()][];
            ColumnReader[] columnReaders = new ColumnReader[nodelayer.getColumnsCount()];
            List<Singlepart.PropertyKey> keys = nodelayer.getKeysList();

            for (int i = 0; i < nodelayer.getSetsList().size(); i++) {
                Singlepart.PropertySet set = nodelayer.getSets(i);
                columnItems[i] = new int[set.getKeysCount()];
                for (int k = 0; k < set.getKeysCount(); k++) {
                    columnItems[i][k] = set.getKeys(k);
                }
            }

            for (int i = 0; i < keys.size(); i++) {
                Singlepart.PropertyKey propertyKey = keys.get(i);
                columnReaders[i] = new ColumnReader(nodelayer.getColumns(i));
                if(propertyKey.getName().isEmpty()) {
                    //Special property
                    switch (propertyKey.getSpecial()) {
                        case NODE_NUM_EDGE_OUT:
                            columnReaders[i].type = Singlepart.PropertyKey.DataType.D_INT;
                            break;
                        case NODE_RANGE_START_END:
                            columnReaders[i].type = Singlepart.PropertyKey.DataType.D_INT;
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
            final int numTotalNodes = stream.getStreamCount() == 0 ? numentries.stream().mapToInt(Integer::intValue).sum() : stream.getStreamCount() ;
            final boolean useDefaultAlways = stream.getStreamCount() == 0;

            for (int i = 0; i < variants.size(); i++) {
                int lastRange = 0;

                String variantName = nodelayer.getVariants(i);
                DocumentNodeLayer variantLayer = doc.store().nodeLayer(layerName, variantName.isEmpty() ? null : variantName);
                nodelayerid2layer.put(layerid, variantLayer);

                final int numNodes = numentries.get(i);
                ArrayList<NodeRef> nodeList = new ArrayList<>(numNodes);
                int j = 0;
                while(k < numTotalNodes && j < numNodes) {
                    int set = 0;
                    if(!useDefaultAlways)
                        set = stream.getStream(k)+lastStream;

                    lastStream = set;

                    int start = Integer.MIN_VALUE, end = Integer.MIN_VALUE;
                    ObjectArrayList<String> propKeys = new ObjectArrayList<>(columnItems[set].length);
                    ObjectArrayList<CoreRef> propValues = new ObjectArrayList<>(columnItems[set].length);
                    IntArrayList edgeData = new IntArrayList();

                    int[] columns = columnItems[set];
                    for (int pk : columns) {
                        final Singlepart.PropertyKey propertyKey = keys.get(pk);
                        final ColumnReader columnReader = columnReaders[pk];

                        if (propertyKey.getName().isEmpty()) {
                            //Special property
                            switch (propertyKey.getSpecial()) {
                                case NODE_NUM_EDGE_OUT:
                                    edgeData.add(propertyKey.getLayerid());
                                    edgeData.add(columnReader.pval.getIntValues(columnReader.index++));
                                    break;
                                case NODE_RANGE_START_END:
                                    int startpos = columnReader.pval.getIntValues(columnReader.index++) + lastRange;
                                    int endpos = columnReader.pval.getIntValues(columnReader.index++) + startpos;
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
            List<Singlepart.Nodes> nodesList = inputdoc.getNodesList();
            List<Singlepart.TypeStream> nodestreamsList = inputdoc.getNodestreamsList();
            int layerid = 0;
            for (int i = 0; i < nodesList.size(); i++) {
                layerid = decodeNodeLayer(layerid, nodesList.get(i), nodestreamsList.get(i));
            }
        }

        private int decodeEdges(int layerid, Singlepart.Edges edgelayer, Singlepart.TypeStream stream) {
            final List<String> variants = edgelayer.getVariantsList();
            final List<Integer> numentries = edgelayer.getNumentriesList();

            String layerName = "";

            if(edgelayer.getName().isEmpty()) {
                layerName = MemoryCoreEdgeLayer.fromId(edgelayer.getBuiltinValue()).layer;
            } else {
                layerName = edgelayer.getName();
            }

            int[][] columnItems = new int[edgelayer.getKeysCount()][];
            ColumnReader[] columnReaders = new ColumnReader[edgelayer.getColumnsCount()];
            List<Singlepart.PropertyKey> keys = edgelayer.getKeysList();

            for (int i = 0; i < edgelayer.getSetsList().size(); i++) {
                Singlepart.PropertySet set = edgelayer.getSets(i);
                columnItems[i] = new int[set.getKeysCount()];
                for (int k = 0; k < set.getKeysCount(); k++) {
                    columnItems[i][k] = set.getKeys(k);
                }
            }

            for (int i = 0; i < keys.size(); i++) {
                Singlepart.PropertyKey propertyKey = keys.get(i);
                columnReaders[i] = new ColumnReader(edgelayer.getColumns(i));
                if(propertyKey.getName().isEmpty()) {
                    //Special property
                    switch (propertyKey.getSpecial()) {
                        case EDGE_HEAD:
                            columnReaders[i].type = Singlepart.PropertyKey.DataType.D_INT;
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
            final int numTotalNodes = stream.getStreamCount() == 0 ? numentries.stream().mapToInt(Integer::intValue).sum() : stream.getStreamCount() ;
            final boolean useDefaultAlways = stream.getStreamCount() == 0;

            for (int i = 0; i < variants.size(); i++) {
                String variantName = edgelayer.getVariants(i);
                DocumentEdgeLayer variantLayer = doc.store().edgeLayer(layerName, variantName.isEmpty() ? null : variantName);
                edgelayerid2layer.put(layerid, variantLayer);

                final int numEdges = numentries.get(i);
                int j = 0;
                ArrayList<NodeRef> tailArray = getTailArray(layerid);
                while(k < numTotalNodes && j < numEdges) {
                    int set = 0;
                    if(!useDefaultAlways)
                        set = stream.getStream(k)+lastStream;

                    lastStream = set;

                    ObjectArrayList<String> propKeys = new ObjectArrayList<>(columnItems[set].length);
                    ObjectArrayList<CoreRef> propValues = new ObjectArrayList<>(columnItems[set].length);

                    NodeRef tail=tailArray.get(j);
                    NodeRef head=null;

                    int[] columns = columnItems[set];
                    for (int pk : columns) {
                        final Singlepart.PropertyKey propertyKey = keys.get(pk);
                        final ColumnReader columnReader = columnReaders[pk];

                        if (propertyKey.getName().isEmpty()) {
                            //Special property
                            switch (propertyKey.getSpecial()) {
                                case EDGE_HEAD:
                                    int targetLayer = propertyKey.getLayerid();
                                    int targetNode = columnReaders[pk].pval.getIntValues(columnReaders[pk].index++);
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
            List<Singlepart.Edges> edgeslist = inputdoc.getEdgesList();
            List<Singlepart.TypeStream> edgestreamlist = inputdoc.getEdgestreamsList();
            int layerid = 0;
            for (int i = 0; i < edgeslist.size(); i++) {
                layerid = decodeEdges(layerid, edgeslist.get(i), edgestreamlist.get(i));
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

package se.lth.cs.docforia.io.multipart;
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

import com.google.protobuf.ByteString;
import com.google.protobuf.ProtocolStringList;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.*;
import se.lth.cs.docforia.*;
import se.lth.cs.docforia.data.CoreRef;
import se.lth.cs.docforia.data.DataRef;
import se.lth.cs.docforia.data.StringRef;
import se.lth.cs.docforia.io.mem.Output;
import se.lth.cs.docforia.memstore.MemoryCoreEdgeLayer;
import se.lth.cs.docforia.memstore.MemoryCoreNodeLayer;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Customizable Memory Document encoder
 * <p>
 * <b>Remarks:</b> Once core properties has been initiated, instances of this class are thread safe for encoding.
 */
public class MultipartWriterV1 implements Serializable {
    private ObjectOpenHashSet<String> coreproperties = new ObjectOpenHashSet<>();

    protected static class Property {
        public byte type;
        public String key;

        public Property(byte type, String key) {
            this.type = type;
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Property property = (Property) o;

            if (type != property.type) return false;
            return key != null ? key.equals(property.key) : property.key == null;

        }

        @Override
        public int hashCode() {
            int result = key != null ? key.hashCode() : 0;
            result = 31 * result + (int) type;
            return result;
        }
    }

    private static class BuildContext {
        Object2IntLinkedOpenHashMap<String> nodeLayers = new Object2IntLinkedOpenHashMap<>();
        Object2IntLinkedOpenHashMap<String> edgeLayers = new Object2IntLinkedOpenHashMap<>();
        Object2IntOpenHashMap<String> numNodesLayer = new Object2IntOpenHashMap<>();
        Object2IntOpenHashMap<NodeRef> nodeIndex = new Object2IntOpenHashMap<>();
    }

    /**
     * Default constructor, separates
     */
    public MultipartWriterV1() {

    }

    public void addCoreProperty(String propertyKey) {
        coreproperties.add(propertyKey);
    }

    private static ArrayList<String> nodeLayers(Document doc) {
        ArrayList<String> layers = new ArrayList<>();

        String lastLayer = "";
        for (DocumentNodeLayer nodeLayer : doc.store().nodeLayers()) {
            if(!lastLayer.equals(nodeLayer.getLayer())) {
                layers.add(nodeLayer.getLayer());
                lastLayer = nodeLayer.getLayer();
            }
        }
        return layers;
    }

    private static ArrayList<String> edgeLayers(Document doc) {
        ArrayList<String> layers = new ArrayList<>();

        String lastLayer = "";
        for (DocumentEdgeLayer nodeLayer : doc.store().edgeLayers()) {
            if(!lastLayer.equals(nodeLayer.getLayer())) {
                layers.add(nodeLayer.getLayer());
                lastLayer = nodeLayer.getLayer();
            }
        }
        return layers;
    }

    private static IntAVLTreeSet findEdgeLayerDependencies(Document doc, Object2IntLinkedOpenHashMap<String> layer2id, String edgeLayer) {
        IntAVLTreeSet uniqueLayers = new IntAVLTreeSet();
        for (String variant : doc.engine().edgeLayerVariants(edgeLayer)) {
            for (EdgeRef edgeRef : doc.engine().edges(edgeLayer, variant)) {
                EdgeStore store = edgeRef.get();
                uniqueLayers.add(layer2id.getInt(store.getHead().layer().getLayer()));
                uniqueLayers.add(layer2id.getInt(store.getTail().layer().getLayer()));
            }
        }

        return uniqueLayers;
    }

    private MultipartBinary.Properties.Entry convert(String key, DataRef value) {
        MultipartBinary.Properties.Entry.Builder entry = MultipartBinary.Properties.Entry.newBuilder();
        entry.setKey(key);

        MultipartBinary.Value.Builder valueBuilder = MultipartBinary.Value.newBuilder();

        if(!(value instanceof CoreRef)) {
            throw new UnsupportedOperationException("Non Core-Ref properties are not supported, when converting property with key: " + key + ", data: " + value.stringValue());
        }

        CoreRef corevalue = (CoreRef)value;

        valueBuilder.setType(MultipartBinary.ValueType.valueOf(Byte.toUnsignedInt(corevalue.id().value)));
        switch (corevalue.id()) {
            case STRING:
                valueBuilder.setStringValue(corevalue.stringValue());
                break;
            case INT:
                valueBuilder.setIntValue(corevalue.intValue());
                break;
            case LONG:
                valueBuilder.setLongValue(corevalue.longValue());
                break;
            case BOOLEAN:
                valueBuilder.setBoolValue(corevalue.booleanValue());
                break;
            case FLOAT:
                valueBuilder.setFloatValue(corevalue.floatValue());
                break;
            case DOUBLE:
                valueBuilder.setDoubleValue(corevalue.doubleValue());
                break;
            default:
                Output binaryData = new Output(32, 1<<30);
                corevalue.write(binaryData);
                valueBuilder.setBinaryValue(ByteString.copyFrom(binaryData.getBuffer(), 0, binaryData.position()));
                break;
        }

        entry.setValue(valueBuilder);
        return entry.build();
    }

    private MultipartBinary.Header buildHeader(Document doc, MultipartBinary.Header prevheader, BuildContext context) {
        Output headerWriter = new Output(256, 256<<20);
        MultipartBinary.Header.Builder builder = prevheader != null ?
                MultipartBinary.Header.newBuilder(prevheader) : MultipartBinary.Header.newBuilder();

        if(doc.hasProperty(Document.PROP_URI))
            builder.addAllUri(Arrays.asList(doc.getStringArrayProperty(Document.PROP_URI)));

        if(doc.id() != null)
            builder.setId(doc.id());

        if(doc.language() != null)
            builder.setLang(doc.language());

        if(doc.type() != null)
            builder.setType(doc.type());

        builder.setLength(doc.length());

        if(prevheader != null) {
            ProtocolStringList nodeLayerList = prevheader.getNodeLayerList();
            for (int i = 0; i < nodeLayerList.size(); i++) {
                context.nodeLayers.add(nodeLayerList.get(i), i);
            }
        }

        //Layers - find ids and dependencies
        int i = 0;
        for (String s : nodeLayers(doc)) {
            if(!context.nodeLayers.containsKey(s)) {
                context.nodeLayers.put(s, builder.getNodeLayerCount());
                builder.addNodeLayer(s);
            }
            i++;
        }

        for (String s : edgeLayers(doc)) {
            if(!context.edgeLayers.containsKey(s)) {
                context.edgeLayers.put(s, builder.getNodeLayerCount()+builder.getEdgeLayerCount());
                builder.addEdgeLayer(s);
            }

            IntAVLTreeSet deps = findEdgeLayerDependencies(doc, context.nodeLayers, s);
            if(builder.getEdgeLayerDepsCount() <= i) {
                builder.addEdgeLayerDeps(MultipartBinary.Header.Dependency.newBuilder().addAllNodeLayerId(deps).build());
            } else {
                builder.setEdgeLayerDeps(i, MultipartBinary.Header.Dependency.newBuilder().addAllNodeLayerId(deps).build());
            }
            i++;
        }

        MultipartBinary.Properties.Builder coreprops = MultipartBinary.Properties.newBuilder();
        for (String coreproperty : coreproperties) {
            DataRef property = doc.getRefProperty(coreproperty);
            coreprops.addEntry(convert(coreproperty, property));
        }

        builder.setCoreproperties(coreprops.build());

        return builder.build();
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

        if(data.getBuffer()[0] == 0) {
            output.writeByte((byte)0xFF);
        }
        else if(data.getBuffer()[0] == (byte)0xFF) {
            output.writeByte((byte)0xFF);
        }
        else if(data.getBuffer()[0] == (byte)0xFE) {
            output.writeByte((byte)0xFF);
        }

        data.writeTo(output);
    }

    private MultipartBinary.PropertyColumn encodePropertyColumn(Property prop, Int2ObjectArrayMap<CoreRef> values) {
        MultipartBinary.PropertyColumn.Builder colBuilder = MultipartBinary.PropertyColumn.newBuilder();
        colBuilder.setKey(prop.key);
        colBuilder.setType(MultipartBinary.ValueType.valueOf(Byte.toUnsignedInt(prop.type)));

        //1. Write layout description, NLE (Null-Length-Encoding), min 3 delta
        Output writer = new Output(32, 1<<30); //Max 1 GB

        //0x00 == skip, 0xFF == escaped byte follows
        int lastIndex = 0;

        for (Int2ObjectMap.Entry<CoreRef> entry : values.int2ObjectEntrySet()) {
            if(entry.getIntKey() - lastIndex >= 3) {
                writer.writeByte((byte)0xFE);
                writer.writeVarInt(entry.getIntKey() - lastIndex, true);
            } else {
                for(int i = 0; i < entry.getIntKey() - lastIndex; i++) {
                    writer.writeByte(0);
                }
            }

            writeEscapedProperty(writer, entry.getValue());
            lastIndex = entry.getIntKey()+1;
        }

        colBuilder.setData(ByteString.copyFrom(writer.getBuffer(), 0, writer.position()));
        return colBuilder.build();
    }

    private MultipartBinary.NodeLayer buildNodeLayer(Document doc, BuildContext context, String nodeLayer) {
        MultipartBinary.NodeLayer.Builder nodeLayerBuilder = MultipartBinary.NodeLayer.newBuilder();
        nodeLayerBuilder.setIdx(context.nodeLayers.getInt(nodeLayer));

        MemoryCoreNodeLayer memoryCoreNodeLayer = MemoryCoreNodeLayer.fromLayerName(nodeLayer);
        if(memoryCoreNodeLayer == MemoryCoreNodeLayer.UNKNOWN) {
            nodeLayerBuilder.setUserdefined(nodeLayer);
        } else {
            nodeLayerBuilder.setId(memoryCoreNodeLayer.id);
        }

        int nodeCounter = 0;
        Object2ObjectOpenHashMap<Property, Int2ObjectArrayMap<CoreRef>> property2indicies = new Object2ObjectOpenHashMap<>();

        for (Optional<String> variant : doc.engine().nodeLayerAllVariants(nodeLayer)) {
            MultipartBinary.NodeLayer.Variant.Builder variantBuilder = MultipartBinary.NodeLayer.Variant.newBuilder();
            if(variant.isPresent())
                variantBuilder.setName(variant.get());

            DocumentNodeLayer nodeRefs = doc.store().nodeLayer(nodeLayer, variant.orElse(null));
            variantBuilder.setNumNodes(nodeRefs.size());
            int currentRange = 0;
            for (NodeRef nodeRef : nodeRefs) {
                NodeStore store = nodeRef.get();
                for (Map.Entry<String, DataRef> entry : store.properties()) {
                    if(!(entry.getValue() instanceof CoreRef)) {
                        throw new UnsupportedOperationException(
                                "In layer " + nodeLayer + "#" + variant + ", node "
                                        + nodeCounter + " contains a property "
                                        + entry.getKey() + " that is not a CoreRef this is unsupported");
                    }

                    CoreRef corevalue  = (CoreRef)entry.getValue();
                    Property prop = new Property(corevalue.id().value, entry.getKey());
                    Int2ObjectArrayMap<CoreRef> columns = property2indicies.get(prop);
                    if(columns == null) {
                        property2indicies.put(prop, columns = new Int2ObjectArrayMap<>());
                    }

                    columns.put(nodeCounter, corevalue);
                }

                if(store.isAnnotation()) {
                    variantBuilder.addRanges(store.getStart() - currentRange);
                    currentRange = store.getStart();
                    variantBuilder.addRanges(store.getEnd() - currentRange);
                    currentRange = store.getEnd();
                }

                context.nodeIndex.put(store, nodeCounter);

                nodeCounter++;
            }

            nodeLayerBuilder.addVariants(variantBuilder);
        }

        context.numNodesLayer.put(nodeLayer, nodeCounter);

        for (Object2ObjectMap.Entry<Property, Int2ObjectArrayMap<CoreRef>> entry : property2indicies.object2ObjectEntrySet()) {
            nodeLayerBuilder.addProperties(encodePropertyColumn(entry.getKey(), entry.getValue()));
        }

        return nodeLayerBuilder.build();
    }

    private static int compareLayers(DocumentNodeLayer x, DocumentNodeLayer y) {
        String xlayer = x.getLayer();
        String ylayer = y.getLayer();

        int result = xlayer.compareTo(ylayer);
        if(result == 0)
        {
            String xvariant = x.getVariant();
            String yvariant = y.getVariant();

            if(xvariant == null && yvariant == null)
                return 0;
            else if(xvariant == null)
                return -1;
            else if(yvariant == null)
                return 1;
            else
                return xvariant.compareTo(yvariant);
        }
        else
            return result;
    }

    private static int compareEdges(EdgeRef x, EdgeRef y) {
        EdgeStore xv = x.get();
        EdgeStore yv = y.get();

        int result = compareLayers(xv.getHead().layer(), yv.getHead().layer());
        if(result == 0)
            return compareLayers(xv.getTail().layer(), yv.getTail().layer());
        else
            return result;
    }

    private MultipartBinary.EdgeLayer buildEdgeLayer(Document doc, BuildContext context, String edgeLayer) {
        MultipartBinary.EdgeLayer.Builder edgeLayerBuilder = MultipartBinary.EdgeLayer.newBuilder();
        edgeLayerBuilder.setIdx(context.edgeLayers.getInt(edgeLayer));

        MemoryCoreEdgeLayer memoryCoreEdgeLayer = MemoryCoreEdgeLayer.fromLayerName(edgeLayer);
        if(memoryCoreEdgeLayer == MemoryCoreEdgeLayer.UNKNOWN) {
            edgeLayerBuilder.setUserdefined(edgeLayer);
        } else {
            edgeLayerBuilder.setId(memoryCoreEdgeLayer.id);
        }

        Object2ObjectOpenHashMap<Property, Int2ObjectArrayMap<CoreRef>> property2indicies = new Object2ObjectOpenHashMap<>();

        int edgeCounter = 0;

        for (Optional<String> variant : doc.engine().edgeLayerAllVariants(edgeLayer)) {
            MultipartBinary.EdgeLayer.Variant.Builder variantBuilder = MultipartBinary.EdgeLayer.Variant.newBuilder();

            if(variant.isPresent())
                variantBuilder.setName(variant.get());

            DocumentEdgeLayer edgeRefs = doc.store().edgeLayer(edgeLayer, variant.orElse(null));
            List<EdgeRef> edges = StreamSupport.stream(edgeRefs.spliterator(), false).collect(Collectors.toList());
            Collections.sort(edges, MultipartWriterV1::compareEdges);

            IntArrayList layerids = new IntArrayList();
            Int2IntOpenHashMap layerstarts = new Int2IntOpenHashMap();
            int allocatedStarts = 0;

            for (EdgeRef edge : edges) {
                EdgeStore edgeStore = edge.get();
                String headLayer = edgeStore.getHead().layer().getLayer();
                if(!layerstarts.containsKey(context.nodeLayers.getInt(headLayer))) {
                    int layerid = context.nodeLayers.getInt(headLayer);
                    layerids.add(layerid);
                    layerstarts.put(layerid, allocatedStarts);

                    allocatedStarts += context.numNodesLayer.getInt(headLayer);
                }
            }

            for (EdgeRef edge : edges) {
                EdgeStore edgeStore = edge.get();
                String tailLayer = edgeStore.getTail().layer().getLayer();
                if(!layerstarts.containsKey(context.nodeLayers.getInt(tailLayer))) {
                    int layerid = context.nodeLayers.getInt(tailLayer);
                    layerids.add(layerid);
                    layerstarts.put(layerid, allocatedStarts);

                    allocatedStarts += context.numNodesLayer.getInt(tailLayer);
                }
            }

            int lastHead = 0;
            int lastTail = 0;

            for (EdgeRef edge : edges) {
                EdgeStore store = edge.get();
                for (Map.Entry<String, DataRef> entry : store.properties()) {
                    if(!(entry.getValue() instanceof CoreRef)) {
                        throw new UnsupportedOperationException(
                                "In edge layer " + edgeLayer + "#" + variant + ", node "
                                        + edgeCounter + " contains a property "
                                        + entry.getKey() + " that is not a CoreRef this is unsupported");
                    }

                    CoreRef corevalue  = (CoreRef)entry.getValue();
                    Property prop = new Property(corevalue.id().value, entry.getKey());
                    Int2ObjectArrayMap<CoreRef> columns = property2indicies.get(prop);
                    if(columns == null) {
                        property2indicies.put(prop, columns = new Int2ObjectArrayMap<>());
                    }

                    columns.put(edgeCounter, corevalue);
                }

                int layerId = context.nodeLayers.getInt(store.getHead().layer().getLayer());
                int id = context.nodeIndex.getInt(store.getHead())+layerstarts.get(layerId);

                variantBuilder.addHead(id - lastHead);
                lastHead = id;

                layerId = context.nodeLayers.getInt(store.getTail().layer().getLayer());
                id = context.nodeIndex.getInt(store.getTail())+layerstarts.get(layerId);
                variantBuilder.addTail(id - lastTail);
                lastTail = id;

                edgeCounter++;
            }

            IntListIterator iter = layerids.iterator();
            while(iter.hasNext()) {
                int layerid;
                variantBuilder.addNodestarts(layerstarts.get(layerid = iter.nextInt()));
                variantBuilder.addNodelayers(layerid);
            }

            edgeLayerBuilder.addVariants(variantBuilder.build());
        }

        for (Object2ObjectMap.Entry<Property, Int2ObjectArrayMap<CoreRef>> entry : property2indicies.object2ObjectEntrySet()) {
            edgeLayerBuilder.addProperties(encodePropertyColumn(entry.getKey(), entry.getValue()));
        }

        return edgeLayerBuilder.build();
    }

    private MultipartBinary.Properties buildProperties(Document doc, BuildContext context) {
        MultipartBinary.Properties.Builder propBuilder = MultipartBinary.Properties.newBuilder();

        for (Map.Entry<String, DataRef> entry : doc.properties()) {
            if(coreproperties.contains(entry.getKey()) || Document.PROP_ALL.contains(entry.getKey()))
                continue;

            propBuilder.addEntry(convert(entry.getKey(), entry.getValue()));
        }

        return propBuilder.build();
    }

    /**
     * Encode given document
     * @param doc      the document to encode
     * @param messages previous messages to merge, may be null
     */
    public MultipartMessages encode(Document doc, MultipartMessages messages) {
        MultipartMessages output = messages != null ? messages : new MultipartMessages();

        BuildContext context = new BuildContext();
        output.header = buildHeader(doc, output.header, context);
        output.text = doc.text();

        output.properties = buildProperties(doc, context);

        for (String nodeLayer : nodeLayers(doc)) {
            MultipartBinary.NodeLayer nodeLayerMessage = buildNodeLayer(doc, context, nodeLayer);
            output.nodeLayers.put(nodeLayer, nodeLayerMessage);
        }

        for (String edgeLayer : edgeLayers(doc)) {
            MultipartBinary.EdgeLayer edgeLayerMessage = buildEdgeLayer(doc, context, edgeLayer);
            output.edgeLayers.put(edgeLayer, edgeLayerMessage);
        }

        return output;
    }
}

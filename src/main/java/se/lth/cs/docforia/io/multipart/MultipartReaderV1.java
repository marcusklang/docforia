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

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import se.lth.cs.docforia.*;
import se.lth.cs.docforia.data.*;
import se.lth.cs.docforia.io.mem.Input;
import se.lth.cs.docforia.memstore.MemoryCoreEdgeLayer;
import se.lth.cs.docforia.memstore.MemoryCoreNodeLayer;

import java.util.Arrays;

/**
 * Customizable Memory Document decoder
 */
public class MultipartReaderV1 {

    private static DataRef convert(MultipartBinary.Value value) {
        MultipartBinary.ValueType type = value.getType();
        switch (type) {
            case BINARY:
                return new BinaryRef(value.getBinaryValue().toByteArray());
            case STRING:
                return new StringRef(value.getStringValue());
            case INT:
                return new IntRef(value.getIntValue());
            case LONG:
                return new LongRef(value.getLongValue());
            case FLOAT:
                return new FloatRef(value.getFloatValue());
            case DOUBLE:
                return new DoubleRef(value.getDoubleValue());
            case BOOLEAN:
                return value.getBoolValue() ? BooleanRef.TRUE : BooleanRef.FALSE;
            default:
                return CoreRefType.fromByteValue((byte)type.getNumber()).read(new Input(value.getBinaryValue().toByteArray()));
        }
    }

    private static class DecodeContext {
        public Int2ObjectOpenHashMap<DocumentNodeLayer> id2nodelayer = new Int2ObjectOpenHashMap<>();
        public Int2ObjectOpenHashMap<ObjectArrayList<NodeRef>> id2index2noderef = new Int2ObjectOpenHashMap<>();
    }

    private static void decodePropertyColumn(MultipartBinary.PropertyColumn column, ObjectArrayList<? extends StoreRef> entries) {
        final String key = column.getKey();
        Int2ObjectArrayMap<DataRef> outputs = new Int2ObjectArrayMap<>();
        Input input = new Input(column.getData().toByteArray());
        CoreRefType type = CoreRefType.fromByteValue((byte)column.getType().getNumber());
        int i = 0;
        while(!input.eof()) {
            byte b = input.readByte();
            if(b == 0x0) {
                i++;
            }
            else if(b == (byte)0xFF) {
                entries.get(i).get().putProperty(key, type.read(input));
                i++;
            }
            else if(b == (byte)0xFE) {
                i += input.readVarInt(true);
            } else {
                input.setPosition(input.position()-1);
                entries.get(i).get().putProperty(key, type.read(input));
                i++;
            }
        }
    }

    private static void decodeNodeLayer(MultipartBinary.NodeLayer nodeLayer, Document target, DecodeContext context) {
        int idx = nodeLayer.getIdx();
        ObjectArrayList<NodeRef> nodeRefs = new ObjectArrayList<>();

        String layer;

        if(nodeLayer.hasId()) {
            layer = MemoryCoreNodeLayer.fromId(nodeLayer.getId()).layer;
        } else {
            layer = nodeLayer.getUserdefined();
        }

        for (MultipartBinary.NodeLayer.Variant variant : nodeLayer.getVariantsList()) {
            String variantName = null;
            if(variant.hasName())
                variantName = variant.getName();

            DocumentNodeLayer nodeLayerStore = target.store().nodeLayer(layer, variantName);

            int numPureNodes = variant.getNumNodes() - (variant.getRangesCount()>>1);
            for (int i = 0; i < numPureNodes; i++) {
                nodeRefs.add(nodeLayerStore.create());
            }

            int lastPos = 0;
            final int numRanges = variant.getRangesCount()>>1;
            for (int i = 0; i < numRanges; i++) {
                int start = variant.getRanges(i*2) + lastPos;
                lastPos = start;
                int end = variant.getRanges(i*2+1) + lastPos;
                lastPos = end;

                nodeRefs.add(nodeLayerStore.create(start,end));
            }
        }

        for (MultipartBinary.PropertyColumn propertyColumn : nodeLayer.getPropertiesList()) {
            decodePropertyColumn(propertyColumn, nodeRefs);
        }

        context.id2index2noderef.put(idx, nodeRefs);
    }

    private static void decodeEdgeLayer(MultipartBinary.EdgeLayer edgeLayer, Document target, DecodeContext context) {
        int idx = edgeLayer.getIdx();
        ObjectArrayList<EdgeRef> edgeRefs = new ObjectArrayList<>();

        String layer;

        if(edgeLayer.hasId()) {
            layer = MemoryCoreEdgeLayer.fromId(edgeLayer.getId()).layer;
        } else {
            layer = edgeLayer.getUserdefined();
        }

        for (MultipartBinary.EdgeLayer.Variant variant : edgeLayer.getVariantsList()) {
            String variantName = null;
            if(variant.hasName())
                variantName = variant.getName();

            DocumentEdgeLayer edgeLayerStore = target.store().edgeLayer(layer, variantName);

            int lastHead = 0;
            int lastTail = 0;
            final int numEdges = variant.getHeadCount();

            int[] starts = variant.getNodestartsList().stream().mapToInt(Integer::intValue).toArray();
            int[] layerids = variant.getNodelayersList().stream().mapToInt(Integer::intValue).toArray();

            for (int i = 0; i < numEdges; i++) {
                int head = variant.getHead(i) + lastHead;
                lastHead = head;

                int tail = variant.getTail(i) + lastTail;
                lastTail = tail;

                int headLayer = Arrays.binarySearch(starts, head);
                int tailLayer = Arrays.binarySearch(starts, tail);

                if(headLayer < 0)
                     headLayer = -headLayer-2;

                if(tailLayer < 0)
                    tailLayer = -tailLayer-2;

                head -= starts[headLayer];
                tail -= starts[tailLayer];

                edgeRefs.add(edgeLayerStore.create(context.id2index2noderef.get(layerids[tailLayer]).get(tail),
                                                   context.id2index2noderef.get(layerids[headLayer]).get(head)));
            }
        }

        for (MultipartBinary.PropertyColumn propertyColumn : edgeLayer.getPropertiesList()) {
            decodePropertyColumn(propertyColumn, edgeRefs);
        }
    }

    private static void decodeProperties(MultipartBinary.Properties properties, Document target) {
        for (MultipartBinary.Properties.Entry entry : properties.getEntryList()) {
            target.putProperty(entry.getKey(), convert(entry.getValue()));
        }
    }

    /**
     * Decode document from messages given a factory
     * @param messages data to decode
     * @param factory document factory
     */
    public static Document decode(MultipartMessages messages, DocumentFactory factory) {
        //Build the document
        Document doc = factory.create();
        if(messages.text == null)
            doc.setLength(messages.header.getLength());
        else
            doc.setText(messages.text);

        if(messages.header.hasId())
            doc.setId(messages.header.getId());

        if(messages.header.getUriCount() > 0)
            doc.setUris(messages.header.getUriList().toArray(new String[messages.header.getUriCount()]));

        if(messages.header.hasLang())
            doc.setLanguage(messages.header.getLang());

        if(messages.header.hasType())
            doc.setType(messages.header.getType());

        for (MultipartBinary.Properties.Entry entry : messages.header.getCoreproperties().getEntryList()) {
            doc.putProperty(entry.getKey(), convert(entry.getValue()));
        }

        if(messages.properties != null)
            decodeProperties(messages.properties, doc);

        DecodeContext context = new DecodeContext();
        for (MultipartBinary.NodeLayer nodeLayer : messages.nodeLayers.values()) {
            decodeNodeLayer(nodeLayer, doc, context);
        }

        for (MultipartBinary.EdgeLayer edgeLayer : messages.edgeLayers.values()) {
            decodeEdgeLayer(edgeLayer, doc, context);
        }

        return doc;
    }
}

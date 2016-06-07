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


import com.google.protobuf.InvalidProtocolBufferException;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import se.lth.cs.docforia.io.mem.Input;
import se.lth.cs.docforia.io.mem.Output;
import se.lth.cs.docforia.memstore.MemoryCoreEdgeLayer;
import se.lth.cs.docforia.memstore.MemoryCoreNodeLayer;

import java.io.IOError;
import java.util.Arrays;

/**
 * Multipart Message container - contains the intermediate encoded form of multipart encoding
 */
public class MultipartMessages {
    protected static final byte[] MAGIC = new byte[] { 'D', 'M', 'P', '1'};

    protected MultipartBinary.Header header;
    protected Object2ObjectOpenHashMap<String,MultipartBinary.NodeLayer> nodeLayers = new Object2ObjectOpenHashMap<>();
    protected Object2ObjectOpenHashMap<String,MultipartBinary.EdgeLayer> edgeLayers = new Object2ObjectOpenHashMap<>();
    protected MultipartBinary.Properties properties;
    protected String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setHeader(byte[] header) {
        try {
            this.header = MultipartBinary.Header.parseFrom(header);
        } catch (InvalidProtocolBufferException e) {
            throw new IOError(e);
        }
    }

    public byte[] getHeader() {
        return header.toByteArray();
    }

    public ObjectSet<String> nodeLayers() {
        return nodeLayers.keySet();
    }

    public ObjectSet<String> edgeLayers() {
        return edgeLayers.keySet();
    }

    public void setProperties(byte[] properties) {
        try {
            this.properties = MultipartBinary.Properties.parseFrom(properties);
        } catch (InvalidProtocolBufferException e) {
            throw new IOError(e);
        }
    }

    public byte[] getProperties() {
        return properties != null ? properties.toByteArray() : null;
    }

    public byte[] getNodeLayer(String nodeLayer) {
        return nodeLayers.get(nodeLayer).toByteArray();
    }

    public byte[] getEdgeLayer(String edgeLayer) {
        return edgeLayers.get(edgeLayer).toByteArray();
    }

    public void addNodeLayer(byte[] layer) {
        try {
            MultipartBinary.NodeLayer nodeLayer = MultipartBinary.NodeLayer.parseFrom(layer);
            if(nodeLayer.hasId()) {
                nodeLayers.put(MemoryCoreNodeLayer.fromId(nodeLayer.getId()).layer, nodeLayer);
            } else {
                nodeLayers.put(nodeLayer.getUserdefined(), nodeLayer);
            }
        } catch (InvalidProtocolBufferException e) {
            throw new IOError(e);
        }
    }

    public void addEdgeLayer(byte[] layer) {
        try {
            MultipartBinary.EdgeLayer edgeLayer = MultipartBinary.EdgeLayer.parseFrom(layer);
            if(edgeLayer.hasId()) {
                edgeLayers.put(MemoryCoreEdgeLayer.fromId(edgeLayer.getId()).layer, edgeLayer);
            } else {
                edgeLayers.put(edgeLayer.getUserdefined(), edgeLayer);
            }
        } catch (InvalidProtocolBufferException e) {
            throw new IOError(e);
        }
    }

    public static MultipartMessages fromBytes(byte[] bytes) {
        MultipartMessages messages = new MultipartMessages();

        Input input = new Input(bytes);
        byte[] header = new byte[4];
        input.readBytes(header);
        if(!Arrays.equals(MAGIC, header)) {
            throw new UnsupportedOperationException("Invalid format of given bytes");
        }

        try {
            int headerSize = input.readVarInt(true);
            messages.header = MultipartBinary.Header.parseFrom(input.readBytes(headerSize));
            messages.text = input.readString();
            if(messages.text.equals(""))
                messages.text = null;

            int propertySize = input.readVarInt(true);
            if(propertySize > 0) {
                messages.properties = MultipartBinary.Properties.parseFrom(input.readBytes(propertySize));
            }

            int numNodeLayers = input.readVarInt(true);
            int numEdgeLayers = input.readVarInt(true);

            for (int i = 0; i < numNodeLayers; i++) {
                int layerSize = input.readVarInt(true);
                MultipartBinary.NodeLayer nodeLayer = MultipartBinary.NodeLayer.parseFrom(input.readBytes(layerSize));
                if(nodeLayer.hasId()) {
                    messages.nodeLayers.put(MemoryCoreNodeLayer.fromId(nodeLayer.getId()).layer, nodeLayer);
                } else {
                    messages.nodeLayers.put(nodeLayer.getUserdefined(), nodeLayer);
                }
            }

            for (int i = 0; i < numEdgeLayers; i++) {
                int layerSize = input.readVarInt(true);
                MultipartBinary.EdgeLayer edgeLayer = MultipartBinary.EdgeLayer.parseFrom(input.readBytes(layerSize));
                if(edgeLayer.hasId()) {
                    messages.edgeLayers.put(MemoryCoreEdgeLayer.fromId(edgeLayer.getId()).layer, edgeLayer);
                } else {
                    messages.edgeLayers.put(edgeLayer.getUserdefined(), edgeLayer);
                }
            }

        } catch (InvalidProtocolBufferException e) {
            throw new IOError(e);
        }

        return messages;
    }

    public byte[] toBytes() {
        Output output = new Output(256, 1<<30); //Max 1 GB
        output.writeBytes(MAGIC);
        {
            byte[] headerData = header.toByteArray();
            output.writeVarInt(headerData.length, true);
            output.writeBytes(headerData);
        }

        output.writeString(text == null ? "" : text);

        {
            if(properties == null) {
                output.writeVarInt(0, true);
            } else {
                byte[] propertyData = properties.toByteArray();
                output.writeVarInt(propertyData.length, true);
                output.writeBytes(propertyData);
            }
        }

        output.writeVarInt(nodeLayers.size(), true);
        output.writeVarInt(edgeLayers.size(), true);

        for (Object2ObjectMap.Entry<String, MultipartBinary.NodeLayer> entry : nodeLayers.object2ObjectEntrySet()) {
            MultipartBinary.NodeLayer layer = entry.getValue();
            byte[] layerData = layer.toByteArray();
            output.writeVarInt(layerData.length, true);
            output.writeBytes(layerData);
        }

        for (Object2ObjectMap.Entry<String, MultipartBinary.EdgeLayer> entry : edgeLayers.object2ObjectEntrySet()) {
            MultipartBinary.EdgeLayer layer = entry.getValue();
            byte[] layerData = layer.toByteArray();
            output.writeVarInt(layerData.length, true);
            output.writeBytes(layerData);
        }

        return output.toBytes();
    }
}

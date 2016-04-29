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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOError;
import java.io.IOException;
import java.util.Iterator;

/**
 * Memory Document JSON Codec
 */
public class MemoryJson {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static MemoryDocument decodeJson(JsonNode jsonNode) {
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
        switch (version) {
            case "DM10":
                return MemoryJsonLevel0Codec.INSTANCE.decode(jsonNode);
            default:
                throw new UnsupportedOperationException("Cannot decode " + version + " format.");
        }
    }

    public static MemoryDocument decodeJson(String json) {
        try {
            JsonNode jsonNode = mapper.readTree(json);
            return decodeJson(jsonNode);
        } catch (IOException e) {
            //System.out.println(json.substring(3817700, json.length()));
            throw new IOError(e);
        }
    }
}

package se.lth.cs.docforia.memstore.encoders;
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

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import se.lth.cs.docforia.StoreRef;
import se.lth.cs.docforia.data.DataRef;
import se.lth.cs.docforia.io.mem.Output;

/**
 * String field encoder
 */
public class StringFieldEncoder {
    boolean equalLength = true;
    boolean tooManyUnique = false;
    boolean numbers = true;
    int lastlen = -1;
    int count = 0;
    int fullReuse = 0;
    int prefixReuse = 0;

    Object2IntLinkedOpenHashMap<String> uniqueTokens = new Object2IntLinkedOpenHashMap<>();

    public static int comparePrefixSubstring(String text, int start, int end, String property) {
        int i = 0;
        int k = start;
        final int nodelen = end-start;
        final int len = Math.min(nodelen, property.length());
        while(i < len) {
            if(text.charAt(k) != property.charAt(i))
                break;

            i++;
            k++;
        }

        if(i == 0)
            return -1;
        else if(i == nodelen) {
            if(property.length() > i)
                return i;
            else
                return 0;
        }
        else {
            return i;
        }
    }


    public static boolean isNumber(String str) {
        return str.chars().allMatch(i -> i >= '\u0030' && i <='\u0039') && !(str.length() > 1 && str.charAt(0) != '0');
    }

    private StringCodec getEncoder() {
        if(!tooManyUnique && count > 3 && uniqueTokens.size() != count) {
            if(equalLength) {
                return EqualLenDictStringCodec.INSTANCE;

            } else {
                return VariableLenDictStringCodec.INSTANCE;
            }
        }
        else if(equalLength && count >= 3) {
            return EqualLenStringCodec.INSTANCE;
        } else {
            return BaselineStringCodec.INSTANCE;
        }
    }

    public void encodeNodeProperties(Output writer, String key, String text, Iterable<? extends StoreRef> entries) {
        //Probe properties
        for (StoreRef storeRef : entries) {
            DataRef property = storeRef.get().getRefProperty(key);
            if (lastlen == -1) {
                lastlen = property.stringValue().length();
            }

            equalLength &= property.stringValue().length() == lastlen;
            count++;

            if (uniqueTokens.size() < 127) {
                if (!uniqueTokens.containsKey(property.stringValue())) {
                    uniqueTokens.put(property.stringValue(), uniqueTokens.size());
                }
            } else {
                tooManyUnique = true;
            }
/*
            NodeStore nodeStore = ((NodeRef) storeRef).get();
            if (nodeStore.isAnnotation()) {
                int result = comparePrefixSubstring(text, nodeStore.getStart(), nodeStore.getEnd(), property.stringValue());
                if (result == 0)
                    fullReuse++;
                else if (result > 0)
                    prefixReuse++;
            }
*/
        }

        getEncoder().encode(writer, text, key, this, entries);
    }

    public void encodeEdgeProperties(Output writer, String key, String text, Iterable<? extends StoreRef> entries) {
        //Probe properties
        for (StoreRef storeRef : entries) {
            DataRef property = storeRef.get().getRefProperty(key);
            if (lastlen == -1) {
                lastlen = property.stringValue().length();
            }

            equalLength &= property.stringValue().length() == lastlen;
            count++;

            if (uniqueTokens.size() < 127) {
                if (!uniqueTokens.containsKey(property.stringValue())) {
                    uniqueTokens.put(property.stringValue(), uniqueTokens.size());
                }
            } else {
                tooManyUnique = true;
            }
        }

        getEncoder().encode(writer, text, key, this, entries);
    }
}

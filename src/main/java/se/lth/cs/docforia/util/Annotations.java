package se.lth.cs.docforia.util;
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

import se.lth.cs.docforia.Node;
import se.lth.cs.docforia.NodeStore;

/**
 * Annotation utiltiy functions
 */
public class Annotations {
    /**
     * Extract context with [ and ] surrounding the annotation
     * @param node the annotation
     * @param size size of annotation
     * @return
     */
    public static String context(Node node, int size, boolean centering) {
        return context(node, "[", "]", size, centering);
    }

    /**
     * Extract context with [ and ] surrounding the annotation
     * @param node the annotation
     * @param size size of annotation
     * @return
     */
    public static String context(Node node, int size) {
        return context(node, "[", "]", size, false);
    }

    /**
     * Get a string with the context
     * @param node    the annotation
     * @param prepend prepend this before the annotation
     * @param append  append this after the annotation
     * @param size    context window size in chars
     * @return context
     */
    public static String context(Node node, String prepend, String append, int size, boolean centering) {
        return context(node, prepend, append, size, true, centering);
    }

    /**
     * Get a string with the context
     * @param node    the annotation
     * @param prepend prepend this before the annotation
     * @param append  append this after the annotation
     * @param size    context window size in chars
     * @param nlToS   new lines to spaces
     * @return context
     */
    public static String context(Node node, String prepend, String append, int size, boolean nlToS, boolean centering) {
        if(!node.isAnnotation()) {
            throw new IllegalArgumentException("Expected an annotation!");
        }

        int start = Math.max(node.getStart() - size, 0);
        int end = Math.min(node.getStart() + size, node.getDocument().getEnd());

        return context(node.getDocument().text(), start, end, node.getStart(), node.getEnd(), prepend, append, size, nlToS, centering);
    }

    /**
     * Get a string with the context
     * @param str      the text
     * @param prepend   prepend this before the annotation
     * @param append    append this after the annotation
     * @param size      context window size in chars
     * @param nlToS     new lines to spaces
     * @param centering fill whitespace to align context
     * @return context
     */
    private static String context(String str, int from, int to, int annoFrom, int annoTo, String prepend, String append, int size, boolean nlToS, boolean centering) {
        StringBuilder sb = new StringBuilder();

        if(nlToS) {
            str = str.replace('\n', ' ');
            str = str.replace('\r', ' ');
        }

        if(centering) {
            int leftPad = size - (annoFrom - from);
            for(int i = 0; i < leftPad; i++)
                sb.append(' ');
        }

        if(from != annoFrom)
            sb.append(str.substring(from,annoFrom));

        sb.append(prepend);
        sb.append(str.substring(annoFrom, annoTo));
        sb.append(append);

        if(to != annoTo)
            sb.append(str.substring(annoTo,to));

        if(centering) {
            int rightPad = size - (to - annoFrom);
            for(int i = 0; i < rightPad; i++)
                sb.append(' ');
        }

        return sb.toString();
    }

    public static boolean coveredBy(NodeStore s1, NodeStore s2) {
        return s1.isAnnotation() && s2.isAnnotation() && (s2.getStart() <= s1.getStart()  && s2.getEnd() >= s1.getEnd());
    }

    public static boolean covering(NodeStore s1, NodeStore s2) {
        return s1.isAnnotation() && s2.isAnnotation() && (s1.getStart() <= s2.getStart() && s1.getEnd() >= s2.getEnd());
    }

    public static boolean coveredBy(int s1, int e1, int s2, int e2) {
        return s2 <= s1  && e2 >= e1;
    }

    public static boolean covering(int s1, int e1, int s2, int e2) {
        return s1 <= s2 && e1 >= e2;
    }
}

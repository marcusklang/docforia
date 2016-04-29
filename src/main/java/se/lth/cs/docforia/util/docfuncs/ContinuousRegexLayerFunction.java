package se.lth.cs.docforia.util.docfuncs;
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

import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.DocumentFunction;
import se.lth.cs.docforia.Node;
import se.lth.cs.docforia.NodeStore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Node layer builder by matching regex ranges
 */
public class ContinuousRegexLayerFunction implements DocumentFunction {

    private final Pattern pattern;
    private final String layer;

    public ContinuousRegexLayerFunction(String layer, Pattern pattern) {
        this.pattern = pattern;
        this.layer = "@" + layer;
    }

    public <T extends Node> ContinuousRegexLayerFunction(Class<T> layer, Pattern pattern) {
        this.pattern = pattern;
        this.layer = layer.getName();
    }

    @Override
    public void apply(Document doc) {
        Matcher matcher = pattern.matcher(doc.getText());
        while(matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            NodeStore nodeStore = doc.store().createNode(layer).get();
            nodeStore.setRanges(start,end);
        }
    }
}

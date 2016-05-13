package se.lth.cs.docforia.io.text.columns;
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
import se.lth.cs.docforia.NodeStore;
import se.lth.cs.docforia.io.text.ColumnReader;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;

import java.util.Arrays;
import java.util.List;

/**
 * Simple suffix property reader, maps column to token
 */
public class SuffixColumnReader implements ColumnReader {
    private List<String> properties;

    public SuffixColumnReader(String...properties) {
        this.properties = Arrays.asList(properties);
    }

    public SuffixColumnReader(List<String> properties) {
        this.properties = properties;
    }

    @Override
    public void load(Document doc, List<TextSentence> sentences) {
        for (TextSentence sentence : sentences) {
            for (TextToken token : sentence) {
                NodeStore tok = token.token();
                for (int i = token.size()-properties.size(), k = 0; i < token.size(); i++, k++) {
                    String s = token.get(i);
                    if(s != null)
                        tok.putProperty(properties.get(k), s);
                }
            }
        }
    }
}

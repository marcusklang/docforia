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
import se.lth.cs.docforia.io.text.ColumnWriter;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reads and writes token properties
 */
public class SequenceColumnRW implements ColumnReader, ColumnWriter {
    private List<String> properties;
    private int offset = 0;

    /**
     * Append to end constructor for writing
     * @param addToEnd    true if add to end (not valid for reading)
     * @param properties  the properties to read/write
     */
    public SequenceColumnRW(boolean addToEnd, String...properties) {
        this(properties);
        if(addToEnd)
            offset = -1;
    }

    public SequenceColumnRW(String...properties) {
        this.properties = Arrays.asList(properties);
    }

    public SequenceColumnRW(int offset, String...properties) {
        this.offset = offset;
        this.properties = Arrays.asList(properties);
    }

    public SequenceColumnRW(int offset, List<String> properties) {
        this.offset = offset;
        this.properties = properties;
    }

    public SequenceColumnRW(List<String> properties) {
        this.properties = properties;
    }

    @Override
    public void load(Document doc, List<TextSentence> sentences) {
        for (TextSentence sentence : sentences) {
            for (TextToken token : sentence) {
                NodeStore tok = token.token();

                for (int i = 0; i < properties.size(); i++) {
                    String property = token.getProperty(offset+i);
                    if(property != null)
                        tok.putProperty(properties.get(i), property);
                }
            }
        }
    }

    @Override
    public void save(Document document, ArrayList<TextSentence> sentences) {
        for (TextSentence sentence : sentences) {
            int realoffset = offset == -1 ? sentence.getMinNumCols() : offset;

            for (TextToken token : sentence) {
                NodeStore tok = token.token();
                for (int i = 0; i < properties.size(); i++) {
                    String propkey = properties.get(i);
                    String propval = tok.getProperty(propkey);
                    token.setProperty(i+realoffset, propval);
                }
            }
            sentence.setMinCols(realoffset+1);
        }
    }
}

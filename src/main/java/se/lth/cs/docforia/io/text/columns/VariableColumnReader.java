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

import java.util.List;

/**
 * Variable number of columns, stores as [Prefix][N]
 */
public class VariableColumnReader implements ColumnReader {
    private String prefix;
    private int fromOffset;
    private int rightMargin;

    /**
     * Default constructor
     * @param prefix The prefix name to the property
     * @param fromOffset  the start offset;
     * @param rightMargin the number of columns to the right, not to be read.
     */
    public VariableColumnReader(String prefix, int fromOffset, int rightMargin) {
        this.fromOffset = fromOffset;
        this.rightMargin = rightMargin;
    }

    @Override
    public void load(Document doc, List<TextSentence> sentences) {
        for (TextSentence sentence : sentences) {
            for (TextToken token : sentence) {
                NodeStore tok = token.token();
                int from = fromOffset;
                int to = token.size()-rightMargin;
                for(int i = from, k = 0; i < to; i++, k++) {
                    String s = token.get(i);
                    if(s != null)
                        tok.putProperty(prefix + String.valueOf(k), s);
                }
            }
        }
    }
}

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
import se.lth.cs.docforia.io.text.ColumnWriter;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;

import java.util.ArrayList;

/**
 * Sequential per sentence counter writer
 */
public class SentenceTokenCounterWriter implements ColumnWriter {
    private int offset;
    private int base;

    /**
     * Default constructor, add to end, and 1-base
     */
    public SentenceTokenCounterWriter() {
        this(1, -1);
    }

    /**
     * Simplified constructor with 1-based counter
     * @param offset the offset to put the counter value at.
     */
    public SentenceTokenCounterWriter(int offset) {
        this(1, offset);
    }

    /**
     * Full constructor
     * @param base    the base value to begin at, defaults to 1
     * @param offset  the offset or -1 to put the index at.
     */
    public SentenceTokenCounterWriter(int base, int offset) {
        this.base = base;
        this.offset = offset;
    }

    @Override
    public void save(Document document, ArrayList<TextSentence> sentences) {
        for (TextSentence sentence : sentences) {
            int realOffset = offset == -1 ? sentence.getMinNumCols() : offset;
            int counter = base;
            for (TextToken token : sentence) {
                token.setProperty(realOffset, String.valueOf(counter));
                counter++;
            }
            sentence.setMinCols(realOffset+1);
        }
    }
}

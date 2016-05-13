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
import se.lth.cs.docforia.io.text.ColumnWriter;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;

import java.util.ArrayList;

/**
 * Write the form text
 */
public class FormColumnWriter implements ColumnWriter {
    private int offset;

    /**
     * Simplified add to current position
     */
    public FormColumnWriter() {
        this(-1);
    }

    /**
     * Full constructor
     * @param offset position or -1 if add to end
     */
    public FormColumnWriter(int offset) {
        this.offset = offset;
    }

    @Override
    public void save(Document document, ArrayList<TextSentence> sentences) {
        for (TextSentence sentence : sentences) {
            int realoffset = offset;
            if(offset == -1)
                realoffset = sentence.getMinNumCols();

            for (TextToken token : sentence) {
                NodeStore raw = token.token();
                token.setProperty(realoffset, document.text(raw.getStart(), raw.getEnd()));
            }

            sentence.setMinCols(realoffset+1);
        }
    }
}

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
import se.lth.cs.docforia.io.text.ColumnReader;
import se.lth.cs.docforia.io.text.ColumnWriter;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Document property field writer
 */
public class DocumentColumn implements ColumnReader, ColumnWriter {
    private String property;
    private int offset;

    public DocumentColumn(int offset, String property) {
        this.property = property;
        this.offset = offset;
    }

    @Override
    public void load(Document doc, List<TextSentence> sentences) {
        if(sentences.size() > 0) {
            String propertyValue = sentences.get(0).get(0).getProperty(offset);
            if(propertyValue != null)
                doc.putProperty(property, propertyValue);
        }
    }

    @Override
    public void save(Document document, ArrayList<TextSentence> sentences) {
        String propertyValue = document.getProperty(property);
        if(propertyValue != null)
        {
            for (TextSentence sentence : sentences) {
                for (TextToken token : sentence) {
                    token.setProperty(offset, propertyValue);
                }
                sentence.setMinCols(offset+1);
            }
        } else {
            for (TextSentence sentence : sentences) {
                for (TextToken token : sentence) {
                    token.setProperty(offset, null);
                }
                sentence.setMinCols(offset+1);
            }
        }

    }
}

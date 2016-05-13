package se.lth.cs.docforia.io.text.rebuilders;
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
import se.lth.cs.docforia.DocumentNodeLayer;
import se.lth.cs.docforia.graph.text.Sentence;
import se.lth.cs.docforia.graph.text.Token;
import se.lth.cs.docforia.io.text.TextDocumentReaderFactory;
import se.lth.cs.docforia.io.text.TextRebuilder;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;

import java.util.List;

/**
 * Simple text rebuilder, that puts each sentence on its own line, and space between each token
 */
public class SimpleRebuilder implements TextRebuilder {
    private int formCol;

    /**
     * Primary constructor
     * @param formCol 0-based index of the form column
     */
    public SimpleRebuilder(int formCol) {
        this.formCol = formCol;
    }

    @Override
    public void parse(TextDocumentReaderFactory reader, Document doc, List<TextSentence> sentence) {
        StringBuilder sb = new StringBuilder();
        if(sentence.size() == 0) {
            doc.setText("");
            return;
        }

        DocumentNodeLayer tokenLayer = doc.store().nodeLayer(Document.nodeLayer(Token.class), null);
        DocumentNodeLayer sentenceLayer = doc.store().nodeLayer(Document.nodeLayer(Sentence.class), null);

        for (TextSentence textSentence : sentence) {
            int start = sb.length();
            if(textSentence.size() == 0)
                continue;

            TextToken first = textSentence.get(0);
            sb.append(first.getProperty(formCol));

            first.setToken(tokenLayer.create(start, sb.length()));

            for (TextToken entries : textSentence.subList(1, textSentence.size())) {
                sb.append(' ');
                int tokstart = sb.length();
                String form = entries.getProperty(formCol);
                sb.append(form == null ? reader.getEmptyColumn() : form);
                entries.setToken(tokenLayer.create(tokstart, sb.length()));
            }

            textSentence.setSentence(sentenceLayer.create(start, sb.length()));

            sb.append('\n');
        }

        doc.setText(sb.toString());
    }
}

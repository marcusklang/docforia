package se.lth.cs.docforia.io.text.builders;
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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import se.lth.cs.docforia.io.text.TextBuilder;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;

/**
 * A text builder that right aligns text and adds space so that the output is human readable
 */
public class PrettyTextBuilder implements TextBuilder {
    private String emptyMarker = "_";

    public PrettyTextBuilder setEmptyMarker(String emptyMarker) {
        this.emptyMarker = emptyMarker;
        return this;
    }

    public String getEmptyMarker() {
        return emptyMarker;
    }

    private void leftpad(StringBuilder sb, String text, int colsize) {
        if(text.length() == colsize)
            sb.append(text);
        else
        {
            final int len = colsize - text.length();
            for(int i = 0; i < len; i++) {
                sb.append(' ');
            }
            sb.append(text);
        }
    }

    @Override
    public String build(TextSentence sentence) {
        IntArrayList columnWidths = new IntArrayList();

        for (TextToken token : sentence) {
            int size = Math.max(sentence.getMinNumCols(), token.size());
            if(columnWidths.size() < size) {
                final int add = size-columnWidths.size();
                for(int i = 0; i < add; i++) {
                    columnWidths.add(0);
                }
            }

            for (int i = 0; i < size; i++) {
                String prop = token.getProperty(i);
                columnWidths.set(i, Math.max(columnWidths.getInt(i), prop != null ? prop.length() : emptyMarker.length()));
            }
        }

        StringBuilder sb = new StringBuilder();
        for (TextToken token : sentence) {
            int size = Math.max(sentence.getMinNumCols(), token.size());
            if(token.size() > 0) {
                String prop = token.getProperty(0);
                leftpad(sb, prop != null ? prop : emptyMarker, columnWidths.getInt(0));
            }

            for (int i = 1; i < size; i++) {
                sb.append('\t');
                String prop = token.getProperty(i);
                leftpad(sb, prop != null ? prop : emptyMarker, columnWidths.getInt(i));
            }
            sb.append('\n');
        }

        return sb.toString();
    }
}

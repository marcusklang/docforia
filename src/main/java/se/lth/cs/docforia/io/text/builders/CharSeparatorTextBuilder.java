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

import se.lth.cs.docforia.io.text.TextBuilder;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;

/**
 * Simple text builder that uses tabs to seperate all properties
 */
public class CharSeparatorTextBuilder implements TextBuilder {

    private String emptyMarker = "_";
    private char separator = '\t';

    public CharSeparatorTextBuilder setSeparator(char separator) {
        this.separator = separator;
        return this;
    }

    public char getSeparator() {
        return separator;
    }

    public CharSeparatorTextBuilder setEmptyMarker(String emptyMarker) {
        this.emptyMarker = emptyMarker;
        return this;
    }

    public String getEmptyMarker() {
        return emptyMarker;
    }

    @Override
    public String build(TextSentence sentence) {
        StringBuilder sb = new StringBuilder();
        final int numCols = sentence.getMinNumCols();

        for (TextToken token : sentence) {
            if(token.size() > 0) {
                String prop = token.get(0);
                sb.append(prop != null ? prop : emptyMarker);
            }

            for (int i = 1; i < numCols; i++) {
                sb.append(separator);
                String prop = token.getProperty(i);
                sb.append(prop != null ? prop : emptyMarker);
            }
            sb.append('\n');
        }

        return sb.toString();
    }
}

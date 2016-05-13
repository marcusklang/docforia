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
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;

import java.util.List;

/**
 * Read id property
 */
public class IdColumnReader implements ColumnReader {

    private int idField = 0;
    private boolean storeInModel = false;

    public IdColumnReader() {
    }

    public IdColumnReader(int idField) {
        this.idField = idField;
    }

    public IdColumnReader(int idField, boolean storeInModel) {
        this.idField = idField;
        this.storeInModel = storeInModel;
    }

    @Override
    public void load(Document doc, List<TextSentence> sentences) {
        for (TextSentence sentence : sentences) {
            for (TextToken token : sentence) {
                token.setId(Integer.parseInt(token.getProperty(idField)));
                if(storeInModel)
                {
                    token.token().putProperty("id", Integer.parseInt(token.getProperty(idField)));
                }
            }
        }
    }
}

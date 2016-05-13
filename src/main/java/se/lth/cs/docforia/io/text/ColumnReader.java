package se.lth.cs.docforia.io.text;
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

import java.util.List;

/**
 * Column reader, used when parsing text formats
 */
public interface ColumnReader {
    /**
     * Load in annotations and/or nodes using the parsed sentences
     * @param doc       the document to modify (has text when used)
     * @param sentences parsed sentences
     */
    void load(Document doc, List<TextSentence> sentences);
}

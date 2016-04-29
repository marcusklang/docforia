package se.lth.cs.docforia.io;
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

import java.util.Collection;

/**
 * Document writer
 */
public interface DocumentWriter {

    /**
     * Write a document to file
     * @param doc the document
     * @throws java.io.IOError in case of I/O errors
     */
    void write(Document doc);

    /**
     * Write a list of documents to file
     * @param docs the document
     * @throws java.io.IOError in case of I/O errors
     */
    default void write(Collection<Document> docs) {
        for (Document doc : docs) {
            write(doc);
        }
    }

    /**
     * Close and flush to file
     */
    void close();
}

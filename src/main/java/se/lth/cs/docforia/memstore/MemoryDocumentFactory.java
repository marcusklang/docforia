package se.lth.cs.docforia.memstore;
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
import se.lth.cs.docforia.DocumentFactory;
import se.lth.cs.docforia.io.DocumentIO;

/**
 * Memory Document factory
 */
public class MemoryDocumentFactory implements DocumentFactory {

    @Override
    public Document create() {
        return new MemoryDocument();
    }

    @Override
    public Document create(Document doc) {
        return Document.convert(this, doc);
    }

    @Override
    public Document create(String uri) {
        return new MemoryDocument().setUri(uri);
    }

    @Override
    public Document create(String uri, String text) {
        return new MemoryDocument(text).setUri(uri);
    }

    public Document createFragment(String id) {
        return new MemoryDocument().setId(id);
    }

    public Document createFragment(String id, String text) {
        return new MemoryDocument(id, text);
    }

    public Document createFragment(Document doc) {
        return Document.convert(this, doc);
    }

    @Override
    public DocumentIO io() {
        return MemoryDocumentIO.getInstance();
    }

    private static final MemoryDocumentFactory INSTANCE = new MemoryDocumentFactory();

    public static MemoryDocumentFactory getInstance() {
        return INSTANCE;
    }
}

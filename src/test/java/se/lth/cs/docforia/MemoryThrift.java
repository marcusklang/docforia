package se.lth.cs.docforia;
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

import se.lth.cs.docforia.io.thrift.MemoryThriftCodec;
import se.lth.cs.docforia.memstore.MemoryDocument;
import se.lth.cs.docforia.memstore.MemoryDocumentFactory;

/**
 * Level 2 test code
 */
public class MemoryThrift extends ModelTest {

    @Override
    public DocumentFactory documentFactory() {
        return MemoryDocumentFactory.getInstance();
    }

    @Override
    protected Document serializeDeserialize(Document doc) {
        return MemoryThriftCodec.decode(MemoryThriftCodec.encode((MemoryDocument) doc));
    }
}

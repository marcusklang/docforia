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

import org.junit.Test;
import se.lth.cs.docforia.io.multipart.MultipartMessages;
import se.lth.cs.docforia.io.multipart.MultipartReaderV1;
import se.lth.cs.docforia.io.multipart.MultipartWriterV1;
import se.lth.cs.docforia.memstore.MemoryDocumentFactory;

public class MultipartTest extends ModelTest {

    @Override
    public DocumentFactory documentFactory() {
        return MemoryDocumentFactory.getInstance();
    }

    @Override
    protected Document serializeDeserialize(Document doc) {
        MultipartWriterV1 writer = new MultipartWriterV1();
        return MultipartReaderV1.decode(MultipartMessages.fromBytes(writer.encode(doc, null).toBytes()), MemoryDocumentFactory.getInstance());
    }

    @Test
    public void testMultipartEncoding() {
        MultipartWriterV1 writer = new MultipartWriterV1();
        Conny_Andersson conny_andersson = new Conny_Andersson();
        Document doc = conny_andersson.createDocument(MemoryDocumentFactory.getInstance());

        MultipartMessages encoded = writer.encode(doc, null);
        byte[] fullyEncoded = encoded.toBytes();
        Document decoded = MultipartReaderV1.decode(MultipartMessages.fromBytes(fullyEncoded), MemoryDocumentFactory.getInstance());
    }
}

package se.lth.cs.docforia;

import se.lth.cs.docforia.io.protobuf3.MemoryProtobufCodec;
import se.lth.cs.docforia.memstore.MemoryDocument;
import se.lth.cs.docforia.memstore.MemoryDocumentFactory;

/**
 * Created by marcusk on 2016-11-03.
 */
public class MemoryProtobuf3Test extends ModelTest {

    @Override
    public DocumentFactory documentFactory() {
        return MemoryDocumentFactory.getInstance();
    }

    @Override
    protected Document serializeDeserialize(Document doc) {
        byte[] bytes = MemoryProtobufCodec.encode((MemoryDocument) doc);
        return MemoryProtobufCodec.decode(bytes);
    }
}

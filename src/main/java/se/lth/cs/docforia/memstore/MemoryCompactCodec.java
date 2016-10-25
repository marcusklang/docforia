package se.lth.cs.docforia.memstore;

import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.DocumentRef;
import se.lth.cs.docforia.data.CoreRef;
import se.lth.cs.docforia.data.DataRef;
import se.lth.cs.docforia.data.DocRef;
import se.lth.cs.docforia.data.StringArrayRef;
import se.lth.cs.docforia.io.singlepart.Singlepart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by marcusk on 2016-10-25.
 */
public class MemoryCompactCodec {
    public static MemoryDocument decode(byte[] data) {
        return null;
    }

    public static byte[] encode(MemoryDocument document) {
        return encodeDocument(document).toByteArray();
    }

    private static Singlepart.Document encodeDocument(MemoryDocument document) {
        ArrayList<Singlepart.PropertyKey> docpropkeys = new ArrayList<>();
        ArrayList<Singlepart.PropertyValue> docpropvalues = new ArrayList<>();

        for (Map.Entry<String, DataRef> prop : document.properties()) {
            if(Document.PROP_ALL.contains(prop.getKey())) {
                //Special!
                switch (prop.getKey()) {
                    case Document.PROP_ID:
                        docpropkeys.add(Singlepart.PropertyKey.newBuilder()
                                .setSpecial(Singlepart.PropertyKey.SpecialKey.DOC_ID)
                                .setType(Singlepart.PropertyKey.DataType.D_STRING)
                                .build());

                        docpropvalues.add(Singlepart.PropertyValue.newBuilder()
                                .setStringValue(prop.getValue().stringValue())
                                .build());
                        break;
                    case Document.PROP_LANG:
                        docpropkeys.add(Singlepart.PropertyKey.newBuilder()
                                .setSpecial(Singlepart.PropertyKey.SpecialKey.DOC_LANG)
                                .setType(Singlepart.PropertyKey.DataType.D_STRING)
                                .build());

                        docpropvalues.add(Singlepart.PropertyValue.newBuilder()
                                .setStringValue(prop.getValue().stringValue())
                                .build());
                        break;
                    case Document.PROP_TITLE:
                        docpropkeys.add(Singlepart.PropertyKey.newBuilder()
                                .setSpecial(Singlepart.PropertyKey.SpecialKey.DOC_LANG)
                                .setType(prop.getValue() instanceof DocRef ? Singlepart.PropertyKey.DataType.D_DOCUMENT : Singlepart.PropertyKey.DataType.D_STRING)
                                .build());

                        if(prop.getValue() instanceof DocumentRef) {
                            docpropvalues.add(Singlepart.PropertyValue.newBuilder()
                                    .addDocValues(encodeDocument( (MemoryDocument)((DocRef)prop.getValue()).documentValue() ))
                                    .build());
                        } else {
                            docpropvalues.add(Singlepart.PropertyValue.newBuilder()
                                    .setStringValue(prop.getValue().stringValue())
                                    .build());
                        }
                        break;
                    case Document.PROP_TYPE:
                        break;
                    case Document.PROP_URI:
                        docpropkeys.add(Singlepart.PropertyKey.newBuilder()
                                .setSpecial(Singlepart.PropertyKey.SpecialKey.DOC_LANG)
                                .setType(Singlepart.PropertyKey.DataType.D_STRING_ARRAY)
                                .build());

                        docpropvalues.add(Singlepart.PropertyValue.newBuilder()
                                .addAllStringValues(Arrays.asList(((StringArrayRef)prop.getValue()).arrayValue()))
                                .build());
                }
            } else {
                if(!(prop.getValue() instanceof CoreRef))
                    throw new UnsupportedOperationException("Only CoreRef types are supported!");

                CoreRef coreValue = (CoreRef)prop.getValue();

            }
        }

        ArrayList<Singlepart.Nodes> nodes = new ArrayList<>();
        ArrayList<Singlepart.Edges> edges = new ArrayList<>();

        return null;
    }
}

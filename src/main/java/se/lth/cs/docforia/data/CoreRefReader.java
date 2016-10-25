package se.lth.cs.docforia.data;

/**
 * Created by marcusk on 2016-10-25.
 */
public interface CoreRefReader {
    default CoreRef read(CoreRefType type) {
        switch (type) {
            case BINARY: return readBinary();
            case INT: return readInt();
            case LONG: return readLong();
            case BOOLEAN: return readBoolean();
            case BOOLEAN_ARRAY: return readBooleanArray();
            case FLOAT: return readFloat();
            case DOUBLE: return readDouble();
            case STRING: return readString();
            case DOCUMENT: return readDocument();
            case INT_ARRAY: return readIntArray();
            case LONG_ARRAY: return readLongArray();
            case FLOAT_ARRAY: return readFloatArray();
            case DOUBLE_ARRAY: return readDoubleArray();
            case PROPERTY_MAP: return readPropertyMap();
            case STRING_ARRAY: return readStringArray();
            case DOCUMENT_ARRAY: return readDocumentArray();
            default:
                throw new UnsupportedOperationException("Do not know how to read " + type.toString());
        }
    }

    StringRef readString();

    StringArrayRef readStringArray();

    PropertyMapRef readPropertyMap();

    BooleanRef readBoolean();

    IntRef readInt();

    LongRef readLong();

    FloatRef readFloat();

    DoubleRef readDouble();

    BinaryRef readBinary();

    DocRef readDocument();

    BooleanArrayRef readBooleanArray();

    IntArrayRef readIntArray();

    LongArrayRef readLongArray();

    FloatArrayRef readFloatArray();

    DoubleArrayRef readDoubleArray();

    DocArrayRef readDocumentArray();
}

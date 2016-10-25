package se.lth.cs.docforia.data;

import se.lth.cs.docforia.memstore.MemoryDocument;

/**
 * Created by marcusk on 2016-10-25.
 */
public interface CoreRefWriter {
    default void write(DataRef ref) {
        throw new UnsupportedOperationException("Writing a generic DataRef is not supported!");
    }
    void write(byte[] binary);
    void write(String string);
    void write(boolean boolValue);
    void write(int intValue);
    void write(long longValue);
    void write(float floatValue);
    void write(double doubleValue);
    void write(MemoryDocument doc);
    void write(PropertyMap propertyMap);
    void writeBooleanArray(boolean[] boolValues);
    void writeIntArray(int[] intValues);
    void writeLongArray(long[] longValues);
    void writeFloatArray(float[] floatValues);
    void writeDoubleArray(double[] doubleValues);
    void writeDocumentArray(MemoryDocument[] docValues);
    void writeStringArray(String[] stringValues);
}

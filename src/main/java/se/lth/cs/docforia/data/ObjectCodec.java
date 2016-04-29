package se.lth.cs.docforia.data;
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

import se.lth.cs.docforia.PropertyStore;

import java.io.*;

/**
 * Object encoder/decoder via Java serialization
 */
public class ObjectCodec<T> implements Encoder<T>, Decoder<T> {

    private ObjectCodec() {

    }

    @Override
    public T decode(PropertyStore store, String key, DataRef ref) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(ref.binaryValue());
            ObjectInputStream ois = new ObjectInputStream(bais);
            T obj = (T)ois.readObject();
            ois.close();
            return obj;
        } catch (IOException | ClassNotFoundException e) {
            throw new IOError(e);
        }
    }

    @Override
    public DataRef encode(PropertyStore store, String key, T value) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(value);
            oos.flush();

            return new BinaryRef(baos.toByteArray());
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private static final ObjectCodec<?> INSTANCE = new ObjectCodec<>();

    /**
     * Get instance of codec, unchecked type, it is up to the programmer to verify types.
     * @param type used to pin down type for return value
     * @param <T> the expected type
     */
    @SuppressWarnings("unchecked")
    public static <T> ObjectCodec<T> instance(Class<T> type) {
        return (ObjectCodec<T>)INSTANCE;
    }
}

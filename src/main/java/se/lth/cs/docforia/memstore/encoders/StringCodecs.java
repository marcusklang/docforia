package se.lth.cs.docforia.memstore.encoders;
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

/**
 * String codecs
 */
public class StringCodecs {
    public static final int BASELINE = 0;
    public static final int EQUAL_LEN = 1;
    public static final int VARIABLE_LEN_DICT = 2;
    public static final int EQUAL_LEN_DICT = 3;

    static final StringCodec[] codecs = new StringCodec[] {
            BaselineStringCodec.INSTANCE,
            EqualLenStringCodec.INSTANCE,
            VariableLenDictStringCodec.INSTANCE,
            EqualLenDictStringCodec.INSTANCE
    };
}

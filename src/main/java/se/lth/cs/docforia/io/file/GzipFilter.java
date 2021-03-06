package se.lth.cs.docforia.io.file;
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

import se.lth.cs.docforia.io.mem.GzipUtil;

import java.nio.ByteBuffer;

/**
 * GZIP Data filter codec
 */
public class GzipFilter implements DataFilter {
    static final byte[] FILTER_ID = new byte[] {'G', 'Z'};

    @Override
    public byte[] id() {
        return FILTER_ID;
    }

    @Override
    public ByteBuffer unapply(ByteBuffer data) {
        return GzipUtil.decompress(data);
    }

    @Override
    public byte[] unapply(byte[] data) {
        return GzipUtil.decompress(data);
    }

    @Override
    public ByteBuffer apply(ByteBuffer data) {
        return GzipUtil.compress(data);
    }

    @Override
    public byte[] apply(byte[] data) {
        return GzipUtil.compress(data);
    }

    private GzipFilter() {

    }

    private static final GzipFilter INSTANCE = new GzipFilter();

    public static GzipFilter getInstance() {
        return INSTANCE;
    }
}

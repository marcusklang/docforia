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

import se.lth.cs.docforia.io.DocumentReader;
import se.lth.cs.docforia.io.mem.Input;
import se.lth.cs.docforia.memstore.MemoryBinary;
import se.lth.cs.docforia.memstore.MemoryDocument;

import java.io.*;
import java.util.Arrays;

/** Sequential document reader that reads documents stored sequentially.
 *
 * <p>
 * @see DocumentFileWriter
 */
public class DocumentFileReader implements DocumentReader {
    private Input input;
    private DataFilter filter;
    private boolean eof;

    public DocumentFileReader(File input) {
        this(input, GzipFilter.getInstance());
    }

    public DocumentFileReader(File input, DataFilter filter) {
        try {
            if(!input.exists())
                throw new IOError(new FileNotFoundException(input.getAbsolutePath()));

            if(input.length() < 6)
                throw new IOError(new IOException("File is too small to be valid."));

            this.input = new Input(new FileInputStream(input));
            this.filter = filter;

            byte[] header = new byte[4];
            byte[] filterid = new byte[2];

            this.input.readBytes(header);
            this.input.readBytes(filterid);

            if(!Arrays.equals(header, DocumentFileWriter.MAGIC_V1)) {
                throw new IOError(new IOException("Invalid magic header!"));
            }

            if(!Arrays.equals(filterid, filter == null ? DocumentFileWriter.FILTER_NA : filter.id())) {
                throw new IOError(new IOException("Invalid filter!"));
            }
        } catch (FileNotFoundException e) {
            throw new IOError(e);
        }
    }

    @Override
    public MemoryDocument next() {
        if(eof)
            return null;

        int frameSize = input.readVarInt(true);
        if(frameSize == 0)
        {
            eof = true;
            return null;
        }

        byte[] docdata = new byte[frameSize];
        input.readBytes(docdata);
        docdata = filter != null ? filter.unapply(docdata) : docdata;
        return MemoryBinary.decode(docdata);
    }

    @Override
    public void close() {
        eof=true;
        input.close();
    }
}

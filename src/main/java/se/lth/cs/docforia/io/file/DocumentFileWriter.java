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

import se.lth.cs.docforia.Document;
import se.lth.cs.docforia.DocumentStorageLevel;
import se.lth.cs.docforia.io.DocumentWriter;
import se.lth.cs.docforia.io.mem.Output;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/** Thread-safe sequential document storage writer
 *
 * @see DocumentFileReader
 **/
public class DocumentFileWriter implements DocumentWriter {
    private static class EntryInfo {
        public long frameStart;
        public long dataStart;
        public int length;

        public EntryInfo() {
        }

        public EntryInfo(long frameStart, long dataStart, int length) {
            this.frameStart = frameStart;
            this.dataStart = dataStart;
            this.length = length;
        }
    }

    private final DocumentStorageLevel storageLevel;
    private final File output;
    private final AsynchronousFileChannel fileChannel;
    private final AtomicLong allocatedSpace = new AtomicLong();
    private final AtomicLong writtenDocuments = new AtomicLong();
    private final DataFilter filter;

    static final byte[] FILTER_NA = new byte[] {'N', 'A'}; //N/A, no filter
    static final byte[] MAGIC_V1 = new byte[] {
            'D', 'S', '1', '0' //Document Storage v1
    };

    /**
     * Simplified Constructor, Level 2 storage level, Gzip filter
     * @param output output file
     * @throws IOException Thrown if there is an I/O error when creating file.
     */
    public DocumentFileWriter(File output) throws IOException {
        this(output, DocumentStorageLevel.LEVEL_2, GzipFilter.getInstance());
    }

    /**
     * Simplified Constructor, Level 2 storage level
     * @param output output file
     * @throws IOException Thrown if there is an I/O error when creating file.
     */
    public DocumentFileWriter(File output, DataFilter filter) throws IOException {
        this(output, DocumentStorageLevel.LEVEL_2, filter);
    }

    /**
     * Simplified Constructor
     * @throws IOException Thrown if there is an I/O error when creating file.
     */
    public DocumentFileWriter(File output, DocumentStorageLevel level) throws IOException {
        this(output, level, null);
    }

    /**
     * Primary constructor
     * @param output output file
     * @throws IOException Thrown if there is an I/O error when creating file.
     */
    public DocumentFileWriter(File output, DocumentStorageLevel level, DataFilter filter) throws IOException {
        this.output = output;
        this.storageLevel = level;
        this.filter = filter;

        this.fileChannel = AsynchronousFileChannel.open(Paths.get(output.getAbsoluteFile().toURI()),  StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

        try {
            fileChannel.write(ByteBuffer.wrap(MAGIC_V1), 0).get();
            fileChannel.write(ByteBuffer.wrap(filter == null ? FILTER_NA : filter.id()), 4).get();

            allocatedSpace.addAndGet(MAGIC_V1.length + 2);
        } catch (ExecutionException | InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(Document doc) {
        write(doc, null);
    }

    @Override
    public void write(Collection<Document> docs) {
        write(docs, null);
    }

    /**
     * Write document
     *
     * <b>Remarks: </b> This method might be slow, Consider using the {@link #write(Collection, List)} method.
     * this is a convience wrapper for that method.
     * @param document the document to write
     */
    public void write(Document document, EntryInfo info) {
        if(info != null) {
            ArrayList<EntryInfo> infos = new ArrayList<>(1);
            write(Collections.singletonList(document), infos);
            EntryInfo item = infos.get(0);
            info.frameStart = item.frameStart;
            info.dataStart = item.dataStart;
            info.length = item.length;
        } else {
            write(Collections.singletonList(document), null);
        }
    }

    /**
     * Write documents
     *
     * <b>Remarks: </b>This methods waits for I/O to finish.
     * @param batch the batch document to write
     */
    public void write(Collection<Document> batch, List<EntryInfo> outEntryInfos) {
        ArrayList<ByteBuffer> filteredData = new ArrayList<>();

        int frameOverhead = 0;
        int compressedLength = 0;

        for (Document document : batch) {
            ByteBuffer unfiltered = ByteBuffer.wrap(document.toBytes(storageLevel));
            ByteBuffer filtered = filter != null ? filter.apply(unfiltered) : unfiltered;

            filteredData.add(filtered);
            frameOverhead += Output.intLength(filtered.remaining(), true);
            compressedLength += filtered.remaining();
        }

        ByteBuffer frame = ByteBuffer.allocate(frameOverhead+compressedLength);
        long allocatedPosition = allocatedSpace.getAndAdd(frameOverhead+compressedLength);

        for (ByteBuffer buffer : filteredData) {
            long frameStart = frame.position()+allocatedPosition;

            Output.writeVarInt(frame, buffer.remaining(), true);
            long dataStart = frame.position()+allocatedPosition;
            int dataLength = buffer.remaining();

            frame.put(buffer);

            if(outEntryInfos != null)
                outEntryInfos.add(new EntryInfo(frameStart, dataStart, dataLength));
        }

        try {
            frame.flip();
            fileChannel.write(frame, allocatedPosition).get();
            writtenDocuments.addAndGet(batch.size());
        } catch (ExecutionException | InterruptedException e) {
            throw new IOError(e);
        }
    }

    /**
     * Close the file
     * <b>Remarks:</b> Not thread safe, only call from one thread, and after all threads have finished!
     */
    public void close() {
        try {
            fileChannel.force(true);
            fileChannel.close();
        } catch (IOException e) {
            throw new IOError(e);
        }

        if(this.output.length() != allocatedSpace.get()) {
            throw new IOError(new IOException("The result was not written to disk!"));
        }
    }
}

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
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/** Thread-safe block based document writer
 *
 * @see DocumentBlockFileReader
 */
public class DocumentBlockFileWriter implements DocumentWriter {
    public static class BlockInfo implements Serializable {
        /** Where in the file does the block start */
        public long blockStart;

        /** The length of the block */
        public int  blockLength;

        /** Lengths of documents inside the filtered block */
        public int unfilteredLength;

        /** Locations of documents inside the filtered block */
        public int[] dataStart;

        public BlockInfo() {
        }

        public BlockInfo(long blockStart, int blockLength, int unfilteredLength, int[] dataStart) {
            this.blockStart = blockStart;
            this.blockLength = blockLength;
            this.unfilteredLength = unfilteredLength;
            this.dataStart = dataStart;
        }
    }

    private final DocumentStorageLevel storageLevel;
    private final File output;
    private final AsynchronousFileChannel fileChannel;
    private final AtomicLong allocatedSpace = new AtomicLong();
    private final DataFilter filter;

    /**
     * Simplified Constructor, Level 2 storage level
     * @param output output file
     * @throws IOException Thrown if there is an I/O error when creating file.
     */
    public DocumentBlockFileWriter(File output) throws IOException {
        this(output, DocumentStorageLevel.LEVEL_2);
    }

    static final byte[] MAGIC_V1 = new byte[] {'D', 'B', 'S', '1'}; //Document Block Storage v1

    /**
     * Simplified constructor, Gzip Filter per default
     * @param output output file
     * @throws IOException Thrown if there is an I/O error when creating file.
     */
    public DocumentBlockFileWriter(File output, DocumentStorageLevel level) throws IOException {
        this(output, level, GzipFilter.getInstance());
    }

    /**
     * Primary constructor
     * @param output output file
     * @throws IOException Thrown if there is an I/O error when creating file.
     */
    public DocumentBlockFileWriter(File output, DocumentStorageLevel level, DataFilter filter) throws IOException {
        this.output = output;
        this.storageLevel = level;
        this.filter = filter;

        this.fileChannel = AsynchronousFileChannel.open(Paths.get(output.toURI()), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

        try {
            byte[] magicHeader = MAGIC_V1;
            byte[] filterHeader = filter != null ? filter.id() : DocumentFileWriter.FILTER_NA;
            fileChannel.write(ByteBuffer.wrap(magicHeader),0).get();
            fileChannel.write(ByteBuffer.wrap(filterHeader),4).get();

            allocatedSpace.addAndGet(magicHeader.length+filterHeader.length);
        } catch (ExecutionException | InterruptedException e) {
            throw new IOException(e);
        }
    }

    /**
     * Compress data for storage
     * @param data the data to comrpess
     * @return compressed data or itself.
     */
    protected ByteBuffer filter(ByteBuffer data) {
        return filter != null ? filter.apply(data) : data;
    }

    @Override
    public void write(Document doc) {
        write(Collections.singleton(doc));
    }

    @Override
    public void write(Collection<Document> docs) {
        write(docs, null);
    }

    /**
     * Write documents
     *
     * <b>Remarks: </b>This methods waits for I/O to finish.
     * Empty block collections will set block info to -1, and null for data starts, for all positions and lengths.
     * @param block the batch document to write
     * @param outBlockInfo [output] stores information about the written block, might be null to ignore this information.
     */
    public void write(Collection<Document> block, BlockInfo outBlockInfo) {
        if(block.isEmpty()) {
            if(outBlockInfo != null) {
                outBlockInfo.blockLength = -1;
                outBlockInfo.blockStart = -1;
                outBlockInfo.unfilteredLength = -1;
                outBlockInfo.dataStart = null;
            }
            return;
        }

        int[] dataStarts = new int[block.size()];
        int unfilteredTotalLength = 0;

        ByteBuffer frame;
        {
            int k = 0;

            ArrayList<ByteBuffer> uncompressedData = new ArrayList<>(block.size());
            for (Document document : block) {
                ByteBuffer uncompressed = ByteBuffer.wrap(document.toBytes(storageLevel));
                uncompressedData.add(uncompressed);

                dataStarts[k] += unfilteredTotalLength;
                unfilteredTotalLength += uncompressed.remaining() + Output.intLength(uncompressed.remaining(), true);
            }

            ByteBuffer compressed;

            {
                ByteBuffer toCompress = ByteBuffer.allocate(unfilteredTotalLength);

                for (ByteBuffer buffer : uncompressedData) {
                    Output.writeVarInt(toCompress, buffer.remaining(), true);
                    toCompress.put(buffer);
                }

                toCompress.flip();
                compressed = filter(toCompress);
            }

            frame = ByteBuffer.allocate(Output.intLength(compressed.remaining(), true)+compressed.remaining());
            Output.writeVarInt(frame, compressed.remaining(), true);
            frame.put(compressed);
            frame.flip();
        }

        long allocatedPosition = allocatedSpace.getAndAdd(frame.remaining());

        if(outBlockInfo != null) {
            outBlockInfo.blockStart = allocatedPosition;
            outBlockInfo.blockLength = frame.remaining();
            outBlockInfo.unfilteredLength = unfilteredTotalLength;
            outBlockInfo.dataStart = dataStarts;
        }

        try {
            fileChannel.write(frame, allocatedPosition).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new IOError(e);
        }
    }

    public void close() {
        try {
            fileChannel.write(ByteBuffer.wrap(new byte[] {0}),allocatedSpace.get()).get();
            allocatedSpace.addAndGet(1);
            fileChannel.force(true);
            fileChannel.close();
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new IOError(e);
        }

        if(this.output.length() != allocatedSpace.get()) {
            throw new IOError(new IOException("The result was not written to disk!"));
        }
    }
}

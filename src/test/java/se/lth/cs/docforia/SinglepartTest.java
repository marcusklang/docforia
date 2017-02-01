package se.lth.cs.docforia;

/*import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.snappy.SnappyCompressorInputStream;*/
import org.junit.Test;
import se.lth.cs.docforia.io.mem.GzipUtil;
import se.lth.cs.docforia.io.mem.Output;
import se.lth.cs.docforia.io.protobuf3.MemoryProtobufCodec;
import se.lth.cs.docforia.memstore.MemoryDocument;
import se.lth.cs.docforia.memstore.MemoryDocumentFactory;

import java.io.IOError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SinglepartTest {
    @Test
    public void testCompression() {
        Conny_Andersson conny = new Conny_Andersson();
        Document doc = conny.createDocument(MemoryDocumentFactory.getInstance());
        byte[] encoded = MemoryProtobufCodec.encode((MemoryDocument) doc);
        System.out.println(encoded.length);
        byte[] compressed = GzipUtil.compress(encoded);
        System.out.println(doc.toBytes().length);
        byte[] compressed2 = GzipUtil.compress(doc.toBytes());
        System.out.println(compressed.length);
        System.out.println(compressed2.length);

    }
/*
    private byte[] bzipcompress(byte[] data) {
        try {
            //Heuristic, 75% compression.
            Output compressed = new Output(32,2147483647);
            BZip2CompressorOutputStream gzipOutputStream = new BZip2CompressorOutputStream(compressed, 9);

            Output output = new Output(gzipOutputStream);
            output.writeVarInt(data.length, true);
            output.write(data);
            output.close();

            return compressed.toBytes();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }*/

    @Test
    public void testLarge() throws IOException {
        byte[] data = Files.readAllBytes(Paths.get("/mnt/fastdisk/NED/Data20160211/mul/Q30.docforia"));
        System.out.println("Decoded.");
        MemoryDocument doc = MemoryDocument.fromBytes(data);

        byte[] encoded = MemoryProtobufCodec.encode((MemoryDocument) doc);
        System.out.println(encoded.length);
        MemoryDocument decdoc = MemoryProtobufCodec.decode(encoded);

        byte[] compressed = GzipUtil.compress(encoded);
        System.out.println(compressed.length);
        //System.out.println(bzipcompress(encoded).length);

        System.out.println("Uncompressed text: " + doc.text().getBytes(StandardCharsets.UTF_8).length);
        System.out.println("Compressed text: " + GzipUtil.compress(doc.text().getBytes(StandardCharsets.UTF_8)).length);

        byte[] raw = doc.toBytes();
        System.out.println(raw.length);
        byte[] compressed2 = GzipUtil.compress(raw);
        System.out.println(compressed2.length);
        //System.out.println(bzipcompress(raw).length);
    }
}

package se.lth.cs.docforia;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import se.lth.cs.docforia.graph.text.DependencyRelation;
import se.lth.cs.docforia.graph.text.Paragraph;
import se.lth.cs.docforia.graph.text.Sentence;
import se.lth.cs.docforia.graph.text.Token;
import se.lth.cs.docforia.io.DocumentReader;
import se.lth.cs.docforia.io.file.DocumentBlockFileReader;
import se.lth.cs.docforia.io.file.DocumentBlockFileWriter;
import se.lth.cs.docforia.io.file.DocumentFileReader;
import se.lth.cs.docforia.io.file.DocumentFileWriter;
import se.lth.cs.docforia.io.mem.GzipUtil;
import se.lth.cs.docforia.memstore.MemoryDocument;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.Assert.*;
import static se.lth.cs.docforia.graph.TokenProperties.POS;

/**
 * IO test code
 */
public class IOTest {

    @Before
    public void setUp() throws Exception {
        File testFile = new File("test.docs");
        if(testFile.exists()) {
            if(!testFile.delete()) {
                throw new IOError(new IOException("Failed to delete file: " + testFile.getAbsolutePath()));
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        File testFile = new File("test.docs");
        if(testFile.exists()) {
            if(!testFile.delete()) {
                throw new IOError(new IOException("Failed to delete file: " + testFile.getAbsolutePath()));
            }
        }
    }

    @Test
    public void testGzip() {
        ByteBuffer compressedbuf = GzipUtil.compress(ByteBuffer.wrap(new byte[]{1, 2, 3, 4}));
        byte[] compressedarr = GzipUtil.compress(new byte[]{1, 2, 3, 4});

        assertArrayEquals(new byte[] {1,2,3,4}, GzipUtil.decompress(compressedbuf).array());
        assertArrayEquals(new byte[] {1,2,3,4}, GzipUtil.decompress(ByteBuffer.wrap(compressedarr)).array());
        assertArrayEquals(new byte[] {1,2,3,4}, GzipUtil.decompress(compressedarr));
    }

    @Test
    public void testDocumentReaderWriter() throws Exception {
        {
            MemoryDocument document = new MemoryDocument("01234567890123456789");

            Token tok1 = new Token(document).setRange(1, 4).putProperty(POS, "NN");
            Token tok2 = new Token(document).setRange(6, 9).putProperty(POS, "PM");
            Token tok3 = new Token(document).setRange(10, 12).putProperty(POS, "JJ");
            Token tok4 = new Token(document).setRange(12, 19).putProperty(POS, "DT");

            DependencyRelation dep1 = tok1.connect(tok2, new DependencyRelation()).setRelation("DT");
            DependencyRelation dep2 = tok3.connect(tok2, new DependencyRelation()).setRelation("OO");
            DependencyRelation dep3 = tok4.connect(tok3, new DependencyRelation()).setRelation("IK");

            Sentence sent1 = new Sentence(document).setRange(0, 12);
            Sentence sent2 = new Sentence(document).setRange(12, 19);

            Paragraph par1 = new Paragraph(document).setRange(0, 19);

            DocumentFileWriter writer = new DocumentFileWriter(new File("test.docs"));
            writer.write(document);
            writer.close();
        }
        {
            DocumentReader reader = new DocumentFileReader(new File("test.docs"));
            Document doc = reader.next();
            assertNotNull(doc);

            assertEquals(4, doc.nodes(Token.class).count());
            assertEquals(3, doc.edges(DependencyRelation.class).count());
            assertEquals(2, doc.nodes(Sentence.class).count());
            assertEquals(1, doc.nodes(Paragraph.class).count());

            List<Token> token = doc.annotations(Token.class).toList();
            assertEquals(4, token.size());
            Token tok1 = token.get(0);
            Token tok2 = token.get(1);
            Token tok3 = token.get(2);
            Token tok4 = token.get(3);

            assertEquals(1, tok1.outboundEdges(DependencyRelation.class).count());
            assertEquals("DT", tok1.outboundEdges(DependencyRelation.class).first().getRelation());

            List<DependencyRelation> rels = tok2.inboundEdges(DependencyRelation.class).toList();
            assertEquals(2, rels.size());

            assertEquals("DT", rels.get(0).getRelation());
            assertEquals("OO", rels.get(1).getRelation());

            assertEquals("123", tok1.text());
            assertEquals("678", tok2.text());
            assertEquals("01", tok3.text());
            assertEquals("2345678", tok4.text());
        }
    }

    @Test
    public void testDocumentBlockReaderWriter() throws Exception {
        {
            MemoryDocument document = new MemoryDocument("01234567890123456789");

            Token tok1 = new Token(document).setRange(1, 4).putProperty(POS, "NN");
            Token tok2 = new Token(document).setRange(6, 9).putProperty(POS, "PM");
            Token tok3 = new Token(document).setRange(10, 12).putProperty(POS, "JJ");
            Token tok4 = new Token(document).setRange(12, 19).putProperty(POS, "DT");

            DependencyRelation dep1 = tok1.connect(tok2, new DependencyRelation()).setRelation("DT");
            DependencyRelation dep2 = tok3.connect(tok2, new DependencyRelation()).setRelation("OO");
            DependencyRelation dep3 = tok4.connect(tok3, new DependencyRelation()).setRelation("IK");

            Sentence sent1 = new Sentence(document).setRange(0, 12);
            Sentence sent2 = new Sentence(document).setRange(12, 19);

            Paragraph par1 = new Paragraph(document).setRange(0, 19);

            DocumentBlockFileWriter writer = new DocumentBlockFileWriter(new File("test.docs"));
            writer.write(document);
            writer.close();
        }
        {
            DocumentReader reader = new DocumentBlockFileReader(new File("test.docs"));
            Document doc = reader.next();
            assertNotNull(doc);

            assertEquals(4, doc.nodes(Token.class).count());
            assertEquals(3, doc.edges(DependencyRelation.class).count());
            assertEquals(2, doc.nodes(Sentence.class).count());
            assertEquals(1, doc.nodes(Paragraph.class).count());

            List<Token> token = doc.annotations(Token.class).toList();
            assertEquals(4, token.size());
            Token tok1 = token.get(0);
            Token tok2 = token.get(1);
            Token tok3 = token.get(2);
            Token tok4 = token.get(3);

            assertEquals(1, tok1.outboundEdges(DependencyRelation.class).count());
            assertEquals("DT", tok1.outboundEdges(DependencyRelation.class).first().getRelation());

            List<DependencyRelation> rels = tok2.inboundEdges(DependencyRelation.class).toList();
            assertEquals(2, rels.size());

            assertEquals("DT", rels.get(0).getRelation());
            assertEquals("OO", rels.get(1).getRelation());

            assertEquals("123", tok1.text());
            assertEquals("678", tok2.text());
            assertEquals("01", tok3.text());
            assertEquals("2345678", tok4.text());
        }
    }
}

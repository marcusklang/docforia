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

import org.junit.Test;
import se.lth.cs.docforia.data.*;
import se.lth.cs.docforia.graph.TokenProperties;
import se.lth.cs.docforia.graph.ast.AstNode;
import se.lth.cs.docforia.graph.ast.AstTextNode;
import se.lth.cs.docforia.graph.hypertext.Anchor;
import se.lth.cs.docforia.graph.text.*;
import se.lth.cs.docforia.io.DocumentIO;
import se.lth.cs.docforia.memstore.MemoryDocument;
import se.lth.cs.docforia.memstore.MemoryDocumentFactory;
import se.lth.cs.docforia.query.*;
import se.lth.cs.docforia.query.Proposition;
import se.lth.cs.docforia.util.DocumentIterable;
import se.lth.cs.docforia.util.DocumentIterables;
import se.lth.cs.docforia.util.GetNode;
import se.lth.cs.docforia.util.Iterables;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static se.lth.cs.docforia.graph.TokenProperties.NORMALIZED;
import static se.lth.cs.docforia.graph.TokenProperties.STOPWORD;

/**
 * Primary test code
 */
@SuppressWarnings("unchecked")
public abstract class ModelTest {

    public abstract DocumentFactory documentFactory();

    public DocumentIO documentIO() {
        return documentFactory().io();
    }

    @Test
    public void testHelloWorld() {
        Document doc = new MemoryDocument("Greetings from Lund, Sweden!");
        //                                 01234567890123456789012345678

        Token Greetings   = new Token(doc).setRange(0,  9);
        Token from        = new Token(doc).setRange(10, 14);
        Token Lund        = new Token(doc).setRange(15, 19);
        Token comma       = new Token(doc).setRange(19, 20);
        Token Sweden      = new Token(doc).setRange(21, 27);
        Token exclamation = new Token(doc).setRange(27, 28);

        Sentence grettingsSentence = new Sentence(doc).setRange(0, 28);

        NamedEntity lundSwedenEntity
                = new NamedEntity(doc).setRange(Lund.getStart(), Sweden.getEnd())
                                      .setLabel("Location");

        NodeTVar<Token> T = Token.var();
        NodeTVar<NamedEntity> NE = NamedEntity.var();

        List<Token> lundLocation = doc.select(T, NE)
                                      .where(T).coveredBy(NE)
                                      .orderByRange(T)
                                      .query()
                                      .map(GetNode.of(T))
                                      .toList();

        assert lundLocation.size() == 3;
        for (Token token : lundLocation) {
            System.out.println(token);
        }

        GroupProposition group = doc.select(T, NE)
                                    .where(T).coveredBy(NE)
                                    .orderByRange(T)
                                    .groupBy(NE)
                                    .query()
                                    .first();

        NamedEntity ne = group.key(NE);
        System.out.println(ne);

        assert group.list(T).size() == 3;
        for (Token token : group.list(T)) {
            System.out.println(token);
        }
    }

    @Test
    public void testAstDocument() {
        String text = "[[Helsingborg]] är en [[stad|Stad_(Sverige)]] i [[Sverige]]. \n\n" +
                "I Helsingborg finns [[K&aumlrnan]].";

        Document doc = documentFactory().createFragment("sv.wikipedia:Helsingborg", text);
        AstNode page =doc.add(new AstNode()).setName("page");

        AstNode paragraph1 =doc.add(new AstNode()).setName("paragraph");
        page.addNode(paragraph1);

        AstNode anchor1 = doc.add(new AstNode()).setName("anchor");
        anchor1.addText(2, 13);
        anchor1.putProperty("target", "Helsingborg");
        paragraph1.addNode(anchor1);

        paragraph1.addText(15,22); // " är en "

        AstNode anchor2 =doc.add(new AstNode()).setName("anchor");
        anchor2.addText(24, 28);
        anchor2.putProperty("target", "Stad_(Sverige)");
        paragraph1.addNode(anchor2);

        paragraph1.addText(45, 48); // " i "

        AstNode anchor3 =doc.add(new AstNode()).setName("anchor");
        anchor3.addText(50,57);
        anchor3.putProperty("target", "Sverige");
        paragraph1.addNode(anchor3);

        paragraph1.addText(59, 60); //"."
        paragraph1.addLiteralText(61,63, "\n\n");

        AstNode paragraph2 =doc.add(new AstNode()).setName("paragraph");
        page.addNode(paragraph2);

        paragraph2.addText(63, 83);

        AstNode anchor4 =doc.add(new AstNode()).setName("anchor");
        anchor4.addLiteralText(85, 95, "Kärnan");

        paragraph2.addNode(anchor4);

        paragraph2.addText(97, text.length()); // "."

        StringBuilder sb = new StringBuilder();
        for(AstTextNode node : doc.nodes(AstTextNode.class)) {
            sb.append(node.text());
        }

        assertEquals("Helsingborg är en stad i Sverige.\n\nI Helsingborg finns Kärnan.", sb.toString());
    }

    private static class Tokens {
        public Token tok_s1t1;
        public Token tok_s1t2;
        public Token tok_s1t3;
        public Token tok_s1t4;
        public Token tok_s1t5;
        public Token tok_s1t6;
        public Token tok_s1t7;

        public Token tok_s2t1;
        public Token tok_s2t2;
        public Token tok_s2t3;
        public Token tok_s2t4;
        public Token tok_s2t5;
    }

    @Test
    public void testWindowQuery1() {
        Tokens tokens = new Tokens();
        Document doc = createTokenSentenceDocument(tokens);

        Token stad = tokens.tok_s1t4;
        NodeTVar<Token> T = Token.var();

        List<Token> window = doc.select(T).where(T).inWindowOf(stad, 1, true).orderByRange(T).query().map(GetNode.of(T)).toList();

        assertEquals(3,window.size());
        assertEquals("en", window.get(0).text());
        assertEquals("stad", window.get(1).text());
        assertEquals("i", window.get(2).text());

        window = doc.select(T).where(T).inWindowOf(stad, 1).orderByRange(T).query().map(GetNode.of(T)).toList();
        assertEquals(3,window.size());
        assertEquals("en", window.get(0).text());
        assertEquals("stad", window.get(1).text());
        assertEquals("i", window.get(2).text());

        window = doc.select(T).where(T).inWindowOf(tokens.tok_s1t1, 1, true).orderByRange(T).query().map(GetNode.of(T)).toList();
        assertEquals(2,window.size());
        assertEquals("Helsingborg", window.get(0).text());
        assertEquals("är", window.get(1).text());

        window = doc.select(T).where(T).inWindowOf(tokens.tok_s1t1, 1).orderByRange(T).query().map(GetNode.of(T)).toList();
        assertEquals(0,window.size());

        window = doc.select(T).where(T).inWindowOf(tokens.tok_s2t5, 1, true).orderByRange(T).query().map(GetNode.of(T)).toList();
        assertEquals(2,window.size());
        assertEquals("Kärnan", window.get(0).text());
        assertEquals(".", window.get(1).text());

        window = doc.select(T).where(T).inWindowOf(tokens.tok_s2t5, 1).orderByRange(T).query().map(GetNode.of(T)).toList();
        assertEquals(0,window.size());
    }

    @Test
    public void testWindowQuery2() {
        Conny_Andersson conny = new Conny_Andersson();
        Document doc = conny.createDocument(documentFactory());

        NodeTVar<NamedEntity> NE = NamedEntity.var();
        Proposition first = doc.select(NE).where(NE, (Function<NamedEntity, Boolean>) in -> {
            return in.text().equals("Conny Andersson");
        }).orderByRange(NE).query().first();

        assertNotNull(first);

        NamedEntity ne = first.get(NE);

        NodeTVar<Token> T = Token.var();

        List<Token> tokens = doc.select(T).where(T).inWindowOf(ne, 2, true).orderByRange(T).query().map(GetNode.of(T)).toList();
        assertEquals(4, tokens.size());

        assertEquals("Conny", tokens.get(0).text());
        assertEquals("Andersson", tokens.get(1).text());
        assertEquals("är", tokens.get(2).text());
        assertEquals("namnet", tokens.get(3).text());

        tokens = doc.select(T).where(T).inWindowOf(ne, 0, 2).orderByRange(T).query().map(GetNode.of(T)).toList();
        assertEquals(4, tokens.size());

        assertEquals("Conny", tokens.get(0).text());
        assertEquals("Andersson", tokens.get(1).text());
        assertEquals("är", tokens.get(2).text());
        assertEquals("namnet", tokens.get(3).text());

        tokens = doc.select(T).where(T).inWindowOf(ne, 0, 2).orderByRange(T).query().map(GetNode.of(T)).toList();
        assertEquals(4, tokens.size());

        assertEquals("Conny", tokens.get(0).text());
        assertEquals("Andersson", tokens.get(1).text());
        assertEquals("är", tokens.get(2).text());
        assertEquals("namnet", tokens.get(3).text());

        DocumentIterable<NamedEntity> query = doc.select(NE).where(NE, (Function<NamedEntity, Boolean>) in -> {
            return in.text().equals("Conny Andersson");
        }).orderByRange(NE).query().map(GetNode.of(NE));
        Iterator<NamedEntity> iterator = query.iterator();
        assertTrue(iterator.hasNext());
        iterator.next();

        assertTrue(iterator.hasNext());
        ne = iterator.next();

        tokens = doc.select(T).where(T).inWindowOf(ne, 2).orderByRange(T).query().map(GetNode.of(T)).toList();
        assertEquals(6, tokens.size());

        assertEquals("personer", tokens.get(0).text());
        assertEquals(":", tokens.get(1).text());
        assertEquals("Conny", tokens.get(2).text());
        assertEquals("Andersson", tokens.get(3).text());
        assertEquals("(", tokens.get(4).text());
        assertEquals("skådespelare", tokens.get(5).text());

        tokens = doc.select(T).where(T).inWindowOf(ne, 2,1).orderByRange(T).query().map(GetNode.of(T)).toList();
        assertEquals(5, tokens.size());

        assertEquals("personer", tokens.get(0).text());
        assertEquals(":", tokens.get(1).text());
        assertEquals("Conny", tokens.get(2).text());
        assertEquals("Andersson", tokens.get(3).text());
        assertEquals("(", tokens.get(4).text());

        tokens = doc.select(T).where(T).inWindowOf(ne, 1,0).orderByRange(T).query().map(GetNode.of(T)).toList();
        assertEquals(3, tokens.size());

        assertEquals(":", tokens.get(0).text());
        assertEquals("Conny", tokens.get(1).text());
        assertEquals("Andersson", tokens.get(2).text());

        tokens = doc.select(T).where(T).inWindowOf(ne, 0,1).orderByRange(T).query().map(GetNode.of(T)).toList();
        assertEquals(3, tokens.size());

        assertEquals("Conny", tokens.get(0).text());
        assertEquals("Andersson", tokens.get(1).text());
        assertEquals("(", tokens.get(2).text());

        tokens = doc.select(T).where(T).inWindowOf(ne, 0).orderByRange(T).query().map(GetNode.of(T)).toList();
        assertEquals(2, tokens.size());

        assertEquals("Conny", tokens.get(0).text());
        assertEquals("Andersson", tokens.get(1).text());
    }

    protected Document createTokenSentenceDocument(Tokens tokens) {
        String text = "Helsingborg är en stad i Sverige.\r\n\r\nI Helsingborg finns Kärnan.";

        Document doc = documentFactory().createFragment("sv.wikipedia:Helsingborg", text);
        Token tok_s1t1 = tokens.tok_s1t1 = doc.add(new Token()).setRange(0, 11)
                .putProperty("id", "s1t1") //Helsingborg
                .putProperty("pos", "NN");

        assertEquals("Helsingborg", tok_s1t1.text());

        Token tok_s1t2 = tokens.tok_s1t2 =doc.add(new Token()).setRange(12, 14)
                                                        .putProperty("id", "s1t2"); //är

        assertEquals("är", tok_s1t2.text());

        Token tok_s1t3 = tokens.tok_s1t3 =doc.add(new Token()).setRange(15, 17)
                                                        .putProperty("id", "s1t3"); //en

        assertEquals("en", tok_s1t3.text());

        Token tok_s1t4 = tokens.tok_s1t4 = doc.add(new Token()).setRange(18, 22)
                                                         .putProperty("id", "s1t4"); //stad

        assertEquals("stad", tok_s1t4.text());

        Token tok_s1t5 = tokens.tok_s1t5 = doc.add(new Token()).setRange(23, 24)
                                                         .putProperty("id", "s1t5"); //i

        assertEquals("i", tok_s1t5.text());

        Token tok_s1t6 = tokens.tok_s1t6 =doc.add(new Token()).setRange(25, 32)
                                                        .putProperty("id", "s1t6"); //Sverige

        assertEquals("Sverige", tok_s1t6.text());

        Token tok_s1t7 = tokens.tok_s1t7 = doc.add(new Token()).setRange(32, 33)
                                                         .putProperty("id", "s1t7"); //.

        assertEquals(".", tok_s1t7.text());

        Sentence s1 = doc.add(new Sentence()).setRange(0, 33);
        assertEquals("Helsingborg är en stad i Sverige.",s1.text());

        //Out of order on purpose!
        Token tok_s2t4 = tokens.tok_s2t4 = doc.add(new Token()).setRange(57, 63)
                                                         .putProperty("id", "s2t4"); //Kärnan

        assertEquals("Kärnan", tok_s2t4.text());

        Token tok_s2t1 = tokens.tok_s2t1 = doc.add(new Token()).setRange(37, 38)
                                                         .putProperty("id", "s2t1"); //I

        assertEquals("I", tok_s2t1.text());

        Token tok_s2t2 = tokens.tok_s2t2 = doc.add(new Token()).setRange(39, 50)
                                                         .putProperty("id", "s2t2"); //Helsingborg

        assertEquals("Helsingborg", tok_s2t2.text());

        Token tok_s2t3 = tokens.tok_s2t3 = doc.add(new Token()).setRange(51, 56)
                                                         .putProperty("id", "s2t3"); //finns

        assertEquals("finns", tok_s2t3.text());

        Token tok_s2t5 = tokens.tok_s2t5 = doc.add(new Token()).setRange(63, 64)
                                                         .putProperty("id", "s2t5"); //.

        assertEquals(".", tok_s2t5.text());

        Sentence s2 = doc.add(new Sentence()).setRange(37, 64);
        assertEquals("I Helsingborg finns Kärnan.",s2.text());

        tok_s1t1.connect(tok_s1t2, doc.add(new DependencyRelation()).setRelation("SS"));
        //tok_s1t2 == ROOT
        tok_s1t3.connect(tok_s1t4, doc.add(new DependencyRelation()).setRelation("DT"));
        tok_s1t4.connect(tok_s1t2, doc.add(new DependencyRelation()).setRelation("SP"));
        tok_s1t5.connect(tok_s1t4, doc.add(new DependencyRelation()).setRelation("ET"));
        tok_s1t6.connect(tok_s1t5, doc.add(new DependencyRelation()).setRelation("PA"));
        tok_s1t7.connect(tok_s1t2, doc.add(new DependencyRelation()).setRelation("IP"));

        tok_s2t1.connect(tok_s2t3, doc.add(new DependencyRelation()).setRelation("RA"));
        tok_s2t2.connect(tok_s2t1, doc.add(new DependencyRelation()).setRelation("PA"));
        //tok_s2t3 == ROOT
        tok_s2t4.connect(tok_s2t3, doc.add(new DependencyRelation()).setRelation("SS"));
        tok_s2t5.connect(tok_s2t3, doc.add(new DependencyRelation()).setRelation("IP"));

        return doc;
    }

    @Test
    public void testIntersection() {
        Document doc = documentFactory().createFragment("dynamic", "");
        doc.setText("Chardonnay-viner är även en av tre tillåtna druvor i champagne-produktion.");
        /*           01234567890123456789012345678901234567890123456789012345678901234567890123
                     0         1         2         3         4         5         6         7
        */
        Sentence s1 = doc.add(new Sentence()).setRange(0,74);

        Token t1 = doc.add(new Token()).setRange(0,16);
        Token t2 = doc.add(new Token()).setRange(17,19);
        Token t3 = doc.add(new Token()).setRange(20,24);
        Token t4 = doc.add(new Token()).setRange(25,27);
        Token t5 = doc.add(new Token()).setRange(28,30);
        Token t6 = doc.add(new Token()).setRange(31,34);
        Token t7 = doc.add(new Token()).setRange(35,43);
        Token t8 = doc.add(new Token()).setRange(44, 50);
        Token t9 = doc.add(new Token()).setRange(51, 52);
        Token t10 = doc.add(new Token()).setRange(53,73);
        Token t11 = doc.add(new Token()).setRange(73, 74);

        Anchor a1 = doc.add(new Anchor()).setRange(0, 10);
        Anchor a2 = doc.add(new Anchor()).setRange(40, 62);

        NodeTVar<Token> T = Token.var();
        NodeTVar<Sentence> S = Sentence.var();

        assertNull(doc.select(T).where(T).coveredBy(a1).query().first());

        assertEquals(t1, doc.select(T).where(T).intersects(a1).query().map(GetNode.of(T)).first());
        assertEquals(s1, doc.select(S).where(S).intersects(a1).query().map(GetNode.of(S)).first());
        assertEquals(s1, doc.select(S).where(S).covering(a1).query().map(GetNode.of(S)).first());

        List<Token> tokens = doc.select(T).where(T).intersects(a2).orderByRange(T).query().map(GetNode.of(T)).toList();
        assertEquals(4, tokens.size());
        assertEquals(t7, tokens.get(0));
        assertEquals(t8, tokens.get(1));
        assertEquals(t9, tokens.get(2));
        assertEquals(t10, tokens.get(3));

        tokens = doc.select(T).where(T).coveredBy(a2).orderByRange(T).query().map(GetNode.of(T)).toList();
        assertEquals(2, tokens.size());
        assertEquals(t8, tokens.get(0));
        assertEquals(t9, tokens.get(1));
    }

    @Test
    public void testImportNodes() {
        Tokens tokens = new Tokens();
        Document doc = createTokenSentenceDocument(tokens);

        Document newDoc = documentFactory().createFragment("main", "");
        newDoc.setText("0123456789" + doc.getText());

        newDoc.importNodes(doc,10);
        Sentence sent = newDoc.nodes(Sentence.class).first();
        assertNotNull(sent);
        assertEquals(10, sent.getStart());
        assertEquals(43, sent.getEnd());

        Token tok = newDoc.nodes(Token.class).first();
        assertNotNull(tok);
        assertEquals(10,tok.getStart());
        assertEquals(21,tok.getEnd());

        DependencyRelation deprel = tok.outboundEdges(DependencyRelation.class).first();
        assertNotNull(deprel);
        assertEquals("SS", deprel.getRelation());
        assertEquals("är", deprel.getHead().text());
    }

    @Test
    public void testPropertySearch() {
        Tokens toks = new Tokens();
        Document doc = createTokenSentenceDocument(toks);
        assertEquals(toks.tok_s1t1, doc.nodesWithProperty("id", "s1t1").first());
        assertEquals(toks.tok_s1t2, doc.nodesWithProperty("id", "s1t2").first());
        assertEquals(toks.tok_s1t3, doc.nodesWithProperty("id", "s1t3").first());
        assertEquals(toks.tok_s1t4, doc.nodesWithProperty("id", "s1t4").first());
        assertEquals(toks.tok_s1t5, doc.nodesWithProperty("id", "s1t5").first());
        assertEquals(toks.tok_s1t6, doc.nodesWithProperty("id", "s1t6").first());
        assertEquals(toks.tok_s1t7, doc.nodesWithProperty("id", "s1t7").first());

        assertEquals(toks.tok_s2t1, doc.nodesWithProperty("id", "s2t1").first());
        assertEquals(toks.tok_s2t2, doc.nodesWithProperty("id", "s2t2").first());
        assertEquals(toks.tok_s2t3, doc.nodesWithProperty("id", "s2t3").first());
        assertEquals(toks.tok_s2t4, doc.nodesWithProperty("id", "s2t4").first());
        assertEquals(toks.tok_s2t5, doc.nodesWithProperty("id", "s2t5").first());

        assertEquals(toks.tok_s1t1, doc.nodesWithProperty(Token.class, "id", "s1t1").first());
        assertEquals(toks.tok_s1t2, doc.nodesWithProperty(Token.class, "id", "s1t2").first());
        assertEquals(toks.tok_s1t3, doc.nodesWithProperty(Token.class, "id", "s1t3").first());
        assertEquals(toks.tok_s1t4, doc.nodesWithProperty(Token.class, "id", "s1t4").first());
        assertEquals(toks.tok_s1t5, doc.nodesWithProperty(Token.class, "id", "s1t5").first());
        assertEquals(toks.tok_s1t6, doc.nodesWithProperty(Token.class, "id", "s1t6").first());
        assertEquals(toks.tok_s1t7, doc.nodesWithProperty(Token.class, "id", "s1t7").first());

        assertEquals(toks.tok_s2t1, doc.nodesWithProperty(Token.class, "id", "s2t1").first());
        assertEquals(toks.tok_s2t2, doc.nodesWithProperty(Token.class, "id", "s2t2").first());
        assertEquals(toks.tok_s2t3, doc.nodesWithProperty(Token.class, "id", "s2t3").first());
        assertEquals(toks.tok_s2t4, doc.nodesWithProperty(Token.class, "id", "s2t4").first());
        assertEquals(toks.tok_s2t5, doc.nodesWithProperty(Token.class, "id", "s2t5").first());

        assertNull(doc.nodesWithProperty(Sentence.class, "id", "s1t1").first());
        assertNull(doc.nodesWithProperty(Sentence.class, "id", "s1t2").first());
        assertNull(doc.nodesWithProperty(Sentence.class, "id", "s1t3").first());
        assertNull(doc.nodesWithProperty(Sentence.class, "id", "s1t4").first());
        assertNull(doc.nodesWithProperty(Sentence.class, "id", "s1t5").first());
        assertNull(doc.nodesWithProperty(Sentence.class, "id", "s1t6").first());
        assertNull(doc.nodesWithProperty(Sentence.class, "id", "s1t7").first());

        assertNull(doc.nodesWithProperty(Sentence.class, "id", "s2t1").first());
        assertNull(doc.nodesWithProperty(Sentence.class, "id", "s2t2").first());
        assertNull(doc.nodesWithProperty(Sentence.class, "id", "s2t3").first());
        assertNull(doc.nodesWithProperty(Sentence.class, "id", "s2t4").first());
        assertNull(doc.nodesWithProperty(Sentence.class, "id", "s2t5").first());
    }

    protected DocumentStorageLevel optimizationLevel() {
        return DocumentStorageLevel.LEVEL_2;
    }

    @Test
    public void testSerializedAdd() {
        Tokens toks = new Tokens();
        Document doc = createTokenSentenceDocument(toks);
        doc = serializeDeserialize(doc);

        //Token(doc, 0, 11) = Helsingborg
        //Token(doc, 25, 32) = Sverige
        //Token(doc, 39, 50) = Helsingborg
        //Token(doc, 57, 63) = Kärnan

        doc.add(new NamedEntity()).setLabel("city").setRange(0, 11);
        doc.add(new NamedEntity()).setLabel("country").setRange(25, 32);
        doc.add(new NamedEntity()).setLabel("city").setRange(39, 50);
        doc.add(new NamedEntity()).setRange(57, 63);

        Iterator<NamedEntity> neIter = doc.nodes(NamedEntity.class).iterator();
        assertTrue(neIter.hasNext());
        assertEquals("city", neIter.next().getLabel());
        assertEquals("country", neIter.next().getLabel());
        assertEquals("city", neIter.next().getLabel());
        assertEquals("Kärnan", neIter.next().text());
        assertFalse(neIter.hasNext());

        doc = serializeDeserialize(doc);

        neIter = doc.nodes(NamedEntity.class).iterator();
        assertTrue(neIter.hasNext());
        assertEquals("city", neIter.next().getLabel());
        assertEquals("country", neIter.next().getLabel());
        assertEquals("city", neIter.next().getLabel());
        assertEquals("Kärnan", neIter.next().text());
        assertFalse(neIter.hasNext());
    }

    @Test
    public void testDocumentAppend() {
        Document doc = documentFactory().createFragment("main", "0123456789");
        Token head = doc.add(new Token()).setRange(0, 3);
        Token tail = doc.add(new Token()).setRange(6, 9);
        doc.add(new DependencyRelation(), tail, head);

        Document appenddoc = documentFactory().createFragment("appended", "--APPEND--");
        Token appendTok = appenddoc.add(new Token()).setRange(2,8);
        DynamicNode dynNode = appenddoc.add(new DynamicNode(), "test");
        DynamicEdge dynEdge = appenddoc.add(new DynamicEdge(), "relation", appendTok, dynNode);

        doc.append(appenddoc);

        NodeVar T = Token.var();

        Iterable<Proposition> query = doc.select(T).query();
        Iterator<Proposition> iter = query.iterator();

        assertTrue(iter.hasNext());
        Token tok1 = iter.next().get(T);

        assertTrue(iter.hasNext());
        Token tok2 = iter.next().get(T);

        assertTrue(iter.hasNext());
        Token tok3 = iter.next().get(T);
        assertFalse(iter.hasNext());

        assertEquals("012", tok1.text());
        assertEquals("678", tok2.text());
        assertEquals("APPEND", tok3.text());

        tok1.putProperty("new-prop", "orginal-does-not-have-this");

        assertTrue(head.hasProperty("new-prop"));

        assertEquals(tok1,tok2.outboundNodes(DependencyRelation.class, Token.class).first());

        Node tok4 = tok3.outboundNodes("relation", "test").first();
        tok4.putProperty("new-prop2", "yet-another");

        assertFalse(dynNode.hasProperty("new-prop2"));
        assertTrue(tok4.hasProperty("new-prop2"));

        assertEquals("0123456789--APPEND--", doc.getText());
    }

    @Test
    public void testDocumentView1() {
        Document doc = documentFactory().createFragment("main", "0123456789");
        View view = doc.view(3, 8);

        Token tok = view.add(new Token()).setRange(0, 2);
        assertEquals("34",tok.text());
        assertEquals(0, tok.getStart());
        assertEquals(2, tok.getEnd());

        NodeVar T = Token.var();
        Token tok2 = doc.select(T).query().first().get(T);
        assertTrue(tok2 != tok);
        assertEquals(3, tok2.getStart());
        assertEquals(5, tok2.getEnd());
    }

    @Test
    public void testDocumentView2() {
        Document doc = new Conny_Andersson().createDocument(documentFactory());

        View view = doc.view(47, 135);

        NodeVar S = Sentence.var();
        Sentence sent = view.select(S).orderByRange(S).query().first().get(S);
        assertEquals(0, sent.getStart());
        assertEquals(88, sent.getEnd());
        assertEquals("Conny Andersson (skådespelare) Conny Andersson (racerförare) Conny Andersson (politiker)", sent.text());

        NodeVar T = Token.var();
        Iterable<Proposition> result = view.select(T).where(T).coveredBy(0,15).coveredBy(sent).query();
        Iterator<Proposition> iter = result.iterator();

        assertTrue(iter.hasNext());
        Token tok1 = iter.next().get(T);
        assertEquals("Conny", tok1.text());

        assertTrue(iter.hasNext());
        Token tok2 = iter.next().get(T);
        assertEquals("Andersson", tok2.text());

        assertFalse(iter.hasNext());

        Sentence realSent = doc.select(S).where(S).coveredBy(47, 135).query().first().get(S);
        assertTrue(sent != realSent);

        assertEquals(47, realSent.getStart());
        assertEquals(135, realSent.getEnd());
        assertEquals(0, sent.getStart());
        assertEquals(88, sent.getEnd());

        NodeVar N = NamedEntity.var();
        Iterable<GroupProposition> groups = view.select(T, N).where(T).coveredBy(N).orderByRange(T).groupBy(N).query();
        Iterator<GroupProposition> giter = groups.iterator();

        assertTrue(giter.hasNext());
        NamedEntity ne1 = giter.next().key().get(N);

        assertTrue(giter.hasNext());
        NamedEntity ne2 = giter.next().key().get(N);

        assertTrue(giter.hasNext());
        NamedEntity ne3 = giter.next().key().get(N);

        assertFalse(giter.hasNext());

        assertEquals(0, ne1.getStart());
        assertEquals(15, ne1.getEnd());
        assertEquals("Conny Andersson", ne1.text());

        assertEquals(31, ne2.getStart());
        assertEquals(46, ne2.getEnd());
        assertEquals("Conny Andersson", ne2.text());

        assertEquals(61, ne3.getStart());
        assertEquals(76, ne3.getEnd());
        assertEquals("Conny Andersson", ne3.text());

        ne1.putProperty("example", 1);

        NamedEntity realNe1 = doc.select(N).where(N).coveredBy(47,62).query().first().get(N);
        assertTrue(realNe1 != ne1);
        assertTrue(realNe1.hasProperty("example"));
        assertEquals("1", realNe1.getProperty("example"));
    }

    public static class Detection extends Node {

        public Detection() {
            super();
        }

    }

    private void validateGroup(NodeVar T, NodeVar A, GroupProposition group) {
        List<Proposition> tokenGroup = group.values();

        assertTrue(tokenGroup.size() > 0);

        for(Proposition prop : tokenGroup) {
            assertTrue(prop.get(T).hasProperty(NORMALIZED));
            assertFalse(prop.get(T).hasProperty(STOPWORD));
        }

        assertTrue(group.key().get(A).hasProperty("resolved"));
    }

    @Test
    public void testQueryGroupBy() {
        Conny_Andersson conny = new Conny_Andersson();
        Document doc = conny.createDocument(documentFactory());

        NodeVar T = Token.var();
        NodeVar A = Anchor.var();
        DocumentIterable<GroupProposition> iterable = doc.select(A,T)
                                                         .where(T).property(NORMALIZED).exists()
                                                         .property(STOPWORD).notExists()
                                                         .coveredBy(A)
                                                         .where(A).property("resolved").exists()
                                                         .orderByRange(T)
                                                         .groupBy(A)
                                                         .query();

        Iterator<GroupProposition> iter = iterable.iterator();

        GroupProposition group = iter.next();
        validateGroup(T,A, group);
        assertEquals(3, group.size());
        assertEquals("conny", group.value(0,T).getProperty(NORMALIZED));
        assertEquals("andersson", group.value(1,T).getProperty(NORMALIZED));
        assertEquals("skådespelare", group.value(2,T).getProperty(NORMALIZED));

        assertEquals("wikidata:Q5162071", group.key(A).getProperty("resolved"));

        doc.add(new Detection()).setRange(group.key(A).getStart(), group.key(A).getEnd());

        group = iter.next();
        validateGroup(T,A,group);
        assertEquals(3, group.size());
        assertEquals("conny", group.value(0,T).getProperty(NORMALIZED));
        assertEquals("andersson", group.value(1,T).getProperty(NORMALIZED));
        assertEquals("racerförare", group.value(2,T).getProperty(NORMALIZED));

        assertEquals("wikidata:Q172242", group.key(A).getProperty("resolved"));

        doc.add(new Detection()).setRange(group.key(A).getStart(), group.key(A).getEnd());

        assertFalse(iter.hasNext());

        NodeVar D = new NodeVar(Detection.class);
        List<Proposition> detections = doc.select(D).orderByRange(D).query().toList();
        assertEquals(2, detections.size());

        assertEquals("Conny Andersson (skådespelare)", detections.get(0).get(D).text());
        assertEquals("Conny Andersson (racerförare)", detections.get(1).get(D).text());
    }

    private static String text(Node node) {
        return node.text();
    }

    public <T> void assertArrayListEquals(List<T> output, T...expected) {
        assertEquals(expected.length, output.size());
        for(int i = 0; i < expected.length; i++) {
            assertEquals("Value at " + i + " did not match expected.", expected[i], output.get(i));
        }
    }

    private void verifyCoreference(Document doc) {
        assertEquals(3,doc.annotations(CoreferenceMention.class, "1").count());
        assertEquals(2,doc.annotations(CoreferenceMention.class, "2").count());
        assertEquals(3,doc.annotations(CoreferenceMention.class, "3").count());
        assertEquals(4,doc.annotations(CoreferenceMention.class, "4").count());
        assertEquals(2,doc.annotations(CoreferenceMention.class, "5").count());
        assertEquals(3,doc.annotations(CoreferenceMention.class, "6").count());

        assertArrayListEquals(doc.annotations(CoreferenceMention.class, "1").map(ModelTest::text).toList(), "Marcus", "Marcus", "Marcus");
        assertArrayListEquals(doc.annotations(CoreferenceMention.class, "2").map(ModelTest::text).toList(), "Pierre", "Pierre");
        assertArrayListEquals(doc.annotations(CoreferenceMention.class, "3").map(ModelTest::text).toList(), "Peter", "Peter", "Peter");
        assertArrayListEquals(doc.annotations(CoreferenceMention.class, "4").map(ModelTest::text).toList(), "MLDM", "MLDM", "MLDM", "MLDM");
        assertArrayListEquals(doc.annotations(CoreferenceMention.class, "5").map(ModelTest::text).toList(), "Marcus, Pierre and Peter", "They");
        assertArrayListEquals(doc.annotations(CoreferenceMention.class, "6").map(ModelTest::text).toList(), "Marcus", "Peter", "Marcus and Peter");

        NodeTVar<CoreferenceMention> C1 = CoreferenceMention.var("2");
        NodeTVar<CoreferenceMention> C2 = CoreferenceMention.var("5");

        Iterator<Proposition> iter = doc.select(C1, C2).where(C1).coveredBy(C2).query().iterator();
        assertTrue(iter.hasNext());
        Proposition prop = iter.next();
        assertEquals("Pierre", prop.get(C1).text());
        assertEquals("Marcus, Pierre and Peter", prop.get(C2).text());

        assertArrayListEquals(doc.engine().nodeLayerVariants(Document.nodeLayer(CoreferenceMention.class)).toList(), "1", "2", "3", "4", "5", "6");
        assertArrayListEquals(doc.engine().nodeLayerAllVariants(Document.nodeLayer(CoreferenceMention.class)).toList(),
                              Optional.of("1"),
                              Optional.of("2"),
                              Optional.of("3"),
                              Optional.of("4"),
                              Optional.of("5"),
                              Optional.of("6"));
    }

    @Test
    public void testCoreference() {
        Document doc = documentFactory()
                .createFragment("sv.wikipedia:TestingCoref",
            "Marcus, Pierre and Peter from LTH. Marcus and Peter shared ideas on MLDM. Marcus wrote MLDM. Pierre is a professor. Peter used MLDM. They tested coref in MLDM.");
           //012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678
           //0         1         2         3         4         5         6         7         8         9         10        11        12        13        14        15

        //Chain 1: Marcus
        doc.add(new CoreferenceMention()).setRange(0,6).setVariant("1");
        doc.add(new CoreferenceMention()).setRange(35,41).setVariant("1");
        doc.add(new CoreferenceMention()).setRange(74,80).setVariant("1");

        //Chain 2: Pierre
        doc.add(new CoreferenceMention()).setRange(8,14).setVariant("2");
        doc.add(new CoreferenceMention()).setRange(93,99).setVariant("2");

        //Chain 3: Peter
        doc.add(new CoreferenceMention()).setRange(19,24).setVariant("3");
        doc.add(new CoreferenceMention()).setRange(46,51).setVariant("3");
        doc.add(new CoreferenceMention()).setRange(116,121).setVariant("3");

        //Chain 4: MLDM
        doc.add(new CoreferenceMention()).setRange(68,72).setVariant("4");
        doc.add(new CoreferenceMention()).setRange(87,91).setVariant("4");
        doc.add(new CoreferenceMention()).setRange(127,131).setVariant("4");
        doc.add(new CoreferenceMention()).setRange(154,158).setVariant("4");

        //Chain 5: Marcus, Pierre and Peter
        doc.add(new CoreferenceMention()).setRange(0,24).setVariant("5");
        doc.add(new CoreferenceMention()).setRange(133,137).setVariant("5");

        //Chain 6: Marcus and Peter
        doc.add(new CoreferenceMention()).setRange(0,6).setVariant("6");
        doc.add(new CoreferenceMention()).setRange(19,24).setVariant("6");
        doc.add(new CoreferenceMention()).setRange(35,51).setVariant("6");

        verifyCoreference(doc);
        verifyCoreference(MemoryDocument.fromBytes(doc.toBytes()));
    }

    @Test
    public void testDynamicVariantQuery() {
        Document doc = documentFactory().createFragment("sv.wikipedia:TestingRanges", "01234567890123456789");

        doc.add(new DynamicNode(), "Sentence").setRange(1, 5).putProperty("test", "0").putProperty("neg", "1").setVariant("alpha");
        doc.add(new DynamicNode(), "Sentence").setRange(6, 14).putProperty("test", "1").setVariant("beta");
        doc.add(new DynamicNode(), "Sentence").setRange(1, 7).putProperty("test", "1").setVariant("delta");

        doc.add(new DynamicNode(), "Token").setRange(0, 2).putProperty("pos", "NN").putProperty("ne", "city").setVariant("delta");
        doc.add(new DynamicNode(), "Token").setRange(3, 5).putProperty("pos", "VB");
        doc.add(new DynamicNode(), "Token").setRange(6, 9);
        doc.add(new DynamicNode(), "Token").setRange(10, 12).putProperty("pos", "JJ").setVariant("delta");
        doc.add(new DynamicNode(), "Token").setRange(12, 17).putProperty("pos", "NN");

        doc.add(new DynamicNode(), "NamedEntity").setRange(6, 12).setVariant("zeta");

        doc = serializeDeserialize(doc);

        //1. Validate that the added bits are correct
        NodeVar S_A = DynamicNode.var("Sentence", "alpha");
        NodeVar S_B = DynamicNode.var("Sentence", "beta");
        NodeVar S_D = DynamicNode.var("Sentence", "delta");
        NodeVar S = DynamicNode.var("Sentence");
        NodeVar T = DynamicNode.var("Token");
        NodeVar T_D = DynamicNode.var("Token", "delta");
        NodeVar T_Z = DynamicNode.var("Token", "zeta");
        NodeVar N = DynamicNode.var("NamedEntity");
        NodeVar N_Z = DynamicNode.var("NamedEntity","zeta");

        assertEquals("1234", doc.select(S_A).where(S_A).covering(1, 5).query().first().get(S_A).text());
        assertEquals("67890123", doc.select(S_B).where(S_B).covering(6, 14).query().first().get(S_B).text());
        assertEquals("123456", doc.select(S_D).where(S_D).covering(1, 7).query().first().get(S_D).text());

        assertFalse(doc.select(S).where(S).covering(1, 5).query().any());
        assertFalse(doc.select(S).where(S).covering(6, 14).query().any());

        assertFalse(doc.select(T).where(T).covering(0, 2).query().any());
        assertEquals("01", doc.select(T_D).where(T_D).covering(0, 2).query().first().get(T_D).text());

        assertEquals("34", doc.select(T).where(T).covering(3, 5).query().first().get(T).text());
        assertEquals("678", doc.select(T).where(T).covering(6, 9).query().first().get(T).text());

        assertFalse(doc.select(T).where(T).covering(10, 12).query().any());
        assertEquals("01", doc.select(T_D).where(T_D).covering(10, 12).query().first().get(T_D).text());

        assertEquals("23456", doc.select(T).where(T).covering(12, 17).query().first().get(T).text());

        assertNull(doc.select(T).where(T).covering(6, 12).query().first());

        //2. Do a query selecting named entity tokens
        DocumentIterable<Proposition> query = doc.select(N, S, T)
                                                 .where(T).coveredBy(N)
                                                 .where(N).coveredBy(S)
                                                 .query();

        Iterator<Proposition> iter = query.iterator();
        assertFalse(iter.hasNext());

        query = doc.select(T_D)
                   .where(T_D).coveredBy(N_Z)
                   .where(N_Z).coveredBy(S_B)
                   .query();

        iter = query.iterator();

        assertTrue(iter.hasNext());
        Proposition current = iter.next();
        assertEquals("01", current.get(T_D).text());
        assertEquals("678901", current.get(N_Z).text());
        assertEquals("67890123", current.get(S_B).text());

        assertFalse(iter.hasNext());

        query = doc.select(T)
                   .where(T).coveredBy(S_D)
                   .query();

        iter = query.iterator();

        assertTrue(iter.hasNext());
        current = iter.next();
        assertEquals("34", current.get(T).text());
        assertEquals("123456", current.get(S_D).text());
        assertFalse(iter.hasNext());

        //3. Test range optimizable query
        query = doc.select(T).where(T).coveredBy(5,10).query();
        iter = query.iterator();

        assertTrue(iter.hasNext());
        assertEquals("678", iter.next().get(T).text());
        assertFalse(iter.hasNext());

        //4. Test multi range optimizable query
        query = doc.select(T).where(T).coveredBy(3,12).coveredBy(5, 10).query();
        iter = query.iterator();

        assertTrue(iter.hasNext());
        assertEquals("678",iter.next().get(T).text());
        assertFalse(iter.hasNext());

        //5. Test another query
        query = doc.select(T).where(T).coveredBy(S_A).query();
        iter = query.iterator();

        assertTrue(iter.hasNext());
        current = iter.next();
        assertEquals("34", current.get(T).text());
        assertEquals("1234", current.get(S_A).text());

        assertFalse(iter.hasNext());
        query = doc.select(T).where(T).coveredBy(S_B).query();
        iter = query.iterator();

        assertTrue(iter.hasNext());
        current = iter.next();

        assertEquals("678", current.get(T).text());
        assertEquals("67890123", current.get(S_B).text());
        assertFalse(iter.hasNext());

        //6. Test filter query
        query = doc.select(T,S)
                   .where(T)
                   .coveredBy(S)
                   .where(S).property("test").equals("0")
                   .query();

        iter = query.iterator();

        assertFalse(iter.hasNext());

        query = doc.select(T, S_A)
                   .where(T).coveredBy(S_A)
                   .where(S_A).property("test").equals("0")
                   .query();

        iter = query.iterator();

        assertTrue(iter.hasNext());

        current = iter.next();
        assertEquals("34", current.get(T).text());
        assertEquals("1234", current.get(S_A).text());
        assertFalse(iter.hasNext());

        GroupProposition groupquery = doc.select(S, T)
                                         .where(T)
                                         .property("pos").exists()
                                         .coveredBy(S)
                                         .where(S)
                                         .property("test").exists()
                                         .property("neg").notExists().groupBy(S)
                                         .query()
                                         .first();

        assertNull(groupquery);

        groupquery
                = doc.select(S_B, T_D)
                     .where(T_D).property("pos").exists()
                     .coveredBy(S_B)
                     .where(S_B).property("test").exists()
                     .property("neg").notExists()
                     .groupBy(S_B)
                     .query()
                     .first();

        assertNotNull(groupquery);
        assertEquals("01", groupquery.value(0, T_D).text());

        assertEquals("67890123", groupquery.key(S_B).text());
        assertEquals(1, groupquery.size());

        //7. Test property query
        query = doc.select(T).where(T).property("ne").exists().query();
        iter = query.iterator();
        assertFalse(iter.hasNext());

        query = doc.select(T_D).where(T_D).property("ne").exists().query();
        iter = query.iterator();
        assertTrue(iter.hasNext());
        assertEquals("city", iter.next().get(T_D).getProperty("ne"));
        assertFalse(iter.hasNext());

        //8. Test property value
        query = doc.select(T).where(T).property("pos").equalsAny("NN", "PM").query();
        iter = query.iterator();
        assertTrue(iter.hasNext());
        assertEquals("23456", iter.next().get(T).text());
        assertFalse(iter.hasNext());

        query = doc.select(T_D).where(T_D).property("pos").equalsAny("NN", "PM").query();
        iter = query.iterator();
        assertTrue(iter.hasNext());
        assertEquals("01", iter.next().get(T_D).text());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDynamicQuery() {
        Document doc = documentFactory().createFragment("sv.wikpedia:TestingRanges", "01234567890123456789");

        doc.add(new DynamicNode(), "Sentence").setRange(1, 5).putProperty("test", "0").putProperty("neg", "1");
        doc.add(new DynamicNode(), "Sentence").setRange(6, 14).putProperty("test", "1");

        doc.add(new DynamicNode(), "Token").setRange(0, 2).putProperty("pos", "NN").putProperty("ne", "city");
        doc.add(new DynamicNode(), "Token").setRange(3, 5).putProperty("pos", "VB");
        doc.add(new DynamicNode(), "Token").setRange(6, 9);
        doc.add(new DynamicNode(), "Token").setRange(10, 12).putProperty("pos", "JJ");
        doc.add(new DynamicNode(), "Token").setRange(12, 17).putProperty("pos", "NN");

        doc.add(new DynamicNode(), "NamedEntity").setRange(6, 12);

        doc = serializeDeserialize(doc);

        NodeVar S = DynamicNode.var("Sentence");
        NodeVar T = DynamicNode.var("Token");
        NodeVar N = DynamicNode.var("NamedEntity");

        //1. Validate that the added bits are correct
        assertEquals("1234", doc.select(S).where(S).covering(1, 5).query().first().get(S).text());
        assertEquals("67890123", doc.select(S).where(S).covering(6, 14).query().first().get(S).text());

        assertEquals("01", doc.select(T).where(T).covering(0, 2).query().first().get(T).text());
        assertEquals("34", doc.select(T).where(T).covering(3, 5).query().first().get(T).text());
        assertEquals("678", doc.select(T).where(T).covering(6, 9).query().first().get(T).text());
        assertEquals("01", doc.select(T).where(T).covering(10, 12).query().first().get(T).text());
        assertEquals("23456", doc.select(T).where(T).covering(12, 17).query().first().get(T).text());

        assertNull(doc.select(T).where(T).covering(6, 12).query().first());

        //2. Do a query selecting named entity tokens
        DocumentIterable<Proposition> query = doc.select(T, N, S)
                                                 .where(N).coveredBy(S)
                                                 .where(T).coveredBy(N)
                                                 .orderByRange(T, N, S)
                                                 .query();

        Iterator<Proposition> iter = query.iterator();

        assertTrue(iter.hasNext());
        Proposition current = iter.next();
        assertEquals("678", current.get(T).text());
        assertEquals("678901", current.get(N).text());
        assertEquals("67890123", current.get(S).text());

        assertTrue(iter.hasNext());
        current = iter.next();
        assertEquals("01", current.get(T).text());
        assertEquals("678901", current.get(N).text());
        assertEquals("67890123", current.get(S).text());

        assertFalse(iter.hasNext());

        //3. Test range optimizable query
        query = doc.select(T).where(T).coveredBy(5,10).orderByRange(T).query();
        iter = query.iterator();

        assertTrue(iter.hasNext());
        current = iter.next();

        assertEquals("678", current.get(T).text());
        assertFalse(iter.hasNext());

        //4. Test multi range optimizable query
        query = doc.select(T).where(T).coveredBy(3, 12).coveredBy(5,10).orderByRange(T).query();
        iter = query.iterator();

        assertTrue(iter.hasNext());
        assertEquals("678", iter.next().get(T).text());
        assertFalse(iter.hasNext());

        //5. Test another query
        query = doc.select(S,T).where(T).coveredBy(S).orderByRange(T).query();
        iter = query.iterator();

        assertTrue(iter.hasNext());
        current = iter.next();
        assertEquals("34", current.get(T).text());
        assertEquals("1234", current.get(S).text());

        current = iter.next();
        assertEquals("678", current.get(T).text());
        assertEquals("67890123", current.get(S).text());

        current = iter.next();
        assertEquals("01", current.get(T).text());
        assertEquals("67890123", current.get(S).text());
        assertFalse(iter.hasNext());

        //6. Test filter query
        query = doc.select(T)
                   .where(T)
                   .coveredBy(S)
                   .where(S).property("test").equals("0")
                   .query();

        iter = query.iterator();

        assertTrue(iter.hasNext());
        current = iter.next();
        assertEquals("34", current.get(T).text());
        assertEquals("1234", current.get(S).text());
        assertFalse(iter.hasNext());

        GroupProposition groupquery
                = doc.select(T)
                     .where(T)
                     .property("pos").exists()
                     .coveredBy(S)
                     .where(S)
                     .property("test").exists()
                     .property("neg").exists()
                     .groupBy(S)
                     .query()
                     .first();

        iter = query.iterator();

        assertTrue(iter.hasNext());
        assertEquals("34", groupquery.value(0, T).text());

        assertEquals("1234", groupquery.key(S).text());
        assertEquals(1, groupquery.size());

        //7. Test property query
        query = doc.select(T).where(T).property("ne").exists().query();
        iter = query.iterator();
        assertTrue(iter.hasNext());
        assertEquals("city", iter.next().get(T).getProperty("ne"));
        assertFalse(iter.hasNext());

        //8. Test property value
        query = doc.select(T).where(T).property("pos").equalsAny("NN", "PM").query();
        iter = query.iterator();
        assertTrue(iter.hasNext());
        assertEquals("01", iter.next().get(T).text());
        assertEquals("23456", iter.next().get(T).text());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testQuery() {
        Document doc = documentFactory().createFragment("sv.wikpedia:TestingRanges", "01234567890123456789");

        doc.add(new Sentence()).setRange(1, 5).putProperty("test", "0").putProperty("neg", "1");
        doc.add(new Sentence()).setRange(6, 14).putProperty("test", "1");

        doc.add(new Token()).setRange(0, 2).putProperty("pos", "NN").putProperty("ne", "city");
        doc.add(new Token()).setRange(3, 5).putProperty("pos", "VB");
        doc.add(new Token()).setRange(6, 9);
        doc.add(new Token()).setRange(10, 12).putProperty("pos", "JJ");
        doc.add(new Token()).setRange(12, 17).putProperty("pos", "NN");

        doc.add(new NamedEntity()).setRange(6, 12);

        doc = serializeDeserialize(doc);

        //1. Validate that the added bits are correct
        NodeVar S = Sentence.var();
        NodeVar T = Token.var();
        NodeVar N = NamedEntity.var();

        List<Proposition> propositions = doc.select(S, T)
                                            .where(T).coveredBy(S)
                                            .query()
                                            .toList();

        assertEquals("1234", doc.select(S).where(S).covering(1, 5).query().first().get(S).text());
        assertEquals("67890123", doc.select(S).where(S).covering(6, 14).query().first().get(S).text());

        assertEquals("01", doc.select(T).where(T).covering(0, 2).query().first().get(T).text());
        assertEquals("34", doc.select(T).where(T).covering(3, 5).query().first().get(T).text());
        assertEquals("678", doc.select(T).where(T).covering(6, 9).query().first().get(T).text());
        assertEquals("01", doc.select(T).where(T).covering(10, 12).query().first().get(T).text());
        assertEquals("23456", doc.select(T).where(T).covering(12, 17).query().first().get(T).text());

        assertNull(doc.select(T).where(T).covering(6, 12).query().first());

        //2. Do a query selecting named entity tokens
        Iterable<Proposition> query = doc.select(T,N,S)
                                         .where(T)
                                         .coveredBy(S)
                                         .coveredBy(N)
                                         .orderByRange(T)
                                         .query();

        Iterator<Proposition> iter = query.iterator();

        assertTrue(iter.hasNext());
        Proposition current = iter.next();
        assertEquals("678", current.get(T).text());
        assertEquals("678901", current.get(N).text());
        assertEquals("67890123", current.get(S).text());

        current = iter.next();
        assertEquals("01", current.get(T).text());
        assertEquals("678901", current.get(N).text());
        assertEquals("67890123", current.get(S).text());

        assertFalse(iter.hasNext());

        //3. Test range optimizable query
        query = doc.select(T).where(T).coveredBy(5, 10).query();
        iter = query.iterator();

        assertTrue(iter.hasNext());
        assertEquals("678", iter.next().get(T).text());
        assertFalse(iter.hasNext());

        //4. Test multi range optimizable query
        query = doc.select(T).where(T).coveredBy(3, 12).coveredBy(5, 10).query();
        iter = query.iterator();

        assertTrue(iter.hasNext());
        assertEquals("678", iter.next().get(T).text());
        assertFalse(iter.hasNext());

        //5. Test another query
        query = doc.select(S,T).where(T).coveredBy(S).orderByRange(T).query();
        iter = query.iterator();

        assertTrue(iter.hasNext());
        current = iter.next();
        assertEquals("34", current.get(T).text());
        assertEquals("1234", current.get(S).text());

        current = iter.next();
        assertEquals("678", current.get(T).text());
        assertEquals("67890123", current.get(S).text());

        current = iter.next();
        assertEquals("01", current.get(T).text());
        assertEquals("67890123", current.get(S).text());
        assertFalse(iter.hasNext());

        //6. Test filter query
        query = doc.select(S, T)
                   .where(T).coveredBy(S)
                   .where(S).property("test").equals("0")
                   .query();

        iter = query.iterator();

        assertTrue(iter.hasNext());
        current = iter.next();
        assertEquals("34", current.get(T).text());
        assertEquals("1234", current.get(S).text());
        assertFalse(iter.hasNext());

        //7. Test property query
        GroupProposition groupquery
                = doc.select(T, S)
                     .where(S).property("test").exists()
                     .property("neg").notExists()

                     .where(T).property("pos").exists()
                     .coveredBy(S)
                     .groupBy(S)
                     .query()
                     .first();

        assertNotNull(groupquery);
        assertEquals("01", groupquery.values().get(0).get(T).text());

        assertEquals("67890123", groupquery.key().get(S).text());
        assertEquals(1, groupquery.size());

        query = doc.select(T).where(T).property("ne").exists().query();

        iter = query.iterator();
        assertTrue(iter.hasNext());
        assertEquals("city", iter.next().get(T).getProperty("ne"));
        assertFalse(iter.hasNext());

        //8. Test property value
        query = doc.select(T)
                   .where(T).property("pos").equalsAny("NN", "PM").query();

        iter = query.iterator();
        assertTrue(iter.hasNext());
        assertEquals("01", iter.next().get(T).text());
        assertEquals("23456", iter.next().get(T).text());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSerializedRemove() {
        Tokens toks = new Tokens();
        Document doc = createTokenSentenceDocument(toks);
        doc = serializeDeserialize(doc);

        ArrayList<Token> remove = new ArrayList<Token>();

        for(Token tok : doc.nodes(Token.class)) {
            if(tok.text().equals("Helsingborg"))
                remove.add(tok);
        }

        assertEquals(2, remove.size());

        doc.remove(remove.get(0));
        doc.remove(remove.get(1));

        Iterator<Sentence> sentences = doc.nodes(Sentence.class).iterator();
        assertTrue(sentences.hasNext());

        Sentence s1 = sentences.next();

        NodeTVar<Token> T = Token.var();

        Iterator<Proposition> token = doc.select(T).where(T).coveredBy(s1).orderByRange(T).query().iterator();

        assertTrue(token.hasNext());
        assertEquals("är", token.next().get(T).text());
        assertEquals("en", token.next().get(T).text());
        assertEquals("stad", token.next().get(T).text());
        assertEquals("i", token.next().get(T).text());
        assertEquals("Sverige", token.next().get(T).text());
        assertEquals(".", token.next().get(T).text());
        assertFalse(token.hasNext());

        Sentence s2 = sentences.next();

        token = doc.select(T).where(T).coveredBy(s2).orderByRange(T).query().iterator();

        assertTrue(token.hasNext());
        assertEquals("I", token.next().get(T).text());
        assertEquals("finns", token.next().get(T).text());
        assertEquals("Kärnan", token.next().get(T).text());
        assertEquals(".", token.next().get(T).text());
        assertFalse(token.hasNext());

        assertFalse(sentences.hasNext());

        doc.removeAllNodes(Token.class);
        token = doc.select(T).where(T).coveredBy(s1).query().iterator();
        assertFalse(token.hasNext());

        token = doc.select(T).where(T).coveredBy(s2).query().iterator();
        assertFalse(token.hasNext());
    }

    @Test
    public void testSerialization() {
        Tokens toks = new Tokens();
        Document doc = createTokenSentenceDocument(toks);
        doc = serializeDeserialize(doc);

        Iterator<Sentence> sentences = doc.nodes(Sentence.class).iterator();
        assertTrue(sentences.hasNext());

        Sentence s1 = sentences.next();

        NodeTVar<Token> T = Token.var();

        Iterator<Proposition> token = doc.select(T)
                                         .where(T)
                                         .coveredBy(s1)
                                         .orderByRange(T)
                                         .query()
                                         .iterator();

        assertTrue(token.hasNext());
        assertEquals("Helsingborg", token.next().get(T).text());
        assertEquals("är", token.next().get(T).text());
        assertEquals("en", token.next().get(T).text());
        assertEquals("stad", token.next().get(T).text());
        assertEquals("i", token.next().get(T).text());
        assertEquals("Sverige", token.next().get(T).text());
        assertEquals(".", token.next().get(T).text());
        assertFalse(token.hasNext());

        Sentence s2 = sentences.next();

        token = doc.select(T).where(T).coveredBy(s2).orderByRange(T).query().iterator();

        assertTrue(token.hasNext());
        assertEquals("I", token.next().get(T).text());
        assertEquals("Helsingborg", token.next().get(T).text());
        assertEquals("finns", token.next().get(T).text());
        assertEquals("Kärnan", token.next().get(T).text());
        assertEquals(".", token.next().get(T).text());
        assertFalse(token.hasNext());

        assertFalse(sentences.hasNext());

        Token tok = doc.nodesWithProperty(Token.class, "id", "s1t6").first();
        assertNotNull(tok);
        DependencyRelation rel = tok.outboundEdges(DependencyRelation.class, Token.class).first();
        assertNotNull(rel);

        assertEquals("PA", rel.getRelation());
        assertEquals("s1t5", tok.outboundNodes(DependencyRelation.class, Token.class).first().getProperty("id"));
    }

    @Test
    public void testSerialization2() {
        Tokens toks = new Tokens();
        Document doc = createTokenSentenceDocument(toks);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(baos);
            objectOutputStream.writeObject(doc);

            objectOutputStream.flush();
            objectOutputStream.close();


            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream inputStream = new ObjectInputStream(bais);

            doc =  (Document)inputStream.readObject();
        } catch (ClassNotFoundException | IOException e) {
            throw new IOError(e);
        }

        Iterator<Sentence> sentences = doc.nodes(Sentence.class).iterator();
        assertTrue(sentences.hasNext());

        Sentence s1 = sentences.next();

        NodeTVar<Token> T = Token.var();

        Iterator<Proposition> token = doc.select(T)
                                         .where(T)
                                         .coveredBy(s1)
                                         .orderByRange(T)
                                         .query()
                                         .iterator();

        assertTrue(token.hasNext());
        assertEquals("Helsingborg", token.next().get(T).text());
        assertEquals("är", token.next().get(T).text());
        assertEquals("en", token.next().get(T).text());
        assertEquals("stad", token.next().get(T).text());
        assertEquals("i", token.next().get(T).text());
        assertEquals("Sverige", token.next().get(T).text());
        assertEquals(".", token.next().get(T).text());
        assertFalse(token.hasNext());

        Sentence s2 = sentences.next();

        token = doc.select(T).where(T).coveredBy(s2).orderByRange(T).query().iterator();

        assertTrue(token.hasNext());
        assertEquals("I", token.next().get(T).text());
        assertEquals("Helsingborg", token.next().get(T).text());
        assertEquals("finns", token.next().get(T).text());
        assertEquals("Kärnan", token.next().get(T).text());
        assertEquals(".", token.next().get(T).text());
        assertFalse(token.hasNext());

        assertFalse(sentences.hasNext());

        Token tok = doc.nodesWithProperty(Token.class, "id", "s1t6").first();
        assertNotNull(tok);
        DependencyRelation rel = tok.outboundEdges(DependencyRelation.class, Token.class).first();
        assertNotNull(rel);

        assertEquals("PA", rel.getRelation());
        assertEquals("s1t5", tok.outboundNodes(DependencyRelation.class, Token.class).first().getProperty("id"));
    }

    @Test
    public void testAnnotation() {
        Tokens toks = new Tokens();
        createTokenSentenceDocument(toks);

        Set<Token> tokens = Collections.newSetFromMap(new HashMap<Token, Boolean>());
        tokens.add(toks.tok_s1t1);
        tokens.add(toks.tok_s1t4);
        tokens.add(toks.tok_s1t7);

        for(DependencyRelation rel : toks.tok_s1t2.inboundEdges(DependencyRelation.class)) {
            Token tok = rel.getTail();
            assertEquals(true, tokens.contains(tok));
        }

        tokens = Collections.newSetFromMap(new HashMap<Token, Boolean>());
        tokens.add(toks.tok_s2t1);
        tokens.add(toks.tok_s2t4);
        tokens.add(toks.tok_s2t5);

        for(DependencyRelation rel : toks.tok_s2t3.inboundEdges(DependencyRelation.class)) {
            Token tok = rel.getTail();
            assertEquals(true, tokens.contains(tok));
        }

        assertEquals(toks.tok_s1t4, toks.tok_s1t5.outboundNodes(DependencyRelation.class, Token.class).first());
        assertEquals("ET", toks.tok_s1t5.outboundEdges(DependencyRelation.class, Token.class).first().getRelation());
        assertEquals("PA", toks.tok_s1t5.inboundEdges(DependencyRelation.class, Token.class).first().getRelation());

        tokens = Collections.newSetFromMap(new HashMap<Token, Boolean>());
        tokens.add(toks.tok_s1t4);
        tokens.add(toks.tok_s1t6);

        for(DependencyRelation rel : toks.tok_s1t5.connectedEdges(DependencyRelation.class)) {
            assertEquals(true, tokens.contains(rel.getOpposite(toks.tok_s1t5)));
        }

        assertNull(toks.tok_s1t7.inboundNodes(DependencyRelation.class, Token.class).first());
        assertNull(toks.tok_s1t2.outboundNodes(DependencyRelation.class, Token.class).first());
    }

    @Test
    public void testSubdocument() {
        Conny_Andersson conny = new Conny_Andersson();
        Document doc = conny.createDocument(documentFactory());
        doc.add(new DynamicNode(), "strong").setRange(0,5);

        Document subdoc = doc.subDocument(0, 44);

        assertEquals("Conny Andersson är namnet på flera personer:", subdoc.text());

        assertEquals(1, subdoc.nodes("strong").count());
        assertEquals("Conny", subdoc.nodes("strong").first().text());

        NodeTVar<NamedEntity> N = NamedEntity.var();
        NodeTVar<Token> T = Token.var();

        NamedEntity connyAndersson = subdoc.select(N).where(N).coveredBy(0, 15).query().first().get(N);
        assertNotNull(connyAndersson);

        assertEquals("Conny Andersson", connyAndersson.text());
        assertEquals("person", connyAndersson.getLabel());

        Token connyTok = subdoc.select(T).where(T).coveredBy(0, 5).query().first().get(T);
        assertNotNull(connyTok);

        assertEquals("PM", connyTok.getProperty(TokenProperties.PPOS));

        DependencyRelation deprel = connyTok.outboundEdges(DependencyRelation.class).first();
        assertEquals("SS", deprel.getRelation());

        Token anderssonTok = subdoc.select(T).where(T).coveredBy(6,15).query().first().get(T);
        assertNotNull(anderssonTok);

        deprel = anderssonTok.outboundEdges(DependencyRelation.class).first();
        assertEquals("HD", deprel.getRelation());

        assertTrue(deprel.getHead() == connyTok);

        DependencyRelation deprel2 = connyTok.inboundEdges(DependencyRelation.class).first();
        assertNotNull(deprel2);

        assertTrue(deprel2 == deprel);

        NodeTVar<Sentence> S = Sentence.var();

        Sentence sent = subdoc.select(S).where(S).coveredBy(0, 44).query().first().get(S);
        assertNotNull(sent);

        subdoc = doc.subDocument(47, 135);
        DocumentIterable<Proposition> iterable = subdoc.select(S)
                                                       .where(S)
                                                       .coveredBy(0, 88)
                                                       .query();

        Iterator<Proposition> iterator = iterable.iterator();

        assertTrue(iterator.hasNext());
        sent = iterator.next().get(S);

        assertEquals("Conny Andersson (skådespelare) Conny Andersson (racer"
                             + "förare) Conny Andersson (politiker)", sent.text());

        connyAndersson = subdoc.select(N).where(N).coveredBy(0, 15).query().first().get(N);

        assertNotNull(connyAndersson);
        assertEquals("Conny Andersson", connyAndersson.text());
    }

    @Test
    public void testEdges(){
        Document doc = documentFactory().createFragment("main", "0123456");
        Token t1 = doc.add(new Token());
        SemanticRole sr = doc.add(new SemanticRole(), t1, t1);

        assertEquals(1, t1.outboundEdges().count());
        assertEquals(1, t1.inboundEdges().count());
        assertEquals(2, t1.connectedEdges().count());

        assertEquals(1, t1.inboundEdges(SemanticRole.class).count());
        assertEquals(1, t1.outboundEdges(SemanticRole.class).count());
        assertEquals(2, t1.connectedEdges(SemanticRole.class).count());

        EdgeTVar<SemanticRole> SR = SemanticRole.var();
        NodeTVar<Token> T = Token.var();

        //Find looping edges - and is optimized to look only at the given token.
        List<Proposition> tokenedges = doc.select(SR, T)
                                            .where(T).isOneOf(t1)
                                            .hasEdge(SR).to(T)
                                            .hasEdge(SR).from(T)
                                            .query().toList();
        assertEquals(1, tokenedges.size());
        assertEquals(sr, tokenedges.get(0).get(SR));
        assertEquals(t1, tokenedges.get(0).get(T));
    }

    @Test
    public void testEdgeQuery() {
        Conny_Andersson connytest = new Conny_Andersson();
        Document doc = connytest.createDocument(documentFactory());

        NodeTVar<Token> T1, T2;
        EdgeTVar<DependencyRelation> E;

        T1 = Token.var(); T2 = Token.var();
        E = DependencyRelation.var();

        Iterable<Proposition> props =
                doc.select(T1,T2,E)
                   .where(T1).hasEdge(E).to(T2)
                   .where(E).property(DependencyRelation.RELATION_PROPERTY).equals("SS")
                   .orderByRange(T1)
                   .query();

        Iterator<Proposition> iter = props.iterator();
        assertTrue(iter.hasNext());
        Proposition prop = iter.next();

        assertEquals("Conny", prop.get(T1).text());
        assertEquals("är", prop.get(T2).text());
        assertEquals("SS", prop.get(E).getRelation());
        assertFalse(iter.hasNext());
    }

    public void testQuestion() {
        Conny_Andersson connytest = new Conny_Andersson();
        Document doc = connytest.createDocument(documentFactory());

        NodeTVar<Token> T1, T2, T3;
        EdgeTVar<DependencyRelation> E1, E2;

        T1 = Token.var(); T2 = Token.var(); T3 = Token.var();
        E1 = DependencyRelation.var(); E2 = DependencyRelation.var();

        Iterable<Proposition> props =
                doc.select(T1,T2,T3,E1,E2)
                   .where(T1).hasEdge(E1).to(T2) // T1 -> T2
                        .where(T2).hasEdge(E2).from(T3) // T3 -> T2
                        .where(E1).property(DependencyRelation.RELATION_PROPERTY).equals("SS")
                        .where(E2).property(DependencyRelation.RELATION_PROPERTY).equals("OO")
                        .orderByRange(T1)
                        .query();
    }

    @Test
    public void testDocumentByteSerialization() {
        DocumentIO serializer = documentIO();
        Conny_Andersson connytest = new Conny_Andersson();
        Document conny = connytest.createDocument(documentFactory());
        byte[] bytes = serializer.toBytes(conny, optimizationLevel());

        Document serializedconny = serializer.fromBytes(bytes);
        assertEquals(serializedconny.text(), conny.text());
    }

    public void compareGram(NodeVar var, Window<Proposition> window, String...text) {
        assertEquals(text.length, window.size());
        int i = 0;
        for(Proposition item : window) {
            assertEquals(text[i], item.get(var).text());
            i++;
        }
    }

    @Test
    public void testEdgeCase() {
        Document doc = documentFactory().createFragment("main", "");
        doc.setText("01234567");
        doc.add(new Sentence()).setRange(0, 4);
        doc.add(new Sentence()).setRange(4, 7);

        doc.add(new Token()).setRange(0,4);
        doc.add(new Token()).setRange(4,4);

        doc.add(new Token()).setRange(4,7);
        doc.add(new Token()).setRange(7,7);

        NodeTVar<Token> T = Token.var();
        NodeTVar<Sentence> S = Sentence.var();

        List<Proposition> list = doc.select(T).where(T).coveredBy(S).orderByRange(T).query().toList();
        assertEquals(3,list.size());

        assertEquals(0,list.get(0).get(T).getStart());
        assertEquals(4,list.get(0).get(T).getEnd());

        assertEquals(4,list.get(1).get(T).getStart());
        assertEquals(4,list.get(1).get(T).getEnd());

        assertEquals(4,list.get(2).get(T).getStart());
        assertEquals(7, list.get(2).get(T).getEnd());

        list = doc.select(T).where(T).coveredBy(S).where(S).coveredBy(4, 7).orderByRange(T).query().toList();
        assertEquals(2, list.size());

        list = doc.select(T).where(T).coveredBy(4, 4).query().toList();
        assertEquals(1,list.size());

        list = doc.select(T).where(T).coveredBy(7, 7).query().toList();
        assertEquals(1, list.size());
    }

    @Test
    public void testNgram() {
        Document doc = documentFactory().createFragment("test", "012345678901234567890");
        doc.add(new Token()).setRange(0,2);
        doc.add(new Token()).setRange(3,5);
        doc.add(new Token()).setRange(6,8);
        doc.add(new Token()).setRange(9,11);
        doc.add(new Token()).setRange(12,14);

        NodeTVar<Token> T = Token.var();

        Iterator<Window<Proposition>> iter = doc.select(T).query().window(5).iterator();
        assertTrue(iter.hasNext());
        compareGram(T, iter.next(), "01", "34", "67", "90", "23");

        assertFalse(iter.hasNext());

        iter = doc.select(T).query().window(4).iterator();
        assertTrue(iter.hasNext());
        compareGram(T, iter.next(), "01", "34", "67", "90");

        assertTrue(iter.hasNext());
        compareGram(T, iter.next(), "34", "67", "90", "23");

        assertFalse(iter.hasNext());

        iter = doc.select(T).query().window(3).iterator();
        assertTrue(iter.hasNext());
        compareGram(T, iter.next(), "01", "34", "67");

        assertTrue(iter.hasNext());
        compareGram(T, iter.next(), "34", "67", "90");

        assertTrue(iter.hasNext());
        compareGram(T, iter.next(), "67", "90", "23");

        assertFalse(iter.hasNext());

        iter = doc.select(T).query().window(2).iterator();
        assertTrue(iter.hasNext());
        compareGram(T, iter.next(), "01", "34");

        assertTrue(iter.hasNext());
        compareGram(T, iter.next(), "34", "67");

        assertTrue(iter.hasNext());
        compareGram(T, iter.next(), "67", "90");

        assertTrue(iter.hasNext());
        compareGram(T, iter.next(), "90", "23");

        assertFalse(iter.hasNext());
    }

    @Test
    public void testVariants() {
        Conny_Andersson conny = new Conny_Andersson();
        Document doc = conny.createDocument(documentFactory());

        assertNull(doc.getDefaultNodeVariant(NamedEntity.class));

        final NodeTVar<NamedEntity> N = NamedEntity.var();

        doc.migrateNodeVariants(NamedEntity.class, null, "gold");
        assertFalse(doc.select(N).query().any());

        doc.setDefaultNodeVariant(NamedEntity.class, "test");
        assertEquals("test", doc.getDefaultNodeVariant(NamedEntity.class));

        doc.add(new NamedEntity()).setLabel("person").setRange(47, 62);

        List<NamedEntity> results = doc.select(N).query().map(in -> in.get(N)).toList();

        assertEquals(1, results.size());
        assertEquals("test", results.get(0).getVariant().get());
        assertEquals("person", results.get(0).getLabel());

        doc.setDefaultNodeVariant(NamedEntity.class, null);

        NodeTVar<NamedEntity> N_Test = NamedEntity.var("test");
        NodeTVar<NamedEntity> N_Gold = NamedEntity.var("gold");

        NamedEntity ne = doc.select(N_Test)
                            .where(N_Test).coveredBy(N_Gold)
                            .query()
                            .first()
                            .get(N_Test);

        assertNotNull(ne);
        assertEquals("test", ne.getVariant().get());

        DocumentIterable<Proposition> neiterable = doc.select(N_Test, N_Gold)
                                                      .where(N_Test).coveredBy(N_Gold)
                                                      .query();

        Iterator<Proposition> neiter = neiterable.iterator();
        assertTrue(neiter.hasNext());

        Proposition current = neiter.next();
        ne = current.get(N_Test);
        assertEquals("test", ne.getVariant().get());

        ne =  current.get(N_Gold);
        assertEquals("gold", ne.getVariant().get());

        assertFalse(neiter.hasNext());
    }

    @Test
    public void testRecord() {
        Document first = documentFactory().create("sv.wikipedia:test", "Lite text att leka med").setLanguage("sv");
        first.putProperty("source", "wikipedia-dump");
        assertEquals("sv.wikipedia:test", first.uri());

        NodeTVar<Token> T = Token.var();

        Token tok = first.add(new Token()).setRange(0, 4);
        assertTrue(first.select(T).query().first().get(T).equals(tok));

        first.addUriAlias("wikidata:Q1000");
        first.addUriAlias("freebase:m.3123");

        assertEquals(3, first.uris().length);
        assertEquals("sv.wikipedia:test", first.uris()[0]);
        assertEquals("wikidata:Q1000", first.uris()[1]);
        assertEquals("freebase:m.3123", first.uris()[2]);

        Document template = MemoryDocumentFactory.getInstance().createFragment("wikipedia:template:1");
        template.setText("magic-number = 42");
        template.setType("template");

        template.add(new DynamicNode(), "key").setRange(0, 12);
        template.add(new DynamicNode(), "value").setRange(15, 17);
        template.add(new DynamicNode(), "entry").setRange(0,17);

        first.putProperty("wikipedia:template:1", template);

        HashSet<String> docs = new HashSet<String>();
        for (Map.Entry<String, DataRef> entry : first.properties()) {
            if(entry.getValue() instanceof DocRef) {
                docs.add(((DocRef) entry.getValue()).documentValue().id());
            }
        }

        assertTrue(docs.contains("wikipedia:template:1"));

        first = serializeDeserialize(first);
        docs = new HashSet<String>();
        for (Map.Entry<String, DataRef> entry : first.properties()) {
            if(entry.getValue() instanceof DocRef) {
                docs.add(((DocRef) entry.getValue()).documentValue().id());
            }
        }

        assertTrue(docs.contains("wikipedia:template:1"));

        assertEquals("Lite", first.select(T).query().first().get(T).text());

        NodeVar E = DynamicNode.var("entry");

        DynamicNode annotation = template.select(E).query().first().get(E);
        assertNotNull(annotation);

        NodeVar K = DynamicNode.var("key");
        NodeVar V = DynamicNode.var("value");

        assertEquals("magic-number", template.select(K).where(K).coveredBy(annotation).query().first().get(K).text());
        assertEquals("42", template.select(V).where(V).coveredBy(annotation).query().first().get(V).text());

        assertEquals("sv.wikipedia:test", first.uri());
        assertEquals("wikipedia-dump", first.getProperty("source"));
        assertEquals("Lite text att leka med", first.text());
        assertEquals("magic-number = 42", first.getProperty("wikipedia:template:1"));
    }

    protected abstract Document serializeDeserialize(Document doc);

    @Test
    public void testTransactions() {
        Document doc = documentFactory().createFragment("main", "01234567890123456789");
        Token t1 = doc.add(new Token()).setRange(3,6);
        t1.putProperty("RemoveMe", "1");

        Token t2 = doc.add(new Token()).setRange(0,2);
        t2.putProperty("ChangeMe", "0");

        DependencyRelation rel = doc.add(new DependencyRelation()).setRelation("SS").connect(t1,t2);

        DocumentTransaction trans = doc.begin();
        Token t3 = trans.add(new Token()).setRange(7,10);
        Token t4 = trans.add(new Token()).setRange(10,15);
        trans.get(t1).putProperty(TokenProperties.POS, "NN").setVariant("odd").setRange(0,2);
        trans.get(t1).removeProperty("RemoveMe");
        trans.get(rel).setRelation("OO").connect(t3, t4);
        trans.get(t2).putProperty("ChangeMe", "1");

        assertEquals(2, doc.nodes(Token.class).count());

        assertEquals(3, t1.getStart());
        assertEquals(6, t1.getEnd());
        assertFalse(t1.hasProperty(TokenProperties.POS));
        assertFalse(t1.getVariant().isPresent());

        assertEquals(1, doc.edges(DependencyRelation.class).count());

        assertEquals("SS", rel.getRelation());
        assertEquals(rel.getTail(), t1);
        assertEquals(rel.getHead(), t2);

        assertEquals(doc, t1.getProxy());
        assertEquals(trans, trans.get(t1).getProxy());

        assertEquals("0", t2.getProperty("ChangeMe"));

        assertEquals(7, t3.getStart());
        assertEquals(10, t3.getEnd());

        assertEquals(10, t4.getStart());
        assertEquals(15, t4.getEnd());

        assertEquals("NN", trans.get(t1).getProperty(TokenProperties.POS));
        assertEquals("odd", trans.get(t1).getVariant().get());
        assertEquals(0, trans.get(t1).getStart());
        assertEquals(2, trans.get(t1).getEnd());
        assertEquals("OO", trans.get(rel).getRelation());
        assertEquals("1", trans.get(t2).getProperty("ChangeMe"));

        List<Map.Entry<String, DataRef>> entries = Iterables.toList(trans.get(t1).properties());
        assertEquals(1, entries.size());
        assertEquals(TokenProperties.POS, entries.get(0).getKey());
        assertEquals("NN", entries.get(0).getValue().stringValue());

        assertEquals(t4, trans.get(rel).getHead());
        assertEquals(t3, trans.get(rel).getTail());

        trans.commit();

        assertEquals(3, doc.nodes(Token.class).count());
        assertEquals(1, doc.nodes(Token.class, "odd").count());
        assertEquals(1, doc.edges(DependencyRelation.class).count());

        assertEquals(doc, t3.getProxy());
        assertEquals(doc, t4.getProxy());

        assertFalse(t1.hasProperty("RemoveMe"));
        assertTrue(t1.hasProperty(TokenProperties.POS));

        //TODO: Make sure that refs work as they are intended!
    }

    @Test
    public void testPrimitiveTypeSupport() {
        Document doc = documentFactory().createFragment("main", "01234567890123456789");
        Token t1 = doc.add(new Token()).setRange(3,6);
        t1.putProperty("Bool", true);
        t1.putProperty("Int", 102);
        t1.putProperty("Long", 420000000000L);
        t1.putProperty("Char", 'Y');
        t1.putProperty("Float", 0.75f);
        t1.putProperty("Double", 0.25);
        t1.putProperty("String", "text");
        t1.putProperty("Binary1", new byte[]{0,1,2,3});
        t1.putProperty("Binary2", new BinaryRef(new byte[]{0,1,2,3}));

        MemoryDocument memDoc = new MemoryDocument("Fragment");
        memDoc.add(new Token()).setRange(0,4).putProperty(TokenProperties.POS, "NN");
        memDoc.add(new Sentence()).setRange(0,8);

        t1.putProperty("Doc", memDoc);

        doc = serializeDeserialize(doc);

        t1 = doc.nodes(Token.class).first();
        assertEquals(new IntRef(102), t1.getRefProperty("Int"));
        assertEquals(new LongRef(420000000000L), t1.getRefProperty("Long"));
        assertEquals(new FloatRef(0.75f), t1.getRefProperty("Float"));
        assertEquals(new DoubleRef(0.25), t1.getRefProperty("Double"));
        assertEquals(new StringRef("text"), t1.getRefProperty("String"));
        assertEquals("Fragment", t1.getProperty("Doc"));

        assertTrue(t1.getIntProperty("Int") == 102);
        assertTrue(t1.getLongProperty("Long") == 420000000000L);
        assertTrue(t1.getCharProperty("Char") == 'Y');
        assertTrue(t1.getFloatProperty("Float") == 0.75f);
        assertTrue(t1.getDoubleProperty("Double") == 0.25);
        assertTrue(t1.getDocumentProperty("Doc") != memDoc);

        assertEquals(1, t1.getDocumentProperty("Doc").nodes(Token.class).count());
        assertEquals(1, t1.getDocumentProperty("Doc").nodes(Sentence.class).count());

        assertEquals("Frag", t1.getDocumentProperty("Doc").nodes(Token.class).first().text());
        assertEquals("NN", t1.getDocumentProperty("Doc").nodes(Token.class).first().getProperty(TokenProperties.POS));
        assertEquals("Fragment", t1.getDocumentProperty("Doc").nodes(Sentence.class).first().text());
    }

    @Test
    public void testDocumentCore() {
        Document doc = documentFactory().createFragment("test123");
        assertEquals(doc.id(), "test123");
        assertEquals(doc.uri(), "test123");

        assertEquals(0, doc.edges().count());
        assertEquals(0, doc.edges(DependencyRelation.class).count());
        assertEquals(0, doc.edges(DependencyRelation.class, "special").count());
        assertEquals(0, doc.nodes().count());
        assertEquals(0, doc.nodes(Token.class).count());
        assertEquals(0, doc.nodes(Token.class, "special").count());

        assertEquals(0, DocumentIterables.wrap(doc.properties()).count());

        assertNull(doc.getTitle());
        doc.setTitle("Exempel");
        assertEquals("Exempel", doc.getTitle());

        assertEquals(0, doc.getStart());
        assertEquals(0, doc.getEnd());
        assertEquals(0, doc.length());

        assertEquals(0, doc.subDocument(0, 0).length());
        assertEquals(0, doc.view(0, 0).length());

        assertEquals(documentFactory(), doc.factory());

        assertFalse(doc.isView());
        assertEquals(0, doc.select(Token.var()).query().count());

        doc.setType("text/plain");
        assertEquals("text/plain", doc.type());

        doc.setLength(10);
        assertEquals(10, doc.length());

        Token t1 = doc.add(new Token()).setRange(5, 10);
        doc.setText("0123456789");

        assertEquals("56789", t1.text());
        assertEquals(1, doc.nodes().count());
        assertEquals(1, doc.nodes(Token.class).count());
        assertEquals(0, doc.nodes(Token.class, "special").count());
        assertEquals(0, doc.nodes(Sentence.class).count());

        Token t2 = doc.add(new Token()).setRange(0,4);
        DependencyRelation dep = doc.add(new DependencyRelation(), t2, t1);
        assertEquals(t1, dep.getHead());
        assertEquals(t2, dep.getTail());

        assertEquals(1, t1.inboundEdges().count());
        assertEquals(1, t1.inboundEdges(DependencyRelation.class).count());
        assertEquals(0, t1.inboundEdges(DependencyRelation.class, "special").count());

        assertEquals(0, t1.outboundEdges().count());
        assertEquals(0, t1.outboundEdges(DependencyRelation.class).count());
        assertEquals(0, t1.outboundEdges(DependencyRelation.class, "special").count());

        assertEquals(1, t1.connectedEdges().count());
        assertEquals(1, t1.connectedEdges(DependencyRelation.class).count());
        assertEquals(0, t1.connectedEdges(DependencyRelation.class, "special").count());

        assertEquals(1, t2.outboundEdges().count());
        assertEquals(1, t2.outboundEdges(DependencyRelation.class).count());
        assertEquals(0, t2.outboundEdges(DependencyRelation.class, "special").count());

        assertEquals(1, t2.connectedEdges().count());
        assertEquals(1, t2.connectedEdges(DependencyRelation.class).count());
        assertEquals(0, t2.connectedEdges(DependencyRelation.class, "special").count());

        assertEquals(2, doc.nodes().count());
        assertEquals(2, doc.nodes(Token.class).count());
        assertEquals(0, doc.nodes(Sentence.class).count());
        assertEquals(0, doc.nodes(Token.class, "special").count());
    }

    @Test
    public void testReplace1() {
        Document doc = documentFactory().createFragment("main", "0123  6789..");

        doc.add(new Sentence()).setRange(0,11);
        doc.add(new Sentence()).setRange(11,12);

        doc.add(new Token()).setRange(0, 4);
        doc.add(new Token()).setRange(6, 10);
        doc.add(new Token()).setRange(10, 11);
        doc.add(new Token()).setRange(11, 12);

        NodeTVar<Token> T = Token.var();
        NodeTVar<Sentence> S = Sentence.var();

        assertEquals(4, doc.select(T).query().count());
        assertEquals(2, doc.select(S).query().count());

        doc.replace(Pattern.compile("\\.+"), ".", true);
        assertEquals("0123  6789.", doc.text());

        List<Proposition> props = doc.select(T).query().toList();

        assertEquals(3, doc.select(T).query().count());
        assertEquals(1, doc.select(S).query().count());

        doc.replace(Pattern.compile("\\s+"), " ", true);
        assertEquals("0123 6789.", doc.text());

        assertEquals(3, doc.select(T).query().count());
        assertEquals(1, doc.select(S).query().count());

        Iterator<Proposition> iter = doc.select(T).orderByRange(T).query().iterator();

        assertTrue(iter.hasNext());
        assertEquals("0123", iter.next().get(T).text());

        assertTrue(iter.hasNext());
        assertEquals("6789", iter.next().get(T).text());

        assertTrue(iter.hasNext());
        assertEquals(".", iter.next().get(T).text());

        assertFalse(iter.hasNext());

        iter = doc.select(S).orderByRange(S).query().iterator();
        assertTrue(iter.hasNext());

        assertEquals("0123 6789.", iter.next().get(S).text());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testReplace2() {
        Document doc = documentFactory().createFragment("main", "0123  678-01..");

        doc.add(new Sentence()).setRange(0,  13);
        doc.add(new Sentence()).setRange(13, 14);

        doc.add(new Token()).setRange(0, 4); //0123
        doc.add(new Token()).setRange(4, 6); //"  "
        doc.add(new Token()).setRange(6, 9); //678
        doc.add(new Token()).setRange(9, 10); //-
        doc.add(new Token()).setRange(10, 12); //01
        doc.add(new Token()).setRange(12, 13); //.
        doc.add(new Token()).setRange(13, 14); //.

        doc.replace(Pattern.compile("\\s+6"), "___");
        doc.replace(Pattern.compile("0|-"), "");
        doc.replace(Pattern.compile("\\.+"), ".");

        //Resulting string: "123___781."

        assertEquals("123___781.", doc.text());

        NodeTVar < Token > T = Token.var();
        NodeTVar<Sentence> S = Sentence.var();

        assertEquals(5, doc.select(T).query().count());
        assertEquals(1, doc.select(S).query().count());

        Iterator<Proposition> iter = doc.select(T).orderByRange(T).query().iterator();

        assertTrue(iter.hasNext());
        assertEquals("123", iter.next().get(T).text());

        assertTrue(iter.hasNext());
        assertEquals("__", iter.next().get(T).text());

        assertTrue(iter.hasNext());
        assertEquals("_78", iter.next().get(T).text());

        assertTrue(iter.hasNext());
        assertEquals("1", iter.next().get(T).text());

        assertTrue(iter.hasNext());
        assertEquals(".", iter.next().get(T).text());
        assertFalse(iter.hasNext());


        iter = doc.select(S).orderByRange(S).query().iterator();
        assertTrue(iter.hasNext());

        assertEquals("123___781.", iter.next().get(S).text());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testProjection() {
        Document doc = documentFactory().create("sv.wikipedia:Test").setLanguage("sv");

        assertNotNull(doc);
        doc.setText("0123456789");

        Token t0 = doc.add(new Token()).setRange(0,1);
        Token t1 = doc.add(new Token()).setRange(1,2);
        Token t2 = doc.add(new Token()).setRange(2,3);
        Token t3 = doc.add(new Token()).setRange(3,4);
        Token t4 = doc.add(new Token()).setRange(4,5);
        Token t5 = doc.add(new Token()).setRange(5,6);
        Token t6 = doc.add(new Token()).setRange(6,7);

        //Created edges
        DependencyRelation dr0 = doc.add(new DependencyRelation(), t5, t4);
        DependencyRelation dr1 = doc.add(new DependencyRelation(), t4, t3);
        DependencyRelation dr2 = doc.add(new DependencyRelation(), t3, t1);
        DependencyRelation dr3 = doc.add(new DependencyRelation(), t1, t2);
        DependencyRelation dr4 = doc.add(new DependencyRelation(), t2, t0);

        SemanticRole s1 = doc.add(new SemanticRole(), t3, t1);
        SemanticRole s2 = doc.add(new SemanticRole(), t1, t4);

        List<Token> totalProjections = t0.projectInbound(Token.class, DependencyRelation.class);
        assertEquals(6, totalProjections.size());

        assertEquals(t0.getRef(), totalProjections.get(0).getRef());
        assertEquals(t2.getRef(), totalProjections.get(1).getRef());
        assertEquals(t1.getRef(), totalProjections.get(2).getRef());
        assertEquals(t3.getRef(), totalProjections.get(3).getRef());
        assertEquals(t4.getRef(), totalProjections.get(4).getRef());
        assertEquals(t5.getRef(), totalProjections.get(5).getRef());

        totalProjections = t5.projectOutbound(Token.class, DependencyRelation.class);
        assertEquals(6, totalProjections.size());

        assertEquals(t5.getRef(), totalProjections.get(0).getRef());
        assertEquals(t4.getRef(), totalProjections.get(1).getRef());
        assertEquals(t3.getRef(), totalProjections.get(2).getRef());
        assertEquals(t1.getRef(), totalProjections.get(3).getRef());
        assertEquals(t2.getRef(), totalProjections.get(4).getRef());
        assertEquals(t0.getRef(), totalProjections.get(5).getRef());

        assertEquals(0, t6.projectInbound(Token.class, DependencyRelation.class, false).size());
        assertEquals(0, t6.projectOutbound(Token.class, DependencyRelation.class, false).size());

        DependencyRelation dr5 = doc.add(new DependencyRelation(), t3, t0);

        assertEquals(2, t3.outboundEdges(DependencyRelation.class).count());

        totalProjections = t0.projectInbound(Token.class, DependencyRelation.class);
        assertEquals(6, totalProjections.size());

        assertEquals(t0.getRef(), totalProjections.get(0).getRef());
        assertEquals(t2.getRef(), totalProjections.get(1).getRef());
        assertEquals(t1.getRef(), totalProjections.get(2).getRef());
        assertEquals(t3.getRef(), totalProjections.get(3).getRef());
        assertEquals(t4.getRef(), totalProjections.get(4).getRef());
        assertEquals(t5.getRef(), totalProjections.get(5).getRef());

        totalProjections = t5.projectOutbound(Token.class, DependencyRelation.class);
        assertEquals(6, totalProjections.size());

        assertEquals(t5.getRef(), totalProjections.get(0).getRef());
        assertEquals(t4.getRef(), totalProjections.get(1).getRef());
        assertEquals(t3.getRef(), totalProjections.get(2).getRef());
        assertEquals(t1.getRef(), totalProjections.get(3).getRef());
        assertEquals(t2.getRef(), totalProjections.get(4).getRef());
        assertEquals(t0.getRef(), totalProjections.get(5).getRef());

        assertEquals(0, t6.projectInbound(Token.class, DependencyRelation.class, false).size());
        assertEquals(0, t6.projectOutbound(Token.class, DependencyRelation.class, false).size());
    }

    @Test
    public void testRecordCore() {
        Document doc = documentFactory().create("sv.wikipedia:Test").setLanguage("sv");
        assertEquals("sv", doc.language());
        assertEquals("sv.wikipedia:Test", doc.uri());

        assertEquals(1, doc.uris().length);
        doc.addUriAlias("wikidata:Q188522");

        assertEquals("sv.wikipedia:Test", doc.uri());
        assertEquals("wikidata:Q188522", doc.uri("wikidata"));

        assertEquals(2, doc.uris().length);

        Document main = documentFactory().createFragment("main");
        assertFalse(doc.documentProperties().iterator().hasNext());

        doc.putProperty("main", main);
        assertTrue(doc.documentProperties().iterator().hasNext());

        doc.putProperty("attr", "true");
        assertEquals("true", doc.getProperty("attr"));

        doc.removeProperty("attr");
        assertNull(doc.getProperty("attr"));
    }
}

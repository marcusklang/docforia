package se.lth.cs.docforia.io.text;
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
import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.graph.text.Sentence;
import se.lth.cs.docforia.graph.text.Token;
import se.lth.cs.docforia.io.DocumentWriter;
import se.lth.cs.docforia.io.text.builders.CharSeparatorTextBuilder;
import se.lth.cs.docforia.query.*;

import java.io.IOError;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Text document writer factory
 */
public class TextDocumentWriterFactory {
    private ArrayList<ColumnWriter> columnWriters = new ArrayList<>();
    private String beginMarker = "#begin document";
    private String endMarker = "#end document";
    private TextBuilder builder = new CharSeparatorTextBuilder();

    public void addColumn(ColumnWriter writer) {
        columnWriters.add(writer);
    }

    /** Set the begin document marker */
    public void setBeginMarker(String beginMarker) {
        this.beginMarker = beginMarker;
    }

    /** Set the end document marker */
    public void setEndMarker(String endMarker) {
        this.endMarker = endMarker;
    }

    /** Get the begin marker used */
    public String getBeginMarker() {
        return beginMarker;
    }

    /** Get the end marker used */
    public String getEndMarker() {
        return endMarker;
    }

    public void setBuilder(TextBuilder builder) {
        this.builder = builder;
    }

    private class DocWriter implements DocumentWriter {
        private boolean useMarkers;
        private Writer writer;

        public DocWriter(Writer writer, boolean useMarkers) {
            this.useMarkers = useMarkers;
            this.writer = writer;
        }

        @Override
        public void write(Document doc) {
            ArrayList<TextSentence> sentences = new ArrayList<>();

            NodeTVar<Token> T = Token.var();
            NodeTVar<Sentence> S = Sentence.var();
            List<PropositionGroup> groups = doc.select(S, T)
                                                .where(T).coveredBy(S)
                                                .stream()
                                                .collect(
                                                        QueryCollectors.toSortedListGroups(
                                                                StreamUtils.subset(doc, S),
                                                                StreamUtils.subset(doc, T),
                                                                StreamUtils.orderBy(S),
                                                                StreamUtils.orderBy(T)));

            for (PropositionGroup propositions : groups)
            {
                TextSentence sentence = new TextSentence(doc, propositions.key().noderef(S));
                for (Proposition props : propositions.values()) {
                    sentence.add((NodeRef)props.data[0]);
                }
                sentences.add(sentence);
            }

            for (ColumnWriter columnWriter : columnWriters) {
                columnWriter.save(doc, sentences);
            }

            try {
                if(useMarkers) {
                    writer.write(beginMarker);
                    writer.write('\n');
                }

                if(sentences.size() > 0) {
                    writer.write(builder.build(sentences.get(0)));
                }

                for (int i = 1; i < sentences.size(); i++) {
                    writer.write('\n');
                    writer.write(builder.build(sentences.get(i)));
                }

                if(useMarkers) {
                    writer.write(endMarker);
                    writer.write('\n');
                }
            } catch (IOException e) {
                throw new IOError(e);
            }
        }

        @Override
        public void close() {

        }
    }

    public DocumentWriter create(Writer writer) {
        return new DocWriter(writer, true);
    }

    public void write(Document document, Writer writer) {
        new DocWriter(writer, false).write(document);
    }

    public String write(Document document) {
        StringWriter writer = new StringWriter();
        DocWriter docWriter = new DocWriter(writer, false);
        docWriter.write(document);
        docWriter.close();
        return writer.toString();
    }
}

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
import se.lth.cs.docforia.DocumentFactory;
import se.lth.cs.docforia.io.DocumentReader;
import se.lth.cs.docforia.io.text.rebuilders.SimpleRebuilder;
import se.lth.cs.docforia.memstore.MemoryDocumentFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * TSV+CoNLL text document reader factory
 */
public class TextDocumentReaderFactory {
    private DocumentFactory factory = MemoryDocumentFactory.getInstance();
    private TextRebuilder rebuilder = new SimpleRebuilder(1);
    private ArrayList<ColumnReader> columnReaders = new ArrayList<>();
    private String beginMarker = "#begin document";
    private String endMarker = "#end document";
    private String emptyProperty = "_";
    private boolean tsv = false;

    public TextDocumentReaderFactory() {

    }

    public TextDocumentReaderFactory(DocumentFactory factory) {
        this.factory = factory;
    }

    /** Set the text rebuilder to use, the rebuilder is used to rebuild/reconstruct the text */
    public void setTextRebuilder(TextRebuilder rebuiler) {
        this.rebuilder = rebuiler;
    }

    /** Add a property reader that converts sentences and tokens into annotations and nodes */
    public void addColumn(ColumnReader reader) {
        columnReaders.add(reader);
    }

    /** Get the document begin marker as read per line */
    public String getDocumentBeginMarker() {
        return beginMarker;
    }

    /** Tell the parser that the input file is seperated by tabs <b>only</b> and not generic whitespace */
    public void setTsv(boolean tsv) {
        this.tsv = tsv;
    }

    /** Set the document begin marker as read per line */
    public void setDocumentBeginMarker(String beginMarker) {
        this.beginMarker = beginMarker;
    }

    /** Get the current document end marker as read per line */
    public String getDocumentEndMarker() {
        return endMarker;
    }

    /** Set the document end marker as read per line */
    public void setDocumentEndMarker(String endMarker) {
        this.endMarker = endMarker;
    }

    public void setEmptyColumnMarker(String emptyColumn) {
        this.emptyProperty = emptyColumn;
    }

    public String getEmptyColumn() {
        return emptyProperty;
    }

    private enum DocReaderState {
        FIND_START,
        READ_SENTENCE
    }

    private class DocReader implements DocumentReader {
        private BufferedReader reader;
        private boolean useMarkers;
        private boolean eof =false;

        public DocReader(BufferedReader reader, boolean useMarkers) {
            this.reader = reader;
            this.useMarkers = useMarkers;
        }

        private void parseTokenLine(TextToken token, String line) {
            String[] parts;
            if(tsv) {
                parts = line.split("\\t+");
            } else {
                parts = line.split("\\s+");
            }

            for (String part : parts) {
                token.addProperty(part.equals(emptyProperty) ? null : part);
            }
        }

        @Override
        public Document next() {
            if(eof)
                return null;

            Document doc = factory.create();
            ArrayList<TextSentence> sentences = new ArrayList<>();
            if(useMarkers) {
                try {
                    DocReaderState state = DocReaderState.FIND_START;
                    String line;
                    TextSentence sentence = new TextSentence(doc);
                    outer: while((line = reader.readLine()) != null) {
                        switch (state) {
                            case FIND_START:
                                if(line.startsWith(beginMarker)) {
                                    doc.setId(line.substring(beginMarker.length()+1).trim());
                                    state = DocReaderState.READ_SENTENCE;
                                    sentence = new TextSentence(doc);
                                }
                                break;
                            case READ_SENTENCE:
                                if(line.length() == 0 || (!line.startsWith("#") && line.trim().length() == 0) ) {
                                    //new sentence
                                    if(!sentence.isEmpty()) {
                                        sentences.add(sentence);
                                        sentence = new TextSentence(doc);
                                    }
                                } else if(line.startsWith("#")) {
                                    if(line.startsWith(endMarker)) {
                                        if (!sentences.isEmpty() || !sentence.isEmpty()) {
                                            break outer;
                                        } else {
                                            state = DocReaderState.FIND_START;
                                        }
                                    }
                                    else
                                        continue;
                                } else {
                                    parseTokenLine(sentence.add(), line);
                                }

                                break;
                        }
                    }

                    if(line == null)
                        eof = true;

                    if(!sentence.isEmpty()) {
                        sentences.add(sentence);
                    } else if(eof) {
                        return null;
                    }
                } catch (IOException e) {
                    throw new IOError(e);
                }
            } else {
                try {
                    String line;
                    TextSentence sentence = new TextSentence(doc);
                    while((line = reader.readLine()) != null) {
                        if(line.length() == 0 || (!line.startsWith("#") && line.trim().length() == 0) ) {
                            //new sentence
                            if(!sentence.isEmpty()) {
                                sentences.add(sentence);
                                sentence = new TextSentence(doc);
                            }
                        } else if(!line.startsWith("#")) {
                            parseTokenLine(sentence.add(), line);
                        }
                    }

                    if(!sentence.isEmpty()) {
                        sentences.add(sentence);
                    }

                    eof = true;
                } catch (IOException e) {
                    throw new IOError(e);
                }
            }

            rebuilder.parse(TextDocumentReaderFactory.this, doc, sentences);
            for (ColumnReader columnReader : columnReaders) {
                columnReader.load(doc, sentences);
            }

            return doc;
        }

        @Override
        public void close() {
            try {
                reader.close();
            } catch (IOException e) {
                throw new IOError(e);
            }
        }
    }

    private static final Charset UTF8 = Charset.forName("utf-8");

    /**
     * Create a document reader from a <code>Reader</code>
     *
     * @param reader the reader;
     */
    public DocumentReader create(Reader reader) {
        return new DocReader(new BufferedReader(reader), true);
    }

    /**
     * Create a document reader from an <code>Inputstream</code>
     *
     * @param stream inputstream to read from
     */
    public DocumentReader create(InputStream stream) {
        return create(new InputStreamReader(stream, UTF8));
    }

    /**
     * Read a single document from a string
     *
     * @param reader the reader;
     */
    public Document read(String reader) {
        return read(new StringReader(reader));
    }

    /**
     * Read a single document from an <code>Inputstream</code>, assumes UTF-8 encoding.
     * <p>
     * <b>Remarks:</b> This method consumes the stream, and closes it after successful read.
     * @param inputStream inputstream to read from
     * @return Document or null if no document was found.
     */
    public Document read(InputStream inputStream) {
        return read(new InputStreamReader(inputStream, UTF8));
    }

    /**
     * Reads everything into one document from a <code>Reader</code>
     * <p>
     * <b>Remarks:</b> This method consumes the reader, and closes it after successful read.
     * @param reader reader to read from
     * @return Document or null if no document was found.
     */
    public Document read(Reader reader) {
        DocReader docReader = new DocReader(new BufferedReader(reader), false);
        Document next = docReader.next();
        docReader.close();
        return next;
    }
}

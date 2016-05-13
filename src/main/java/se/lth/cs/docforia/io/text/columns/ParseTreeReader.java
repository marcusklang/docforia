package se.lth.cs.docforia.io.text.columns;
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
import se.lth.cs.docforia.graph.text.ParseTreeEdge;
import se.lth.cs.docforia.graph.text.ParseTreeNode;
import se.lth.cs.docforia.io.text.ColumnReader;
import se.lth.cs.docforia.io.text.TextSentence;
import se.lth.cs.docforia.io.text.TextToken;

import java.util.ArrayDeque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read a parse tree property
 */
public class ParseTreeReader implements ColumnReader {
    private int col;

    public ParseTreeReader(int col) {
        this.col = col;
    }

    Pattern pattern = Pattern.compile("\\(([^\\(\\)\\*\\|]+)|(\\*)|(\\))", Pattern.CASE_INSENSITIVE);

    @Override
    public void load(Document doc, List<TextSentence> sentences) {
        ArrayDeque<ParseTreeNode> stack = new ArrayDeque<>();

        for (TextSentence sentence : sentences) {
            for (TextToken token : sentence) {
                String property = token.getProperty(col);
                if(property == null)
                    break;

                Matcher matcher = pattern.matcher(property);
                while(matcher.find()) {
                    if(matcher.group(1) != null) {
                        //Create node
                        stack.push(new ParseTreeNode(doc).setLabel(matcher.group(1)));
                    }
                    else if(matcher.group(2) != null) {
                        //Add child
                        stack.peek().connect(token.getRepresentation(), new ParseTreeEdge(doc));
                    }
                    else if(matcher.group(3) != null) {
                        //Pop node, connect to parent
                        ParseTreeNode popped = stack.pop();
                        if(!stack.isEmpty()) { //Is Root node if empty
                            stack.peek().connect(popped, new ParseTreeEdge(doc));
                        } else {
                            new ParseTreeEdge(doc).connect(sentence.representation(), popped);
                        }
                    }
                }
            }
        }
    }
}

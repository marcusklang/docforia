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

import se.lth.cs.docforia.NodeRef;
import se.lth.cs.docforia.NodeStore;
import se.lth.cs.docforia.graph.text.Token;

import java.util.ArrayList;

/**
 * Simplified token
 */
public class TextToken extends ArrayList<String> implements Iterable<String> {
    private NodeRef tokenRef;
    private TextSentence parent;
    private int id;

    public TextToken(TextSentence parent) {
        this.parent = parent;
    }

    public TextToken setId(int id) {
        this.id = id;
        return this;
    }

    public int getId() {
        return id;
    }

    public NodeStore token() {
        return tokenRef.get();
    }

    public NodeRef ref() {
        return tokenRef;
    }

    public Token getRepresentation() {
        return (Token)parent.getDocument().representation(tokenRef);
    }

    public TextToken setToken(NodeRef tokenRef) {
        this.tokenRef = tokenRef;
        return this;
    }

    public String getProperty(int prop) {
        if(prop >= size())
            return null;
        else
            return get(prop);
    }

    public String getPropertyOrEmpty(int propId) {
        String prop = getProperty(propId);
        return prop != null ? prop : "";
    }

    /** Append to the end of current list of values */
    public TextToken addProperty(String value) {
        add(value);
        return this;
    }

    /** Sets a property, possibly adding null values in between if they are missing */
    public TextToken setProperty(int col, String value) {
        if(this.size() < col) {
            for(int i = this.size(); i < col; i++) {
                add(null);
            }
            add(value);
        }
        else if(this.size() == col) {
            addProperty(value);
        }
        else {
            set(col, value);
        }

        return this;
    }

    public int getEnd() {
        return tokenRef.get().getEnd();
    }

    public int getStart() {
        return tokenRef.get().getStart();
    }
}

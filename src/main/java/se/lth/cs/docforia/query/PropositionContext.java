package se.lth.cs.docforia.query;
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

import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import se.lth.cs.docforia.Document;

/**
 * Proposition context information
 */
public class PropositionContext {
    private Document document;
    private Reference2IntOpenHashMap<Var> var2index;

    public PropositionContext(Document document, Reference2IntOpenHashMap<Var> var2index) {
        this.document = document;
        this.var2index = var2index;
        this.var2index.defaultReturnValue(-1);
    }

    public Document getDocument() {
        return document;
    }

    public int indexOf(Var var) {
        return var2index.getInt(var);
    }

    public int numVars() {
        return var2index.size();
    }

    public Reference2IntMap.FastEntrySet<Var> entries() {
        return var2index.reference2IntEntrySet();
    }
}

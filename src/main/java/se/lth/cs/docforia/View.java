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

import se.lth.cs.docforia.query.Var;
import se.lth.cs.docforia.query.dsl.CommonClause;

/**
 * View implementation
 */
public class View extends Document {

    protected final Document parent;
    protected final ViewStore store;
    protected final ViewEngine engine;
    protected int start;
    protected int end;
    protected DocumentRepresentations instances =  new DocumentRepresentations(this);

    public View(Document parent, int start, int end) {
        this.parent = parent;
        this.store = new ViewStore(this);
        this.engine = new ViewEngine(this);
        this.start = start;
        this.end = end;
    }

    @Override
    public DocumentRepresentations representations() {
        return instances;
    }

    @Override
    public DocumentStore store() {
        return store;
    }

    @Override
    public Document newInstance(String id, String text) {
        return parent.newInstance(id, text);
    }

    @Override
    public Document copy() {
        return new View(parent.copy(), start, end);
    }

    @Override
    public CommonClause select(Var... vars) {
        return super.select(vars).where(vars).coveredBy(0, end-start);
    }

    @Override
    public DocumentEngine engine() {
        return engine;
    }

    public Document getParent() {
        return parent;
    }

    @Override
    public int getStart() {
        return start;
    }

    @Override
    public int getEnd() {
        return end;
    }

    @Override
    public View view(int start, int end) {
        return parent.view(start + this.start, end + this.start);
    }

    @Override
    public int inverseTransform(int pos) {
        return pos + start;
    }

    @Override
    public Range inverseTransform(Range range) {
        return new MutableRange(range.getStart() + start, range.getEnd() + start);
    }

    @Override
    public void inverseTransform(MutableRange range) {
        range.setStart(range.getStart()+start);
        range.setEnd(range.getEnd()+start);
    }

    @Override
    public void transform(MutableRange range) {
        range.setStart(transform(range.getStart()));
        range.setEnd(transform(range.getEnd()));
    }

    @Override
    public int transform(int pos) {
        return pos - start;
    }

    @Override
    public Range transform(Range range) {
        return new MutableRange(range.getStart() - start, range.getEnd() - start);
    }

    @Override
    public void setText(String text) {
        throw new UnsupportedOperationException("Not supported within a view!");
    }

    @Override
    public Document append(Document doc) {
        throw new UnsupportedOperationException("Not supported within a view!");
    }

    @Override
    public Document append(Iterable<Document> docs) {
        throw new UnsupportedOperationException("Not supported within a view!");
    }

    @Override
    public boolean isView() {
        return true;
    }

    @Override
    public DocumentFactory factory() {
        return parent.factory();
    }
}

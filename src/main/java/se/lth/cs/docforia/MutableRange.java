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

/**
 * Mutable range
 */
public final class MutableRange implements ComparableRange {
    private int start;
    private int end;

    public MutableRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
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
    public float getMidpoint() {
        return 0;
    }

    @Override
    public int length() {
        return end - start;
    }

    @Override
    public int compareTo(Range o) {
        final int comp1;
        return (comp1 = Integer.compare(start, o.getStart())) != 0
                ? comp1
                : Integer.compare(end, o.getEnd());
    }
}

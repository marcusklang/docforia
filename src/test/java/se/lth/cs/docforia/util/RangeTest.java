package se.lth.cs.docforia.util;
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

import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Range test code
 */
public class RangeTest {
    @Test
    public void testForwardAddition() {
        AnnotationIndex<String> test = new AnnotationIndex<>();
        test.add(2, 10, "T1");
        assertTrue(test.verifyBalance());
        test.add(4, 12, "T2");
        assertTrue(test.verifyBalance());
        test.add(6, 14, "T3");
        assertTrue(test.verifyBalance());
        test.add(8, 16, "T4");
        assertTrue(test.verifyBalance());
        test.add(10, 18, "T5");
        assertTrue(test.verifyBalance());
        test.add(12, 20, "T6");
        assertTrue(test.verifyBalance());
        test.add(14, 22, "T7");
        assertTrue(test.verifyBalance());
        assertEquals(7, test.size());

        int last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
    }

    @Test
    public void testNavigator() {
        AnnotationIndex<String> test = new AnnotationIndex<>();
        test.add(2, 10, "T1");
        assertTrue(test.verifyBalance());
        test.add(4, 12, "T2");
        assertTrue(test.verifyBalance());
        test.add(20, 28, "T10");
        assertTrue(test.verifyBalance());
        test.add(6, 14, "T18");
        assertTrue(test.verifyBalance());
        test.add(6, 14, "T3");
        assertTrue(test.verifyBalance());
        test.add(8, 16, "T4");
        assertTrue(test.verifyBalance());
        test.add(10, 18, "T5");
        assertTrue(test.verifyBalance());
        test.add(14, 22, "T7");
        assertTrue(test.verifyBalance());
        test.add(32, 40, "T16");
        assertTrue(test.verifyBalance());
        test.add(16, 24, "T8");
        assertTrue(test.verifyBalance());
        test.add(18, 26, "T9");
        assertTrue(test.verifyBalance());
        test.add(22, 30, "T11");
        assertTrue(test.verifyBalance());
        test.add(12, 20, "T6");
        assertTrue(test.verifyBalance());
        test.add(24, 32, "T12");
        assertTrue(test.verifyBalance());
        test.add(26, 34, "T13");
        assertTrue(test.verifyBalance());
        test.add(28, 36, "T14");
        assertTrue(test.verifyBalance());
        test.add(30, 38, "T15");
        assertTrue(test.verifyBalance());

        AnnotationNavigator<String> navigator = test.navigator();
        int last = 0;
        int count = 0;
        while(navigator.next()) {
            assertTrue(last <= navigator.start());
            last = navigator.start();
            System.out.println(navigator.start());
            count++;
        }

        assertEquals(count, test.size());

        while(navigator.prev()) {
            assertTrue(last >= navigator.start());
            last = navigator.start();
            System.out.println(navigator.start());
            count--;
        }

        assertEquals(count, 0);

        navigator.reset();
        assertTrue(navigator.next(5));
        assertEquals(navigator.start(), 6);

        assertTrue(navigator.next(13));
        assertEquals(navigator.start(), 14);

        navigator.reset();
        assertTrue(navigator.next(25));
        assertEquals(navigator.start(), 26);

        navigator.reset();
        assertTrue(navigator.nextFloor(5));
        assertEquals(navigator.start(), 4);

        assertTrue(navigator.nextFloor(13));
        assertEquals(navigator.start(), 12);

        navigator.reset();
        assertTrue(navigator.nextFloor(25));
        assertEquals(navigator.start(), 24);
    }

    @Test
    public void testBackwardAddition() {
        AnnotationIndex<String> test = new AnnotationIndex<>();
        test.add(32, 42, "T17");
        assertTrue(test.verifyBalance());
        test.add(30, 40, "T16");
        assertTrue(test.verifyBalance());
        test.add(28, 38, "T15");
        assertTrue(test.verifyBalance());
        test.add(26, 36, "T14");
        assertTrue(test.verifyBalance());
        test.add(24, 34, "T13");
        assertTrue(test.verifyBalance());
        test.add(22, 33, "T12");
        assertTrue(test.verifyBalance());
        test.add(22, 32, "T11");
        assertTrue(test.verifyBalance());
        test.add(20, 30, "T10");
        assertTrue(test.verifyBalance());
        test.add(18, 26, "T9");
        assertTrue(test.verifyBalance());
        test.add(16, 24, "T8");
        assertTrue(test.verifyBalance());
        test.add(14, 22, "T7");
        assertTrue(test.verifyBalance());
        test.add(12, 20, "T6");
        assertTrue(test.verifyBalance());
        test.add(10, 18, "T5");
        assertTrue(test.verifyBalance());
        test.add(8, 16, "T4");
        assertTrue(test.verifyBalance());
        test.add(6, 14, "T3");
        assertTrue(test.verifyBalance());
        test.add(4, 12, "T2");
        assertTrue(test.verifyBalance());
        test.add(2, 10, "T1");
        assertTrue(test.verifyBalance());
        assertEquals(17, test.size());

        int last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last <= entry.getStart());
            last = entry.getStart();
        }
    }

    @Test
    public void testDuplication() {
        AnnotationIndex<String> test = new AnnotationIndex<>();
        test.add(10, 18, "T7");
        test.add(12, 20, "T6");
        test.add(10, 18, "T5");
        test.add(8, 16, "T4");
        assertEquals(4, test.size());

        int last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last <= entry.getStart());
            last = entry.getStart();
        }

        Iterator<AnnotationIndex<String>.Entry> cover = test.coverEntries(10, 18);
        assertTrue(cover.hasNext());
        AnnotationIndex<String>.Entry next = cover.next();
        assertEquals(next.getStart(), 10);
        assertEquals(next.getEnd(), 18);

        assertTrue(cover.hasNext());
        next = cover.next();
        assertEquals(next.getStart(), 10);
        assertEquals(next.getEnd(), 18);

        assertFalse(cover.hasNext());
    }

    @Test
    public void testOverlaps() {
        AnnotationIndex<String> test = new AnnotationIndex<>();
        AnnotationIndex.Entry e6 = test.add(4, 12, "T2");
        AnnotationIndex.Entry e1 = test.add(14, 22, "T7");
        AnnotationIndex.Entry e2 = test.add(12, 20, "T6");
        AnnotationIndex.Entry e5 = test.add(6, 14, "T3");
        AnnotationIndex.Entry e3 = test.add(10, 18, "T5");
        AnnotationIndex.Entry e7 = test.add(2, 10, "T1");
        AnnotationIndex.Entry e4 = test.add(8, 16, "T4");
        assertEquals(7, test.size());

        Iterator<AnnotationIndex<String>.Entry> overlapiter = test.overlapEntries(15, 17);
        int count = 0;
        while(overlapiter.hasNext()) {
            AnnotationIndex<String>.Entry overlap = overlapiter.next();
            assertTrue(overlap.getStart() < 17 && overlap.getEnd() > 15);
            count++;
        }

        System.out.println("----");

        Iterator<AnnotationIndex<String>.Entry> overlap2 = test.overlapEntries(2, 6);
        while(overlap2.hasNext()) {
            System.out.println(overlap2.next());
        }
    }

    @Test
    public void testCover() {
        AnnotationIndex<String> test = new AnnotationIndex<>();
        AnnotationIndex.Entry e2 = test.add(12, 20, "T6");
        AnnotationIndex.Entry e1 = test.add(14, 22, "T7");
        AnnotationIndex.Entry e3 = test.add(10, 18, "T5");
        AnnotationIndex.Entry e4 = test.add(8, 16, "T4");
        AnnotationIndex.Entry e7 = test.add(2, 10, "T1");
        AnnotationIndex.Entry e5 = test.add(6, 14, "T3");
        AnnotationIndex.Entry e6 = test.add(4, 12, "T2");
        assertEquals(7, test.size());

        Iterator<AnnotationIndex<String>.Entry> overlap = test.coverEntries(8, 18);
        assertTrue(overlap.hasNext());


        Iterator<AnnotationIndex<String>.Entry> overlap2 = test.coverEntries(2, 12);
        while(overlap2.hasNext()) {
            System.out.println(overlap2.next());
        }
    }

    @Test
    public void testBackwardAdditionSemiRandomRemoval() {
        AnnotationIndex<String> test = new AnnotationIndex<>();
        AnnotationIndex.Entry e1 = test.add(14, 22, "T7");
        assertTrue(test.verifyBalance());
        AnnotationIndex.Entry e4 = test.add(8, 16, "T4");
        assertTrue(test.verifyBalance());
        AnnotationIndex.Entry e5 = test.add(6, 14, "T3");
        assertTrue(test.verifyBalance());
        AnnotationIndex.Entry e3 = test.add(10, 18, "T5");
        assertTrue(test.verifyBalance());
        AnnotationIndex.Entry e7 = test.add(2, 10, "T1");
        assertTrue(test.verifyBalance());
        AnnotationIndex.Entry e6 = test.add(4, 12, "T2");
        assertTrue(test.verifyBalance());
        AnnotationIndex.Entry e2 = test.add(12, 20, "T6");
        assertTrue(test.verifyBalance());
        assertEquals(7, test.size());

        int last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }

        test.remove(e2);
        assertEquals(6, test.size());
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e5);
        assertEquals(5, test.size());
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e3);
        assertEquals(4, test.size());
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e4);
        assertEquals(3, test.size());
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e1);
        assertEquals(2, test.size());
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e7);
        assertEquals(1, test.size());
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e6);
        assertEquals(0, test.size());
        assertEquals(true, test.isEmpty());
        assertTrue(test.verifyBalance());
    }

    @Test
    public void testBackwardAdditionRemoval() {
        AnnotationIndex<String> test = new AnnotationIndex<>();
        AnnotationIndex.Entry e1 = test.add(14, 22, "T7");
        AnnotationIndex.Entry e2 = test.add(12, 20, "T6");
        AnnotationIndex.Entry e3 = test.add(10, 18, "T5");
        AnnotationIndex.Entry e4 = test.add(8, 16, "T4");
        AnnotationIndex.Entry e5 = test.add(6, 14, "T3");
        AnnotationIndex.Entry e6 = test.add(4, 12, "T2");
        AnnotationIndex.Entry e7 = test.add(2, 10, "T1");

        int last = 0;
        int count = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
            count++;
        }
        assertEquals(test.size(), count);

        test.remove(e7);
        assertTrue(test.verifyBalance());
        last = 0;
        count = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
            count++;
        }
        assertEquals(test.size(), count);
        test.remove(e6);
        assertTrue(test.verifyBalance());
        last = 0;
        count = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
            count++;
        }
        assertEquals(test.size(), count);
        test.remove(e5);
        assertTrue(test.verifyBalance());
        last = 0;
        count = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
            count++;
        }
        assertEquals(test.size(), count);
        test.remove(e4);
        assertTrue(test.verifyBalance());
        last = 0;
        count = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
            count++;
        }
        assertEquals(test.size(), count);
        test.remove(e3);
        assertTrue(test.verifyBalance());
        last = 0;
        count = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
            count++;
        }
        assertEquals(test.size(), count);
        test.remove(e2);
        assertTrue(test.verifyBalance());
        last = 0;
        count = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
            count++;
        }
        assertEquals(test.size(), count);
        test.remove(e1);
        assertTrue(test.verifyBalance());

        assertEquals(0, test.size());
    }

    @Test
    public void testBackwardAdditionRemoval2() {
        AnnotationIndex<String> test = new AnnotationIndex<>();
        AnnotationIndex.Entry e1 = test.add(14, 22, "T7");
        AnnotationIndex.Entry e2 = test.add(12, 20, "T6");
        AnnotationIndex.Entry e3 = test.add(10, 18, "T5");
        AnnotationIndex.Entry e4 = test.add(8, 16, "T4");
        AnnotationIndex.Entry e5 = test.add(6, 14, "T3");
        AnnotationIndex.Entry e6 = test.add(4, 12, "T2");
        AnnotationIndex.Entry e7 = test.add(2, 10, "T1");

        int last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }

        test.remove(e1);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e6);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e5);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e4);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e2);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e3);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test.entries()) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e7);
    }
}

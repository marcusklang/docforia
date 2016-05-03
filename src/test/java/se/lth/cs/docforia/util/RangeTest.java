package se.lth.cs.docforia.util;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Created by csz-mkg on 2016-04-30.
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

        int last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
    }

    @Test
    public void testBackwardAddition() {
        AnnotationIndex<String> test = new AnnotationIndex<>();
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

        int last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
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

        int last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last <= entry.getStart());
            last = entry.getStart();
        }

        Iterator<AnnotationIndex<String>.Entry> cover = test.cover(10, 18);
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

        Iterator<AnnotationIndex<String>.Entry> overlap = test.overlap(15, 17);
        while(overlap.hasNext()) {
            System.out.println(overlap.next());
        }

        System.out.println("----");

        Iterator<AnnotationIndex<String>.Entry> overlap2 = test.overlap(2, 6);
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

        Iterator<AnnotationIndex<String>.Entry> overlap = test.cover(8, 18);
        assertTrue(overlap.hasNext());


        Iterator<AnnotationIndex<String>.Entry> overlap2 = test.cover(2, 12);
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

        int last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }

        test.remove(e2);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e5);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e3);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e4);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e1);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e7);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e6);
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
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }

        test.remove(e7);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e6);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e5);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e4);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e3);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e2);
        assertTrue(test.verifyBalance());
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e1);
        assertTrue(test.verifyBalance());
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
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }

        test.remove(e1);
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e6);
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e5);
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e4);
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e2);
        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e3);

        last = 0;
        for (AnnotationIndex.Entry entry : test) {
            assertTrue(last < entry.getStart());
            last = entry.getStart();
        }
        test.remove(e7);
    }
}

//
// Copyright 2016 Jeff Bush
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

import static org.junit.Assert.*;
import org.junit.*;

public class TransitionVectorTest {
    @Test public void testFindTransition() {
        TransitionVector vec = new TransitionVector(8);
        vec.appendTransition(100, new BitVector("00000001", 2));
        vec.appendTransition(110, new BitVector("00000010", 2));
        vec.appendTransition(111, new BitVector("00001000", 2));
        vec.appendTransition(112, new BitVector("00010000", 2));
        vec.appendTransition(115, new BitVector("00010000", 2));

        Transition t = vec.findTransition(99).next();
        assertEquals(t.getTimestamp(), 100);
        assertEquals(0, t.compare(new BitVector("00000001", 2)));

        t = vec.findTransition(100).next();
        assertEquals(100, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000001", 2)));

        t = vec.findTransition(101).next();
        assertEquals(100, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000001", 2)));

        t = vec.findTransition(105).next();
        assertEquals(100, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000001", 2)));

        t = vec.findTransition(109).next();
        assertEquals(100, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000001", 2)));

        t = vec.findTransition(110).next();
        assertEquals(110, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000010", 2)));

        t = vec.findTransition(111).next();
        assertEquals(111, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00001000", 2)));

        t = vec.findTransition(112).next();
        assertEquals(112, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00010000", 2)));

        t = vec.findTransition(113).next();
        assertEquals(112, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00010000", 2)));

        t = vec.findTransition(116).next();
        assertEquals(115, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00010000", 2)));

        t = vec.findTransition(20000).next();
        assertEquals(115, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00010000", 2)));
    }

    @Test public void testIterator() {
        TransitionVector vec = new TransitionVector(8);
        vec.appendTransition(100, new BitVector("00000001", 2));
        vec.appendTransition(110, new BitVector("00000010", 2));
        vec.appendTransition(111, new BitVector("00001000", 2));

        // Note: previously there was a bug where calling current() would
        // iterate to the next value. We call current here multiple times
        // to confirm that works correctly now.
        TransitionVector.Iterator ti = vec.findTransition(99);
        assertTrue(ti.hasNext());
        Transition t = ti.next();
        assertEquals(110, ti.getNextTimestamp());
        assertEquals(-1, ti.getPrevTimestamp());
        assertTrue(ti.hasNext());
        assertEquals(100, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000001", 2)));

        t = ti.next();
        assertEquals(111, ti.getNextTimestamp());
        assertEquals(100, ti.getPrevTimestamp());
        assertTrue(ti.hasNext());
        assertEquals(110, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000010", 2)));

        t = ti.next();
        assertEquals(-1, ti.getNextTimestamp());
        assertEquals(110, ti.getPrevTimestamp());
        assertFalse(ti.hasNext());
        assertEquals(111, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00001000", 2)));
    }

    /// The passed bitvector is larger than the transition vector width.
    /// Ensure it is truncated
    @Test public void testTruncateVector() {
        TransitionVector vec = new TransitionVector(4);
        vec.appendTransition(100, new BitVector("00001111", 2));
        TransitionVector.Iterator ti = vec.findTransition(0);
        Transition t = ti.next();
        assertEquals("1111", t.toString(2));
    }

    /// The passed bitvector is smaller than the transition vector width
    @Test public void testPadVector() {
        TransitionVector vec = new TransitionVector(16);
        vec.appendTransition(100, new BitVector("101", 2));
        TransitionVector.Iterator ti = vec.findTransition(0);
        Transition t = ti.next();
        assertEquals("0000000000000101", t.toString(2));
    }
}

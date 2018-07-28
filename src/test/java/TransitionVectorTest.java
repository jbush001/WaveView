
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.Test;
import waveview.BitValue;
import waveview.BitVector;
import waveview.Transition;
import waveview.TransitionVector;

public class TransitionVectorTest {
    private void makeBitVectorFromInt(BitVector vec, int value) {
        for (int i = 0; i < vec.getWidth(); i++) {
            vec.setBit(i, BitValue.fromOrdinal((value >> i) & 1));
        }
    }

    @Test
    public void findBeforeFirstTransition() {
        TransitionVector vec = new TransitionVector.Builder(8)
                .appendTransition(100, new BitVector("00000001", 2))
                .appendTransition(110, new BitVector("00000010", 2))
                .getTransitionVector();

        Transition t = vec.findTransition(99).next();
        assertEquals(t.getTimestamp(), 100);
        assertEquals(0, t.compare(new BitVector("00000001", 2)));
    }

    @Test
    public void findFirstTransition() {
        TransitionVector vec = new TransitionVector.Builder(8)
                .appendTransition(100, new BitVector("00000001", 2))
                .appendTransition(110, new BitVector("00000010", 2))
                .getTransitionVector();

        Transition t = vec.findTransition(100).next();
        assertEquals(t.getTimestamp(), 100);
        assertEquals(0, t.compare(new BitVector("00000001", 2)));
    }

    @Test
    public void findAfterFirstTransition() {
        TransitionVector vec = new TransitionVector.Builder(8)
                .appendTransition(100, new BitVector("00000001", 2))
                .appendTransition(110, new BitVector("00000010", 2))
                .appendTransition(120, new BitVector("10101010", 2))
                .getTransitionVector();

        Transition t = vec.findTransition(101).next();
        assertEquals(t.getTimestamp(), 100);
        assertEquals(0, t.compare(new BitVector("00000001", 2)));
    }

    @Test
    public void findMiddleTransition() {
        TransitionVector vec = new TransitionVector.Builder(8)
                .appendTransition(100, new BitVector("00000001", 2))
                .appendTransition(110, new BitVector("00000010", 2))
                .appendTransition(120, new BitVector("10101010", 2))
                .getTransitionVector();

        Transition t = vec.findTransition(110).next();
        assertEquals(t.getTimestamp(), 110);
        assertEquals(0, t.compare(new BitVector("00000010", 2)));
    }
    @Test
    public void findLastTransition() {
        TransitionVector vec = new TransitionVector.Builder(8)
                .appendTransition(100, new BitVector("00000001", 2))
                .appendTransition(110, new BitVector("00000010", 2))
                .getTransitionVector();

        Transition t = vec.findTransition(110).next();
        assertEquals(110, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000010", 2)));
    }

    @Test
    public void findAfterLastTransition() {
        TransitionVector vec = new TransitionVector.Builder(8)
                .appendTransition(100, new BitVector("00000001", 2))
                .appendTransition(110, new BitVector("00000010", 2))
                .getTransitionVector();

        Transition t = vec.findTransition(20000).next();
        assertEquals(110, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000010", 2)));
    }

    @Test
    public void iteratorFirst() {
        TransitionVector vec = new TransitionVector.Builder(8)
                .appendTransition(100, new BitVector("00000001", 2))
                .appendTransition(110, new BitVector("00000010", 2))
                .getTransitionVector();

        Iterator<Transition> ti = vec.findTransition(99);
        assertTrue(ti.hasNext());
        Transition t = ti.next();
        assertTrue(ti.hasNext());
        assertEquals(100, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000001", 2)));
    }

    @Test
    public void iteratorSecond() {
        TransitionVector vec = new TransitionVector.Builder(8)
                .appendTransition(100, new BitVector("00000001", 2))
                .appendTransition(110, new BitVector("00000010", 2))
                .appendTransition(111, new BitVector("00001000", 2))
                .getTransitionVector();

        Iterator<Transition> ti = vec.findTransition(99);
        ti.next();
        Transition t = ti.next();
        assertTrue(ti.hasNext());
        assertEquals(110, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000010", 2)));
    }

    @Test
    public void iteratorEnd() {
        TransitionVector vec = new TransitionVector.Builder(8)
                .appendTransition(100, new BitVector("00000001", 2))
                .appendTransition(110, new BitVector("00000010", 2))
                .appendTransition(111, new BitVector("00001000", 2))
                .getTransitionVector();
        Iterator<Transition> ti = vec.findTransition(99);
        ti.next();
        ti.next();
        ti.next();
        assertFalse(ti.hasNext());

        try {
            ti.next();
            fail("next didn't throw exception");
        } catch (NoSuchElementException exc) {
            // Should arrive here on success
        }
    }

    // Remove is not supported
    @Test
    public void iteratorRemove() {
        TransitionVector vec = new TransitionVector.Builder(8)
                .appendTransition(100, new BitVector("00000001", 2))
                .getTransitionVector();
        Iterator<Transition> ti = vec.findTransition(99);

        try {
            ti.remove();
            fail("remove didn't throw exception");
        } catch (UnsupportedOperationException exc) {
            // Should arrive here on success
        }
    }

    /// The passed bitvector is larger than the transition vector width.
    /// Ensure it is truncated
    @Test
    public void truncateVector() {
        TransitionVector vec = new TransitionVector.Builder(4)
            .appendTransition(100, new BitVector("00001111", 2))
            .getTransitionVector();

        Iterator<Transition> ti = vec.findTransition(0);
        Transition t = ti.next();

        assertEquals("1111", t.toString(2));
    }

    /// The passed bitvector is smaller than the transition vector width
    @Test
    public void padVector() {
        TransitionVector vec = new TransitionVector.Builder(16)
                .appendTransition(100, new BitVector("101", 2))
                .getTransitionVector();

        Iterator<Transition> ti = vec.findTransition(0);
        Transition t = ti.next();
        assertEquals("0000000000000101", t.toString(2));
    }

    /// Build a large transition vector, which will require reallocating the data array.
    @Test
    public void largeTransitionVector() {
        TransitionVector.Builder builder = new TransitionVector.Builder(16);
        BitVector bvec = new BitVector(16);
        for (int idx = 0; idx < 100000; idx++) {
            makeBitVectorFromInt(bvec, idx);
            builder.appendTransition(idx * 5, bvec);
        }

        TransitionVector tvec = builder.getTransitionVector();

        Iterator<Transition> iter = tvec.findTransition(0);
        for (int idx = 0; idx < 100000; idx++) {
            makeBitVectorFromInt(bvec, idx);
            Transition t = iter.next();
            assertEquals(idx * 5, t.getTimestamp());
            assertEquals(0, t.compare(bvec));
        }
    }

    @Test
    public void getMaxTimestampEmpty() {
        TransitionVector.Builder builder = new TransitionVector.Builder(1);
        TransitionVector tvec = builder.getTransitionVector();

        assertEquals(0, tvec.getMaxTimestamp());
    }

    @Test
    public void getMaxTimestamp() {
        TransitionVector tvec = new TransitionVector.Builder(1)
                .appendTransition(100, new BitVector("1", 2))
                .getTransitionVector();
        assertEquals(100, tvec.getMaxTimestamp());
    }
}


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
import java.util.Random;
import org.junit.Test;
import waveview.wavedata.BitValue;
import waveview.wavedata.BitVector;
import waveview.wavedata.Transition;
import waveview.wavedata.TransitionVector;

public class TransitionVectorTest {
    private BitVector makeBitVectorFromInt(int width, int value) {
        BitVector vec = new BitVector(width);
        for (int i = 0; i < vec.getWidth(); i++) {
            vec.setBit(i, BitValue.fromOrdinal((value >> i) & 1));
        }

        return vec;
    }

    @Test
    public void findBeforeFirstTransition() {
        TransitionVector vec =
            TransitionVector.Builder.createBuilder(8)
                .appendTransition(100, new BitVector("00000001", 2))
                .appendTransition(110, new BitVector("00000010", 2))
                .getTransitionVector();

        Transition t = vec.findTransition(99).next();
        assertEquals(t.getTimestamp(), 100);
        assertEquals(0, t.compare(new BitVector("00000001", 2)));
    }

    @Test
    public void findFirstTransition() {
        TransitionVector vec =
            TransitionVector.Builder.createBuilder(8)
                .appendTransition(100, new BitVector("00000001", 2))
                .appendTransition(110, new BitVector("00000010", 2))
                .getTransitionVector();

        Transition t = vec.findTransition(100).next();
        assertEquals(t.getTimestamp(), 100);
        assertEquals(0, t.compare(new BitVector("00000001", 2)));
    }

    @Test
    public void findAfterFirstTransition() {
        TransitionVector vec =
            TransitionVector.Builder.createBuilder(8)
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
        TransitionVector vec =
            TransitionVector.Builder.createBuilder(8)
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
        TransitionVector vec =
            TransitionVector.Builder.createBuilder(8)
                .appendTransition(100, new BitVector("00000001", 2))
                .appendTransition(110, new BitVector("00000010", 2))
                .getTransitionVector();

        Transition t = vec.findTransition(110).next();
        assertEquals(110, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000010", 2)));
    }

    @Test
    public void findAfterLastTransition() {
        TransitionVector vec =
            TransitionVector.Builder.createBuilder(8)
                .appendTransition(100, new BitVector("00000001", 2))
                .appendTransition(110, new BitVector("00000010", 2))
                .getTransitionVector();

        Transition t = vec.findTransition(20000).next();
        assertEquals(110, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("00000010", 2)));
    }

    @Test
    public void iteratorFirst() {
        TransitionVector vec =
            TransitionVector.Builder.createBuilder(8)
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
        TransitionVector vec =
            TransitionVector.Builder.createBuilder(8)
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

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void iteratorEnd() {
        TransitionVector vec =
            TransitionVector.Builder.createBuilder(8)
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
            // Expected
        }
    }

    // Remove is not supported
    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void iteratorRemove() {
        TransitionVector vec =
            TransitionVector.Builder.createBuilder(8)
                .appendTransition(100, new BitVector("00000001", 2))
                .getTransitionVector();
        Iterator<Transition> ti = vec.findTransition(99);

        try {
            ti.remove();
            fail("remove didn't throw exception");
        } catch (UnsupportedOperationException exc) {
            // Expected
        }
    }

    /// The passed bitvector is larger than the transition vector width.
    /// Ensure it is truncated
    @Test
    public void truncateVector() {
        TransitionVector vec =
            TransitionVector.Builder.createBuilder(4)
                .appendTransition(100, new BitVector("00001111", 2))
                .getTransitionVector();

        Iterator<Transition> ti = vec.findTransition(0);
        Transition t = ti.next();

        assertEquals("1111", t.toString(2));
    }

    /// The passed bitvector is smaller than the transition vector width
    @Test
    public void padVector() {
        TransitionVector vec =
            TransitionVector.Builder.createBuilder(16)
                .appendTransition(100, new BitVector("101", 2))
                .getTransitionVector();

        Iterator<Transition> ti = vec.findTransition(0);
        Transition t = ti.next();
        assertEquals("0000000000000101", t.toString(2));
    }

    /// Build a large transition vector, which will require reallocating the
    /// data array.
    @Test
    public void largeTransitionVector() {
        TransitionVector.Builder builder =
            TransitionVector.Builder.createBuilder(16);
        for (int idx = 0; idx < 100000; idx++) {
            BitVector bvec = makeBitVectorFromInt(16, idx);
            builder.appendTransition(idx * 5, bvec);
        }

        TransitionVector tvec = builder.getTransitionVector();

        Iterator<Transition> iter = tvec.findTransition(0);
        for (int idx = 0; idx < 100000; idx++) {
            BitVector bvec = makeBitVectorFromInt(16, idx);
            Transition t = iter.next();
            assertEquals(idx * 5, t.getTimestamp());
            assertEquals(0, t.compare(bvec));
        }
    }

    // Each value in the vector is larger than a word (the underlying storage
    // size) This validates logic within the iterator that increments to the
    // next word.
    @Test
    public void wideValue() {
        final int VEC_WIDTH = 65;

        TransitionVector.Builder builder =
            TransitionVector.Builder.createBuilder(VEC_WIDTH);
        BitVector bvec1 = new BitVector(VEC_WIDTH);
        BitVector bvec2 = new BitVector(VEC_WIDTH);
        BitVector bvec3 = new BitVector(VEC_WIDTH);
        Random random = new Random();

        for (int i = 0; i < VEC_WIDTH; i++) {
            bvec1.setBit(i,
                         random.nextBoolean() ? BitValue.ONE : BitValue.ZERO);
            bvec2.setBit(i,
                         random.nextBoolean() ? BitValue.ONE : BitValue.ZERO);
            bvec3.setBit(i,
                         random.nextBoolean() ? BitValue.ONE : BitValue.ZERO);
        }

        builder.appendTransition(10, bvec1);
        builder.appendTransition(20, bvec2);
        builder.appendTransition(30, bvec3);

        TransitionVector tvec = builder.getTransitionVector();
        Iterator<Transition> iter = tvec.findTransition(0);
        Transition t = iter.next();
        assertEquals(0, t.compare(bvec1));
        t = iter.next();
        assertEquals(0, t.compare(bvec2));
        t = iter.next();
        assertEquals(0, t.compare(bvec3));
    }

    @Test
    public void getMaxTimestampEmpty() {
        TransitionVector.Builder builder =
            TransitionVector.Builder.createBuilder(1);
        TransitionVector tvec = builder.getTransitionVector();

        assertEquals(0, tvec.getMaxTimestamp());
    }

    @Test
    public void getMaxTimestamp() {
        TransitionVector tvec =
            TransitionVector.Builder.createBuilder(1)
                .appendTransition(100, new BitVector("1", 2))
                .getTransitionVector();
        assertEquals(100, tvec.getMaxTimestamp());
    }

    // Regression test: array out of bounds exception was thrown when reading
    // last transition if it was word aligned. Allocate a large buffer to ensure
    // this will hit the end of the allocated array even if the allocator
    // changes (assuming power of two)
    @Test
    public void transitionAtArrayEnd() {
        final int NUM_BITS = 1024 * 32;
        TransitionVector.Builder builder =
            TransitionVector.Builder.createBuilder(1);
        for (int i = 0; i < NUM_BITS; i++) {
            builder.appendTransition(i, new BitVector("1", 2));
        }

        Iterator<Transition> iter =
            builder.getTransitionVector().findTransition(0);
        for (int i = 0; i < NUM_BITS; i++) {
            iter.next();
        }
    }
}

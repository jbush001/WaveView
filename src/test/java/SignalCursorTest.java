
//
// Copyright 2019 Jeff Bush
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

import org.junit.Test;
import waveview.decoder.SignalCursor;
import waveview.wavedata.BitValue;
import waveview.wavedata.BitVector;
import waveview.wavedata.TransitionVector;

public class SignalCursorTest {
    @Test
    public void nextLevelTrue() {
        TransitionVector val = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2))
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(20, new BitVector("1", 2))
            .getTransitionVector();
        SignalCursor cursor = new SignalCursor(val);
        assertEquals(17, cursor.nextLevel(17, BitValue.ZERO));
    }

    // Level is initially false at the passed timestamp, iterate.
    @Test
    public void nextLevelFalse() {
        TransitionVector val = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2))
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(20, new BitVector("1", 2))
            .getTransitionVector();
        SignalCursor cursor = new SignalCursor(val);
        System.out.println("nextLevelFalse");
        assertEquals(20, cursor.nextLevel(17, BitValue.ONE));
    }

    @Test
    public void nextLevelNotFound() {
        TransitionVector val = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2))
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(20, new BitVector("1", 2))
            .getTransitionVector();
        SignalCursor cursor = new SignalCursor(val);
        assertEquals(-1, cursor.nextLevel(20, BitValue.ZERO));
    }

    // The signal cursor cache the current segment that is the same
    // level. Exercise the case where the queried value is in the
    // same segment.
    @Test
    public void nextLevelSameSegment() {
        TransitionVector val = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2))
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(20, new BitVector("1", 2))
            .getTransitionVector();
        SignalCursor cursor = new SignalCursor(val);
        assertEquals(20, cursor.nextLevel(17, BitValue.ONE));
        assertEquals(20, cursor.nextLevel(18, BitValue.ONE));
    }

    // The signal cursor cache the current segment that is the same
    // level. Pick a time that is before the last searched segment
    // so it will need to do a lookup again.
    // Note also that the first segment we search for is hte last
    // one in the trace. This exercises an edge case where it would
    // normally trigger a not-found.
    @Test
    public void nextLevelBackward() {
        TransitionVector val = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2))
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(20, new BitVector("1", 2))
            .getTransitionVector();
        SignalCursor cursor = new SignalCursor(val);
        assertEquals(20, cursor.nextLevel(17, BitValue.ONE));
        assertEquals(15, cursor.nextLevel(7, BitValue.ZERO));
    }

    @Test
    public void nextRisingEdgeActive() {
        TransitionVector val = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2))
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(20, new BitVector("1", 2))
            .getTransitionVector();
        SignalCursor cursor = new SignalCursor(val);
        assertEquals(20, cursor.nextEdge(7, BitValue.ONE));
    }

    @Test
    public void nextRisingEdgeInactive() {
        TransitionVector val = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2))
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(20, new BitVector("1", 2))
            .getTransitionVector();
        SignalCursor cursor = new SignalCursor(val);
        assertEquals(20, cursor.nextEdge(17, BitValue.ONE));
    }

    @Test
    public void nextRisingEdgeNotFound() {
        TransitionVector val = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2))
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(20, new BitVector("1", 2))
            .getTransitionVector();
        SignalCursor cursor = new SignalCursor(val);
        assertEquals(-1, cursor.nextEdge(20, BitValue.ONE));
    }

    @Test
    public void nextFallingEdgeInactive() {
        TransitionVector val = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2))
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(20, new BitVector("1", 2))
            .getTransitionVector();
        SignalCursor cursor = new SignalCursor(val);
        assertEquals(15, cursor.nextEdge(7, BitValue.ZERO));
    }

    @Test
    public void redundantEdge() {
        TransitionVector val = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2))
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(20, new BitVector("1", 2))
            .getTransitionVector();
        SignalCursor cursor = new SignalCursor(val);
        assertEquals(20, cursor.nextEdge(7, BitValue.ONE));
    }

    @Test
    public void getLevelAt() {
        TransitionVector val = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2))
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(20, new BitVector("1", 2))
            .getTransitionVector();
        SignalCursor cursor = new SignalCursor(val);
        assertEquals(BitValue.ONE, cursor.getValueAt(7));
        assertEquals(BitValue.ZERO, cursor.getValueAt(15)); // jump, at boundary
        assertEquals(BitValue.ZERO, cursor.getValueAt(17)); // same segment
    }

    // The signal cursor cache the current segment that is the same
    // level. Pick a time that is before the last searched segment
    // so it will need to do a lookup again.
    @Test
    public void getLevelAtBackward() {
        TransitionVector val = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2))
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(20, new BitVector("1", 2))
            .getTransitionVector();
        SignalCursor cursor = new SignalCursor(val);
        assertEquals(BitValue.ZERO, cursor.getValueAt(17));
        assertEquals(BitValue.ONE, cursor.getValueAt(7));
    }
}

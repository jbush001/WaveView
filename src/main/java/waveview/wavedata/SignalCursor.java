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

package waveview.wavedata;

import java.util.Iterator;
import waveview.wavedata.BitValue;
import waveview.wavedata.Transition;
import waveview.wavedata.TransitionVector;

/// Convenience class for decoding waveforms
public class SignalCursor {
    // The iterator is always at the end of the current segment
    final TransitionVector transitionVector;
    Iterator<Transition> iterator;
    long segmentBegin;
    long segmentEnd;
    BitValue currentValue;
    BitValue nextValue;

    public SignalCursor(TransitionVector transitionVector) {
        this.transitionVector = transitionVector;
        jumpToTime(0);
    }

    /// Return the next timestamp where the signal transitions to a
    /// given level.
    /// @param timestamp Search after this timestamp
    /// @param expectLevel Stop when the signal transitions to this level
    ///    if the signal is already at this level, scan until the next
    ///    transition to this level.
    /// @returns A positive timestamp, which will be greater or equal
    ///    to the timestamp parameter, or -1 if there are no more
    ///    transitions.
    public long nextEdge(long timestamp, BitValue expectPolarity) {
        assert timestamp >= 0;

        timestamp = nextLevel(timestamp, expectPolarity.invert());
        if (timestamp < 0) {
            return -1;
        }

        return nextLevel(timestamp, expectPolarity);
    }

    /// Return the next timestamp where the signal is at a given level
    /// @param timestamp Search at or after this timestamp
    /// @param expectLevel Stop when the signal is at this level
    /// @returns A positive timestamp, which will be greater or equal
    ///    to the timestamp parameter, or -1 if there are no more
    ///    transitions.
    public long nextLevel(long timestamp, BitValue expectLevel) {
        assert timestamp >= 0;
        if (timestamp < segmentBegin || timestamp >= segmentEnd) {
            jumpToTime(timestamp);
        }

        // Cursor is at the first transition before this timestamp

        while (currentValue != expectLevel) {
            if (!nextSegment()) {
                return -1;
            }
        }

        return Long.max(segmentBegin, timestamp);
    }

    public BitValue getValueAt(long timestamp) {
        if (timestamp < segmentBegin || timestamp >= segmentEnd) {
            jumpToTime(timestamp);
        }

        return currentValue;
    }

    private void jumpToTime(long timestamp) {
        iterator = transitionVector.findTransition(timestamp);
        segmentEnd = 0; // Avoid nextSegment returning -1
        nextSegment();
        nextSegment();
    }

    private boolean nextSegment() {
        if (segmentEnd == Long.MAX_VALUE) {
            return false;
        }

        currentValue = nextValue;
        segmentBegin = segmentEnd;
        if (iterator.hasNext()) {
            Transition t = iterator.next();
            nextValue = t.getBit(0);
            segmentEnd = t.getTimestamp();
        } else {
            // Last segment
            segmentEnd = Long.MAX_VALUE;
        }

        return true;
    }
}
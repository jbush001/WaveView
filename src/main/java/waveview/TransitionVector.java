//
// Copyright 2011-2012 Jeff Bush
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package waveview;

import java.util.*;

///
/// An ordered series of value changes on a single net (which may have
/// one or more bits).  This is a convenience class used by TraceDataModel.
/// Allocating hundreds of thousands of Transition objects would be slow and
/// inefficient, so this stores the values packed into a single array.
/// Because it is sorted, it supports efficient binary searches for values
/// at specific timestamps.
///
/// @bug This doesn't propertly handle nets that are uninitalized at the beginning
/// of the trace.  They are assumed to have the value of the first transition.
///
public class TransitionVector {
    public TransitionVector(int width) {
        assert width > 0;
        fWidth = width;
    }

    /// @returns Iterator at transition. If there isn't a transition at this transition,
    ///   returns the transition before it. If this is before the first transition, returns
    ///   the first transition.
    public Iterator<Transition> findTransition(long timestamp) {
        // Binary search
        int low = 0;                      // Lowest possible index
        int high = fTransitionCount - 1;  // Highest possible index

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midKey = fTimestamps[mid];
            if (timestamp < midKey)
                high = mid - 1;
            else if (timestamp > midKey)
                low = mid + 1;
            else
                return new TransitionVectorIterator(mid);
        }

        // No exact match. Low is equal to the index the element would be
        // at if it existed. We want to return the element before the
        // timestamp. If low == 0, this is before the first element:
        // return 0.
        return new TransitionVectorIterator(low == 0 ? 0 : low - 1);
    }

    public long getMaxTimestamp() {
        if (fTransitionCount == 0)
            return 0;

        return fTimestamps[fTransitionCount - 1];
    }

    public int getWidth() {
        return fWidth;
    }

    private class TransitionVectorIterator implements Iterator<Transition> {
        TransitionVectorIterator(int index) {
            assert index >= 0;
            fNextIndex = index;
            fTransition.setWidth(fWidth);
        }

        @Override
        public boolean hasNext() {
            return fNextIndex < fTransitionCount;
        }

        /// @note the Transition returned from next will be clobbered
        /// if next() is called again (since it's preallocated and
        /// reused)
        @Override
        public Transition next() {
            if (!hasNext())
                throw new NoSuchElementException();

            int bitOffset = fNextIndex * fWidth * 2;
            int wordOffset = bitOffset >> 5;
            bitOffset &= 31;
            int currentWord = fValues[wordOffset] >> bitOffset;

            // Copy values out of packed array
            for (int i = 0; i < fWidth; i++) {
                fTransition.setBit(fWidth - i - 1, currentWord & 3);
                bitOffset += 2;
                if (bitOffset == 32) {
                    wordOffset++;
                    currentWord = fValues[wordOffset];
                    bitOffset = 0;
                } else
                    currentWord >>= 2;
            }

            fTransition.setTimestamp(fTimestamps[fNextIndex]);
            fNextIndex++;

            return fTransition;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private int fNextIndex;

        // Reuse the same Transition/BitVector so we don't have to keep
        // reallocating.
        private Transition fTransition = new Transition();
    }

    /// Called while the waveform is being loaded.
    /// The timestamp must be after the last transition that was
    /// appended
    public void appendTransition(long timestamp, BitVector values) {
        if (fTransitionCount == fAllocSize) {
            // Grow the array
            if (fAllocSize < 128)
                fAllocSize = 128;
            else
                fAllocSize *= 2;

            long[] newTimestamps = new long[fAllocSize];
            int[] newValues = new int[fAllocSize * fWidth / 16];

            if (fTimestamps != null) {
                System.arraycopy(fTimestamps, 0, newTimestamps,
                                 0, fTransitionCount);
                System.arraycopy(fValues, 0, newValues,
                                 0, fTransitionCount * fWidth / 16);
            }

            fTimestamps = newTimestamps;
            fValues = newValues;
        }

        if (fTransitionCount > 0)
            assert timestamp >= fTimestamps[fTransitionCount - 1];

        fTimestamps[fTransitionCount] = timestamp;

        int bitIndex = fTransitionCount * fWidth;

        // If the passed value is smaller than the vector width, pad with zeroes
        if (fWidth > values.getWidth())
            bitIndex += fWidth - values.getWidth();

        int wordOffset = bitIndex / 16;
        int bitOffset = (bitIndex * 2) % 32;

        // If the passed value is wider than the vector width, only copy the
        // low order bits of it.
        for (int i = Math.min(values.getWidth(), fWidth) - 1; i >= 0; i--) {
            fValues[wordOffset] |= values.getBit(i) << bitOffset;
            bitOffset += 2;
            if (bitOffset == 32) {
                wordOffset++;
                bitOffset = 0;
            }
        }

        fTransitionCount++;
    }

    // Number of bits for this net
    private int fWidth;

    private long[] fTimestamps;

    // Values are packed into this array. Each bit in the output requires two
    // bits in this array (to represent four values: 0, 1, X, and Z). These
    // are stored starting with the first bit as the LSB of each array word
    // up to the MSB. The next bit is then stored in the next higher array
    // entry. There is no padding between adjacent transitions.
    private int[] fValues;
    private int fTransitionCount;
    private int fAllocSize; // Used only while building
}

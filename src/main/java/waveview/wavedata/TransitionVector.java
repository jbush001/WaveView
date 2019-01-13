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

package waveview.wavedata;

import java.util.Iterator;
import java.util.NoSuchElementException;

///
/// An ordered series of value changes on a single net (which may have
/// one or more bits).  This is a convenience class used by WaveformDataModel.
/// Allocating hundreds of thousands of Transition objects would be slow and
/// use a lot more memory, so this packs the values into a single array.
/// Because it is sorted, it supports efficient binary searches for values
/// at specific timestamps.
///
/// @bug This doesn't propertly handle nets that are uninitalized at the
/// beginning of the waveform.  They are assumed to have the value of the first
/// transition.
///
public final class TransitionVector {
    private final int width; // Number of bits for this net
    private long[] timestamps;

    // Values are packed into this array. Each bit in the output requires two
    // bits in this array (to represent four values: 0, 1, X, and Z). These
    // are stored starting with the first bit as the LSB of each array word
    // up to the MSB. The next bit is then stored in the next higher array
    // entry. There is no padding between adjacent transitions.
    private long[] packedValues;
    private int transitionCount;

    private TransitionVector(int width) {
        assert width > 0;
        this.width = width;
    }

    /// @returns Iterator at transition. If there isn't a transition at this
    /// transition, returns the transition before it. If this is before the
    /// first transition, returns the first transition.
    public Iterator<Transition> findTransition(long timestamp) {
        // Binary search
        int low = 0; // Lowest possible index
        int high = transitionCount - 1; // Highest possible index

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midKey = timestamps[mid];
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
        if (transitionCount == 0)
            return 0;

        return timestamps[transitionCount - 1];
    }

    public int getWidth() {
        return width;
    }

    private final class TransitionVectorIterator implements Iterator<Transition> {
        private int transitionIndex;

        // Reuse the same Transition/BitVector so we don't have to keep
        // reallocating.
        private final Transition transition = new Transition();

        TransitionVectorIterator(int transitionIndex) {
            assert transitionIndex >= 0;
            this.transitionIndex = transitionIndex;
            transition.setWidth(width);
        }

        @Override
        public boolean hasNext() {
            return transitionIndex < transitionCount;
        }

        /// @note the Transition returned from next will be clobbered
        /// if next() is called again (since it's preallocated and
        /// reused)
        @Override
        public Transition next() {
            if (!hasNext())
                throw new NoSuchElementException();

            int encodedBitIndex = transitionIndex * width * 2;
            int wordIndex = encodedBitIndex / 64;
            int shiftAmount = encodedBitIndex % 64;
            long currentWord = packedValues[wordIndex] >> shiftAmount;

            // Copy values out of packed array
            for (int i = 0; i < width; i++) {
                if (shiftAmount == 64) {
                    wordIndex++;
                    currentWord = packedValues[wordIndex];
                    shiftAmount = 0;
                }

                transition.setBit(width - i - 1, BitValue.fromOrdinal((int) (currentWord & 3)));
                shiftAmount += 2;
                currentWord >>= 2;
            }

            transition.setTimestamp(timestamps[transitionIndex]);
            transitionIndex++;

            return transition;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static final class Builder {
        private final TransitionVector vector;
        private int allocatedTransitions;

        public static Builder createBuilder(int width) {
            return new Builder(new TransitionVector(width));
        }

        private Builder(TransitionVector vector) {
            this.vector = vector;
        }

        public TransitionVector getTransitionVector() {
            return vector;
        }

        // The timestamp must be after the last transition that was appended
        // (transitions must be appended in order)
        public Builder appendTransition(long timestamp, BitVector value) {
            if (vector.transitionCount == allocatedTransitions) {
                // Grow the array
                if (allocatedTransitions < 128) {
                    allocatedTransitions = 128;
                } else {
                    allocatedTransitions *= 2;
                }

                long[] newTimestamps = new long[allocatedTransitions];
                long[] newPackedValues = new long[allocatedTransitions * vector.width * 2 / 64];

                if (vector.timestamps != null) {
                    System.arraycopy(
                        vector.timestamps, 0, newTimestamps, 0, vector.transitionCount);
                    System.arraycopy(vector.packedValues, 0, newPackedValues, 0,
                        vector.transitionCount * vector.width * 2 / 64);
                }

                vector.timestamps = newTimestamps;
                vector.packedValues = newPackedValues;
            }

            if (vector.transitionCount > 0) {
                assert timestamp >= vector.timestamps[vector.transitionCount - 1];
            }

            vector.timestamps[vector.transitionCount] = timestamp;

            int encodedBitIndex = vector.transitionCount * vector.width * 2;

            // If the passed value is smaller than the vector width, pad with
            // zeroes
            if (vector.width > value.getWidth()) {
                encodedBitIndex += (vector.width - value.getWidth()) * 2;
            }

            int wordIndex = encodedBitIndex / 64;
            int shiftAmount = encodedBitIndex % 64;

            // If the passed value is wider than the vector width, only copy the
            // low order bits of it.
            for (int i = Math.min(value.getWidth(), vector.width) - 1; i >= 0; i--) {
                vector.packedValues[wordIndex] |= ((long) value.getBit(i).ordinal()) << shiftAmount;
                shiftAmount += 2;
                if (shiftAmount == 64) {
                    wordIndex++;
                    shiftAmount = 0;
                }
            }

            vector.transitionCount++;
            return this;
        }
    }
}

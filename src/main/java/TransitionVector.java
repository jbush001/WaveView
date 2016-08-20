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

import java.util.*;

///
/// A compact representation of a time series of value changes on a single net
/// This is a convenience class to be used inside TraceDataModel.
///
/// @bug This doesn't propertly handle nets that are uninitalized at the beginning
/// of the trace.  They are assumed to have the value of the first transition.
///
class TransitionVector
{
    TransitionVector(int width)
    {
        assert width > 0;
        fWidth = width;
    }

    /// @returns Iterator at transition. If there isn't a transition at this transition,
    ///   returns the transition before it. If this is before the first transition, returns
    ///   the first transition.
    Iterator findTransition(long timestamp)
    {
        // Binary search
        int low = 0;
        int high = fTransitionCount;

        while (low < high)
        {
            int mid = (low + high) / 2;
            long elemKey = fTimestamps[mid];
            if (timestamp < elemKey)
                high = mid;
            else
                low = mid + 1;
        }

        return new Iterator(low == 0 ? 0 : low - 1);
    }

    long getMaxTimestamp()
    {
        if (fTransitionCount == 0)
            return 0;

        return fTimestamps[fTransitionCount - 1];
    }

    int getWidth()
    {
        return fWidth;
    }

    /// This augments the normal iterator with methods to obtain
    /// the timestamp of the next and previous events.
    public class Iterator implements java.util.Iterator<Transition>
    {
        Iterator(int index)
        {
            assert index >= 0;
            fNextIndex = index;
            fTransition.setWidth(fWidth);
        }

        public boolean hasNext()
        {
            return fNextIndex < fTransitionCount;
        }

        public Transition next()
        {
            if (!hasNext())
                return null;

            int bitOffset = (fNextIndex * fWidth * 2);
            int wordOffset = bitOffset >> 5;
            bitOffset &= 31;
            int currentWord = fValues[wordOffset] >> bitOffset;

            // Copy values out of packed array
            for (int i = 0; i < fWidth; i++)
            {
                fTransition.setBit(fWidth - i - 1, currentWord & 3);
                bitOffset += 2;
                if (bitOffset == 32)
                {
                    wordOffset++;
                    currentWord = fValues[wordOffset];
                    bitOffset = 0;
                }
                else
                    currentWord >>= 2;
            }

            fTransition.setTimestamp(fTimestamps[fNextIndex]);
            fNextIndex++;

            return fTransition;
        }

        public long getNextTimestamp()
        {
            if (fNextIndex >= fTransitionCount)
                return -1;

            return fTimestamps[fNextIndex];
        }

        public long getPrevTimestamp()
        {
            if (fNextIndex < 2)
                return -1;

            return fTimestamps[fNextIndex - 2];
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        private int fNextIndex;

        // Reuse the same Transition/BitVector so we don't have to keep
        // reallocating.
        private Transition fTransition = new Transition();
    }

    /// The timestamp must be after the last transition that was
    /// appended
    public void appendTransition(long timestamp, BitVector values)
    {
        if (fTransitionCount == fAllocSize)
        {
            // Grow the array
            if (fAllocSize < 128)
                fAllocSize = 128;
            else
                fAllocSize *= 2;

            long[] newTimestamps = new long[fAllocSize];
            int[] newValues = new int[fAllocSize * fWidth / 16];

            if (fTimestamps != null)
            {
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
        int wordOffset = fTransitionCount * fWidth / 16;
        int bitOffset = (fTransitionCount * fWidth * 2) % 32;

        for (int i = values.getWidth() - 1; i >= 0; i--)
        {
            fValues[wordOffset] |= values.getBit(i) << bitOffset;
            bitOffset += 2;
            if (bitOffset == 32)
            {
                wordOffset++;
                bitOffset = 0;
            }
        }

        fTransitionCount++;
    }

    private int fWidth;
    private long[] fTimestamps;

    // Values are packed into this array
    private int[] fValues;
    private int fTransitionCount;
    private int fAllocSize; // Used only while building
}
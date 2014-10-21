// 
// Copyright 2011-2012 Jeff Bush
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//	   http://www.apache.org/licenses/LICENSE-2.0
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
/// This is really a convenience class to be used inside TraceDataModel.
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

	AbstractTransitionIterator findTransition(long timestamp)
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

		return new ConcreteTransitionIterator(low - 1);
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

	private class ConcreteTransitionIterator implements AbstractTransitionIterator
	{
		ConcreteTransitionIterator(int index)
		{
			fIndex = index;
			int offset = index * fWidth;
			fWordOffset = offset / 16;
			fBitOffset = (offset * 2) % 32;
			fCurrentWord = fValues[fWordOffset] >> fBitOffset;
			fTransition.setWidth(fWidth);
		}

		public Transition current()
		{
			for (int i = 0; i < fWidth; i++)
			{
				fTransition.setValue(i, fCurrentWord & 3);
				fBitOffset += 2;
				if (fBitOffset == 32)
				{
					fWordOffset++;
					fCurrentWord = fValues[fWordOffset];
					fBitOffset = 0;
				}
				else
					fCurrentWord >>= 2;
			}
			
			fTransition.setTimestamp(fTimestamps[fIndex]);
			return fTransition;
		}

		public boolean hasNext()
		{
			return fIndex < fTransitionCount;
		}
		
		public Transition next()
		{
			if (!hasNext())
				return null;

			Transition t = current();
			fIndex++;
			
			return t;
		}

		public long getNextTimestamp()
		{
			if (fIndex >= fTransitionCount - 1)
				return -1;
		
			return fTimestamps[fIndex + 1];
		}

		public long getPrevTimestamp()
		{
			if (fIndex <= 0)
				return -1;
			
			return fTimestamps[fIndex - 1];
		}
		
		public void remove()
		{
			throw new UnsupportedOperationException();
		}

		private int fWordOffset;
		private int fBitOffset;
		private int fCurrentWord;
		private int fIndex;
		private Transition fTransition = new Transition();
	}

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

		assert timestamp >= fTimestamps[fTransitionCount - 1];
		fTimestamps[fTransitionCount] = timestamp;
		int wordOffset = fTransitionCount * fWidth / 16;
		int bitOffset = (fTransitionCount * fWidth * 2) % 32;

		for (int i = 0; i < values.getWidth(); i++)
		{
			fValues[wordOffset] |= values.getValue(i) << bitOffset;
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
	private int[] fValues;
	private int fTransitionCount;
	private int fAllocSize; // Used only while building
}
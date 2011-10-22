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

	Iterator<Transition> findTransition(long timestamp)
	{
		// Binary search
		int low = 0;
		int high = fTransitionCount + 1;	// The +1 handles the case where we search for the last transition

		while (high - low > 1)
		{
			int mid = (low + high) / 2;
			long elemKey = fTimestamps[mid - 1];
			if (timestamp == elemKey)
			{
				low = mid - 1;
				break;
			}
			else if (timestamp < elemKey)
				high = mid;
			else
				low = mid;
		}
		
		return new TransitionIterator(low > 0 ? low - 1 : 0);
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

	private class TransitionIterator implements Iterator<Transition>
	{
		TransitionIterator(int index)
		{
			fIndex = index;
			int offset = index * fWidth;
			fWordOffset = offset / 16;
			fBitOffset = (offset * 2) % 32;
			fCurrentWord = fValues[fWordOffset] >> fBitOffset;
			fTransition.setWidth(fWidth);
		}
	
		public boolean hasNext()
		{
			return fIndex < fTransitionCount;
		}
		
		public Transition next()
		{
			if (!hasNext())
				return null;
		
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
			fIndex++;
			
			return fTransition;
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
	private int fAllocSize;	// Used only while building
}
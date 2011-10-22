import java.util.*;

class SortedVector<T> extends Vector<T>
{
	public interface Keyed
	{
		public long getKey();
	}

	public SortedVector() 
	{
	}

	public SortedVector(int initialCapacity) 
	{
		super(initialCapacity);
	}

	public SortedVector(int initialCapacity, int capacityIncrement) 
	{
		super(initialCapacity, capacityIncrement);
	}

	void addSorted(long key, T value)
	{
		if (false)
		{
			/// @todo Optimized path, need to debug
			if (size() == 0)
				add(value);
			else
			{
				int index = lookupValue(key) + 1;
				insertElementAt(value, index);
			}
		}
		else
		{
			for (int i = 0; ; i++)
			{
				if (i == size()
					|| ((Keyed) elementAt(i)).getKey() > key)
				{
					insertElementAt(value, i);
					break;
				}
			}
		}
	}

	Iterator<T> find(long key)
	{
		return new SortedVectorIterator(this, lookupValue(key));
	}

	int lookupValue(long key)
	{
		// Binary search
		int low = 0;
		int high = size();

		while (high - low > 1)
		{
			int mid = (low + high) / 2;
			long elemKey = ((Keyed) elementAt(mid - 1)).getKey();
			if (key == elemKey)
				return mid - 1;
			else if (key < elemKey)
				high = mid;
			else
				low = mid;
		}

		if (low > 0)
			return low - 1;

		return 0;	// Before the first entry	
	}
	
	private class SortedVectorIterator<T> implements Iterator<T>
	{
		public SortedVectorIterator(SortedVector<T> vector, int index)
		{
			fVector = vector;
			fIndex = index;
		}
	
		public boolean hasNext()
		{
			return fIndex < fVector.size();
		}
		
		public T next()
		{
			T val = fVector.elementAt(fIndex);
			fIndex++;
			return val;
		}

		public void remove()
		{
		}
	
		SortedVector<T> fVector;
		int fIndex;
	}
}

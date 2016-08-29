//
// Copyright 2011-2012 Jeff Bush
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

import java.util.*;

///
/// ArrayList that allows sorted inserts and binary search lookup.
///

class SortedArrayList<T extends SortedArrayList.Keyed> extends ArrayList<T> {
    public interface Keyed {
        public long getKey();
    }

    public SortedArrayList() {}

    public boolean add(T value) {
        long key = ((Keyed) value).getKey();
        for (int i = 0; ; i++) {
            if (i == size()
                    || ((Keyed) get(i)).getKey() > key) {
                add(i, value);
                break;
            }
        }

        return true;
    }

    public Iterator<T> find(long key) {
        return new SortedArrayListIterator(this, findIndex(key));
    }

    /// @param key key value to search for
    /// @returns index into array of element that matches key. If this
    ///   element isn't matched exactly, return the element before this one.
    ///   If the key is before the first element, return 0.
    /// @todo Investigate using java.util.Arrays.binarySearch instead of
    ///    hand rolled implementation here.
    public int findIndex(long key) {
        // Binary search
        int low = 0;
        int high = size();

        while (low < high) {
            int mid = (low + high) / 2;
            long elemKey = ((Keyed) get(mid)).getKey();
            if (key == elemKey)
                return mid;
            else if (key < elemKey)
                high = mid;
            else
                low = mid + 1;
        }

        if (low > 0)
            return low - 1;

        return 0;    // Before the first entry
    }

    private class SortedArrayListIterator implements Iterator<T> {
        public SortedArrayListIterator(SortedArrayList<T> vector, int index) {
            fVector = vector;
            fIndex = index;
        }

        @Override
        public boolean hasNext() {
            return fIndex < fVector.size();
        }

        @Override
        public T next() {
            if (!hasNext())
                throw new NoSuchElementException();

            T val = fVector.get(fIndex);
            fIndex++;
            return val;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private SortedArrayList<T> fVector;
        private int fIndex;
    }
}

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

package waveview;

import java.util.*;

///
/// ArrayList that allows sorted inserts and binary search lookup.
///

public class SortedArrayList<T extends SortedArrayList.Keyed> extends ArrayList<T> {
    public interface Keyed {
        long getKey();
    }

    @Override
    public boolean add(T value) {
        long key = ((Keyed) value).getKey();
        for (int i = 0; ; i++) {
            if (i == size() || ((Keyed) get(i)).getKey() > key) {
                add(i, value);
                break;
            }
        }

        return true;
    }

    public Iterator<T> find(long key) {
        return new SortedArrayListIterator(findIndex(key));
    }

    /// @param key key value to search for
    /// @returns index into array of element that matches key. If an
    ///   element isn't at this timestamp, return the element before the
    ///   timestamp. If the key is before the first element, return 0.
    public int findIndex(long key) {
        // Binary search
        int low = 0;            // Lowest possible index
        int high = size() - 1;  // Highest possible index

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midKey = ((Keyed) get(mid)).getKey();
            if (key < midKey)
                high = mid - 1;
            else if (key > midKey)
                low = mid + 1;
            else
                return mid;
        }

        // No exact match. Low is equal to the index the transition would be
        // at if it existed. We want to return the transition before the
        // timestamp. If low == 0, this is before the first transition:
        // return 0.
        if (low == 0)
            return 0;

        return low - 1;
    }

    private class SortedArrayListIterator implements Iterator<T> {
        SortedArrayListIterator(int index) {
            fIndex = index;
        }

        @Override
        public boolean hasNext() {
            return fIndex < SortedArrayList.this.size();
        }

        @Override
        public T next() {
            if (!hasNext())
                throw new NoSuchElementException();

            T val = SortedArrayList.this.get(fIndex);
            fIndex++;
            return val;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private int fIndex;
    }
}

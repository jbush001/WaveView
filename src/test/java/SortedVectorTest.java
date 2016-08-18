//
// Copyright 2016 Jeff Bush
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

import static org.junit.Assert.*;
import org.junit.*;

public class SortedVectorTest
{
	class KeyedElement implements SortedVector.Keyed
	{
		public KeyedElement(long keyval)
		{
			fKeyValue = keyval;
		}

		public long getKey()
		{
			return fKeyValue;
		}

		long fKeyValue;
	}

    @Test public void testAddLookup1()
	{
		SortedVector<KeyedElement> vec = new SortedVector<KeyedElement>();

        // Note: odd number of elements
		vec.addSorted(100, new KeyedElement(100));
		vec.addSorted(110, new KeyedElement(110));
		vec.addSorted(115, new KeyedElement(115));
		vec.addSorted(116, new KeyedElement(116));
		vec.addSorted(117, new KeyedElement(117));

		assertEquals(vec.lookupValue(100), 0);
		assertEquals(vec.lookupValue(110), 1);
		assertEquals(vec.lookupValue(115), 2);
		assertEquals(vec.lookupValue(116), 3);

        // XXX broken
        // assertEquals(vec.lookupValue(117), 4);
    }

    // XXX test with even number of elements
    // XXX test saerching before first element (make sure it doesn't crash)
    // XXX test searching after last element
}

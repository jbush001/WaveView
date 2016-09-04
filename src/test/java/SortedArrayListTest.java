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

import waveapp.*;
import static org.junit.Assert.*;
import org.junit.*;
import java.util.*;

public class SortedArrayListTest {
    class KeyedElement implements SortedArrayList.Keyed {
        public KeyedElement(long keyval) {
            fKeyValue = keyval;
        }

        public long getKey() {
            return fKeyValue;
        }

        long fKeyValue;
    }

    @Test
    public void testAddLookup1() {
        SortedArrayList<KeyedElement> vec = new SortedArrayList<KeyedElement>();

        // Note: odd number of elements
        vec.add(new KeyedElement(100));
        vec.add(new KeyedElement(117));
        vec.add(new KeyedElement(110));
        vec.add(new KeyedElement(116));
        vec.add(new KeyedElement(115));

        assertEquals(0, vec.findIndex(10));
        assertEquals(0, vec.findIndex(100));
        assertEquals(1, vec.findIndex(110));
        assertEquals(1, vec.findIndex(112));
        assertEquals(2, vec.findIndex(115));
        assertEquals(3, vec.findIndex(116));
        assertEquals(4, vec.findIndex(117));
        assertEquals(4, vec.findIndex(120));
    }

    @Test
    public void testAddLookup2() {
        SortedArrayList<KeyedElement> vec = new SortedArrayList<KeyedElement>();

        // Note: even number of elements
        vec.add(new KeyedElement(100));
        vec.add(new KeyedElement(115));
        vec.add(new KeyedElement(110));
        vec.add(new KeyedElement(116));

        assertEquals(0, vec.findIndex(10));
        assertEquals(0, vec.findIndex(100));
        assertEquals(1, vec.findIndex(110));
        assertEquals(1, vec.findIndex(112));
        assertEquals(2, vec.findIndex(115));
        assertEquals(3, vec.findIndex(116));
        assertEquals(3, vec.findIndex(117));
        assertEquals(3, vec.findIndex(120));
    }

    @Test
    public void testIterator() {
        SortedArrayList<KeyedElement> vec = new SortedArrayList<KeyedElement>();
        vec.add(new KeyedElement(140));
        vec.add(new KeyedElement(100));
        vec.add(new KeyedElement(120));
        vec.add(new KeyedElement(130));
        vec.add(new KeyedElement(110));

        Iterator<KeyedElement> kei = vec.find(110);
        assertTrue(kei.hasNext());
        assertEquals(110, kei.next().getKey());
        assertTrue(kei.hasNext());
        assertEquals(120, kei.next().getKey());
        assertTrue(kei.hasNext());
        assertEquals(130, kei.next().getKey());
        assertTrue(kei.hasNext());
        assertEquals(140, kei.next().getKey());
        assertTrue(!kei.hasNext());
        try {
            kei.next();
            fail("didn't throw exception");
        } catch (NoSuchElementException exc) {
        }
    }
}

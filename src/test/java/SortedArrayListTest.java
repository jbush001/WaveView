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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.Test;
import waveview.SortedArrayList;

public class SortedArrayListTest {
    static class KeyedElement implements SortedArrayList.Keyed {
        long keyValue;

        public KeyedElement(long keyValue) {
            this.keyValue = keyValue;
        }

        @Override
        public long getKey() {
            return keyValue;
        }
    }

    @Test
    public void addLookupOddCount() {
        SortedArrayList<KeyedElement> vec = new SortedArrayList<>();

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
    public void addLookupEvenCount() {
        SortedArrayList<KeyedElement> vec = new SortedArrayList<>();

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
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void iterator() {
        SortedArrayList<KeyedElement> vec = new SortedArrayList<>();
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
        assertFalse(kei.hasNext());
        try {
            kei.next();
            fail("didn't throw exception");
        } catch (NoSuchElementException exc) {
            // Expected
        }
    }
}

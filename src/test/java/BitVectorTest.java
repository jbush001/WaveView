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

public class BitVectorTest
{
    @Test public void testParseHex()
    {
        BitVector bv = new BitVector("1234ABCD", 16);
        assertFalse(bv.isZ());
        assertFalse(bv.isX());
        assertEquals(32, bv.getWidth());
        assertEquals("1234ABCD", bv.toString(16));
    }

    @Test public void testParseBinary()
    {
        BitVector bv = new BitVector("11011101010010010010", 2);
        assertFalse(bv.isZ());
        assertFalse(bv.isX());
        assertEquals(20, bv.getWidth());
        assertEquals("DD492", bv.toString(16));
    }

    @Test public void testParseX()
    {
        BitVector bv = new BitVector("xxxxxxxxxxx", 2);
        assertEquals(11, bv.getWidth());
        assertFalse(bv.isZ());
        assertTrue(bv.isX());
    }

    @Test public void testParseZ()
    {
        BitVector bv = new BitVector("zzzzzzzz", 2);
        assertEquals(8, bv.getWidth());
        assertTrue(bv.isZ());
        assertTrue(bv.isX());   // XXX bug?
    }

    @Test public void testSetValue()
    {
        BitVector bv = new BitVector(8);
        bv.setValue(0, BitVector.VALUE_1);
        bv.setValue(1, BitVector.VALUE_0);
        bv.setValue(2, BitVector.VALUE_1);
        bv.setValue(3, BitVector.VALUE_1);
        bv.setValue(4, BitVector.VALUE_0);
        bv.setValue(5, BitVector.VALUE_1);
        bv.setValue(6, BitVector.VALUE_0);
        bv.setValue(7, BitVector.VALUE_1);
        assertFalse(bv.isZ());
        assertFalse(bv.isX());
        assertEquals(0xb5, bv.intValue());
    }

    @Test public void testGetValue()
    {
        BitVector bv = new BitVector("1x010z10", 2);
        assertEquals(BitVector.VALUE_1, bv.getValue(0));
        assertEquals(BitVector.VALUE_X, bv.getValue(1));
        assertEquals(BitVector.VALUE_0, bv.getValue(2));
        assertEquals(BitVector.VALUE_1, bv.getValue(3));
        assertEquals(BitVector.VALUE_0, bv.getValue(4));
        assertEquals(BitVector.VALUE_Z, bv.getValue(5));
        assertEquals(BitVector.VALUE_1, bv.getValue(6));
        assertEquals(BitVector.VALUE_0, bv.getValue(7));
    }

    @Test public void testCompare()
    {
        BitVector bv1 = new BitVector("1", 2);      // Shorter than others
        BitVector bv2 = new BitVector("01001", 2);  // Has leading zeros
        BitVector bv3 = new BitVector("10100", 2);
        BitVector bv4 = new BitVector("10101", 2);  // Same length as last, trailing 1

        assertEquals(0, bv1.compare(bv1));
        assertEquals(-1, bv1.compare(bv2));
        assertEquals(-1, bv1.compare(bv3));
        assertEquals(-1, bv1.compare(bv4));
        assertEquals(1, bv2.compare(bv1));
        assertEquals(0, bv2.compare(bv2));
        assertEquals(-1, bv2.compare(bv3));
        assertEquals(-1, bv2.compare(bv4));
        assertEquals(1, bv3.compare(bv1));
        assertEquals(1, bv3.compare(bv2));
        assertEquals(0, bv3.compare(bv3));
        assertEquals(-1, bv3.compare(bv4));
        assertEquals(1, bv3.compare(bv1));
        assertEquals(1, bv3.compare(bv2));
        assertEquals(0, bv3.compare(bv3));
        assertEquals(-1, bv3.compare(bv4));
    }
}

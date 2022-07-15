
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

import org.junit.Test;
import waveview.wavedata.BitValue;
import waveview.wavedata.BitVector;

public class BitVectorTest {
    @Test
    public void parseBinary() {
        BitVector bv = new BitVector("110111010100100100101100101001001010001001", 2);
        assertEquals(42, bv.getWidth());
        assertEquals("110111010100100100101100101001001010001001", bv.toString(2));
    }

    @Test
    public void parseOctal() {
        BitVector bv = new BitVector("145252366215757412216507426", 8);
        assertEquals(27 * 3, bv.getWidth());
        assertEquals("145252366215757412216507426", bv.toString(8));
    }

    @Test
    public void parseDecimal() {
        BitVector bv = new BitVector("3492343343482759676947735281634934371324", 10);
        assertEquals("3492343343482759676947735281634934371324", bv.toString(10));
    }

    @Test
    public void parseHex() {
        assertEquals(0, new BitVector("0", 16).intValue());
        assertEquals(1, new BitVector("1", 16).intValue());
        assertEquals(2, new BitVector("2", 16).intValue());
        assertEquals(12, new BitVector("c", 16).intValue());
        assertEquals(12, new BitVector("C", 16).intValue());
        assertEquals(16, new BitVector("10", 16).intValue());
        assertEquals(17, new BitVector("11", 16).intValue());
        assertEquals(432, new BitVector("1B0", 16).intValue());
        assertEquals(512, new BitVector("200", 16).intValue());
        assertEquals(513, new BitVector("201", 16).intValue());

        BitVector bv = new BitVector("1234567890abcdefABCDEF", 16);
        assertEquals(88, bv.getWidth());
        assertEquals("1234567890ABCDEFABCDEF", bv.toString(16));
    }

    @Test
    public void parseX() {
        BitVector bv = new BitVector("xxxxxxxxxxx", 2);
        assertEquals(11, bv.getWidth());
        assertEquals("xxxxxxxxxxx", bv.toString(2));

        bv = new BitVector("XXXXX", 2);
        assertEquals(5, bv.getWidth());
        assertEquals("xxxxx", bv.toString(2));

        bv = new BitVector("xxx", 16);
        assertEquals(12, bv.getWidth());
        assertEquals("XXX", bv.toString(16));

        bv = new BitVector("XXX", 16);
        assertEquals(12, bv.getWidth());
        assertEquals("XXX", bv.toString(16));
    }

    @Test
    public void parseZ() {
        BitVector bv = new BitVector("zzzzzzzz", 2);
        assertEquals(8, bv.getWidth());
        assertEquals("zzzzzzzz", bv.toString(2));

        bv = new BitVector("ZZZZZZZZ", 2);
        assertEquals(8, bv.getWidth());
        assertEquals("zzzzzzzz", bv.toString(2));

        bv = new BitVector("zz", 16);
        assertEquals(8, bv.getWidth());
        assertEquals("ZZ", bv.toString(16));

        bv = new BitVector("ZZ", 16);
        assertEquals(8, bv.getWidth());
        assertEquals("ZZ", bv.toString(16));

        bv = new BitVector("zzzzzzzzzzzzzzzzz", 16);
        assertEquals(68, bv.getWidth());
        assertEquals("ZZZZZZZZZZZZZZZZZ", bv.toString(16));
    }

    @Test
    public void isZX() {
        BitVector bv = new BitVector("zzzzzzzz", 2);
        assertTrue(bv.isZ());
        assertTrue(bv.isX());

        // This is larger than 64 bits (68), and has a partial word
        // at the beginning.
        bv = new BitVector("zzzzzzzzzzzzzzzzz", 16);
        assertTrue(bv.isZ());
        assertTrue(bv.isX());

        // Larger than 64 bits and top word is not Z
        bv = new BitVector("0zzzzzzzzzzzzzzzz", 16);
        assertFalse(bv.isZ());
        assertTrue(bv.isX());

        // Larger than 64 bits and bottom word is not all Zs
        bv = new BitVector("0zzzzzzzzzzzzzzxz", 16);
        assertFalse(bv.isZ());
        assertTrue(bv.isX());

        // Larger than 64 bits, and has a partial word
        // that is X at the beginning.
        bv = new BitVector("x0000000000000000", 16);
        assertFalse(bv.isZ());
        assertTrue(bv.isX());
        assertEquals("X0000000000000000", bv.toString(16));

        // Has an X
        bv = new BitVector("100000x0", 2);
        assertFalse(bv.isZ());
        assertTrue(bv.isX());

        // No Xs or Zs, smaller than 64 bits
        bv = new BitVector("11111111", 2);
        assertFalse(bv.isZ());
        assertFalse(bv.isX());

        // No Xs or Zs, larger than 64 bits
        bv = new BitVector("0000000000000000", 16);
        assertFalse(bv.isZ());
        assertFalse(bv.isX());
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void binaryNumberFormatException() {
        // Digits other than 0/1 in binary
        try {
            new BitVector("0110102", 2);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Expected
        }

        try {
            new BitVector("011010A", 2);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Expected
        }

        try {
            new BitVector("011010a", 2);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Expected
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void octalNumberFormatException() {
        try {
            new BitVector("12348", 2);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Expected
        }

        try {
            new BitVector("123A", 2);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Expected
        }

        try {
            new BitVector("123a", 2);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Expected
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void decimalNumberFormatException() {
        // Hex digits
        try {
            new BitVector("1234a", 10);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Expected
        }

        // z not legal in decimal format
        try {
            new BitVector("z", 10);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Expected
        }

        // x not legal in decimal format
        try {
            new BitVector("x", 10);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Expected
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void hexNumberFormatException() {
        //
        // Invalid hex digits (I picked the invalid characters ASCII codes
        // to hit various conditions in the chain of if statements in
        // parseHex).
        //
        try {
            // / is < '0'
            new BitVector("ABCDEFG/", 16);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Expected
        }

        try {
            // : is > '9' and < 'A'
            new BitVector("ABCDEFG:", 16);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Expected
        }

        try {
            // 'G' is > 'F' and < 'a'
            new BitVector("ABCDEFG", 16);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Expected
        }

        try {
            // '~' is > 'f'
            new BitVector("ABCDEFG~", 16);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Expected
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void badRadix() {
        try {
            new BitVector("1", 12);
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Expected
        }
    }

    @Test
    public void setBit() {
        BitVector bv = new BitVector(8);
        bv.setBit(7, BitValue.ONE);
        bv.setBit(6, BitValue.ZERO);
        bv.setBit(5, BitValue.ONE);
        bv.setBit(4, BitValue.Z);
        bv.setBit(3, BitValue.ZERO);
        bv.setBit(2, BitValue.ONE);
        bv.setBit(1, BitValue.X);
        bv.setBit(0, BitValue.ONE);
        assertEquals("101z01x1", bv.toString(2));
    }

    @Test
    public void getBit() {
        BitVector bv = new BitVector("1x010z10", 2);
        assertEquals(BitValue.ONE, bv.getBit(7));
        assertEquals(BitValue.X, bv.getBit(6));
        assertEquals(BitValue.ZERO, bv.getBit(5));
        assertEquals(BitValue.ONE, bv.getBit(4));
        assertEquals(BitValue.ZERO, bv.getBit(3));
        assertEquals(BitValue.Z, bv.getBit(2));
        assertEquals(BitValue.ONE, bv.getBit(1));
        assertEquals(BitValue.ZERO, bv.getBit(0));
    }

    @Test
    public void compareWider() {
        BitVector bv1 = new BitVector("8000000000000000", 16);
        BitVector bv2 = new BitVector("10000000000000000", 16);
        BitVector bv3 = new BitVector("00000000000000000", 16);

        assertEquals(-1, bv1.compare(bv2)); // second is wider, non-zero
        assertEquals(1, bv1.compare(bv3));  // second is wider, zero
        assertEquals(1, bv2.compare(bv1)); // first is wider, non-zero
        assertEquals(-1, bv3.compare(bv1));  // first is wider, zero
    }

    // Both of the longs in the internal array are positive
    @Test
    public void compareBothPositive() {
        BitVector bv1 = new BitVector("111", 2);
        BitVector bv2 = new BitVector("11", 2);

        assertEquals(1, bv1.compare(bv2));
        assertEquals(-1, bv2.compare(bv1));
    }

    // One of the longs in the internal array is negative
    @Test
    public void compareOneNegative() {
        BitVector bv1 = new BitVector("8000000000000000", 16);
        BitVector bv2 = new BitVector("7fffffffffffffff", 16);

        assertEquals(1, bv1.compare(bv2));
        assertEquals(-1, bv2.compare(bv1));
    }

    // Both of the longs in the internal array are negative
    @Test
    public void compareBothNeg() {
        BitVector bv1 = new BitVector("8000000000000000", 16);
        BitVector bv2 = new BitVector("8000000000000001", 16);

        assertEquals(-1, bv1.compare(bv2));
        assertEquals(1, bv2.compare(bv1));
    }

    // Ensure x and z values are ignored
    @Test
    public void compareXz() {
        BitVector bv1 = new BitVector("x0z10", 2);
        BitVector bv2 = new BitVector("10110", 2);
        BitVector bv3 = new BitVector("00010", 2);
        BitVector bv4 = new BitVector("01010", 2);
        assertEquals(0, bv1.compare(bv2));
        assertEquals(0, bv2.compare(bv1));
        assertEquals(0, bv1.compare(bv3));
        assertEquals(0, bv3.compare(bv1));
        assertEquals(-1, bv1.compare(bv4));
        assertEquals(1, bv4.compare(bv1));
    }

    @Test
    public void convertToString() {
        BitVector bv1 = new BitVector("1010001001", 2); // Length is not multiple of 4
        BitVector bv2 = new BitVector("1z01xxxx11110000", 2);

        assertEquals("1010001001", bv1.toString());
        assertEquals("1z01xxxx11110000", bv2.toString());
        assertEquals("1010001001", bv1.toString(2));
        assertEquals("1z01xxxx11110000", bv2.toString(2));
        assertEquals("1211", bv1.toString(8));
        assertEquals("1XXX60", bv2.toString(8));
        assertEquals("649", bv1.toString(10));
        assertEquals("289", bv1.toString(16));
        assertEquals("XXF0", bv2.toString(16));

        try {
            bv1.toString(12);
            fail("did not throw exception");
        } catch (NumberFormatException exc) {
            // Expected
            assertEquals("bad radix", exc.getMessage());
        }
    }

    // Edge case: when a value who's width is not a multiple of
    // the octal digit bit width (3) is printed, the first
    // partial word is printed. If this is at a word boundary, ensure
    // there isn't an array out-of-bounds exception.
    @Test
    public void octalSpillover() {
        BitVector bv = new BitVector("ffffffffffffffff", 16);
        assertEquals("1777777777777777777777", bv.toString(8));
    }

    @Test
    public void copy() {
        final String SRC_VALUE = "010001001";

        BitVector bv1 = new BitVector(SRC_VALUE, 2);

        // Copy constructor
        BitVector bv2 = new BitVector(bv1);
        assertEquals(SRC_VALUE, bv2.toString());

        // Assign with same width
        BitVector bv3 = new BitVector("000000000", 2);
        bv3.assign(bv1);
        assertEquals(SRC_VALUE, bv3.toString());

        // Assign with different width
        BitVector bv4 = new BitVector("101", 2);
        bv4.assign(bv1);
        assertEquals(SRC_VALUE, bv4.toString());

        // Assign to empty bitvector
        BitVector bv5 = new BitVector();
        bv5.assign(bv1);
        assertEquals(SRC_VALUE, bv5.toString());

        // Assign from empty bitvector
        BitVector bv6 = new BitVector();
        BitVector bv7 = new BitVector(SRC_VALUE, 2);
        bv7.assign(bv6);
        assertEquals("0", bv7.toString());
    }

    @Test
    public void slice() {
        BitVector bv1 = new BitVector("010001011101", 2);
        assertEquals("011", bv1.slice(3, 5).toString());
        assertEquals("101", bv1.slice(4, 6).toString());
        assertEquals("0010", bv1.slice(5, 8).toString());

        BitVector vec128 = new BitVector("B79B7B6A8AFE8C54ECEF9F1CE6F9B6FA", 16);
        BitVector vec192 = new BitVector("AA70E91A5517D2BAC8AEB073834AC62DDDD4DEC8EAA6332F", 16);

        // Crosses 64 bit boundary, result < 64 bits
        assertEquals("C54E", vec128.slice(60, 75).toString(16));

        // Crosses 64 bit boundary, result = 64 bits
        assertEquals("79B7B6A8AFE8C54E", vec128.slice(60, 123).toString(16));

        // Crosses 64 bit boundary, result > 64 bits
        assertEquals("C8AEB073834AC62DD", vec192.slice(60, 127).toString(16));

        // 64 bit aligned, result < 64 bits
        assertEquals("8C54", vec128.slice(64, 79).toString(16));
    }

    @Test
    public void negativeLowSliceIndex() {
        BitVector bv = new BitVector("11111111", 2);
        try {
            bv.slice(-1, 5);
            fail("Did not throw exception");
        } catch (IllegalArgumentException exc) {
            // Expected
            assertEquals("invalid bit slice range -1:5", exc.getMessage());
        }
    }

    @Test
    public void highSliceIndexTooBig() {
        BitVector bv = new BitVector("11111111", 2);
        try {
            bv.slice(0, 8);
            fail("Did not throw exception");
        } catch (IllegalArgumentException exc) {
            // Expected
            assertEquals("invalid bit slice range 0:8", exc.getMessage());
        }
    }

    @Test
    public void lowHighSliceIndexSwapped() {
        BitVector bv = new BitVector("11111111", 2);
        try {
            bv.slice(5, 3);
            fail("Did not throw exception");
        } catch (IllegalArgumentException exc) {
            // Expected
            assertEquals("invalid bit slice range 5:3", exc.getMessage());
        }
    }
}

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

public class SearchTest {
    /// Utility function to create a sample trace with a clock signal
    TraceDataModel makeSingleBitModel() {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int id1 = builder.newNet("clk", -1, 1);
        builder.exitScope();
        builder.appendTransition(id1, 5, new BitVector("0", 2));
        builder.appendTransition(id1, 10, new BitVector("1", 2));
        builder.appendTransition(id1, 15, new BitVector("0", 2));
        builder.appendTransition(id1, 20, new BitVector("1", 2));
        builder.loadFinished();

        return traceDataModel;
    }

    TraceDataModel makeFourBitModel() {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterScope("m");
        builder.newNet("a", -1, 1);
        builder.newNet("b", -1, 1);
        builder.newNet("c", -1, 1);
        builder.newNet("d", -1, 1);
        builder.exitScope();

        BitVector bv = new BitVector(1);
        for (int t = 0; t < 16; t++)
        {
            for (int bit = 0; bit < 4; bit++){
                bv.setBit(0, (t >> (3 - bit)) & 1);
                builder.appendTransition(bit, t, bv);
                System.out.println("builder.appendTransition(" + bit + ", " + t + ", " + bv + ")");
            }
        }

        builder.loadFinished();

        return traceDataModel;
    }

    /// Test various forms of whitespace (and lack thereof)
    @Test
    public void testWhitespace() throws Exception {
        Search search = new Search(makeSingleBitModel(), "\r  \n \t  mod1.clk          =  1\n");
        assertEquals(10, search.getNextMatch(4));

        search = new Search(makeSingleBitModel(), "mod1.clk=1");
        assertEquals(10, search.getNextMatch(4));
    }

    /// Test identifier characters
    @Test
    public void testIdentifier() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int id1 = builder.newNet("_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", -1, 1);
        builder.exitScope();
        builder.appendTransition(id1, 5, new BitVector("0", 2));
        builder.appendTransition(id1, 10, new BitVector("1", 2));
        builder.loadFinished();

        Search search = new Search(traceDataModel, "mod1._abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 = 1\n");
        assertEquals(10, search.getNextMatch(4));
    }

    @Test
    public void testSimpleSearch() throws Exception {
        // clk is high 10-14, 20-
        Search search = new Search(makeSingleBitModel(), "mod1.clk");
        assertEquals(10, search.getNextMatch(4));
        assertEquals(10, search.getNextMatch(9));
        assertEquals(20, search.getNextMatch(10));
        assertEquals(20, search.getNextMatch(15));
        assertEquals(-1, search.getNextMatch(20));
        assertEquals(-1, search.getNextMatch(30));

        assertEquals(14, search.getPreviousMatch(21));
        assertEquals(14, search.getPreviousMatch(20));
        assertEquals(14, search.getPreviousMatch(15));
        assertEquals(-1, search.getPreviousMatch(10));
        assertEquals(-1, search.getPreviousMatch(5));
        assertEquals(-1, search.getPreviousMatch(4));
    }

    @Test
    public void testLiteralBases() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int id1 = builder.newNet("value", -1, 4);
        builder.exitScope();
        builder.appendTransition(id1, 17, new BitVector("5", 10));
        builder.appendTransition(id1, 23, new BitVector("10", 10));
        builder.loadFinished();

        Search search = new Search(traceDataModel, "mod1.value = 10");
        assertEquals(23, search.getNextMatch(0));

        search = new Search(traceDataModel, "mod1.value = 'd10");
        assertEquals(23, search.getNextMatch(0));

        search = new Search(traceDataModel, "mod1.value = 'ha");
        assertEquals(23, search.getNextMatch(0));

        search = new Search(traceDataModel, "mod1.value = 'hA");
        assertEquals(23, search.getNextMatch(0));

        search = new Search(traceDataModel, "mod1.value = 'b1010");
        assertEquals(23, search.getNextMatch(0));
    }

    /// @bug This doesn't match correctly yet: Xes are ignored for
    /// matching.
    /// Verify it at least parses the search.
    @Test
    public void testMatchXZ() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.enterScope("mod1");
        int id1 = builder.newNet("value", -1, 4);
        builder.exitScope();
        builder.appendTransition(id1, 17, new BitVector("1111", 2));
        builder.appendTransition(id1, 23, new BitVector("xxxx", 2));
        builder.loadFinished();

        // Try upper and lower case versions of X and Z for hex and decimal
        // bases. Ensure this doesn't throw an exception.
        new Search(traceDataModel, "mod1.value = 'bxxxx");
        new Search(traceDataModel, "mod1.value = 'bXXXX");
        new Search(traceDataModel, "mod1.value = 'hx");
        new Search(traceDataModel, "mod1.value = 'hX");
        new Search(traceDataModel, "mod1.value = 'bzzzz");
        new Search(traceDataModel, "mod1.value = 'bZZZZ");
        new Search(traceDataModel, "mod1.value = 'hz");
        new Search(traceDataModel, "mod1.value = 'hZ");

        // X and Z with decimal values should fail
        try {
            new Search(traceDataModel, "mod1.value = 'dx");
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Should throw this
        }

        try {
            new Search(traceDataModel, "mod1.value = 'dX");
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Should throw this
        }

        try {
            new Search(traceDataModel, "mod1.value = 'dz");
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Should throw this
        }

        try {
            new Search(traceDataModel, "mod1.value = 'dZ");
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Should throw this
        }
    }

    @Test
    public void testUnknownNet() {
        try {
            new Search(makeSingleBitModel(), "mod1.stall_pipeline = 2");
            fail("Did not throw exception");
        } catch (Search.ParseException exc) {
            assertEquals("unknown net \"mod1.stall_pipeline\"", exc.getMessage());
            assertEquals(0, exc.getStartOffset());
            assertEquals(18, exc.getEndOffset());
        }
    }

    @Test
    public void testStrayIdentifier() {
        try {
            new Search(makeSingleBitModel(), "mod1.clk = 'h2 foo");
            fail("Did not throw exception");
        } catch (Search.ParseException exc) {
            assertEquals("unexpected value", exc.getMessage());
            assertEquals(15, exc.getStartOffset());
            assertEquals(17, exc.getEndOffset());
        }
    }

    @Test
    public void testMissingCompareValue() {
        try {
            new Search(makeSingleBitModel(), "mod1.clk = ");
            fail("Did not throw exception");
        } catch (Search.ParseException exc) {
            assertEquals("unexpected end of string", exc.getMessage());
            assertEquals(10, exc.getStartOffset());
            assertEquals(10, exc.getEndOffset());
        }
    }

    @Test
    public void testMissingLiteral() {
        try {
            new Search(makeSingleBitModel(), "mod1.clk = >");
            fail("Did not throw exception");
        } catch (Search.ParseException exc) {
            assertEquals("unexpected value", exc.getMessage());
            assertEquals(11, exc.getStartOffset());
            assertEquals(11, exc.getEndOffset());
        }
    }

    @Test
    public void testUnknownLiteralType() {
        try {
            new Search(makeSingleBitModel(), "mod1.clk = 'q3z");
            fail("Did not throw exception");
        } catch (Search.ParseException exc) {
            assertEquals("unknown type q", exc.getMessage());
            assertEquals(11, exc.getStartOffset());
            assertEquals(11, exc.getEndOffset());
        }
    }

    @Test
    public void testMissingParen() {
        try {
            new Search(makeSingleBitModel(), "(mod1.clk = 'h3 foo");
            fail("Did not throw exception");
        } catch (Search.ParseException exc) {
            assertEquals("unexpected value", exc.getMessage());
            assertEquals(16, exc.getStartOffset());
            assertEquals(18, exc.getEndOffset());
        }
    }

    @Test
    public void testMissingIdentifier() {
        try {
            new Search(makeSingleBitModel(), "> 'h12");
            fail("Did not throw exception");
        } catch (Search.ParseException exc) {
            assertEquals("unexpected value", exc.getMessage());
            assertEquals(0, exc.getStartOffset());
            assertEquals(0, exc.getEndOffset());
        }
    }

    /// Basic test for 'and' search functionailty
    /// This implicitly verifies search hinting by starting at different
    /// transition points
    @Test
    public void testAnd() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int id1 = builder.newNet("a", -1, 1);
        int id2 = builder.newNet("b", -1, 1);
        builder.exitScope();

        // Fast toggle (a).
        // True 5-9, 15-19, 25-29, 35-40, 45-
        builder.appendTransition(id1, 0, new BitVector("0", 2));
        builder.appendTransition(id1, 5, new BitVector("1", 2));
        builder.appendTransition(id1, 10, new BitVector("0", 2));
        builder.appendTransition(id1, 15, new BitVector("1", 2));
        builder.appendTransition(id1, 20, new BitVector("0", 2));
        builder.appendTransition(id1, 25, new BitVector("1", 2));
        builder.appendTransition(id1, 30, new BitVector("0", 2));
        builder.appendTransition(id1, 35, new BitVector("1", 2));
        builder.appendTransition(id1, 40, new BitVector("0", 2));
        builder.appendTransition(id1, 45, new BitVector("1", 2));

        // Slow toggle (b)
        // True 15-29, 45-
        builder.appendTransition(id2, 0, new BitVector("0", 2));
        builder.appendTransition(id2, 15, new BitVector("1", 2));
        builder.appendTransition(id2, 30, new BitVector("0", 2));
        builder.appendTransition(id2, 45, new BitVector("1", 2));

        builder.loadFinished();

        System.out.println("parse search");
        Search search = new Search(traceDataModel, "mod1.a and mod1.b");
        System.out.println("finished parsing");

        // Expression is true:
        // 15-19, 25-29, 45-

        // Test forward
        assertEquals(15, search.getNextMatch(0));  // false & false = false
        assertEquals(15, search.getNextMatch(5));  // true & false = false
        assertEquals(15, search.getNextMatch(10)); // false & false = false
        assertEquals(25, search.getNextMatch(15)); // true & true = true
        assertEquals(25, search.getNextMatch(20)); // false & true = false
        assertEquals(45, search.getNextMatch(25)); // true & true = true
        assertEquals(45, search.getNextMatch(30)); // false & false = false
        assertEquals(45, search.getNextMatch(35)); // true & false = false
        assertEquals(45, search.getNextMatch(40)); // false & false = false
        assertEquals(-1, search.getNextMatch(45)); // true & true  = true
        assertEquals(-1, search.getNextMatch(50)); // false & true = false

        // Test backward
        assertEquals(-1, search.getPreviousMatch(0));  // false & false = false
        assertEquals(-1, search.getPreviousMatch(5));  // true & false = false
        assertEquals(-1, search.getPreviousMatch(10)); // false & false = false
        assertEquals(-1, search.getPreviousMatch(15)); // true & true = true
        assertEquals(19, search.getPreviousMatch(20)); // false & true = false
        assertEquals(19, search.getPreviousMatch(25)); // true & true = true
        assertEquals(29, search.getPreviousMatch(30)); // false & false = false
        assertEquals(29, search.getPreviousMatch(35)); // true & false = false
        assertEquals(29, search.getPreviousMatch(40)); // false & false = false
        assertEquals(29, search.getPreviousMatch(45)); // true & true  = true
        assertEquals(29, search.getPreviousMatch(50)); // false & true = false
    }

    /// Basic test for 'or' search functionailty
    /// This implicitly verifies search hinting by starting at different
    /// transition points
    @Test
    public void testOr() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int id1 = builder.newNet("a", -1, 1);
        int id2 = builder.newNet("b", -1, 1);
        builder.exitScope();

        // Fast toggle (a).
        // True 5-9, 15-19, 25-29, 35-40, 45-
        builder.appendTransition(id1, 0, new BitVector("0", 2));
        builder.appendTransition(id1, 5, new BitVector("1", 2));
        builder.appendTransition(id1, 10, new BitVector("0", 2));
        builder.appendTransition(id1, 15, new BitVector("1", 2));
        builder.appendTransition(id1, 20, new BitVector("0", 2));
        builder.appendTransition(id1, 25, new BitVector("1", 2));
        builder.appendTransition(id1, 30, new BitVector("0", 2));
        builder.appendTransition(id1, 35, new BitVector("1", 2));
        builder.appendTransition(id1, 40, new BitVector("0", 2));
        builder.appendTransition(id1, 45, new BitVector("1", 2));

        // Slow toggle (b)
        // True 15-29, 45-
        builder.appendTransition(id2, 0, new BitVector("0", 2));
        builder.appendTransition(id2, 15, new BitVector("1", 2));
        builder.appendTransition(id2, 30, new BitVector("0", 2));
        builder.appendTransition(id2, 45, new BitVector("1", 2));

        builder.loadFinished();

        Search search = new Search(traceDataModel, "mod1.a or mod1.b");
        System.out.println("search is " + search.toString());

        // Expression is true:
        // 5-9, 15-29, 35-39, 45-

        // Test forward
        assertEquals(5, search.getNextMatch(0));  // false | false = false
        assertEquals(15, search.getNextMatch(5));  // true | false = true
        assertEquals(15, search.getNextMatch(10)); // false | false = false
        assertEquals(35, search.getNextMatch(15)); // true | true = true
        assertEquals(35, search.getNextMatch(20)); // false | true = true
        assertEquals(35, search.getNextMatch(25)); // true | true = true
        assertEquals(35, search.getNextMatch(30)); // false | false = false
        assertEquals(45, search.getNextMatch(35)); // true | false = true
        assertEquals(45, search.getNextMatch(40)); // false | false = false
        assertEquals(-1, search.getNextMatch(45)); // true | true  = true
        assertEquals(-1, search.getNextMatch(50)); // false | true = true

        // Test backward
        assertEquals(-1, search.getPreviousMatch(0));  // false | false = false
        assertEquals(-1, search.getPreviousMatch(5));  // true | false = true
        assertEquals(9, search.getPreviousMatch(10));  // false | false = false
        assertEquals(9, search.getPreviousMatch(15));  // true | true = true
        assertEquals(9, search.getPreviousMatch(20)); // false | true = true
        assertEquals(9, search.getPreviousMatch(25)); // true | true = true
        assertEquals(29, search.getPreviousMatch(30)); // false | false = false
        assertEquals(29, search.getPreviousMatch(35)); // true | false = true
        assertEquals(39, search.getPreviousMatch(40)); // false | false = false
        assertEquals(39, search.getPreviousMatch(45)); // true | true  = true
        assertEquals(39, search.getPreviousMatch(50)); // false | true = true
    }

    @Test
    public void testComparisons() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int id1 = builder.newNet("value", -1, 4);
        builder.exitScope();

        builder.appendTransition(id1, 1, new BitVector("0", 16));
        builder.appendTransition(id1, 2, new BitVector("2", 16));
        builder.appendTransition(id1, 3, new BitVector("3", 16));
        builder.appendTransition(id1, 4, new BitVector("4", 16));
        builder.appendTransition(id1, 5, new BitVector("5", 16));
        builder.appendTransition(id1, 6, new BitVector("6", 16));
        builder.appendTransition(id1, 7, new BitVector("7", 16));
        builder.appendTransition(id1, 8, new BitVector("8", 16));
        builder.appendTransition(id1, 9, new BitVector("9", 16));
        builder.loadFinished();

        Search search = new Search(traceDataModel, "mod1.value > 'h5");
        assertEquals(6, search.getNextMatch(0));

        search = new Search(traceDataModel, "'h5 < mod1.value");
        assertEquals(6, search.getNextMatch(0));

        search = new Search(traceDataModel, "mod1.value >= 'h5");
        assertEquals(5, search.getNextMatch(0));

        search = new Search(traceDataModel, "'h5 <= mod1.value");
        assertEquals(5, search.getNextMatch(0));

        search = new Search(traceDataModel, "mod1.value < 'h5");
        assertEquals(4, search.getPreviousMatch(10));

        search = new Search(traceDataModel, "'h5 > mod1.value");
        assertEquals(4, search.getPreviousMatch(10));

        search = new Search(traceDataModel, "mod1.value <= 'h5");
        assertEquals(5, search.getPreviousMatch(10));

        search = new Search(traceDataModel, "'h5 >= mod1.value");
        assertEquals(5, search.getPreviousMatch(10));

        search = new Search(traceDataModel, "mod1.value = 'h5");
        assertEquals(5, search.getNextMatch(0));

        search = new Search(traceDataModel, "'h5 = mod1.value");
        assertEquals(5, search.getNextMatch(0));

        search = new Search(traceDataModel, "mod1.value <> 'h5");
        assertEquals(6, search.getNextMatch(4));

        search = new Search(traceDataModel, "'h5 <> mod1.value");
        assertEquals(6, search.getNextMatch(4));

        search = new Search(traceDataModel, "mod1.value >< 'h5");
        assertEquals(6, search.getNextMatch(4));

        // This will never find the next, because it is never false.
        // Ensure it doesn't go into an infinite loop
        search = new Search(traceDataModel, "mod1.value = mod1.value");
        assertEquals(-1, search.getNextMatch(4));

        // Likewise, this will never find the next, because it is never
        // true
        search = new Search(traceDataModel, "mod1.value <> mod1.value");
        assertEquals(-1, search.getNextMatch(4));
    }

    @Test
    public void testPrecedence() throws Exception {
        TraceDataModel traceDataModel = makeFourBitModel();
        Search search;

        search = new Search(traceDataModel, "m.a and m.b and m.c and m.d");
        assertFalse(search.matches(0));
        assertFalse(search.matches(1));
        assertFalse(search.matches(2));
        assertFalse(search.matches(3));
        assertFalse(search.matches(4));
        assertFalse(search.matches(5));
        assertFalse(search.matches(6));
        assertFalse(search.matches(7));
        assertFalse(search.matches(8));
        assertFalse(search.matches(9));
        assertFalse(search.matches(10));
        assertFalse(search.matches(11));
        assertFalse(search.matches(12));
        assertFalse(search.matches(13));
        assertFalse(search.matches(14));
        assertTrue(search.matches(15));

        search = new Search(traceDataModel, "m.a and m.b and m.c or m.d");
        assertFalse(search.matches(0));
        assertTrue(search.matches(1));
        assertFalse(search.matches(2));
        assertTrue(search.matches(3));
        assertFalse(search.matches(4));
        assertTrue(search.matches(5));
        assertFalse(search.matches(6));
        assertTrue(search.matches(7));
        assertFalse(search.matches(8));
        assertTrue(search.matches(9));
        assertFalse(search.matches(10));
        assertTrue(search.matches(11));
        assertFalse(search.matches(12));
        assertTrue(search.matches(13));
        assertTrue(search.matches(14));
        assertTrue(search.matches(15));

        search = new Search(traceDataModel, "m.a and m.b or m.c and m.d");
        assertFalse(search.matches(0));
        assertFalse(search.matches(1));
        assertFalse(search.matches(2));
        assertTrue(search.matches(3));
        assertFalse(search.matches(4));
        assertFalse(search.matches(5));
        assertFalse(search.matches(6));
        assertTrue(search.matches(7));
        assertFalse(search.matches(8));
        assertFalse(search.matches(9));
        assertFalse(search.matches(10));
        assertTrue(search.matches(11));
        assertTrue(search.matches(12));
        assertTrue(search.matches(13));
        assertTrue(search.matches(14));
        assertTrue(search.matches(15));

        search = new Search(traceDataModel, "m.a and m.b or m.c or m.d");
        assertFalse(search.matches(0));
        assertTrue(search.matches(1));
        assertTrue(search.matches(2));
        assertTrue(search.matches(3));
        assertFalse(search.matches(4));
        assertTrue(search.matches(5));
        assertTrue(search.matches(6));
        assertTrue(search.matches(7));
        assertFalse(search.matches(8));
        assertTrue(search.matches(9));
        assertTrue(search.matches(10));
        assertTrue(search.matches(11));
        assertTrue(search.matches(12));
        assertTrue(search.matches(13));
        assertTrue(search.matches(14));
        assertTrue(search.matches(15));

        search = new Search(traceDataModel, "m.a or m.b and m.c and m.d");
        assertFalse(search.matches(0));
        assertFalse(search.matches(1));
        assertFalse(search.matches(2));
        assertFalse(search.matches(3));
        assertFalse(search.matches(4));
        assertFalse(search.matches(5));
        assertFalse(search.matches(6));
        assertTrue(search.matches(7));
        assertTrue(search.matches(8));
        assertTrue(search.matches(9));
        assertTrue(search.matches(10));
        assertTrue(search.matches(11));
        assertTrue(search.matches(12));
        assertTrue(search.matches(13));
        assertTrue(search.matches(14));
        assertTrue(search.matches(15));

        search = new Search(traceDataModel, "m.a or m.b and m.c or m.d");
        assertFalse(search.matches(0));
        assertTrue(search.matches(1));
        assertFalse(search.matches(2));
        assertTrue(search.matches(3));
        assertFalse(search.matches(4));
        assertTrue(search.matches(5));
        assertTrue(search.matches(6));
        assertTrue(search.matches(7));
        assertTrue(search.matches(8));
        assertTrue(search.matches(9));
        assertTrue(search.matches(10));
        assertTrue(search.matches(11));
        assertTrue(search.matches(12));
        assertTrue(search.matches(13));
        assertTrue(search.matches(14));
        assertTrue(search.matches(15));

        search = new Search(traceDataModel, "m.a or m.b or m.c and m.d");
        assertFalse(search.matches(0));
        assertFalse(search.matches(1));
        assertFalse(search.matches(2));
        assertTrue(search.matches(3));
        assertTrue(search.matches(4));
        assertTrue(search.matches(5));
        assertTrue(search.matches(6));
        assertTrue(search.matches(7));
        assertTrue(search.matches(8));
        assertTrue(search.matches(9));
        assertTrue(search.matches(10));
        assertTrue(search.matches(11));
        assertTrue(search.matches(12));
        assertTrue(search.matches(13));
        assertTrue(search.matches(14));
        assertTrue(search.matches(15));

        search = new Search(traceDataModel, "m.a or m.b or m.c or m.d");
        assertFalse(search.matches(0));
        assertTrue(search.matches(1));
        assertTrue(search.matches(2));
        assertTrue(search.matches(3));
        assertTrue(search.matches(4));
        assertTrue(search.matches(5));
        assertTrue(search.matches(6));
        assertTrue(search.matches(7));
        assertTrue(search.matches(8));
        assertTrue(search.matches(9));
        assertTrue(search.matches(10));
        assertTrue(search.matches(11));
        assertTrue(search.matches(12));
        assertTrue(search.matches(13));
        assertTrue(search.matches(14));
        assertTrue(search.matches(15));

        // Parentheses
        search = new Search(traceDataModel, "m.a and (m.b or m.c) and m.d");
        assertFalse(search.matches(0));
        assertFalse(search.matches(1));
        assertFalse(search.matches(2));
        assertFalse(search.matches(3));
        assertFalse(search.matches(4));
        assertFalse(search.matches(5));
        assertFalse(search.matches(6));
        assertFalse(search.matches(7));
        assertFalse(search.matches(8));
        assertFalse(search.matches(9));
        assertFalse(search.matches(10));
        assertTrue(search.matches(11));
        assertFalse(search.matches(12));
        assertTrue(search.matches(13));
        assertFalse(search.matches(14));
        assertTrue(search.matches(15));

        search = new Search(traceDataModel, "m.a and m.b and (m.c or m.d)");
        assertFalse(search.matches(0));
        assertFalse(search.matches(1));
        assertFalse(search.matches(2));
        assertFalse(search.matches(3));
        assertFalse(search.matches(4));
        assertFalse(search.matches(5));
        assertFalse(search.matches(6));
        assertFalse(search.matches(7));
        assertFalse(search.matches(8));
        assertFalse(search.matches(9));
        assertFalse(search.matches(10));
        assertFalse(search.matches(11));
        assertFalse(search.matches(12));
        assertTrue(search.matches(13));
        assertTrue(search.matches(14));
        assertTrue(search.matches(15));

        search = new Search(traceDataModel, "(m.a or m.b) and m.c and m.d");
        assertFalse(search.matches(0));
        assertFalse(search.matches(1));
        assertFalse(search.matches(2));
        assertFalse(search.matches(3));
        assertFalse(search.matches(4));
        assertFalse(search.matches(5));
        assertFalse(search.matches(6));
        assertTrue(search.matches(7));
        assertFalse(search.matches(8));
        assertFalse(search.matches(9));
        assertFalse(search.matches(10));
        assertTrue(search.matches(11));
        assertFalse(search.matches(12));
        assertFalse(search.matches(13));
        assertFalse(search.matches(14));
        assertTrue(search.matches(15));
    }

    /// Test running off the end of the trace without finding a match. In
    /// this case, we start in a match and hit the end while searching for
    /// the end of the match
    @Test
    public void testGetNextMatchEnd1() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int id1 = builder.newNet("a", -1, 1);
        builder.exitScope();
        builder.appendTransition(id1, 0, new BitVector("1", 2));
        Search search = new Search(traceDataModel, "mod1.a");
        assertEquals(-1, search.getNextMatch(0));
    }

    // Similar to above, except finds the end of the first match then
    // doesn't find a second match
    @Test
    public void testGetNextMatchEnd2() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int id1 = builder.newNet("a", -1, 1);
        builder.exitScope();
        builder.appendTransition(id1, 0, new BitVector("1", 2));
        builder.appendTransition(id1, 10, new BitVector("0", 2));
        Search search = new Search(traceDataModel, "mod1.a");
        assertEquals(-1, search.getNextMatch(0));
    }

    // Same as testGetNextMatchEnd1, except searching backward
    @Test
    public void testGetPrevMatchEnd1() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int id1 = builder.newNet("a", -1, 1);
        builder.exitScope();
        builder.appendTransition(id1, 0, new BitVector("1", 2));
        Search search = new Search(traceDataModel, "mod1.a");
        assertEquals(-1, search.getPreviousMatch(50));
    }

    // Same as testGetNextMatchEnd2, except searching backward
    @Test
    public void testGetPrevMatchEnd2() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int id1 = builder.newNet("a", -1, 1);
        builder.exitScope();
        builder.appendTransition(id1, 0, new BitVector("1", 2));
        builder.appendTransition(id1, 40, new BitVector("1", 2));
        Search search = new Search(traceDataModel, "mod1.a");
        assertEquals(-1, search.getPreviousMatch(50));
    }

    // Tests that the search expression is parsed and converted to an expression
    // tree correctly, as well as various operators.
    @Test
    public void testSearchToString() throws Exception {
        TraceDataModel traceDataModel = makeFourBitModel();

        // Basic comparisons
        assertEquals("(eq net0 net1)", (new Search(traceDataModel, "m.a = m.b")).toString());
        assertEquals("(eq 00000111 net1)", (new Search(traceDataModel, "7 = m.b")).toString());
        assertEquals("(eq net0 00000101)", (new Search(traceDataModel, "m.a = 5")).toString());
        assertEquals("(eq 00001000 00000101)", (new Search(traceDataModel, "8 = 5")).toString());

        // Use all comparison operators.
        assertEquals("(or (and (ne net0 0) (lt net1 00000001)) (and (gt net2 00000010) (eq net3 00000000)))",
            (new Search(traceDataModel, "m.a and m.b < 1 or m.c > 2 and m.d = 0")).toString());

        assertEquals("(or (and (ne net0 00000000) (le net1 00000001)) (and (ge net2 00000010) (ne net3 00000101)))",
            (new Search(traceDataModel, "m.a >< 0 and m.b <= 1 or m.c >= 2 and m.d <> 5")).toString());

        // Precedence tests. These mirror the test above, but ensure the expression tree was
        // set up correctly.
        assertEquals("(and (and (and (ne net0 0) (ne net1 0)) (ne net2 0)) (ne net3 0))",
            (new Search(traceDataModel, "m.a and m.b and m.c and m.d")).toString());
        assertEquals("(or (and (and (ne net0 0) (ne net1 0)) (ne net2 0)) (ne net3 0))",
            (new Search(traceDataModel, "m.a and m.b and m.c or m.d")).toString());
        assertEquals("(or (and (ne net0 0) (ne net1 0)) (and (ne net2 0) (ne net3 0)))",
            (new Search(traceDataModel, "m.a and m.b or m.c and m.d")).toString());
        assertEquals("(or (or (and (ne net0 0) (ne net1 0)) (ne net2 0)) (ne net3 0))",
            (new Search(traceDataModel, "m.a and m.b or m.c or m.d")).toString());
        assertEquals("(or (ne net0 0) (and (and (ne net1 0) (ne net2 0)) (ne net3 0)))",
            (new Search(traceDataModel, "m.a or m.b and m.c and m.d")).toString());
        assertEquals("(or (or (ne net0 0) (and (ne net1 0) (ne net2 0))) (ne net3 0))",
            (new Search(traceDataModel, "m.a or m.b and m.c or m.d")).toString());
        assertEquals("(or (or (ne net0 0) (ne net1 0)) (and (ne net2 0) (ne net3 0)))",
            (new Search(traceDataModel, "m.a or m.b or m.c and m.d")).toString());
        assertEquals("(or (or (or (ne net0 0) (ne net1 0)) (ne net2 0)) (ne net3 0))",
            (new Search(traceDataModel, "m.a or m.b or m.c or m.d")).toString());
    }

    // Basically tests that parens are treated as part of an indeitifer.
    // When a module is generated, it has the instance number: mod1.core_gen(0).mod2
    @Test
    public void testGenerateInstance() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterScope("mod1");
        builder.enterScope("mod_gen(0)");
        builder.enterScope("mod2");
        int id1 = builder.newNet("a", -1, 1);
        builder.exitScope();
        builder.exitScope();
        builder.exitScope();
        builder.appendTransition(id1, 0, new BitVector("0", 2));
        builder.appendTransition(id1, 5, new BitVector("1", 2));
        builder.loadFinished();

        Search search = new Search(traceDataModel, "mod1.mod_gen(0).mod2.a = 1");
        assertEquals(5, search.getNextMatch(0));
    }

    /// Test comparing a net to another instead of a constant value
    @Test
    public void testCompareNets() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int id1 = builder.newNet("a", -1, 4);
        int id2 = builder.newNet("b", -1, 4);
        builder.exitScope();
        builder.appendTransition(id1, 0, new BitVector("0", 10));
        builder.appendTransition(id1, 1, new BitVector("1", 10));
        builder.appendTransition(id1, 2, new BitVector("2", 10));
        builder.appendTransition(id1, 3, new BitVector("3", 10));
        builder.appendTransition(id2, 0, new BitVector("3", 10));
        builder.appendTransition(id2, 2, new BitVector("2", 10));
        builder.appendTransition(id2, 3, new BitVector("1", 10));

        Search search = new Search(traceDataModel, "mod1.a = mod1.b");
        assertEquals(2, search.getNextMatch(0));
        assertEquals(2, search.getPreviousMatch(5));

        // Swap the terms to ensure the hint is handled correctly.
        search = new Search(traceDataModel, "mod1.b = mod1.a");
        assertEquals(2, search.getNextMatch(0));
        assertEquals(2, search.getPreviousMatch(5));
    }
}


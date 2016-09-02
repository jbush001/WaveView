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

public class QueryTest {
    /// Utility function to create a sample trace with a clock signal
    TraceDataModel makeSingleBitModel() {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterModule("mod1");
        int id1 = builder.newNet("clk", -1, 1);
        builder.exitModule();
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
        builder.enterModule("m");
        builder.newNet("a", -1, 1);
        builder.newNet("b", -1, 1);
        builder.newNet("c", -1, 1);
        builder.newNet("d", -1, 1);
        builder.exitModule();

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
        Query query = new Query(makeSingleBitModel(), "\r  \n \t  mod1.clk          =  1\n");
        assertEquals(10, query.getNextMatch(4));

        query = new Query(makeSingleBitModel(), "mod1.clk=1");
        assertEquals(10, query.getNextMatch(4));
    }

    /// Test identifier characters
    @Test
    public void testIdentifier() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterModule("mod1");
        int id1 = builder.newNet("_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", -1, 1);
        builder.exitModule();
        builder.appendTransition(id1, 5, new BitVector("0", 2));
        builder.appendTransition(id1, 10, new BitVector("1", 2));
        builder.loadFinished();

        Query query = new Query(traceDataModel, "mod1._abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 = 1\n");
        assertEquals(10, query.getNextMatch(4));
    }

    @Test
    public void testSimpleQuery() throws Exception {
        // clk is high 10-14, 20-
        Query query = new Query(makeSingleBitModel(), "mod1.clk");
        assertEquals(10, query.getNextMatch(4));
        assertEquals(10, query.getNextMatch(9));
        assertEquals(20, query.getNextMatch(10));
        assertEquals(20, query.getNextMatch(15));
        assertEquals(-1, query.getNextMatch(20));
        assertEquals(-1, query.getNextMatch(30));

        assertEquals(14, query.getPreviousMatch(21));
        assertEquals(14, query.getPreviousMatch(20));
        assertEquals(14, query.getPreviousMatch(15));
        assertEquals(-1, query.getPreviousMatch(10));
        assertEquals(-1, query.getPreviousMatch(5));
        assertEquals(-1, query.getPreviousMatch(4));
    }

    @Test
    public void testLiteralBases() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterModule("mod1");
        int id1 = builder.newNet("value", -1, 4);
        builder.exitModule();
        builder.appendTransition(id1, 17, new BitVector("5", 10));
        builder.appendTransition(id1, 23, new BitVector("10", 10));
        builder.loadFinished();

        Query query = new Query(traceDataModel, "mod1.value = 10");
        assertEquals(23, query.getNextMatch(0));

        query = new Query(traceDataModel, "mod1.value = 'd10");
        assertEquals(23, query.getNextMatch(0));

        query = new Query(traceDataModel, "mod1.value = 'ha");
        assertEquals(23, query.getNextMatch(0));

        query = new Query(traceDataModel, "mod1.value = 'hA");
        assertEquals(23, query.getNextMatch(0));

        query = new Query(traceDataModel, "mod1.value = 'b1010");
        assertEquals(23, query.getNextMatch(0));
    }

    /// @bug This doesn't match correctly yet: Xes are ignored for
    /// matching.
    /// Verify it at least parses the query.
    @Test
    public void testMatchXZ() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.enterModule("mod1");
        int id1 = builder.newNet("value", -1, 4);
        builder.exitModule();
        builder.appendTransition(id1, 17, new BitVector("1111", 2));
        builder.appendTransition(id1, 23, new BitVector("xxxx", 2));
        builder.loadFinished();

        // Try upper and lower case versions of X and Z for hex and decimal
        // bases
        Query query = new Query(traceDataModel, "mod1.value = 'bxxxx");
        query = new Query(traceDataModel, "mod1.value = 'bXXXX");
        query = new Query(traceDataModel, "mod1.value = 'hx");
        query = new Query(traceDataModel, "mod1.value = 'hX");
        query = new Query(traceDataModel, "mod1.value = 'bzzzz");
        query = new Query(traceDataModel, "mod1.value = 'bZZZZ");
        query = new Query(traceDataModel, "mod1.value = 'hz");
        query = new Query(traceDataModel, "mod1.value = 'hZ");

        // X and Z with decimal values should fail
        try {
            query = new Query(traceDataModel, "mod1.value = 'dx");
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Should throw this
        }

        try {
            query = new Query(traceDataModel, "mod1.value = 'dX");
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Should throw this
        }

        try {
            query = new Query(traceDataModel, "mod1.value = 'dz");
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Should throw this
        }

        try {
            query = new Query(traceDataModel, "mod1.value = 'dZ");
            fail("Did not throw exception");
        } catch (NumberFormatException exc) {
            // Should throw this
        }
    }

    @Test
    public void testUnknownNet() {
        try {
            Query query = new Query(makeSingleBitModel(), "mod1.stall_pipeline = 2");
            fail("Did not throw exception");
        } catch (Query.ParseException exc) {
            assertEquals("unknown net \"mod1.stall_pipeline\"", exc.toString());
            assertEquals(0, exc.getStartOffset());
            assertEquals(18, exc.getEndOffset());
        }
    }

    @Test
    public void testStrayIdentifier() {
        try {
            Query query = new Query(makeSingleBitModel(), "mod1.clk = 'h2 foo");
            fail("Did not throw exception");
        } catch (Query.ParseException exc) {
            assertEquals("unexpected value", exc.toString());
            assertEquals(15, exc.getStartOffset());
            assertEquals(17, exc.getEndOffset());
        }
    }

    @Test
    public void testMissingCompareValue() {
        try {
            Query query = new Query(makeSingleBitModel(), "mod1.clk = ");
            fail("Did not throw exception");
        } catch (Query.ParseException exc) {
            assertEquals("unexpected end of string", exc.toString());
            assertEquals(10, exc.getStartOffset());
            assertEquals(10, exc.getEndOffset());
        }
    }

    @Test
    public void testMissingLiteral() {
        try {
            Query query = new Query(makeSingleBitModel(), "mod1.clk = mod2");
            fail("Did not throw exception");
        } catch (Query.ParseException exc) {
            assertEquals("unexpected value", exc.toString());
            assertEquals(11, exc.getStartOffset());
            assertEquals(14, exc.getEndOffset());
        }
    }

    @Test
    public void testUnknownLiteralType() {
        try {
            Query query = new Query(makeSingleBitModel(), "mod1.clk = 'q3z");
            fail("Did not throw exception");
        } catch (Query.ParseException exc) {
            assertEquals("unknown type q", exc.toString());
            assertEquals(11, exc.getStartOffset());
            assertEquals(11, exc.getEndOffset());
        }
    }

    @Test
    public void testMissingParen() {
        try {
            Query query = new Query(makeSingleBitModel(), "(mod1.clk = 'h3 foo");
            fail("Did not throw exception");
        } catch (Query.ParseException exc) {
            assertEquals("unexpected value", exc.toString());
            assertEquals(16, exc.getStartOffset());
            assertEquals(18, exc.getEndOffset());
        }
    }

    @Test
    public void testMissingIdentifier() {
        try {
            Query query = new Query(makeSingleBitModel(), "> 'h12");
            fail("Did not throw exception");
        } catch (Query.ParseException exc) {
            assertEquals("unexpected value", exc.toString());
            assertEquals(0, exc.getStartOffset());
            assertEquals(0, exc.getEndOffset());
        }
    }

    /// Basic test for 'and' functionailty
    /// This implicitly verifies query hinting by starting at different
    /// transition points
    @Test
    public void testAnd() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterModule("mod1");
        int id1 = builder.newNet("a", -1, 1);
        int id2 = builder.newNet("b", -1, 1);
        builder.exitModule();

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

        Query query = new Query(traceDataModel, "mod1.a & mod1.b");

        // Expression is true:
        // 15-19, 25-29, 45-

        // Test forward
        assertEquals(15, query.getNextMatch(0));  // false & false = false
        assertEquals(15, query.getNextMatch(5));  // true & false = false
        assertEquals(15, query.getNextMatch(10)); // false & false = false
        assertEquals(25, query.getNextMatch(15)); // true & true = true
        assertEquals(25, query.getNextMatch(20)); // false & true = false
        assertEquals(45, query.getNextMatch(25)); // true & true = true
        assertEquals(45, query.getNextMatch(30)); // false & false = false
        assertEquals(45, query.getNextMatch(35)); // true & false = false
        assertEquals(45, query.getNextMatch(40)); // false & false = false
        assertEquals(-1, query.getNextMatch(45)); // true & true  = true
        assertEquals(-1, query.getNextMatch(50)); // false & true = false

        // Test backward
        assertEquals(-1, query.getPreviousMatch(0));  // false & false = false
        assertEquals(-1, query.getPreviousMatch(5));  // true & false = false
        assertEquals(-1, query.getPreviousMatch(10)); // false & false = false
        assertEquals(-1, query.getPreviousMatch(15)); // true & true = true
        assertEquals(19, query.getPreviousMatch(20)); // false & true = false
        assertEquals(19, query.getPreviousMatch(25)); // true & true = true
        assertEquals(29, query.getPreviousMatch(30)); // false & false = false
        assertEquals(29, query.getPreviousMatch(35)); // true & false = false
        assertEquals(29, query.getPreviousMatch(40)); // false & false = false
        assertEquals(29, query.getPreviousMatch(45)); // true & true  = true
        assertEquals(29, query.getPreviousMatch(50)); // false & true = false
    }

    /// Basic test for 'or' functionailty
    /// This implicitly verifies query hinting by starting at different
    /// transition points
    @Test
    public void testOr() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterModule("mod1");
        int id1 = builder.newNet("a", -1, 1);
        int id2 = builder.newNet("b", -1, 1);
        builder.exitModule();

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

        Query query = new Query(traceDataModel, "mod1.a | mod1.b");

        // Expression is true:
        // 5-9, 15-29, 35-39, 45-

        // Test forward
        assertEquals(5, query.getNextMatch(0));  // false | false = false
        assertEquals(15, query.getNextMatch(5));  // true | false = true
        assertEquals(15, query.getNextMatch(10)); // false | false = false
        assertEquals(35, query.getNextMatch(15)); // true | true = true
        assertEquals(35, query.getNextMatch(20)); // false | true = true
        assertEquals(35, query.getNextMatch(25)); // true | true = true
        assertEquals(35, query.getNextMatch(30)); // false | false = false
        assertEquals(45, query.getNextMatch(35)); // true | false = true
        assertEquals(45, query.getNextMatch(40)); // false | false = false
        assertEquals(-1, query.getNextMatch(45)); // true | true  = true
        assertEquals(-1, query.getNextMatch(50)); // false | true = true

        // Test backward
        assertEquals(-1, query.getPreviousMatch(0));  // false | false = false
        assertEquals(-1, query.getPreviousMatch(5));  // true | false = true
        assertEquals(9, query.getPreviousMatch(10));  // false | false = false
        assertEquals(9, query.getPreviousMatch(15));  // true | true = true
        assertEquals(9, query.getPreviousMatch(20)); // false | true = true
        assertEquals(9, query.getPreviousMatch(25)); // true | true = true
        assertEquals(29, query.getPreviousMatch(30)); // false | false = false
        assertEquals(29, query.getPreviousMatch(35)); // true | false = true
        assertEquals(39, query.getPreviousMatch(40)); // false | false = false
        assertEquals(39, query.getPreviousMatch(45)); // true | true  = true
        assertEquals(39, query.getPreviousMatch(50)); // false | true = true
    }

    @Test
    public void testComparisons() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterModule("mod1");
        int id1 = builder.newNet("value", -1, 4);
        builder.exitModule();

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

        Query query = new Query(traceDataModel, "mod1.value > 'h5");
        assertEquals(6, query.getNextMatch(0));

        query = new Query(traceDataModel, "mod1.value < 'h5");
        assertEquals(4, query.getPreviousMatch(10));

        query = new Query(traceDataModel, "mod1.value = 'h5");
        assertEquals(5, query.getPreviousMatch(10));
    }

    @Test
    public void testPrecedence() throws Exception {
        TraceDataModel traceDataModel = makeFourBitModel();
        Query query;

        query = new Query(traceDataModel, "m.a & m.b & m.c & m.d");
        assertFalse(query.matches(0));
        assertFalse(query.matches(1));
        assertFalse(query.matches(2));
        assertFalse(query.matches(3));
        assertFalse(query.matches(4));
        assertFalse(query.matches(5));
        assertFalse(query.matches(6));
        assertFalse(query.matches(7));
        assertFalse(query.matches(8));
        assertFalse(query.matches(9));
        assertFalse(query.matches(10));
        assertFalse(query.matches(11));
        assertFalse(query.matches(12));
        assertFalse(query.matches(13));
        assertFalse(query.matches(14));
        assertTrue(query.matches(15));

        query = new Query(traceDataModel, "m.a & m.b & m.c | m.d");
        assertFalse(query.matches(0));
        assertTrue(query.matches(1));
        assertFalse(query.matches(2));
        assertTrue(query.matches(3));
        assertFalse(query.matches(4));
        assertTrue(query.matches(5));
        assertFalse(query.matches(6));
        assertTrue(query.matches(7));
        assertFalse(query.matches(8));
        assertTrue(query.matches(9));
        assertFalse(query.matches(10));
        assertTrue(query.matches(11));
        assertFalse(query.matches(12));
        assertTrue(query.matches(13));
        assertTrue(query.matches(14));
        assertTrue(query.matches(15));

        query = new Query(traceDataModel, "m.a & m.b | m.c & m.d");
        assertFalse(query.matches(0));
        assertFalse(query.matches(1));
        assertFalse(query.matches(2));
        assertTrue(query.matches(3));
        assertFalse(query.matches(4));
        assertFalse(query.matches(5));
        assertFalse(query.matches(6));
        assertTrue(query.matches(7));
        assertFalse(query.matches(8));
        assertFalse(query.matches(9));
        assertFalse(query.matches(10));
        assertTrue(query.matches(11));
        assertTrue(query.matches(12));
        assertTrue(query.matches(13));
        assertTrue(query.matches(14));
        assertTrue(query.matches(15));

        query = new Query(traceDataModel, "m.a & m.b | m.c | m.d");
        assertFalse(query.matches(0));
        assertTrue(query.matches(1));
        assertTrue(query.matches(2));
        assertTrue(query.matches(3));
        assertFalse(query.matches(4));
        assertTrue(query.matches(5));
        assertTrue(query.matches(6));
        assertTrue(query.matches(7));
        assertFalse(query.matches(8));
        assertTrue(query.matches(9));
        assertTrue(query.matches(10));
        assertTrue(query.matches(11));
        assertTrue(query.matches(12));
        assertTrue(query.matches(13));
        assertTrue(query.matches(14));
        assertTrue(query.matches(15));

        query = new Query(traceDataModel, "m.a | m.b & m.c & m.d");
        assertFalse(query.matches(0));
        assertFalse(query.matches(1));
        assertFalse(query.matches(2));
        assertFalse(query.matches(3));
        assertFalse(query.matches(4));
        assertFalse(query.matches(5));
        assertFalse(query.matches(6));
        assertTrue(query.matches(7));
        assertTrue(query.matches(8));
        assertTrue(query.matches(9));
        assertTrue(query.matches(10));
        assertTrue(query.matches(11));
        assertTrue(query.matches(12));
        assertTrue(query.matches(13));
        assertTrue(query.matches(14));
        assertTrue(query.matches(15));

        query = new Query(traceDataModel, "m.a | m.b & m.c | m.d");
        assertFalse(query.matches(0));
        assertTrue(query.matches(1));
        assertFalse(query.matches(2));
        assertTrue(query.matches(3));
        assertFalse(query.matches(4));
        assertTrue(query.matches(5));
        assertTrue(query.matches(6));
        assertTrue(query.matches(7));
        assertTrue(query.matches(8));
        assertTrue(query.matches(9));
        assertTrue(query.matches(10));
        assertTrue(query.matches(11));
        assertTrue(query.matches(12));
        assertTrue(query.matches(13));
        assertTrue(query.matches(14));
        assertTrue(query.matches(15));

        query = new Query(traceDataModel, "m.a | m.b | m.c & m.d");
        assertFalse(query.matches(0));
        assertFalse(query.matches(1));
        assertFalse(query.matches(2));
        assertTrue(query.matches(3));
        assertTrue(query.matches(4));
        assertTrue(query.matches(5));
        assertTrue(query.matches(6));
        assertTrue(query.matches(7));
        assertTrue(query.matches(8));
        assertTrue(query.matches(9));
        assertTrue(query.matches(10));
        assertTrue(query.matches(11));
        assertTrue(query.matches(12));
        assertTrue(query.matches(13));
        assertTrue(query.matches(14));
        assertTrue(query.matches(15));

        query = new Query(traceDataModel, "m.a | m.b | m.c | m.d");
        assertFalse(query.matches(0));
        assertTrue(query.matches(1));
        assertTrue(query.matches(2));
        assertTrue(query.matches(3));
        assertTrue(query.matches(4));
        assertTrue(query.matches(5));
        assertTrue(query.matches(6));
        assertTrue(query.matches(7));
        assertTrue(query.matches(8));
        assertTrue(query.matches(9));
        assertTrue(query.matches(10));
        assertTrue(query.matches(11));
        assertTrue(query.matches(12));
        assertTrue(query.matches(13));
        assertTrue(query.matches(14));
        assertTrue(query.matches(15));

        // Parentheses
        query = new Query(traceDataModel, "m.a & (m.b | m.c) & m.d");
        assertFalse(query.matches(0));
        assertFalse(query.matches(1));
        assertFalse(query.matches(2));
        assertFalse(query.matches(3));
        assertFalse(query.matches(4));
        assertFalse(query.matches(5));
        assertFalse(query.matches(6));
        assertFalse(query.matches(7));
        assertFalse(query.matches(8));
        assertFalse(query.matches(9));
        assertFalse(query.matches(10));
        assertTrue(query.matches(11));
        assertFalse(query.matches(12));
        assertTrue(query.matches(13));
        assertFalse(query.matches(14));
        assertTrue(query.matches(15));

        query = new Query(traceDataModel, "m.a & m.b & (m.c | m.d)");
        assertFalse(query.matches(0));
        assertFalse(query.matches(1));
        assertFalse(query.matches(2));
        assertFalse(query.matches(3));
        assertFalse(query.matches(4));
        assertFalse(query.matches(5));
        assertFalse(query.matches(6));
        assertFalse(query.matches(7));
        assertFalse(query.matches(8));
        assertFalse(query.matches(9));
        assertFalse(query.matches(10));
        assertFalse(query.matches(11));
        assertFalse(query.matches(12));
        assertTrue(query.matches(13));
        assertTrue(query.matches(14));
        assertTrue(query.matches(15));

        query = new Query(traceDataModel, "(m.a | m.b) & m.c & m.d");
        assertFalse(query.matches(0));
        assertFalse(query.matches(1));
        assertFalse(query.matches(2));
        assertFalse(query.matches(3));
        assertFalse(query.matches(4));
        assertFalse(query.matches(5));
        assertFalse(query.matches(6));
        assertTrue(query.matches(7));
        assertFalse(query.matches(8));
        assertFalse(query.matches(9));
        assertFalse(query.matches(10));
        assertTrue(query.matches(11));
        assertFalse(query.matches(12));
        assertFalse(query.matches(13));
        assertFalse(query.matches(14));
        assertTrue(query.matches(15));
    }

    /// Test running off the end of the trace without finding a match. In
    /// this case, we start in a match and hit the end while searching for
    /// the end of the match
    @Test
    public void testGetNextMatchEnd1() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterModule("mod1");
        int id1 = builder.newNet("a", -1, 1);
        builder.exitModule();
        builder.appendTransition(id1, 0, new BitVector("1", 2));
        Query query = new Query(traceDataModel, "mod1.a");
        assertEquals(-1, query.getNextMatch(0));
    }

    // Similar to above, except finds the end of the first match then
    // doesn't find a second match
    @Test
    public void testGetNextMatchEnd2() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterModule("mod1");
        int id1 = builder.newNet("a", -1, 1);
        builder.exitModule();
        builder.appendTransition(id1, 0, new BitVector("1", 2));
        builder.appendTransition(id1, 10, new BitVector("0", 2));
        Query query = new Query(traceDataModel, "mod1.a");
        assertEquals(-1, query.getNextMatch(0));
    }

    // Same as testGetNextMatchEnd1, except searching backward
    @Test
    public void testGetPrevMatchEnd1() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterModule("mod1");
        int id1 = builder.newNet("a", -1, 1);
        builder.exitModule();
        builder.appendTransition(id1, 0, new BitVector("1", 2));
        Query query = new Query(traceDataModel, "mod1.a");
        assertEquals(-1, query.getPreviousMatch(50));
    }

    // Same as testGetNextMatchEnd2, except searching backward
    @Test
    public void testGetPrevMatchEnd2() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.setTimescale(-9);
        builder.enterModule("mod1");
        int id1 = builder.newNet("a", -1, 1);
        builder.exitModule();
        builder.appendTransition(id1, 0, new BitVector("1", 2));
        builder.appendTransition(id1, 40, new BitVector("1", 2));
        Query query = new Query(traceDataModel, "mod1.a");
        assertEquals(-1, query.getPreviousMatch(50));
    }

    /// Converting a query to a string is a debug feature.
    @Test
    public void testQueryToString() throws Exception {
        TraceDataModel traceDataModel = makeFourBitModel();

        // Use all comparison operators
        assertEquals("(or (and (equal net0 00000000) (lessthan net1 00000001)) (and (greater net2 00000010) (notequal net3 0)))",
            (new Query(traceDataModel, "m.a = 0 & m.b < 1 | m.c > 2 & m.d")).toString());

        // Precedence tests. These mirror the test above, but ensure the expression tree was
        // set up correctly.
        assertEquals("(and (and (and (notequal net0 0) (notequal net1 0)) (notequal net2 0)) (notequal net3 0))",
            (new Query(traceDataModel, "m.a & m.b & m.c & m.d")).toString());
        assertEquals("(or (and (and (notequal net0 0) (notequal net1 0)) (notequal net2 0)) (notequal net3 0))",
            (new Query(traceDataModel, "m.a & m.b & m.c | m.d")).toString());
        assertEquals("(or (and (notequal net0 0) (notequal net1 0)) (and (notequal net2 0) (notequal net3 0)))",
            (new Query(traceDataModel, "m.a & m.b | m.c & m.d")).toString());
        assertEquals("(or (or (and (notequal net0 0) (notequal net1 0)) (notequal net2 0)) (notequal net3 0))",
            (new Query(traceDataModel, "m.a & m.b | m.c | m.d")).toString());
        assertEquals("(or (notequal net0 0) (and (and (notequal net1 0) (notequal net2 0)) (notequal net3 0)))",
            (new Query(traceDataModel, "m.a | m.b & m.c & m.d")).toString());
        assertEquals("(or (or (notequal net0 0) (and (notequal net1 0) (notequal net2 0))) (notequal net3 0))",
            (new Query(traceDataModel, "m.a | m.b & m.c | m.d")).toString());
        assertEquals("(or (or (notequal net0 0) (notequal net1 0)) (and (notequal net2 0) (notequal net3 0)))",
            (new Query(traceDataModel, "m.a | m.b | m.c & m.d")).toString());
        assertEquals("(or (or (or (notequal net0 0) (notequal net1 0)) (notequal net2 0)) (notequal net3 0))",
            (new Query(traceDataModel, "m.a | m.b | m.c | m.d")).toString());
    }
}


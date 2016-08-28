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
    TraceDataModel makeTraceDataModel() {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.enterModule("mod1");
        int id1 = builder.newNet("clk", -1, 1);
        int id2 = builder.newNet("dat", -1, 8);
        int id3 = builder.newNet("addr", -1, 8);
        builder.exitModule();
        builder.appendTransition(id1, 5, new BitVector("0", 2));
        builder.appendTransition(id1, 10, new BitVector("1", 2));
        builder.appendTransition(id1, 15, new BitVector("0", 2));
        builder.appendTransition(id1, 20, new BitVector("1", 2));
        builder.appendTransition(id2, 10, new BitVector("11111111", 2));
        builder.appendTransition(id2, 15, new BitVector("01010101", 2));
        builder.appendTransition(id3, 12, new BitVector("00000000", 2));
        builder.appendTransition(id3, 16, new BitVector("11110000", 2));
        builder.loadFinished();

        return traceDataModel;
    }

    /// Test various forms of whitespace (and lack thereof)
    @Test
    public void testWhitespace() throws Exception {
        Query query = new Query(makeTraceDataModel(), "\r  \n \t  mod1.clk          =  1\n");
        assertEquals(10, query.getNextMatch(4));

        query = new Query(makeTraceDataModel(), "mod1.clk=1");
        assertEquals(10, query.getNextMatch(4));
    }

    /// Test identifier characters
    @Test
    public void testIdentifier() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.enterModule("mod1");
        int id1 = builder.newNet("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", -1, 1);
        builder.exitModule();
        builder.appendTransition(id1, 5, new BitVector("0", 2));
        builder.appendTransition(id1, 10, new BitVector("1", 2));
        builder.loadFinished();

        Query query = new Query(traceDataModel, "mod1.abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 = 1\n");
        assertEquals(10, query.getNextMatch(4));
    }


    @Test
    public void testSimpleQuery() throws Exception {
        // clk is high 10-14, 20-
        Query query = new Query(makeTraceDataModel(), "mod1.clk = 1");
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
            Query query = new Query(makeTraceDataModel(), "mod1.stall_pipeline = 2");
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
            Query query = new Query(makeTraceDataModel(), "mod1.clk = 'h2 foo");
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
            Query query = new Query(makeTraceDataModel(), "mod1.clk = ");
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
            Query query = new Query(makeTraceDataModel(), "mod1.clk = mod2");
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
            Query query = new Query(makeTraceDataModel(), "mod1.clk = 'q3z");
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
            Query query = new Query(makeTraceDataModel(), "(mod1.clk = 'h3 foo");
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
            Query query = new Query(makeTraceDataModel(), "> 'h12");
            fail("Did not throw exception");
        } catch (Query.ParseException exc) {
            assertEquals("unexpected value", exc.toString());
            assertEquals(0, exc.getStartOffset());
            assertEquals(0, exc.getEndOffset());
        }
    }

    @Test
    public void testBadOperator() {
        try {
            Query query = new Query(makeTraceDataModel(), "mod1.clk ? 'h12");
            fail("Did not throw exception");
        } catch (Query.ParseException exc) {
            assertEquals("unknown conditional operator", exc.toString());
            assertEquals(9, exc.getStartOffset());
            assertEquals(9, exc.getEndOffset());
        }
    }

    /// This implicitly verifies query hinting by starting at different
    /// transition points
    @Test
    public void testAnd() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
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

        Query query = new Query(traceDataModel, "mod1.a = 1 & mod1.b = 1");

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

    /// This implicitly verifies query hinting by starting at different
    /// transition points
    @Test
    public void testOr() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
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

        Query query = new Query(traceDataModel, "mod1.a = 1 | mod1.b = 1");

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
    }

    @Test
    public void testParens() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.enterModule("mod1");
        int id1 = builder.newNet("a", -1, 1);
        int id2 = builder.newNet("b", -1, 1);
        int id3 = builder.newNet("c", -1, 1);
        builder.exitModule();

        // a
        builder.appendTransition(id1, 0, new BitVector("0", 2));
        builder.appendTransition(id1, 1, new BitVector("0", 2));
        builder.appendTransition(id1, 2, new BitVector("1", 2));
        builder.appendTransition(id1, 3, new BitVector("0", 2));
        builder.appendTransition(id1, 4, new BitVector("1", 2));
        builder.appendTransition(id1, 5, new BitVector("1", 2));
        builder.appendTransition(id1, 6, new BitVector("1", 2));

        // b
        builder.appendTransition(id2, 0, new BitVector("0", 2));
        builder.appendTransition(id2, 1, new BitVector("0", 2));
        builder.appendTransition(id2, 2, new BitVector("1", 2));
        builder.appendTransition(id2, 3, new BitVector("1", 2));
        builder.appendTransition(id2, 4, new BitVector("0", 2));
        builder.appendTransition(id2, 5, new BitVector("0", 2));
        builder.appendTransition(id2, 6, new BitVector("1", 2));

        // c
        builder.appendTransition(id3, 0, new BitVector("0", 2));
        builder.appendTransition(id3, 1, new BitVector("1", 2));
        builder.appendTransition(id3, 2, new BitVector("0", 2));
        builder.appendTransition(id3, 3, new BitVector("0", 2));
        builder.appendTransition(id3, 4, new BitVector("0", 2));
        builder.appendTransition(id3, 5, new BitVector("1", 2));
        builder.appendTransition(id3, 6, new BitVector("1", 2));
        builder.loadFinished();

        //   abc
        //   000  0
        //   001  1
        //   110  2
        //   010  3
        //   100  4
        //   101  5
        //   111  6

        Query query = new Query(traceDataModel, "(mod1.a = 1 | mod1.b = 1) & mod1.c = 1");
        assertEquals(5, query.getNextMatch(0));

        query = new Query(traceDataModel, "mod1.a = 1 & (mod1.b = 1 | mod1.c = 1)");
        assertEquals(2, query.getNextMatch(0));
    }

    /// Test running off the end of the trace without finding a match. In
    /// this case, we start in a match and hit the end while searching for
    /// the end of the match
    @Test
    public void testGetNextMatchEnd1() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.enterModule("mod1");
        int id1 = builder.newNet("a", -1, 1);
        builder.exitModule();
        builder.appendTransition(id1, 0, new BitVector("1", 2));
        Query query = new Query(traceDataModel, "mod1.a = 1");
        assertEquals(-1, query.getNextMatch(0));
    }

    // Similar to above, except finds the end of the first match then
    // doesn't find a second match
    @Test
    public void testGetNextMatchEnd2() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.enterModule("mod1");
        int id1 = builder.newNet("a", -1, 1);
        builder.exitModule();
        builder.appendTransition(id1, 0, new BitVector("1", 2));
        builder.appendTransition(id1, 10, new BitVector("0", 2));
        Query query = new Query(traceDataModel, "mod1.a = 1");
        assertEquals(-1, query.getNextMatch(0));
    }

    // Same as testGetNextMatchEnd1, except searching backward
    @Test
    public void testGetPrevMatchEnd1() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.enterModule("mod1");
        int id1 = builder.newNet("a", -1, 1);
        builder.exitModule();
        builder.appendTransition(id1, 0, new BitVector("1", 2));
        Query query = new Query(traceDataModel, "mod1.a = 1");
        assertEquals(-1, query.getPreviousMatch(50));
    }

    // Same as testGetNextMatchEnd2, except searching backward
    @Test
    public void testGetPrevMatchEnd2() throws Exception {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.enterModule("mod1");
        int id1 = builder.newNet("a", -1, 1);
        builder.exitModule();
        builder.appendTransition(id1, 0, new BitVector("1", 2));
        builder.appendTransition(id1, 40, new BitVector("1", 2));
        Query query = new Query(traceDataModel, "mod1.a = 1");
        assertEquals(-1, query.getPreviousMatch(50));
    }
}

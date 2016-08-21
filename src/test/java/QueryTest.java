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

public class QueryTest
{
    TraceDataModel makeTraceDataModel()
    {
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

    @Test public void testSimpleQuery()
    {
        try
        {
            Query query = new Query(makeTraceDataModel(), "mod1.clk = 1");
            assertEquals(10, query.getNextMatch(4));
            assertEquals(10, query.getNextMatch(9));
            assertEquals(20, query.getNextMatch(10));
            assertEquals(20, query.getNextMatch(15));
            assertEquals(-1, query.getNextMatch(20));
            assertEquals(-1, query.getNextMatch(30));

            assertEquals(10, query.getPreviousMatch(21));
            assertEquals(10, query.getPreviousMatch(20));
            assertEquals(10, query.getPreviousMatch(15));
            assertEquals(-1, query.getPreviousMatch(10));
            assertEquals(-1, query.getPreviousMatch(5));
            assertEquals(-1, query.getPreviousMatch(4));
        }
        catch (Query.ParseException exc)
        {
            fail("caught query parse exception " + exc);
        }
    }

    @Test public void testLiteralBases()
    {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.enterModule("mod1");
        int id1 = builder.newNet("value", -1, 4);
        builder.exitModule();
        builder.appendTransition(id1, 17, new BitVector("5", 10));
        builder.appendTransition(id1, 23, new BitVector("10", 10));
        builder.loadFinished();

        try
        {
            Query query = new Query(traceDataModel, "mod1.value = 10");
            assertEquals(23, query.getNextMatch(0));

            query = new Query(traceDataModel, "mod1.value = 'd10");
            assertEquals(23, query.getNextMatch(0));

            query = new Query(traceDataModel, "mod1.value = 'ha");
            assertEquals(23, query.getNextMatch(0));

            query = new Query(traceDataModel, "mod1.value = 'b1010");
            assertEquals(23, query.getNextMatch(0));
        }
        catch (Query.ParseException exc)
        {
            fail("Threw query parse exception");
        }
    }

    /// @bug This doesn't match correctly yet: Xes are ignored for
    /// matching.
    /// Verify it at least parses the query.
    @Test public void testMatchXZ()
    {
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
        try
        {
            Query query = new Query(traceDataModel, "mod1.value = 'bxxxx");
            query = new Query(traceDataModel, "mod1.value = 'bXXXX");
            query = new Query(traceDataModel, "mod1.value = 'hx");
            query = new Query(traceDataModel, "mod1.value = 'hX");
            query = new Query(traceDataModel, "mod1.value = 'bzzzz");
            query = new Query(traceDataModel, "mod1.value = 'bZZZZ");
            query = new Query(traceDataModel, "mod1.value = 'hz");
            query = new Query(traceDataModel, "mod1.value = 'hZ");
        }
        catch (Exception exc)
        {
            fail("Threw query parse exception");
        }

        // X and Z with decimal values should fail
        try
        {
            Query query = new Query(traceDataModel, "mod1.value = 'dx");
            fail("Did not throw exception");
        }
        catch (NumberFormatException exc)
        {
            // Should throw this
        }
        catch (Query.ParseException exc)
        {
            fail("threw query parse exception");
        }

        try
        {
            Query query = new Query(traceDataModel, "mod1.value = 'dX");
            fail("Did not throw exception");
        }
        catch (NumberFormatException exc)
        {
            // Should throw this
        }
        catch (Query.ParseException exc)
        {
            fail("threw query parse exception");
        }

        try
        {
            Query query = new Query(traceDataModel, "mod1.value = 'dz");
            fail("Did not throw exception");
        }
        catch (NumberFormatException exc)
        {
            // Should throw this
        }
        catch (Query.ParseException exc)
        {
            fail("threw query parse exception");
        }

        try
        {
            Query query = new Query(traceDataModel, "mod1.value = 'dZ");
            fail("Did not throw exception");
        }
        catch (NumberFormatException exc)
        {
            // Should throw this
        }
        catch (Query.ParseException exc)
        {
            fail("threw query parse exception");
        }
    }

    @Test public void testUnknownNet()
    {
        try
        {
            Query query = new Query(makeTraceDataModel(), "mod1.stall_pipeline = 2");
            fail("Did not throw exception");
        }
        catch (Query.ParseException exc)
        {
            assertEquals("unknown net \"mod1.stall_pipeline\"", exc.toString());
            assertEquals(0, exc.getStartOffset());
            assertEquals(18, exc.getEndOffset());
        }
    }

    @Test public void testStrayIdentifier()
    {
        try
        {
            Query query = new Query(makeTraceDataModel(), "mod1.clk = 'h2 foo");
            fail("Did not throw exception");
        }
        catch (Query.ParseException exc)
        {
            assertEquals("unexpected value", exc.toString());
            assertEquals(15, exc.getStartOffset());
            assertEquals(17, exc.getEndOffset());
        }
    }

    @Test public void testMissingCompareValue()
    {
        try
        {
            Query query = new Query(makeTraceDataModel(), "mod1.clk = ");
            fail("Did not throw exception");
        }
        catch (Query.ParseException exc)
        {
            assertEquals("unexpected end of string", exc.toString());
            assertEquals(10, exc.getStartOffset());
            assertEquals(10, exc.getEndOffset());
        }
    }

    @Test public void testMissingLiteral()
    {
        try
        {
            Query query = new Query(makeTraceDataModel(), "mod1.clk = mod2");
            fail("Did not throw exception");
        }
        catch (Query.ParseException exc)
        {
            assertEquals("unexpected value", exc.toString());
            assertEquals(11, exc.getStartOffset());
            assertEquals(14, exc.getEndOffset());
        }
    }

    @Test public void testUnknownLiteralType()
    {
        try
        {
            Query query = new Query(makeTraceDataModel(), "mod1.clk = 'q3z");
            fail("Did not throw exception");
        }
        catch (Query.ParseException exc)
        {
            assertEquals("unknown type q", exc.toString());
            assertEquals(11, exc.getStartOffset());
            assertEquals(11, exc.getEndOffset());
        }

    }

    @Test public void testAnd()
    {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.enterModule("mod1");
        int id1 = builder.newNet("a", -1, 1);
        int id2 = builder.newNet("b", -1, 1);
        builder.exitModule();

        builder.appendTransition(id1, 1, new BitVector("0", 2));
        builder.appendTransition(id2, 1, new BitVector("0", 2));
        builder.appendTransition(id1, 5, new BitVector("1", 2));
        builder.appendTransition(id2, 5, new BitVector("0", 2));
        builder.appendTransition(id1, 10, new BitVector("1", 2));
        builder.appendTransition(id2, 10, new BitVector("1", 2));
        builder.appendTransition(id1, 15, new BitVector("0", 2));
        builder.appendTransition(id2, 15, new BitVector("1", 2));
        builder.appendTransition(id1, 20, new BitVector("1", 2));
        builder.appendTransition(id2, 20, new BitVector("0", 2));
        builder.appendTransition(id1, 25, new BitVector("1", 2));
        builder.appendTransition(id2, 25, new BitVector("1", 2));
        builder.appendTransition(id1, 30, new BitVector("0", 2));
        builder.appendTransition(id2, 30, new BitVector("0", 2));
        builder.loadFinished();

        try
        {
            Query query = new Query(traceDataModel, "mod1.a = 1 & mod1.b = 1");
            assertEquals(10, query.getNextMatch(0));
            assertEquals(25, query.getNextMatch(10));

            // XXX this doesn't seem right: should seek back to the end of the
            // region where it changes (30 and 15)
            assertEquals(25, query.getPreviousMatch(40));
            assertEquals(10, query.getPreviousMatch(25));
        }
        catch (Query.ParseException exc)
        {
            fail("Threw query parse exception");
        }
    }

    @Test public void testOr()
    {
        TraceDataModel traceDataModel = new TraceDataModel();
        TraceBuilder builder = traceDataModel.startBuilding();
        builder.enterModule("mod1");
        int id1 = builder.newNet("a", -1, 1);
        int id2 = builder.newNet("b", -1, 1);
        builder.exitModule();

        builder.appendTransition(id1, 1, new BitVector("0", 2));
        builder.appendTransition(id2, 1, new BitVector("0", 2));
        builder.appendTransition(id1, 5, new BitVector("1", 2));
        builder.appendTransition(id2, 5, new BitVector("0", 2));
        builder.appendTransition(id1, 10, new BitVector("0", 2));
        builder.appendTransition(id2, 10, new BitVector("0", 2));
        builder.appendTransition(id1, 15, new BitVector("0", 2));
        builder.appendTransition(id2, 15, new BitVector("1", 2));
        builder.appendTransition(id1, 20, new BitVector("0", 2));
        builder.appendTransition(id2, 20, new BitVector("0", 2));
        builder.appendTransition(id1, 25, new BitVector("1", 2));
        builder.appendTransition(id2, 25, new BitVector("1", 2));
        builder.loadFinished();

        try
        {
            Query query = new Query(traceDataModel, "mod1.a = 1 | mod1.b = 1");
            assertEquals(5, query.getNextMatch(0));
            assertEquals(15, query.getNextMatch(5));
            assertEquals(25, query.getNextMatch(15));

            /// @bug get next match will do weird things with or...
        }
        catch (Query.ParseException exc)
        {
            fail("Threw query parse exception");
        }
    }

    @Test public void testComparisons()
    {
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

        try
        {
            Query query = new Query(traceDataModel, "mod1.value > 'h5");
            assertEquals(6, query.getNextMatch(0));

            query = new Query(traceDataModel, "mod1.value < 'h5");
            assertEquals(4, query.getPreviousMatch(10));
        }
        catch (Query.ParseException exc)
        {
            fail("Threw query parse exception");
        }
    }
}

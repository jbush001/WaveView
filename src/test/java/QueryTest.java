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
        catch (Query.QueryParseException exc)
        {
            fail("caught query parse exception " + exc);
        }
    }

    @Test public void testUnknownNet()
    {
        try
        {
            Query query = new Query(makeTraceDataModel(), "mod1.stall_pipeline = 2");
            fail("Did not throw exception");
        }
        catch (Query.QueryParseException exc)
        {
            assertEquals("unknown net \"mod1.stall_pipeline\"\nmod1.stall_pipeline = 2\n^", exc.toString());
        }
    }

    // XXX test various query operators
    // XXX test query hinting:
    //   - With and, where one signal is more frequent than other
    //   - With or, ...
    //   - With comparison
    // XXX test all query parse errors
    // XXX test all bases (hex, decimal, binary) for literals

}

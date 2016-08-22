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

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.nio.file.Paths;
import java.io.*;
import java.util.Vector;

public class VCDLoaderTest {
    @Rule
    public TemporaryFolder fTempFolder = new TemporaryFolder();

    // Implements TraceBuilder interface, which the VCDLoader will write into,
    // but asserts if events don't match a pre-defined sequence.
    public class ExpectTraceBuilder implements TraceBuilder {
        static final int EXPECT_ENTER = 0;
        static final int EXPECT_EXIT = 1;
        static final int EXPECT_NET = 2;
        static final int EXPECT_TRANSITION = 3;
        static final int EXPECT_FINISHED = 4;

        class Event {
            Event(int type) {
                fType = type;
            }

            int fType;
            String fName;
            int fId;
            int fWidth;
            long fTimestamp;
            BitVector fValues;
        }

        public void enterModule(String name) {
            System.out.println("enterModule " + name);

            Event event = fEventList.elementAt(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_ENTER);
        }

        public void exitModule() {
            System.out.println("exitModule");

            Event event = fEventList.elementAt(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_EXIT);
        }

        public int newNet(String shortName, int cloneId, int width) {
            System.out.println("newNet " + shortName + " " + cloneId + " " + width);

            Event event = fEventList.elementAt(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_NET);
            assertEquals(event.fName, shortName);
            assertEquals(event.fId, cloneId);
            assertEquals(event.fWidth, width);

            fNetWidths.add(new Integer(width));
            return fNetWidths.size() - 1;
        }

        public int getNetWidth(int netId) {
            return fNetWidths.elementAt(netId);
        }

        public void appendTransition(int id, long timestamp, BitVector values) {
            System.out.println("appendTransition " + id + " " + timestamp + " " + values.toString(2));

            Event event = fEventList.elementAt(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_TRANSITION);
            assertEquals(event.fTimestamp, timestamp);
            assertEquals(0, values.compare(event.fValues));
        }

        public void loadFinished() {
            System.out.println("loadFinished");

            Event event = fEventList.elementAt(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_FINISHED);
        }

        void expectEnterModule(String name) {
            Event event = new Event(EXPECT_ENTER);
            event.fName = name;
            fEventList.add(event);
        }

        void expectExitModule() {
            fEventList.add(new Event(EXPECT_EXIT));
        }

        void expectNewNet(String name, int cloneId, int width) {
            Event event = new Event(EXPECT_NET);
            event.fName = name;
            event.fId = cloneId;
            event.fWidth = width;
            fEventList.add(event);
        }

        void expectAppendTransition(int id, long timestamp, String bitString) {
            Event event = new Event(EXPECT_TRANSITION);
            event.fId = id;
            event.fTimestamp = timestamp;
            event.fValues = new BitVector(bitString, 2);
            fEventList.add(event);
        }

        void expectLoadFinished() {
            fEventList.add(new Event(EXPECT_FINISHED));
        }

        private Vector<Integer> fNetWidths = new Vector<Integer>();
        private Vector<Event> fEventList = new Vector<Event>();
        private int fCurrentEvent = 0;
    }

    public class DummyTraceBuilder implements TraceBuilder {
        public void enterModule(String name) {}
        public void exitModule() {}
        public int newNet(String shortName, int cloneId, int width) {
            fNetWidths.add(new Integer(width));
            return fNetWidths.size() - 1;
        }

        public int getNetWidth(int netId) {
            return fNetWidths.elementAt(netId);
        }

        public void appendTransition(int id, long timestamp, BitVector values) {}
        public void loadFinished() {}

        private Vector<Integer> fNetWidths = new Vector<Integer>();
    }

    // Simulataneously builds VCD file contents and populates the
    // ExpectTraceBuilder with matching events
    class TestBuilder {
        void setTimestampMultiplier(int value) {
            fTimestampMultiplier = value;
        }

        void addString(String data) {
            fVCDContents.append(data);
        }

        void enterModule(String name) {
            fVCDContents.append("$scope module ");
            fVCDContents.append(name);
            fVCDContents.append(" $end\n");
            fTraceBuilder.expectEnterModule(name);
        }

        void exitModule() {
            fVCDContents.append("$upscope $end\n");
            fTraceBuilder.expectExitModule();
        }

        int defineNet(String name, int cloneId, int width) {
            int index = fNetIsMultiBit.size();
            fNetIsMultiBit.add(width != 1);
            fVCDContents.append("$var wire ");
            fVCDContents.append(width);
            fVCDContents.append(' ');
            fVCDContents.append(getIdForIndex(index));
            fVCDContents.append(' ');
            fVCDContents.append(name);
            if (width > 1) {
                fVCDContents.append(" [");
                fVCDContents.append(width - 1);
                fVCDContents.append(":0]");
            }

            fVCDContents.append(" $end\n");

            fTraceBuilder.expectNewNet(name, cloneId, width);
            return index;
        }

        void endDefinitions() {
            fVCDContents.append("$enddefinitions $end\n");
        }

        void appendTransition(int netId, long timestamp, int value) {
            StringBuffer tmp = new StringBuffer();
            for (int i = 0; i < 32; i++)
                tmp.append((value & (0x80000000 >> i)) != 0 ? '1' : '0');

            appendTransition(netId, timestamp, tmp.toString());
        }

        void appendTransition(int netId, long timestamp, String bitString) {
            if (fLastTimestamp != timestamp) {
                fLastTimestamp = timestamp;
                fVCDContents.append('#');
                fVCDContents.append(timestamp);
                fVCDContents.append('\n');
            }

            if (fNetIsMultiBit.elementAt(netId)) {
                fVCDContents.append('b');
                fVCDContents.append(bitString);
                fVCDContents.append(' ');
            } else
                fVCDContents.append(bitString);

            fVCDContents.append(getIdForIndex(netId));
            fVCDContents.append('\n');

            fTraceBuilder.expectAppendTransition(netId, timestamp * fTimestampMultiplier, bitString);
        }

        void finish() {
            fTraceBuilder.expectLoadFinished();
            System.out.println("output is\n" + fVCDContents.toString());
        }

        File getVCDFile() {
            return tempFileFrom(fVCDContents.toString());
        }

        private String getIdForIndex(int index) {
            return String.valueOf((char) (index + 33));
        }

        TraceBuilder getTraceBuilder() {
            return fTraceBuilder;
        }

        Vector<Boolean> fNetIsMultiBit = new Vector<Boolean>();
        ExpectTraceBuilder fTraceBuilder = new ExpectTraceBuilder();
        StringBuffer fVCDContents = new StringBuffer();
        int fTimestampMultiplier;
        long fLastTimestamp = -1;
    }

    File tempFileFrom(String contents) {
        try {
            File f = fTempFolder.newFile("test.vcd");
            (new FileOutputStream(f)).write(contents.getBytes());
            return f;
        } catch (IOException exc) {
            fail("Caught I/O exception trying to create temporary file");
            return null;
        }
    }

    File testFile(String name) {
        return new File("src/test/resources/" + name);
    }

    // Timescale is one microsecond
    @Test
    public void testTimescaleUs() {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectEnterModule("mod1");
        builder.expectNewNet("clk", -1, 1);
        builder.expectExitModule();
        builder.expectAppendTransition(0, 0, "1");
        builder.expectAppendTransition(0, 1000, "0");
        builder.expectAppendTransition(0, 2000, "1");
        builder.expectAppendTransition(0, 3000, "0");
        builder.expectAppendTransition(0, 4000, "1");
        builder.expectLoadFinished();

        try {
            (new VCDLoader()).load(testFile("timescale-us.vcd"),
                builder, null);
        } catch (Exception exc) {
            fail("caught exception");
        }
    }

    // Timescale is one nanosecond
    @Test
    public void testTimescaleNs() {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectEnterModule("mod1");
        builder.expectNewNet("clk", -1, 1);
        builder.expectExitModule();
        builder.expectAppendTransition(0, 0, "1");
        builder.expectAppendTransition(0, 1, "0");
        builder.expectAppendTransition(0, 2, "1");
        builder.expectAppendTransition(0, 3, "0");
        builder.expectAppendTransition(0, 4, "1");
        builder.expectLoadFinished();

        try {
            (new VCDLoader()).load(testFile("timescale-ns.vcd"),
                builder, null);
        } catch (Exception exc) {
            fail("caught exception");
        }
    }

    // Ensure it doesn't choke when it gets types it doesn't recognize
    // (for example $date, $version)
    @Test
    public void testUnknownHeaderFields() {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectEnterModule("mod1");
        builder.expectNewNet("clk", -1, 1);
        builder.expectExitModule();
        builder.expectAppendTransition(0, 0, "1");
        builder.expectLoadFinished();

        try {
            (new VCDLoader()).load(testFile("unknown-fields.vcd"),
                builder, null);
        } catch (Exception exc) {
            fail("caught exception");
        }
    }

    @Test
    public void testMultibit() {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectEnterModule("mod1");
        builder.expectNewNet("addr", -1, 16);
        builder.expectNewNet("data", -1, 3);
        builder.expectExitModule();
        builder.expectAppendTransition(0, 0, "1010111000101011");
        builder.expectAppendTransition(1, 0, "011");
        builder.expectAppendTransition(0, 5, "1101010010100010");
        builder.expectAppendTransition(1, 5, "100");
        builder.expectLoadFinished();

        try {
            (new VCDLoader()).load(testFile("multibit.vcd"), builder,
                null);
        } catch (Exception exc) {
            fail("caught exception " + exc);
        }
    }

    @Test
    public void testUnknownTimescale() {
        try {
            (new VCDLoader()).load(testFile("unknown-timescale.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 2: unknown timescale value 1qs",
                         exc.getMessage());
        } catch (IOException exc) {
            fail("IOException " + exc);
        }
    }

    @Test
    public void testUnknownNetId() {
        try {
            (new VCDLoader()).load(testFile("unknown-net-id.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 6: Unknown net id $", exc.getMessage());
        } catch (IOException exc) {
            fail("IOException " + exc);
        }
    }

    @Test
    public void testInvalidLogicValue() {
        try {
            (new VCDLoader()).load(testFile("invalid-logic-value.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 6: Invalid logic value",
                         exc.getMessage());
        } catch (IOException exc) {
            fail("IOException " + exc);
        }
    }

    @Test
    public void testInvalidScope() {
        try {
            (new VCDLoader()).load(testFile("scope-parse-error.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 2: parse error, expected $end got $var",
                         exc.getMessage());
        } catch (IOException exc) {
            fail("IOException " + exc);
        }
    }

    @Test
    public void testInvalidUpscope() {
        try {
            (new VCDLoader()).load(testFile("upscope-parse-error.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 4: parse error, expected $end got $enddefinitions",
                         exc.getMessage());
        } catch (IOException exc) {
            fail("IOException " + exc);
        }
    }

    @Test
    public void testVarParseError() {
        try {
            (new VCDLoader()).load(testFile("var-parse-error.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 3: parse error, expected $end got $upscope",
                         exc.getMessage());
        } catch (IOException exc) {
            fail("IOException " + exc);
        }
    }

    @Test
    public void testTimescaleParseError() {
        try {
            (new VCDLoader()).load(testFile("timescale-parse-error.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 3: parse error, expected $end got $scope",
                         exc.getMessage());
        } catch (IOException exc) {
            fail("IOException " + exc);
        }
    }

    @Test
    public void testTruncatedFile() {
        try {
            (new VCDLoader()).load(testFile("truncated.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 2: unexpected end of file", exc.getMessage());
        } catch (IOException exc) {
            fail("IOException " + exc);
        }
    }

    @Test
    public void testTraceAlias() {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectEnterModule("mod1");
        builder.expectNewNet("foo", -1, 1);
        builder.expectNewNet("source", -1, 1);
        builder.expectNewNet("alias", 1, 1);
        builder.expectExitModule();
        builder.expectAppendTransition(1, 0, "1");
        builder.expectLoadFinished();

        try {
            (new VCDLoader()).load(testFile("trace-alias.vcd"),
                builder, null);
        } catch (Exception exc) {
            fail("Threw exception " + exc);
        }
    }

    // Put everything together with more data and multiple signals
    @Test
    public void testMixed() {
        TestBuilder builder = new TestBuilder();
        builder.setTimestampMultiplier(1);
        builder.addString("$date\n	Mon Aug 15 22:28:13 2016\n$end\n");
        builder.addString("$version\n	Icarus Verilog\n$end\n");
        builder.addString("$timescale\n	1us\n$end\n");

        builder.enterModule("mod1");
        builder.setTimestampMultiplier(1000);
        builder.defineNet("clk", -1, 1);
        builder.defineNet("reset", -1, 1);
        builder.enterModule("mod2");
        builder.defineNet("addr", -1, 32);
        builder.defineNet("data", -1, 32);
        builder.exitModule();
        builder.exitModule();
        builder.endDefinitions();
        for (int i = 0; i < 10000; i++) {
            long time = i * 5;

            if (i % 2 == 0)
                builder.appendTransition(0, time, "0");
            else {
                if (i > 5) {
                    builder.appendTransition(0, time, "1");
                    builder.appendTransition(2, time, i * 8);
                    builder.appendTransition(3, time, (i * 123123123) ^ i);
                }

                if (i == 0)
                    builder.appendTransition(1, time, "1");
                else if (i == 5)
                    builder.appendTransition(1, time, "0");
            }
        }

        builder.finish();

        try {
            VCDLoader loader = new VCDLoader();
            loader.load(builder.getVCDFile(), builder.getTraceBuilder(),
                        null);
        } catch (Exception exc) {
            fail("caught exception " + exc);
        }
    }
}

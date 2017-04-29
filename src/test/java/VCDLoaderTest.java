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

import waveview.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.ArrayList;

public class VCDLoaderTest {
    @Rule
    public TemporaryFolder fTempFolder = new TemporaryFolder();

    // Implements TraceBuilder interface, which the VCDLoader will write into,
    // but asserts if events don't match a pre-defined sequence.
    static class ExpectTraceBuilder implements TraceBuilder {
        static final int EXPECT_ENTER = 0;
        static final int EXPECT_EXIT = 1;
        static final int EXPECT_NET = 2;
        static final int EXPECT_TRANSITION = 3;
        static final int EXPECT_FINISHED = 4;
        static final int EXPECT_TIMESCALE = 5;

        static class Event {
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

        @Override
        public void setTimescale(int order) {
            System.out.println("setTimescale " + order);

            Event event = fEventList.get(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_TIMESCALE);
            assertEquals(event.fTimestamp, (long) order);
        }

        @Override
        public void enterScope(String name) {
            System.out.println("enterScope " + name);

            Event event = fEventList.get(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_ENTER);
        }

        @Override
        public void exitScope() {
            System.out.println("exitScope");

            Event event = fEventList.get(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_EXIT);
        }

        @Override
        public int newNet(String shortName, int cloneId, int width) {
            System.out.println("newNet " + shortName + " " + cloneId + " " + width);

            Event event = fEventList.get(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_NET);
            assertEquals(event.fName, shortName);
            assertEquals(event.fId, cloneId);
            assertEquals(event.fWidth, width);

            return fNextNetId++;
        }

        @Override
        public void appendTransition(int id, long timestamp, BitVector values) {
            System.out.println("appendTransition " + id + " " + timestamp + " " + values.toString(2));

            Event event = fEventList.get(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_TRANSITION);
            assertEquals(event.fTimestamp, timestamp);

            // Convert to string instead of using compare so Z and X values are
            // handled correctly.
            assertEquals(event.fValues.toString(2), values.toString(2));
        }

        @Override
        public void loadFinished() {
            System.out.println("loadFinished");

            Event event = fEventList.get(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_FINISHED);
        }

        void expectTimescale(int order) {
            Event event = new Event(EXPECT_TIMESCALE);
            event.fTimestamp = order;
            fEventList.add(event);
        }

        void expectEnterScope(String name) {
            Event event = new Event(EXPECT_ENTER);
            event.fName = name;
            fEventList.add(event);
        }

        void expectExitScope() {
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

        private ArrayList<Event> fEventList = new ArrayList<>();
        private int fCurrentEvent;
        private int fNextNetId;
    }

    static class DummyTraceBuilder implements TraceBuilder {
        @Override
        public void setTimescale(int order) {}

        @Override
        public void enterScope(String name) {}

        @Override
        public void exitScope() {}

        @Override
        public int newNet(String shortName, int cloneId, int width) {
            return fNextNetId++;
        }

        @Override
        public void appendTransition(int id, long timestamp, BitVector values) {}

        @Override
        public void loadFinished() {}

        private int fNextNetId;
    }

    // Simulataneously builds VCD file contents and populates the
    // ExpectTraceBuilder with matching events
    class TestBuilder {
        void addString(String data) {
            fVCDContents.append(data);
        }

        void setTimescale(String str, int order) {
            fVCDContents.append("$timescale\n  1us\n$end\n");
            fTraceBuilder.expectTimescale(order);
        }

        void enterScope(String name) {
            fVCDContents.append("$scope module ");
            fVCDContents.append(name);
            fVCDContents.append(" $end\n");
            fTraceBuilder.expectEnterScope(name);
        }

        void exitScope() {
            fVCDContents.append("$upscope $end\n");
            fTraceBuilder.expectExitScope();
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

            if (fNetIsMultiBit.get(netId)) {
                fVCDContents.append('b');
                fVCDContents.append(bitString);
                fVCDContents.append(' ');
            } else
                fVCDContents.append(bitString);

            fVCDContents.append(getIdForIndex(netId));
            fVCDContents.append('\n');

            fTraceBuilder.expectAppendTransition(netId, timestamp, bitString);
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

        ArrayList<Boolean> fNetIsMultiBit = new ArrayList<>();
        ExpectTraceBuilder fTraceBuilder = new ExpectTraceBuilder();
        StringBuffer fVCDContents = new StringBuffer();
        long fLastTimestamp = -1;
    }

    File tempFileFrom(String contents) {
        try {
            File f = fTempFolder.newFile("test.vcd");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(contents.getBytes(StandardCharsets.US_ASCII));
            fos.close();
            return f;
        } catch (IOException exc) {
            fail("Caught I/O exception trying to create temporary file");
            return null;
        }
    }

    File testFile(String name) {
        return new File("src/test/resources/vcd/" + name);
    }

    @Test
    public void testTimescaleFs() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectTimescale(-15);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-fs.vcd"),
            builder, null);
    }

    @Test
    public void testTimescalePs() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectTimescale(-12);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-ps.vcd"),
            builder, null);
    }

    @Test
    public void testTimescaleNs() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectTimescale(-9);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-ns.vcd"),
            builder, null);
    }

    @Test
    public void testTimescaleUs() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectTimescale(-6);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-us.vcd"),
            builder, null);
    }

    @Test
    public void testTimescaleMs() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectTimescale(-3);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-ms.vcd"),
            builder, null);
    }

    @Test
    public void testTimescaleS() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectTimescale(0);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-s.vcd"),
            builder, null);
    }

    // Ensure it handles both the unit and the number correctly.
    @Test
    public void testTimescale10Us() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectTimescale(-5);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-10us.vcd"),
            builder, null);
    }

    @Test
    public void testTimescale100Us() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectTimescale(-4);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-100us.vcd"),
            builder, null);
    }

    // Check that it handles a space between the number and unit in
    // the timescale definition.
    @Test
    public void testTimescaleSpace() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectTimescale(-12);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-space.vcd"),
            builder, null);
    }

    @Test
    public void testUnknownTimescale() throws Exception {
        try {
            new VCDLoader().load(testFile("unknown-timescale.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 2: unknown timescale value 1qs",
                         exc.getMessage());
        }
    }

    @Test
    public void testTimescaleMissingUnit() throws Exception {
        try {
            new VCDLoader().load(testFile("timescale-missing-unit.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 3: unknown timescale value $end",
                         exc.getMessage());
        }
    }

    @Test
    public void testTimescaleBadValue() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        try {
            new VCDLoader().load(testFile("timescale-bad-value.vcd"),
                builder, null);
            fail("didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 2: bad timescale value 17us", exc.getMessage());
        }
    }

    // Ensure it doesn't choke when it gets types it doesn't recognize
    // (for example $date, $version)
    @Test
    public void testUnknownHeaderFields() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectTimescale(-6);
        builder.expectEnterScope("mod1");
        builder.expectNewNet("clk", -1, 1);
        builder.expectExitScope();
        builder.expectAppendTransition(0, 0, "1");
        builder.expectLoadFinished();

        new VCDLoader().load(testFile("unknown-header-fields.vcd"),
            builder, null);
    }

    // When a timestamp is out of order, the loader will set it to the
    // last value silently.
    @Test
    public void testTimestampOutOfOrder() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectTimescale(-9);
        builder.expectEnterScope("mod1");
        builder.expectNewNet("foo", -1, 1);
        builder.expectExitScope();
        builder.expectAppendTransition(0, 0, "1");
        builder.expectAppendTransition(0, 5, "0");
        builder.expectAppendTransition(0, 5, "1");
        builder.expectAppendTransition(0, 6, "0");
        builder.expectLoadFinished();

        new VCDLoader().load(testFile("timestamp-out-of-order.vcd"), builder,
            null);
    }

    // This tests a few things:
    // - Parsing both single and multibit transitions
    // - X and Z values (multibit like x10200x and splatted with a single x/z)
    // wire definitions with and without space:
    //    $var wire 16 A addr [15:0] $end
    //    $var wire 3 B data[2:0] $end
    @Test
    public void testMultibit() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectTimescale(-9);
        builder.expectEnterScope("mod1");
        builder.expectNewNet("addr", -1, 16);
        builder.expectNewNet("data", -1, 3);
        builder.expectNewNet("enable", -1, 1);
        builder.expectExitScope();
        builder.expectAppendTransition(0, 0, "1010111000101011");
        builder.expectAppendTransition(1, 0, "011");
        builder.expectAppendTransition(2, 0, "z");
        builder.expectAppendTransition(0, 5, "1101010010100010");
        builder.expectAppendTransition(1, 5, "100");
        builder.expectAppendTransition(2, 5, "x");
        builder.expectAppendTransition(1, 10, "01zxxz10zxzx1010");
        builder.expectAppendTransition(2, 10, "1");
        builder.expectAppendTransition(0, 15, "zzzzzzzzzzzzzzzz");
        builder.expectAppendTransition(1, 15, "xxx");
        builder.expectLoadFinished();

        new VCDLoader().load(testFile("multibit.vcd"), builder,
            null);
    }

    @Test
    public void testPadding() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectTimescale(-9);
        builder.expectEnterScope("mod1");
        builder.expectNewNet("value", -1, 16);
        builder.expectExitScope();
        builder.expectAppendTransition(0, 0, "zzzzzzzzzzzx1010");
        builder.expectAppendTransition(0, 1, "xxxxxxxxxxxz1010");
        builder.expectAppendTransition(0, 2, "0000000000001101");
        builder.expectAppendTransition(0, 3, "0000000000000010");
        builder.expectLoadFinished();

        new VCDLoader().load(testFile("padding.vcd"), builder,
            null);
    }

    // Test that $dumpvars is handled correctly
    @Test
    public void testDumpvars() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectTimescale(-9);
        builder.expectEnterScope("mod1");
        builder.expectNewNet("addr", -1, 16);
        builder.expectNewNet("data", -1, 3);
        builder.expectNewNet("enable", -1, 1);
        builder.expectExitScope();
        builder.expectAppendTransition(0, 0, "1010111000101011");
        builder.expectAppendTransition(1, 0, "011");
        builder.expectAppendTransition(2, 0, "z");
        builder.expectAppendTransition(0, 5, "1101010010100010");
        builder.expectAppendTransition(1, 5, "100");
        builder.expectAppendTransition(2, 5, "x");
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("dumpvars.vcd"), builder,
            null);
    }

    @Test
    public void testUnknownNetId() throws Exception {
        try {
            new VCDLoader().load(testFile("unknown-net-id.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 6: Unknown net id $", exc.getMessage());
        }
    }

    @Test
    public void testInvalidValueType() throws Exception {
        try {
            new VCDLoader().load(testFile("bad-transition-type.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 6: invalid value type 'q'", exc.getMessage());
        }
    }

    @Test
    public void testInvalidScope() throws Exception {
        try {
            new VCDLoader().load(testFile("scope-parse-error.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 2: parse error, expected $end got $var",
                         exc.getMessage());
        }
    }

    @Test
    public void testInvalidUpscope() throws Exception {
        try {
            new VCDLoader().load(testFile("upscope-parse-error.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 4: parse error, expected $end got $enddefinitions",
                         exc.getMessage());
        }
    }

    @Test
    public void testVarParseError() throws Exception {
        try {
            new VCDLoader().load(testFile("var-parse-error.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 3: parse error, expected $end got $upscope",
                         exc.getMessage());
        }
    }

    @Test
    public void testTimescaleParseError() throws Exception {
        try {
            new VCDLoader().load(testFile("timescale-parse-error.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 3: parse error, expected $end got $scope",
                         exc.getMessage());
        }
    }

    @Test
    public void invalidLogicValue() throws Exception {
        try {
            new VCDLoader().load(testFile("invalid-logic-value.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 9: invalid logic value", exc.getMessage());
        }
    }

    @Test
    public void testTruncatedFile() throws Exception {
        try {
            new VCDLoader().load(testFile("truncated.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 2: unexpected end of file", exc.getMessage());
        }
    }

    // This file is spec compliant, but we don't support real values
    @Test
    public void testRealValueType() throws Exception {
        try {
            new VCDLoader().load(testFile("real-value.vcd"),
                new DummyTraceBuilder(), null);
            fail("Didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("line 6: real values are not supported", exc.getMessage());
        }
    }

    @Test
    public void testTraceAlias() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectEnterScope("mod1");
        builder.expectNewNet("foo", -1, 1);
        builder.expectNewNet("source", -1, 1);
        builder.expectNewNet("alias", 1, 1);
        builder.expectExitScope();
        builder.expectAppendTransition(1, 0, "1");
        builder.expectLoadFinished();

        new VCDLoader().load(testFile("trace-alias.vcd"),
            builder, null);
    }

    // Put everything together with more data and multiple signals
    @Test
    public void testMixed() throws Exception {
        TestBuilder builder = new TestBuilder();
        builder.addString("$date\n	Mon Aug 15 22:28:13 2016\n$end\n");
        builder.addString("$version\n	Icarus Verilog\n$end\n");
        builder.setTimescale("1us", -6);
        builder.enterScope("mod1");
        builder.defineNet("clk", -1, 1);
        builder.defineNet("reset", -1, 1);
        builder.enterScope("mod2");
        builder.defineNet("addr", -1, 32);
        builder.defineNet("data", -1, 32);
        builder.exitScope();
        builder.exitScope();
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
        VCDLoader loader = new VCDLoader();
        loader.load(builder.getVCDFile(), builder.getTraceBuilder(),
                    null);
    }

    static class TestProgressListener implements TraceLoader.ProgressListener {
        @Override
        public boolean updateProgress(int percentRead) {
            assertTrue(percentRead >= fLastUpdate);
            assertTrue(percentRead >= 0);
            assertTrue(percentRead <= 100);
            fLastUpdate = percentRead;
            return true;
        }

        int fLastUpdate = -1;
    }

    // Write a bunch of data, make sure we get callbacks
    @Test
    public void testProgressListener() throws Exception {
        TestBuilder builder = new TestBuilder();
        builder.setTimescale("1us", -6);
        builder.enterScope("mod1");
        builder.defineNet("value", -1, 16);
        builder.exitScope();
        builder.endDefinitions();

        // Need to append enough data to cause at least one progress
        // update
        for (int i = 0; i < 10000; i++)
            builder.appendTransition(0, i * 5, "0000000000000000");

        builder.finish();

        TestProgressListener listener = new TestProgressListener();
        VCDLoader loader = new VCDLoader();
        loader.load(builder.getVCDFile(), builder.getTraceBuilder(),
                    listener);
        assertNotEquals(-1, listener.fLastUpdate);
    }

    // If the user clicks cancel, the progress listener update
    // method will return false. Ensure this aborts the load.
    @Test
    public void testInterruptedLoad() throws IOException {
        TestBuilder builder = new TestBuilder();
        builder.setTimescale("1us", -6);
        builder.enterScope("mod1");
        builder.defineNet("value", -1, 16);
        builder.exitScope();
        builder.endDefinitions();
        for (int i = 0; i < 10000; i++)
            builder.appendTransition(0, i * 5, "0000000000000000");

        builder.finish();

        try {
            VCDLoader loader = new VCDLoader();
            loader.load(builder.getVCDFile(), builder.getTraceBuilder(),
                new VCDLoader.ProgressListener() {
                    @Override
                    public boolean updateProgress(int percentRead) {
                        return false;
                    }
                });
            fail("Loader didn't throw exception");
        } catch (TraceLoader.LoadException exc) {
            assertEquals("load cancelled", exc.getMessage());
        }
    }

    // File produced by Accellera SystemC
    @Test
    public void testAccellera() throws Exception {
        ExpectTraceBuilder builder = new ExpectTraceBuilder();
        builder.expectTimescale(-12);
        builder.expectEnterScope("SystemC");
        builder.expectNewNet("int_val", -1, 32);
        builder.expectNewNet("float_val", -1, 1);
        builder.expectNewNet("clk", -1, 1);
        builder.expectNewNet("rstn", -1, 1);
        builder.expectExitScope();
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("accellera.vcd"),
            builder, null);
    }
}

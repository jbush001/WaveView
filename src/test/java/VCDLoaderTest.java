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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import waveview.WaveformBuilder;
import waveview.WaveformLoader;
import waveview.VCDLoader;

public class VCDLoaderTest {
    @Rule
    public final TemporaryFolder fTempFolder = new TemporaryFolder();

    // Simulataneously builds VCD file contents and populates the
    // ExpectWaveformBuilder with matching events
    class TestBuilder {
        private final ArrayList<Boolean> netIsMultiBit = new ArrayList<>();
        private final ExpectWaveformBuilder waveformBuilder = new ExpectWaveformBuilder();
        private final StringBuilder vcdContents = new StringBuilder();
        private long lastTimestamp = -1;

        void addString(String data) {
            vcdContents.append(data);
        }

        void setTimescale(String str, int order) {
            vcdContents.append("$timescale\n  1us\n$end\n");
            waveformBuilder.expectTimescale(order);
        }

        void enterScope(String name) {
            vcdContents.append("$scope module ");
            vcdContents.append(name);
            vcdContents.append(" $end\n");
            waveformBuilder.expectEnterScope(name);
        }

        void exitScope() {
            vcdContents.append("$upscope $end\n");
            waveformBuilder.expectExitScope();
        }

        int defineNet(String name, int cloneId, int width) {
            int index = netIsMultiBit.size();
            netIsMultiBit.add(width != 1);
            vcdContents.append("$var wire ");
            vcdContents.append(width);
            vcdContents.append(' ');
            vcdContents.append(getIdForIndex(index));
            vcdContents.append(' ');
            vcdContents.append(name);
            if (width > 1) {
                vcdContents.append(" [");
                vcdContents.append(width - 1);
                vcdContents.append(":0]");
            }

            vcdContents.append(" $end\n");

            waveformBuilder.expectNewNet(name, cloneId, width);
            return index;
        }

        void endDefinitions() {
            vcdContents.append("$enddefinitions $end\n");
        }

        void appendTransition(int netId, long timestamp, int value) {
            StringBuilder tmp = new StringBuilder();
            for (int i = 0; i < 32; i++)
                tmp.append((value & (0x80000000 >> i)) == 0 ? '0' : '1');

            appendTransition(netId, timestamp, tmp.toString());
        }

        void appendTransition(int netId, long timestamp, String bitString) {
            if (lastTimestamp != timestamp) {
                lastTimestamp = timestamp;
                vcdContents.append('#');
                vcdContents.append(timestamp);
                vcdContents.append('\n');
            }

            if (netIsMultiBit.get(netId)) {
                vcdContents.append('b');
                vcdContents.append(bitString);
                vcdContents.append(' ');
            } else
                vcdContents.append(bitString);

            vcdContents.append(getIdForIndex(netId));
            vcdContents.append('\n');

            waveformBuilder.expectAppendTransition(netId, timestamp, bitString);
        }

        void finish() {
            waveformBuilder.expectLoadFinished();
            System.out.println("output is\n" + vcdContents.toString());
        }

        File getVCDFile() {
            return tempFileFrom(vcdContents.toString());
        }

        private String getIdForIndex(int index) {
            return String.valueOf((char) (index + 33));
        }

        WaveformBuilder getWaveformBuilder() {
            return waveformBuilder;
        }
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
    public void timescaleFs() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
        builder.expectTimescale(-15);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-fs.vcd"), builder, null);
    }

    @Test
    public void timescalePs() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
        builder.expectTimescale(-12);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-ps.vcd"), builder, null);
    }

    @Test
    public void timescaleNs() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
        builder.expectTimescale(-9);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-ns.vcd"), builder, null);
    }

    @Test
    public void timescaleUs() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
        builder.expectTimescale(-6);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-us.vcd"), builder, null);
    }

    @Test
    public void timescaleMs() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
        builder.expectTimescale(-3);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-ms.vcd"), builder, null);
    }

    @Test
    public void timescaleS() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
        builder.expectTimescale(0);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-s.vcd"), builder, null);
    }

    // Ensure it handles both the unit and the number correctly.
    @Test
    public void timescale10Us() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
        builder.expectTimescale(-5);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-10us.vcd"), builder, null);
    }

    @Test
    public void timescale100Us() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
        builder.expectTimescale(-4);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-100us.vcd"), builder, null);
    }

    // Check that it handles a space between the number and unit in
    // the timescale definition.
    @Test
    public void timescaleSpace() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
        builder.expectTimescale(-12);
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("timescale-space.vcd"), builder, null);
    }

    @Test
    public void unknownTimescale() throws Exception {
        try {
            new VCDLoader().load(testFile("unknown-timescale.vcd"), new DummyWaveformBuilder(), null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadException exc) {
            assertEquals("line 2: unknown timescale value 1qs", exc.getMessage());
        }
    }

    @Test
    public void timescaleMissingUnit() throws Exception {
        try {
            new VCDLoader().load(testFile("timescale-missing-unit.vcd"), new DummyWaveformBuilder(), null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadException exc) {
            assertEquals("line 3: unknown timescale value $end", exc.getMessage());
        }
    }

    @Test
    public void timescaleBadValue() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
        try {
            new VCDLoader().load(testFile("timescale-bad-value.vcd"), builder, null);
            fail("didn't throw exception");
        } catch (WaveformLoader.LoadException exc) {
            assertEquals("line 2: bad timescale value 17us", exc.getMessage());
        }
    }

    // Ensure it doesn't choke when it gets types it doesn't recognize
    // (for example $date, $version)
    @Test
    public void unknownHeaderFields() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
        builder.expectTimescale(-6);
        builder.expectEnterScope("mod1");
        builder.expectNewNet("clk", -1, 1);
        builder.expectExitScope();
        builder.expectAppendTransition(0, 0, "1");
        builder.expectLoadFinished();

        new VCDLoader().load(testFile("unknown-header-fields.vcd"), builder, null);
    }

    // When a timestamp is out of order, the loader will set it to the
    // last value silently.
    @Test
    public void timestampOutOfOrder() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
        builder.expectTimescale(-9);
        builder.expectEnterScope("mod1");
        builder.expectNewNet("foo", -1, 1);
        builder.expectExitScope();
        builder.expectAppendTransition(0, 0, "1");
        builder.expectAppendTransition(0, 5, "0");
        builder.expectAppendTransition(0, 5, "1");
        builder.expectAppendTransition(0, 6, "0");
        builder.expectLoadFinished();

        new VCDLoader().load(testFile("timestamp-out-of-order.vcd"), builder, null);
    }

    // This tests a few things:
    // - Parsing both single and multibit transitions
    // - X and Z values (multibit like x10200x and splatted with a single x/z)
    // wire definitions with and without space:
    // $var wire 16 A addr [15:0] $end
    // $var wire 3 B data[2:0] $end
    @Test
    public void multibit() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
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

        new VCDLoader().load(testFile("multibit.vcd"), builder, null);
    }

    @Test
    public void padding() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
        builder.expectTimescale(-9);
        builder.expectEnterScope("mod1");
        builder.expectNewNet("value", -1, 16);
        builder.expectExitScope();
        builder.expectAppendTransition(0, 0, "zzzzzzzzzzzx1010");
        builder.expectAppendTransition(0, 1, "xxxxxxxxxxxz1010");
        builder.expectAppendTransition(0, 2, "0000000000001101");
        builder.expectAppendTransition(0, 3, "0000000000000010");
        builder.expectLoadFinished();

        new VCDLoader().load(testFile("padding.vcd"), builder, null);
    }

    // Test that $dumpvars is handled correctly
    @Test
    public void dumpvars() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
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
        new VCDLoader().load(testFile("dumpvars.vcd"), builder, null);
    }

    @Test
    public void unknownNetId() throws Exception {
        try {
            new VCDLoader().load(testFile("unknown-net-id.vcd"), new DummyWaveformBuilder(), null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadException exc) {
            assertEquals("line 6: Unknown net id $", exc.getMessage());
        }
    }

    @Test
    public void invalidValueType() throws Exception {
        try {
            new VCDLoader().load(testFile("bad-transition-type.vcd"), new DummyWaveformBuilder(), null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadException exc) {
            assertEquals("line 6: invalid value type 'q'", exc.getMessage());
        }
    }

    @Test
    public void invalidScope() throws Exception {
        try {
            new VCDLoader().load(testFile("scope-parse-error.vcd"), new DummyWaveformBuilder(), null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadException exc) {
            assertEquals("line 2: parse error, expected $end got $var", exc.getMessage());
        }
    }

    @Test
    public void invalidUpscope() throws Exception {
        try {
            new VCDLoader().load(testFile("upscope-parse-error.vcd"), new DummyWaveformBuilder(), null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadException exc) {
            assertEquals("line 4: parse error, expected $end got $enddefinitions", exc.getMessage());
        }
    }

    @Test
    public void varParseError() throws Exception {
        try {
            new VCDLoader().load(testFile("var-parse-error.vcd"), new DummyWaveformBuilder(), null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadException exc) {
            assertEquals("line 3: parse error, expected $end got $upscope", exc.getMessage());
        }
    }

    @Test
    public void timescaleParseError() throws Exception {
        try {
            new VCDLoader().load(testFile("timescale-parse-error.vcd"), new DummyWaveformBuilder(), null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadException exc) {
            assertEquals("line 3: parse error, expected $end got $scope", exc.getMessage());
        }
    }

    @Test
    public void invalidLogicValue() throws Exception {
        try {
            new VCDLoader().load(testFile("invalid-logic-value.vcd"), new DummyWaveformBuilder(), null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadException exc) {
            assertEquals("line 9: invalid logic value", exc.getMessage());
        }
    }

    @Test
    public void truncatedFile() throws Exception {
        try {
            new VCDLoader().load(testFile("truncated.vcd"), new DummyWaveformBuilder(), null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadException exc) {
            assertEquals("line 2: unexpected end of file", exc.getMessage());
        }
    }

    // This file is spec compliant, but we don't support real values
    @Test
    public void realValueType() throws Exception {
        try {
            new VCDLoader().load(testFile("real-value.vcd"), new DummyWaveformBuilder(), null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadException exc) {
            assertEquals("line 6: real values are not supported", exc.getMessage());
        }
    }

    @Test
    public void waveformAlias() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
        builder.expectEnterScope("mod1");
        builder.expectNewNet("foo", -1, 1);
        builder.expectNewNet("source", -1, 1);
        builder.expectNewNet("alias", 1, 1);
        builder.expectExitScope();
        builder.expectAppendTransition(1, 0, "1");
        builder.expectLoadFinished();

        new VCDLoader().load(testFile("waveform-alias.vcd"), builder, null);
    }

    // Put everything together with more data and multiple signals
    @Test
    public void mixed() throws Exception {
        TestBuilder builder = new TestBuilder();
        builder.addString("$date\n\tMon Aug 15 22:28:13 2016\n$end\n");
        builder.addString("$version\n\tIcarus Verilog\n$end\n");
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
            long time = (long) i * 5;

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
        loader.load(builder.getVCDFile(), builder.getWaveformBuilder(), null);
    }

    static class TestProgressListener implements WaveformLoader.ProgressListener {
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
    public void progressListener() throws Exception {
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
        loader.load(builder.getVCDFile(), builder.getWaveformBuilder(), listener);
        assertNotEquals(-1, listener.fLastUpdate);
    }

    // If the user clicks cancel, the progress listener update
    // method will return false. Ensure this aborts the load.
    @Test
    public void interruptedLoad() throws IOException {
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
            loader.load(builder.getVCDFile(), builder.getWaveformBuilder(), new VCDLoader.ProgressListener() {
                @Override
                public boolean updateProgress(int percentRead) {
                    return false;
                }
            });
            fail("Loader didn't throw exception");
        } catch (WaveformLoader.LoadException exc) {
            assertEquals("load cancelled", exc.getMessage());
        }
    }

    // File produced by Accellera SystemC
    @Test
    public void accellera() throws Exception {
        ExpectWaveformBuilder builder = new ExpectWaveformBuilder();
        builder.expectTimescale(-12);
        builder.expectEnterScope("SystemC");
        builder.expectNewNet("int_val", -1, 32);
        builder.expectNewNet("float_val", -1, 1);
        builder.expectNewNet("clk", -1, 1);
        builder.expectNewNet("rstn", -1, 1);
        builder.expectExitScope();
        builder.expectLoadFinished();
        new VCDLoader().load(testFile("accellera.vcd"), builder, null);
    }
}

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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;
import waveview.wavedata.VcdLoader;
import waveview.wavedata.WaveformBuilder;
import waveview.wavedata.WaveformLoader;

public class VcdLoaderTest {
    @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

    WaveformBuilder builder = mock(WaveformBuilder.class);

    File tempFileFrom(String contents) {
        try {
            File f = tempFolder.newFile("test.vcd");
            OutputStream os = Files.newOutputStream(f.toPath());
            os.write(contents.getBytes(StandardCharsets.US_ASCII));
            os.close();
            return f;
        } catch (IOException exc) {
            // Expected
            fail("Caught I/O exception trying to create temporary file");
            return null;
        }
    }

    File getTestFile(String name) {
        return new File("src/test/resources/vcd/" + name);
    }

    @Test
    public void timescaleFs() throws IOException {
        new VcdLoader().load(getTestFile("timescale-fs.vcd"), builder, null);
        verify(builder).setTimescale(-15);
        verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    @Test
    public void timescalePs() throws IOException {
        new VcdLoader().load(getTestFile("timescale-ps.vcd"), builder, null);
        verify(builder).setTimescale(-12);
        verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    @Test
    public void timescaleNs() throws IOException {
        new VcdLoader().load(getTestFile("timescale-ns.vcd"), builder, null);
        verify(builder).setTimescale(-9);
        verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    @Test
    public void timescaleUs() throws IOException {
        new VcdLoader().load(getTestFile("timescale-us.vcd"), builder, null);
        verify(builder).setTimescale(-6);
        verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    @Test
    public void timescaleMs() throws IOException {
        new VcdLoader().load(getTestFile("timescale-ms.vcd"), builder, null);
        verify(builder).setTimescale(-3);
        verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    @Test
    public void timescaleS() throws IOException {
        new VcdLoader().load(getTestFile("timescale-s.vcd"), builder, null);
        verify(builder).setTimescale(0);
        verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    // Ensure it handles both the unit and the number correctly.
    @Test
    public void timescale10Us() throws IOException {
        new VcdLoader().load(getTestFile("timescale-10us.vcd"), builder, null);
        verify(builder).setTimescale(-5);
        verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    @Test
    public void timescale100Us() throws IOException {
        new VcdLoader().load(getTestFile("timescale-100us.vcd"), builder, null);
        verify(builder).setTimescale(-4);
        verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    // Check that it handles a space between the number and unit in
    // the timescale definition.
    @Test
    public void timescaleSpace() throws IOException {
        new VcdLoader().load(getTestFile("timescale-space.vcd"), builder, null);
        verify(builder).setTimescale(-12);
        verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    // Ensure it doesn't choke when it gets types it doesn't recognize
    // (for example $date, $version)
    @Test
    public void unknownHeaderFields() throws IOException {
        new VcdLoader().load(getTestFile("unknown-header-fields.vcd"), builder, null);

        InOrder ord = inOrder(builder);
        ord.verify(builder).setTimescale(-6);
        ord.verify(builder).enterScope("mod1");
        ord.verify(builder).newNet(0, "clk", 1);
        ord.verify(builder).exitScope();
        ord.verify(builder).appendTransition(eq(0), eq(0L), argThat(new BitVectorMatcher("1")));
        ord.verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    // When a timestamp is out of order, the loader will set it to the
    // last value silently.
    // last value silently.
    @Test
    public void timestampOutOfOrder() throws IOException {
        new VcdLoader().load(getTestFile("timestamp-out-of-order.vcd"), builder, null);

        InOrder ord = inOrder(builder);
        ord.verify(builder).setTimescale(-9);
        ord.verify(builder).enterScope("mod1");
        ord.verify(builder).newNet(0, "foo", 1);
        ord.verify(builder).exitScope();
        ord.verify(builder).appendTransition(eq(0), eq(0L), argThat(new BitVectorMatcher("1")));
        ord.verify(builder).appendTransition(eq(0), eq(5L), argThat(new BitVectorMatcher("0")));
        ord.verify(builder).appendTransition(eq(0), eq(5L), argThat(new BitVectorMatcher("1")));
        ord.verify(builder).appendTransition(eq(0), eq(6L), argThat(new BitVectorMatcher("0")));
        ord.verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    // This tests a few things:
    // - Parsing both single and multibit transitions
    // - X and Z values (multibit like x10200x and splatted with a single x/z)
    // wire definitions with and without space:
    // $var wire 16 A addr [15:0] $end
    // $var wire 3 B data[2:0] $end
    @Test
    public void multibit() throws IOException {
        new VcdLoader().load(getTestFile("multibit.vcd"), builder, null);

        InOrder ord = inOrder(builder);
        ord.verify(builder).setTimescale(-9);
        ord.verify(builder).enterScope("mod1");
        ord.verify(builder).newNet(0, "addr", 16);
        ord.verify(builder).newNet(1, "data", 3);
        ord.verify(builder).newNet(2, "enable", 1);
        ord.verify(builder).exitScope();
        ord.verify(builder).appendTransition(
            eq(0), eq(0L), argThat(new BitVectorMatcher("1010111000101011")));
        ord.verify(builder).appendTransition(eq(1), eq(0L), argThat(new BitVectorMatcher("011")));
        ord.verify(builder).appendTransition(eq(2), eq(0L), argThat(new BitVectorMatcher("z")));
        ord.verify(builder).appendTransition(
            eq(0), eq(5L), argThat(new BitVectorMatcher("1101010010100010")));
        ord.verify(builder).appendTransition(eq(1), eq(5L), argThat(new BitVectorMatcher("100")));
        ord.verify(builder).appendTransition(eq(2), eq(5L), argThat(new BitVectorMatcher("x")));
        ord.verify(builder).appendTransition(
            eq(0), eq(10L), argThat(new BitVectorMatcher("01zxxz10zxzx1010")));
        ord.verify(builder).appendTransition(eq(2), eq(10L), argThat(new BitVectorMatcher("1")));
        ord.verify(builder).appendTransition(
            eq(0), eq(15L), argThat(new BitVectorMatcher("zzzzzzzzzzzzzzzz")));
        ord.verify(builder).appendTransition(eq(1), eq(15L), argThat(new BitVectorMatcher("xxx")));
        ord.verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    @Test
    public void bitSelectNoSpace() throws IOException {
        new VcdLoader().load(getTestFile("bit-select-no-space.vcd"), builder, null);

        InOrder ord = inOrder(builder);
        ord.verify(builder).enterScope("mod1");
        ord.verify(builder).newNet(0, "addr", 16);
        ord.verify(builder).exitScope();
        ord.verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    @Test
    public void bitSelectSpace() throws IOException {
        new VcdLoader().load(getTestFile("bit-select-space.vcd"), builder, null);

        InOrder ord = inOrder(builder);
        ord.verify(builder).enterScope("mod1");
        ord.verify(builder).newNet(0, "addr", 16);
        ord.verify(builder).exitScope();
        ord.verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    @Test
    public void padding() throws IOException {
        new VcdLoader().load(getTestFile("padding.vcd"), builder, null);

        InOrder ord = inOrder(builder);
        ord.verify(builder).setTimescale(-9);
        ord.verify(builder).enterScope("mod1");
        ord.verify(builder).newNet(0, "value", 16);
        ord.verify(builder).exitScope();
        ord.verify(builder).appendTransition(
            eq(0), eq(0L), argThat(new BitVectorMatcher("zzzzzzzzzzzx1010")));
        ord.verify(builder).appendTransition(
            eq(0), eq(1L), argThat(new BitVectorMatcher("xxxxxxxxxxxz1010")));
        ord.verify(builder).appendTransition(
            eq(0), eq(2L), argThat(new BitVectorMatcher("0000000000001101")));
        ord.verify(builder).appendTransition(
            eq(0), eq(3L), argThat(new BitVectorMatcher("0000000000000010")));
        ord.verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    // Test that $dumpvars is handled correctly
    @Test
    public void dumpvars() throws IOException {
        new VcdLoader().load(getTestFile("dumpvars.vcd"), builder, null);

        InOrder ord = inOrder(builder);
        ord.verify(builder).setTimescale(-9);
        ord.verify(builder).enterScope("mod1");
        ord.verify(builder).newNet(0, "addr", 16);
        ord.verify(builder).newNet(1, "data", 3);
        ord.verify(builder).newNet(2, "enable", 1);
        ord.verify(builder).exitScope();
        ord.verify(builder).appendTransition(
            eq(0), eq(0L), argThat(new BitVectorMatcher("1010111000101011")));
        ord.verify(builder).appendTransition(eq(1), eq(0L), argThat(new BitVectorMatcher("011")));
        ord.verify(builder).appendTransition(eq(2), eq(0L), argThat(new BitVectorMatcher("z")));
        ord.verify(builder).appendTransition(
            eq(0), eq(5L), argThat(new BitVectorMatcher("1101010010100010")));
        ord.verify(builder).appendTransition(eq(1), eq(5L), argThat(new BitVectorMatcher("100")));
        ord.verify(builder).appendTransition(eq(2), eq(5L), argThat(new BitVectorMatcher("x")));
        ord.verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    @Test
    public void waveformAlias() throws IOException {
        new VcdLoader().load(getTestFile("waveform-alias.vcd"), builder, null);

        InOrder ord = inOrder(builder);
        ord.verify(builder).enterScope("mod1");
        ord.verify(builder).newNet(0, "foo", 1);
        ord.verify(builder).newNet(1, "source", 1);
        ord.verify(builder).newNet(1, "alias", 1);
        ord.verify(builder).newNet(2, "bar",
            1); // Ensure index incremented properly
        ord.verify(builder).exitScope();
        ord.verify(builder).appendTransition(eq(1), eq(0L), argThat(new BitVectorMatcher("1")));
        ord.verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    static class MockProgressListener implements WaveformLoader.ProgressListener {
        int lastUpdate = -1;

        @Override
        public boolean updateProgress(int percentRead) {
            assertTrue(percentRead >= lastUpdate);
            assertTrue(percentRead >= 0);
            assertTrue(percentRead <= 100);
            lastUpdate = percentRead;
            return true;
        }
    }

    // Write a bunch of data, make sure we get callbacks
    @Test
    public void progressListener() throws IOException {
        // Need to append enough data to cause at least one progress
        // update
        StringBuilder vcdContents = new StringBuilder(0x20000);
        vcdContents.append(
            "$timescale 1ns $end $scope module mod1 $end $var wire 1 A data $end $upscope $end $enddefinitions $end\n");
        for (int i = 0; i < 10000; i++) {
            vcdContents.append('#');
            vcdContents.append(i * 5);
            vcdContents.append("\n1A\n");
        }

        File vcdFile = tempFileFrom(vcdContents.toString());
        MockProgressListener progressListener = new MockProgressListener();
        VcdLoader loader = new VcdLoader();
        loader.load(vcdFile, builder, progressListener);
        assertTrue(progressListener.lastUpdate > 90);
    }

    // If the user clicks cancel, the progress listener update
    // method will return false. Ensure this aborts the load.
    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void interruptedLoad() throws IOException {
        StringBuilder vcdContents = new StringBuilder(0x20000);
        vcdContents.append(
            "$timescale 1ns $end $scope module mod1 $end $var wire 1 A data $end $upscope $end $enddefinitions $end\n");
        for (int i = 0; i < 10000; i++) {
            vcdContents.append('#');
            vcdContents.append(i * 5);
            vcdContents.append("\n1A\n");
        }

        File vcdFile = tempFileFrom(vcdContents.toString());

        try {
            new VcdLoader().load(vcdFile, builder, new VcdLoader.ProgressListener() {
                @Override
                public boolean updateProgress(int percentRead) {
                    return false;
                }
            });
            fail("Loader didn't throw exception");
        } catch (WaveformLoader.LoadFormatException exc) {
            // Expected
            assertEquals("load cancelled", exc.getMessage());
        }
    }

    // File produced by Accellera SystemC. Regression test, this used to have
    // issues loading.
    @Test
    public void accellera() throws IOException {
        InOrder ord = inOrder(builder);

        new VcdLoader().load(getTestFile("accellera.vcd"), builder, null);

        ord.verify(builder).setTimescale(-12);
        ord.verify(builder).enterScope("SystemC");
        ord.verify(builder).newNet(0, "int_val", 32);
        ord.verify(builder).newNet(1, "float_val", 1);
        ord.verify(builder).newNet(2, "clk", 1);
        ord.verify(builder).newNet(3, "rstn", 1);
        ord.verify(builder).exitScope();
        ord.verify(builder).loadFinished();
        verifyNoMoreInteractions(builder);
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void unknownNetId() throws IOException {
        try {
            new VcdLoader().load(getTestFile("unknown-net-id.vcd"), builder, null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadFormatException exc) {
            // Expected
            assertEquals("line 6: Unknown var id $", exc.getMessage());
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void invalidValueType() throws IOException {
        try {
            new VcdLoader().load(getTestFile("bad-transition-type.vcd"), builder, null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadFormatException exc) {
            // Expected
            assertEquals("line 6: invalid value type 'q'", exc.getMessage());
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void invalidScope() throws IOException {
        try {
            new VcdLoader().load(getTestFile("scope-parse-error.vcd"), builder, null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadFormatException exc) {
            // Expected
            assertEquals("line 2: parse error, expected $end got $var", exc.getMessage());
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void invalidUpscope() throws IOException {
        try {
            new VcdLoader().load(getTestFile("upscope-parse-error.vcd"), builder, null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadFormatException exc) {
            // Expected
            assertEquals(
                "line 4: parse error, expected $end got $enddefinitions", exc.getMessage());
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void varParseError() throws IOException {
        try {
            new VcdLoader().load(getTestFile("var-parse-error.vcd"), builder, null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadFormatException exc) {
            // Expected
            assertEquals("line 3: parse error, expected $end got $upscope", exc.getMessage());
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void timescaleParseError() throws IOException {
        try {
            new VcdLoader().load(getTestFile("timescale-parse-error.vcd"), builder, null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadFormatException exc) {
            // Expected
            assertEquals("line 3: parse error, expected $end got $scope", exc.getMessage());
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void invalidLogicValue() throws IOException {
        try {
            new VcdLoader().load(getTestFile("invalid-logic-value.vcd"), builder, null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadFormatException exc) {
            // Expected
            assertEquals("line 9: invalid logic value", exc.getMessage());
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void truncatedFile() throws IOException {
        try {
            new VcdLoader().load(getTestFile("truncated.vcd"), builder, null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadFormatException exc) {
            // Expected
            assertEquals("line 2: unexpected end of file", exc.getMessage());
        }
    }

    // This file is spec compliant, but we don't support real values
    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void realValueType() throws IOException {
        try {
            new VcdLoader().load(getTestFile("real-value.vcd"), builder, null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadFormatException exc) {
            // Expected
            assertEquals("line 6: real values are not supported", exc.getMessage());
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void aliasWidthMismatch() throws IOException {
        try {
            new VcdLoader().load(getTestFile("alias-bad-width.vcd"), builder, null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadFormatException exc) {
            // Expected
            assertEquals(
                "line 3: alias net does not match width of parent (15 != 16)", exc.getMessage());
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void invalidFile() throws IOException {
        try {
            new VcdLoader().load(
                getTestFile("invalid_file_shasdjkfhaldkfhadfhadsjkfhadsf.vcd"), builder, null);
            fail("Didn't throw exception");
        } catch (IOException exc) {
            // Expected
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void unknownTimescale() throws IOException {
        try {
            new VcdLoader().load(getTestFile("unknown-timescale.vcd"), builder, null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadFormatException exc) {
            // Expected
            assertEquals("line 2: unknown timescale value 1qs", exc.getMessage());
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void timescaleMissingUnit() throws IOException {
        try {
            new VcdLoader().load(getTestFile("timescale-missing-unit.vcd"), builder, null);
            fail("Didn't throw exception");
        } catch (WaveformLoader.LoadFormatException exc) {
            // Expected
            assertEquals("line 3: unknown timescale value $end", exc.getMessage());
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    public void timescaleBadValue() throws IOException {
        try {
            new VcdLoader().load(getTestFile("timescale-bad-value.vcd"), builder, null);
            fail("didn't throw exception");
        } catch (WaveformLoader.LoadFormatException exc) {
            // Expected
            assertEquals("line 2: bad timescale value 17us", exc.getMessage());
        }
    }
}

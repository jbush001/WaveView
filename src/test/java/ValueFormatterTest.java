
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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import org.junit.Test;
import waveview.AsciiValueFormatter;
import waveview.BinaryValueFormatter;
import waveview.OctalValueFormatter;
import waveview.DecimalValueFormatter;
import waveview.EnumValueFormatter;
import waveview.HexadecimalValueFormatter;
import waveview.ValueFormatter;
import waveview.wavedata.BitVector;

public class ValueFormatterTest {
    @Test
    public void enumValueFormatter() throws IOException {
        File mappingFile = new File("src/test/resources/enum_mapping/test1.txt");
        EnumValueFormatter vf = new EnumValueFormatter(mappingFile);
        assertEquals("STATE_INIT", vf.format(new BitVector("1", 10)));
        assertEquals("STATE_LOAD", vf.format(new BitVector("2", 10)));
        assertEquals("STATE_WAIT", vf.format(new BitVector("3", 10)));
        assertEquals("??? (4)", vf.format(new BitVector("4", 10)));
    }

    @Test
    public void enumValueFormatterMissingFile() {
        File mappingFile = new File("askfkalsd8unskdgsdfghsdfkgsdfkghsdfgksdfuzxcjk");
        try {
            new EnumValueFormatter(mappingFile);
            fail("didn't throw exception");
        } catch (IOException exc) {
            // Expected
        }
    }

    // Each line should have two parts: a number and an identifier. This
    // one has more than two.
    @Test
    public void enumValueFormatterTooManyTokens() throws IOException {
        File mappingFile = new File("src/test/resources/enum_mapping/too-many-tokens.txt");
        try {
            new EnumValueFormatter(mappingFile);
            fail("didn't throw exception");
        } catch (EnumValueFormatter.FormatException exc) {
            // Expected
            assertEquals("Line 2 parse error", exc.getMessage());
        }
    }

    // Same as above, except missing identifier
    @Test
    public void enumValueFormatterTooFewTokens() throws IOException {
        File mappingFile = new File("src/test/resources/enum_mapping/too-many-tokens.txt");
        try {
            new EnumValueFormatter(mappingFile);
            fail("didn't throw exception");
        } catch (EnumValueFormatter.FormatException exc) {
            // Expected
            assertEquals("Line 2 parse error", exc.getMessage());
        }
    }

    // First token can't be parsed as integer
    @Test
    public void enumValueFormatterNotNumber() throws IOException {
        File mappingFile = new File("src/test/resources/enum_mapping/not-number.txt");
        try {
            new EnumValueFormatter(mappingFile);
            fail("didn't throw exception");
        } catch (EnumValueFormatter.FormatException exc) {
            // Expected
            assertEquals("Line 2 invalid number format", exc.getMessage());
        }
    }

    @Test
    public void binaryValueFormatter() {
        ValueFormatter bf = new BinaryValueFormatter();
        assertEquals("0101zxz110", bf.format(new BitVector("0101zxz110", 2)));
    }

    @Test
    public void octalValueFormatter() {
        ValueFormatter bf = new OctalValueFormatter();
        assertEquals("1234567", bf.format(new BitVector("1234567", 8)));
    }

    @Test
    public void asciiValueFormatter() {
        ValueFormatter bf = new AsciiValueFormatter();
        assertEquals("A", bf.format(new BitVector("01000001", 2)));
        assertEquals("!", bf.format(new BitVector("00100001", 2)));
        assertEquals("{", bf.format(new BitVector("01111011", 2)));
    }

    @Test
    public void decimalValueFormatter() {
        ValueFormatter df = new DecimalValueFormatter();
        assertEquals(
            "123843954387523457345345", df.format(new BitVector("123843954387523457345345", 10)));
    }

    @Test
    public void hexadecimalValueFormatter() {
        ValueFormatter hf = new HexadecimalValueFormatter();
        assertEquals("ABCDEF12345678", hf.format(new BitVector("ABCDEF12345678", 16)));
    }
}

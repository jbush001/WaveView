
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
import java.io.File;
import java.io.IOException;
import org.junit.Test;
import waveview.ASCIIValueFormatter;
import waveview.BinaryValueFormatter;
import waveview.BitVector;
import waveview.DecimalValueFormatter;
import waveview.EnumValueFormatter;
import waveview.HexadecimalValueFormatter;

public class ValueFormatterTest {
    File getTestFile(String name) {
        return new File("src/test/resources/enum_mapping/" + name);
    }

    @Test
    public void testEnumValueFormatter() throws IOException {
        EnumValueFormatter vf = new EnumValueFormatter(getTestFile("test1.txt"));
        assertEquals("STATE_INIT", vf.format(new BitVector("1", 10)));
        assertEquals("STATE_LOAD", vf.format(new BitVector("2", 10)));
        assertEquals("STATE_WAIT", vf.format(new BitVector("3", 10)));
        assertEquals("??? (4)", vf.format(new BitVector("4", 10)));
    }

    @Test
    public void testBinaryValueFormatter() {
        BinaryValueFormatter bf = new BinaryValueFormatter();
        assertEquals("0101zxz110", bf.format(new BitVector("0101zxz110", 2)));
    }

    @Test
    public void testASCIIValueFormatter() {
        ASCIIValueFormatter bf = new ASCIIValueFormatter();
        assertEquals("A", bf.format(new BitVector("01000001", 2)));
        assertEquals("!", bf.format(new BitVector("00100001", 2)));
        assertEquals("{", bf.format(new BitVector("01111011", 2)));
    }

    @Test
    public void testDecimalValueFormatter() {
        DecimalValueFormatter df = new DecimalValueFormatter();
        assertEquals("123843954387523457345345", df.format(new BitVector("123843954387523457345345", 10)));
    }

    @Test
    public void testHexadecimalValueFormatter() {
        HexadecimalValueFormatter hf = new HexadecimalValueFormatter();
        assertEquals("ABCDEF12345678", hf.format(new BitVector("ABCDEF12345678", 16)));
    }
}

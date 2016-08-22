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

public class EnumValueFormatterTest {
    @Test
    public void testFormat() {
        EnumValueFormatter vf = new EnumValueFormatter();
        vf.addMapping(11, "primero");
        vf.addMapping(13, "segundo");
        vf.addMapping(17, "tercero");
        assertEquals(3, vf.getMappingCount());

        assertEquals(11, vf.getValueByIndex(0));
        assertEquals("primero", vf.getNameByIndex(0));
        assertEquals(13, vf.getValueByIndex(1));
        assertEquals("segundo", vf.getNameByIndex(1));
        assertEquals(17, vf.getValueByIndex(2));
        assertEquals("tercero", vf.getNameByIndex(2));

        vf.setValueAtIndex(1, 12);
        vf.setNameAtIndex(1, "secondo");
        assertEquals(12, vf.getValueByIndex(1));
        assertEquals("secondo", vf.getNameByIndex(1));

        assertEquals("primero", vf.format(new BitVector("1011", 2)));
        assertEquals("secondo", vf.format(new BitVector("1100", 2)));
        assertEquals("??? (13)", vf.format(new BitVector("1101", 2)));
        assertEquals("tercero", vf.format(new BitVector("10001", 2)));
        assertEquals("??? (256)", vf.format(new BitVector("100000000", 2)));
    }
}

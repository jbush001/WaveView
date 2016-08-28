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

public class EnumMappingTableModelTest {

    @Test
    public void test() {
        EnumValueFormatter evf = new EnumValueFormatter();
        evf.addMapping(0, "STATE_INIT");
        evf.addMapping(1, "STATE_READ");
        evf.addMapping(2, "STATE_WRITE");
        evf.addMapping(3, "STATE_WAIT");

        EnumMappingTableModel mtm = new EnumMappingTableModel(evf);
        assertEquals("value", mtm.getColumnName(0));
        assertEquals("name", mtm.getColumnName(1));

        // There is an extra row at the bottom that allows creating
        // a new mapping
        assertEquals(5, mtm.getRowCount());

        assertEquals(2, mtm.getColumnCount());

        // Values
        assertEquals("0", mtm.getValueAt(0, 0));
        assertEquals("1", mtm.getValueAt(1, 0));
        assertEquals("2", mtm.getValueAt(2, 0));
        assertEquals("3", mtm.getValueAt(3, 0));
        assertEquals("", mtm.getValueAt(4, 0));

        // Names
        assertEquals("STATE_INIT", (String) mtm.getValueAt(0, 1));
        assertEquals("STATE_READ", (String) mtm.getValueAt(1, 1));
        assertEquals("STATE_WRITE", (String) mtm.getValueAt(2, 1));
        assertEquals("STATE_WAIT", (String) mtm.getValueAt(3, 1));
        assertEquals("", (String) mtm.getValueAt(4, 1));

        assertTrue(mtm.isCellEditable(0, 0));
        assertTrue(mtm.isCellEditable(0, 1));
        assertTrue(mtm.isCellEditable(1, 0));
        assertTrue(mtm.isCellEditable(1, 1));

        mtm.setValueAt("4", 3, 0);
        assertEquals(4, evf.getValue(3));
        mtm.setValueAt("STATE_STALL", 3, 1);
        assertEquals("STATE_STALL", evf.getName(3));

        assertEquals(5, mtm.getRowCount());
        mtm.setValueAt("5", 4, 0);
        assertEquals(5, evf.getValue(4));

        // The above will add a new blank row at the bottom
        assertEquals(6, mtm.getRowCount());
    }
}
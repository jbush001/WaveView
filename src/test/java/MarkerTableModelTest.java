
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import waveview.MarkerTableModel;
import waveview.WaveformPresentationModel;

public class MarkerTableModelTest {
    private final WaveformPresentationModel tdm =
        new WaveformPresentationModel();
    private MarkerTableModel mtm;

    @Before
    public void initTest() {
        mtm = new MarkerTableModel(tdm);
    }

    @Test
    public void emptyTable() {
        assertEquals(3, mtm.getColumnCount());
        assertEquals("ID", mtm.getColumnName(0));
        assertEquals("Timestamp", mtm.getColumnName(1));
        assertEquals("Comment", mtm.getColumnName(2));
        assertEquals(0, mtm.getRowCount());
    }

    @Test
    public void readMarkers() {
        tdm.addMarker("foo", 1234);
        tdm.addMarker("bar", 5678);

        assertEquals(2, mtm.getRowCount());
        assertEquals("1", (String)mtm.getValueAt(0, 0));
        assertEquals("1234", (String)mtm.getValueAt(0, 1));
        assertEquals("foo", (String)mtm.getValueAt(0, 2));

        assertEquals("2", (String)mtm.getValueAt(1, 0));
        assertEquals("5678", (String)mtm.getValueAt(1, 1));
        assertEquals("bar", (String)mtm.getValueAt(1, 2));

        assertFalse(mtm.isCellEditable(0, 0));
        assertFalse(mtm.isCellEditable(0, 1));
        assertTrue(mtm.isCellEditable(0, 2));
        assertFalse(mtm.isCellEditable(1, 0));
        assertFalse(mtm.isCellEditable(1, 1));
        assertTrue(mtm.isCellEditable(1, 2));
    }

    @Test
    public void setValueAt() {
        tdm.addMarker("foo", 1234);
        tdm.addMarker("bar", 5678);
        mtm.setValueAt("baz", 1, 2);
        assertEquals("baz", (String)mtm.getValueAt(1, 2));
    }

    // Column index past end, should check and return empty string.
    @Test
    public void invalidColumn() {
        assertEquals("", (String)mtm.getValueAt(1, 3));
    }
}

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
import static org.junit.Assert.*;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;

public class NetSearchListModelAdapterTest {

    class TestListDataListener implements ListDataListener {
        TestListDataListener() {
        }

        public void contentsChanged(ListDataEvent evt) {
            fGotEvent = true;
            assertEquals(0, evt.getIndex0());
            fEventSize = evt.getIndex1();
        }


        public void intervalAdded(ListDataEvent evt) {
            fail("unexpected intervalAdded");
        }

        public void intervalRemoved(ListDataEvent evt) {
            fail("unexpected intervalAdded");
        }

        void checkEvent(int size) {
            assertTrue(fGotEvent);
            assertEquals(size, fEventSize);
            fGotEvent = false;  // Reset for next check
        }

        boolean fGotEvent;
        int fEventSize;
    }

    @Test
    public void testFilter()
    {
        // Build a dummy trace data model
        TraceDataModel model = new TraceDataModel();
        TraceBuilder builder = model.startBuilding();

        builder.enterModule("mod1");
        builder.newNet("fooxxx", -1, 1);
        builder.newNet("xfooxx", -1, 1);
        builder.newNet("xxfoox", -1, 1);
        builder.newNet("xxxfoo", -1, 1);
        builder.newNet("bbbbb", -1, 1);
        builder.newNet("bbbb", -1, 1);
        builder.newNet("bbb", -1, 1);
        builder.newNet("bb", -1, 1);
        builder.newNet("yyy", -1, 1);
        builder.exitModule();

        TestListDataListener listener = new TestListDataListener();
        NetSearchListModelAdapter nslma = new NetSearchListModelAdapter(model);
        nslma.addListDataListener(listener);

        assertEquals(9, nslma.getSize());
        assertEquals("mod1.fooxxx", nslma.getElementAt(0));
        assertEquals("mod1.xfooxx", nslma.getElementAt(1));
        assertEquals("mod1.xxfoox", nslma.getElementAt(2));
        assertEquals("mod1.xxxfoo", nslma.getElementAt(3));
        assertEquals("mod1.bbbbb", nslma.getElementAt(4));
        assertEquals("mod1.bbbb", nslma.getElementAt(5));
        assertEquals("mod1.bbb", nslma.getElementAt(6));
        assertEquals("mod1.bb", nslma.getElementAt(7));
        assertEquals("mod1.yyy", nslma.getElementAt(8));

        nslma.setPattern("foo");
        listener.checkEvent(4);
        assertEquals(4, nslma.getSize());
        assertEquals("mod1.fooxxx", nslma.getElementAt(0));
        assertEquals("mod1.xfooxx", nslma.getElementAt(1));
        assertEquals("mod1.xxfoox", nslma.getElementAt(2));
        assertEquals("mod1.xxxfoo", nslma.getElementAt(3));

        nslma.setPattern("bbbb");
        listener.checkEvent(2);
        assertEquals(2, nslma.getSize());
        assertEquals("mod1.bbbbb", nslma.getElementAt(0));
        assertEquals("mod1.bbbb", nslma.getElementAt(1));

        nslma.setPattern("y");
        listener.checkEvent(1);
        assertEquals(1, nslma.getSize());
        assertEquals("mod1.yyy", nslma.getElementAt(0));

        nslma.setPattern("z");
        listener.checkEvent(0);
        assertEquals(0, nslma.getSize());
    }
}


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
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.junit.Before;
import org.junit.Test;
import waveview.NetSearchListModelAdapter;
import waveview.TraceBuilder;
import waveview.TraceDataModel;

public class NetSearchListModelAdapterTest {
    private final TraceDataModel model = new TraceDataModel();
    private final TestListDataListener listener = new TestListDataListener();
    NetSearchListModelAdapter nslma;

    @Before
    public void setUpTest() {
        TraceBuilder builder = model.startBuilding();
        builder.enterScope("mod1");
        builder.newNet("fooxxx", -1, 1);
        builder.newNet("xfooxx", -1, 1);
        builder.newNet("xxfoox", -1, 1);
        builder.newNet("xxxfoo", -1, 1);
        builder.newNet("bbbbb", -1, 1);
        builder.newNet("bbbb", -1, 1);
        builder.newNet("bbb", -1, 1);
        builder.newNet("bb", -1, 1);
        builder.newNet("yyy", -1, 1);
        builder.exitScope();

        nslma = new NetSearchListModelAdapter(model);
        nslma.addListDataListener(listener);
    }

    static class TestListDataListener implements ListDataListener {
        boolean gotEvent;
        int eventSize;

        TestListDataListener() {
        }

        @Override
        public void contentsChanged(ListDataEvent evt) {
            gotEvent = true;
            assertEquals(0, evt.getIndex0());
            eventSize = evt.getIndex1();
        }

        @Override
        public void intervalAdded(ListDataEvent evt) {
            fail("unexpected intervalAdded");
        }

        @Override
        public void intervalRemoved(ListDataEvent evt) {
            fail("unexpected intervalAdded");
        }

        void checkEvent(int size) {
            assertTrue(gotEvent);
            assertEquals(size, eventSize);
            gotEvent = false; // Reset for next check
        }
    }

    @Test
    public void defaultPattern() {
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
    }

    @Test
    public void testPartialMatch1() {
        nslma.setPattern("foo");
        listener.checkEvent(4);
        assertEquals(4, nslma.getSize());
        assertEquals("mod1.fooxxx", nslma.getElementAt(0));
        assertEquals("mod1.xfooxx", nslma.getElementAt(1));
        assertEquals("mod1.xxfoox", nslma.getElementAt(2));
        assertEquals("mod1.xxxfoo", nslma.getElementAt(3));
    }

    @Test
    public void testPartialMatch2() {
        nslma.setPattern("bbbb");
        listener.checkEvent(2);
        assertEquals(2, nslma.getSize());
        assertEquals("mod1.bbbbb", nslma.getElementAt(0));
        assertEquals("mod1.bbbb", nslma.getElementAt(1));
    }

    @Test
    public void testNoMatch() {
        nslma.setPattern("z");
        listener.checkEvent(0);
        assertEquals(0, nslma.getSize());
    }
}

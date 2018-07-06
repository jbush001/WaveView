
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import waveview.DecimalValueFormatter;
import waveview.NetDataModel;
import waveview.TraceDisplayModel;
import waveview.TransitionVector;

public class TraceDisplayModelTest {
    private final TraceDisplayModel tdm = new TraceDisplayModel();
    private final TraceDisplayModelListener listener = new TraceDisplayModelListener();

    @Before
    public void setUpTest() {
        tdm.addListener(listener);
    }

    @Test
    public void scaleChange() {
        tdm.setHorizontalScale(123.0);
        assertEquals(TraceDisplayModelListener.SCALE_CHANGED, listener.notifications);
        assertEquals(123.0, listener.doubleArg, 0.0);
        assertEquals(123.0, tdm.getHorizontalScale(), 0.0);
    }

    @Test
    public void cursorChange() {
        tdm.setCursorPosition(1024);
        assertEquals(TraceDisplayModelListener.CURSOR_CHANGED, listener.notifications);
        assertEquals(0, listener.longArg0);
        assertEquals(1024, listener.longArg1);
    }

    @Test
    public void makeNetVisible() {
        NetDataModel net1 = new NetDataModel("net1", "net1", new TransitionVector(1));

        // Add a net
        tdm.makeNetVisible(net1);
        assertEquals(TraceDisplayModelListener.NETS_ADDED, listener.notifications);
        assertEquals(0, listener.longArg0);
        assertEquals(0, listener.longArg1);
        assertEquals(1, tdm.getVisibleNetCount());
        assertSame(net1, tdm.getVisibleNet(0));
    }

    @Test
    public void reorderNets() {
        NetDataModel[] nets = new NetDataModel[6];
        for (int i = 0; i < nets.length; i++) {
            String name = "net" + i;
            nets[i] = new NetDataModel(name, name, new TransitionVector(1));
            tdm.makeNetVisible(nets[i]);
        }

        // Ensure some elements are below the insertion index to check that
        // it adjusts properly.
        // original: 0 1 2 3 4 5
        // after: 0 2 1 4 3 5
        int[] indices = { 1, 4 };
        listener.reset();
        tdm.moveNets(indices, 3);
        assertEquals(TraceDisplayModelListener.NETS_REMOVED | TraceDisplayModelListener.NETS_ADDED,
            listener.notifications);
        // XXX doesn't check parameters (multiple notifications from this)

        assertEquals(6, tdm.getVisibleNetCount());
        assertSame(nets[0], tdm.getVisibleNet(0));
        assertSame(nets[2], tdm.getVisibleNet(1));
        assertSame(nets[1], tdm.getVisibleNet(2));
        assertSame(nets[4], tdm.getVisibleNet(3));
        assertSame(nets[3], tdm.getVisibleNet(4));
        assertSame(nets[5], tdm.getVisibleNet(5));
    }

    @Test
    public void removeNet() {
        NetDataModel net1 = new NetDataModel("net1", "net1", new TransitionVector(1));
        NetDataModel net2 = new NetDataModel("net2", "net2", new TransitionVector(1));
        NetDataModel net3 = new NetDataModel("net3", "net3", new TransitionVector(1));
        tdm.makeNetVisible(net1);
        tdm.makeNetVisible(net2);
        tdm.makeNetVisible(net3);
        listener.reset();

        tdm.removeNet(1);

        assertEquals(TraceDisplayModelListener.NETS_REMOVED, listener.notifications);
        assertEquals(1, listener.longArg0); // low index
        assertEquals(1, listener.longArg1); // high index
        assertEquals(2, tdm.getVisibleNetCount());
        assertSame(net1, tdm.getVisibleNet(0));
        assertSame(net3, tdm.getVisibleNet(1));
    }

    @Test
    public void removeAllNets() {
        NetDataModel net1 = new NetDataModel("net1", "net1", new TransitionVector(1));
        NetDataModel net2 = new NetDataModel("net2", "net2", new TransitionVector(1));
        NetDataModel net3 = new NetDataModel("net3", "net3", new TransitionVector(1));
        tdm.makeNetVisible(net1);
        tdm.makeNetVisible(net2);
        tdm.makeNetVisible(net3);
        tdm.addMarker("a_marker", 1000);
        listener.reset();

        tdm.removeAllNets();

        assertEquals(TraceDisplayModelListener.NETS_REMOVED, listener.notifications);
        assertEquals(0, listener.longArg0); // Low index
        assertEquals(2, listener.longArg1); // High index
        assertEquals(0, tdm.getVisibleNetCount());

        // Ensure this didn't remove a marker
        assertEquals(1, tdm.getMarkerCount());
    }

    // When removing all nets on from an empty model, don't create
    // a notification with a negative index. This was causing an exception
    // that broke file loading.
    @Test
    public void removeAllEmpty() {
        tdm.removeAllNets();
        assertEquals(0, listener.notifications);
    }

    @Test
    public void clear() {
        NetDataModel net1 = new NetDataModel("net1", "net1", new TransitionVector(1));
        NetDataModel net2 = new NetDataModel("net2", "net2", new TransitionVector(1));

        tdm.makeNetVisible(net1);
        tdm.makeNetVisible(net2);
        listener.reset();

        tdm.clear();

        assertEquals(TraceDisplayModelListener.NETS_REMOVED, listener.notifications);
        assertEquals(0, listener.longArg0);
        assertEquals(2, listener.longArg1);
        assertEquals(0, tdm.getVisibleNetCount());
    }

    @Test
    public void saveRestoreNetSet() {
        NetDataModel net1 = new NetDataModel("net1", "net1", new TransitionVector(1));
        NetDataModel net2 = new NetDataModel("net2", "net2", new TransitionVector(1));
        NetDataModel net3 = new NetDataModel("net3", "net3", new TransitionVector(1));
        NetDataModel net4 = new NetDataModel("net4", "net4", new TransitionVector(1));
        NetDataModel net5 = new NetDataModel("net5", "net5", new TransitionVector(1));
        tdm.makeNetVisible(net1);
        tdm.makeNetVisible(net2);

        tdm.saveNetSet("set1");
        listener.reset();
        tdm.removeAllNets();
        tdm.makeNetVisible(net3);
        tdm.makeNetVisible(net4);
        tdm.makeNetVisible(net5);
        tdm.selectNetSet(0);

        assertEquals(TraceDisplayModelListener.NETS_ADDED | TraceDisplayModelListener.NETS_REMOVED,
                listener.notifications);
        assertEquals(0, listener.longArg0); // First index (of added nets)
        assertEquals(1, listener.longArg1); // Last index (not count)
        assertEquals(2, tdm.getVisibleNetCount());
        assertSame(net1, tdm.getVisibleNet(0));
        assertSame(net2, tdm.getVisibleNet(1));
    }

    @Test
    public void updateNetSet() {
        NetDataModel net1 = new NetDataModel("net1", "net1", new TransitionVector(1));
        NetDataModel net2 = new NetDataModel("net2", "net2", new TransitionVector(1));
        NetDataModel net3 = new NetDataModel("net3", "net3", new TransitionVector(1));
        tdm.makeNetVisible(net1);
        tdm.makeNetVisible(net2);
        tdm.saveNetSet("set2");

        tdm.makeNetVisible(net3);
        tdm.saveNetSet("set2");
        tdm.removeAllNets();
        tdm.selectNetSet(0);

        assertEquals(3, tdm.getVisibleNetCount());
        assertSame(net1, tdm.getVisibleNet(0));
        assertSame(net2, tdm.getVisibleNet(1));
        assertSame(net3, tdm.getVisibleNet(2));
    }

    @Test
    public void zeroMinorTick() {
        tdm.setHorizontalScale(100);
        assertEquals(1, tdm.getMinorTickInterval());
    }

    @Test
    public void setGetValueFormatter() {
        NetDataModel net1 = new NetDataModel("net1", "net1", new TransitionVector(1));
        NetDataModel net2 = new NetDataModel("net2", "net2", new TransitionVector(1));
        tdm.makeNetVisible(net1);
        tdm.makeNetVisible(net2);
        listener.reset();

        DecimalValueFormatter dvf = new DecimalValueFormatter();
        tdm.setValueFormatter(1, dvf);

        assertEquals(TraceDisplayModelListener.FORMAT_CHANGED, listener.notifications);
        assertEquals(1, listener.longArg0);
        assertTrue(tdm.getValueFormatter(1) == dvf);
    }

    // Ensure the minor tick interval is initialized when TraceDisplayModel
    // is created.
    @Test
    public void minorTickInit() {
        assertNotEquals(0, tdm.getMinorTickInterval(), 0);
    }

    // Ensure this records multiple listeners properly. We don't test
    // that all notification types will notify all listeners.
    @Test
    public void multipleListeners() {
        TraceDisplayModelListener listener2 = new TraceDisplayModelListener();
        tdm.addListener(listener2);
        tdm.setHorizontalScale(1);
        assertEquals(TraceDisplayModelListener.SCALE_CHANGED, listener.notifications);
        assertEquals(TraceDisplayModelListener.SCALE_CHANGED, listener2.notifications);
    }

    @Test
    public void adjustingCursor() {
        tdm.setAdjustingCursor(true);
        assertTrue(tdm.isAdjustingCursor());
        tdm.setAdjustingCursor(false);
        assertFalse(tdm.isAdjustingCursor());
    }
}

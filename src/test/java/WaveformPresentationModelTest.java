
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
import waveview.WaveformPresentationModel;
import waveview.TransitionVector;

public class WaveformPresentationModelTest {
    private final WaveformPresentationModel model = new WaveformPresentationModel();
    private final WaveformPresentationModelListener listener = new WaveformPresentationModelListener();

    @Before
    public void setUpTest() {
        model.addListener(listener);
    }

    @Test
    public void scaleChange() {
        model.setHorizontalScale(123.0);
        assertEquals(WaveformPresentationModelListener.SCALE_CHANGED, listener.notifications);
        assertEquals(123.0, listener.doubleArg, 0.0);
        assertEquals(123.0, model.getHorizontalScale(), 0.0);
    }

    @Test
    public void cursorChange() {
        model.setCursorPosition(1024);
        assertEquals(WaveformPresentationModelListener.CURSOR_CHANGED, listener.notifications);
        assertEquals(0, listener.longArg0);
        assertEquals(1024, listener.longArg1);
    }

    @Test
    public void addNet() {
        NetDataModel net1 = new NetDataModel("net1", "net1", new TransitionVector(1));

        model.addNet(net1);

        assertEquals(WaveformPresentationModelListener.NETS_ADDED, listener.notifications);
        assertEquals(0, listener.longArg0);
        assertEquals(0, listener.longArg1);
        assertEquals(1, model.getVisibleNetCount());
        assertSame(net1, model.getVisibleNet(0));
    }

    @Test
    public void reorderNets() {
        NetDataModel[] nets = new NetDataModel[6];
        for (int i = 0; i < nets.length; i++) {
            String name = "net" + i;
            nets[i] = new NetDataModel(name, name, new TransitionVector(1));
            model.addNet(nets[i]);
        }

        // Ensure some elements are below the insertion index to check that
        // it adjusts properly.
        // original: 0 1 2 3 4 5
        // after: 0 2 1 4 3 5
        int[] indices = { 1, 4 };
        listener.reset();
        model.moveNets(indices, 3);
        assertEquals(WaveformPresentationModelListener.NETS_REMOVED | WaveformPresentationModelListener.NETS_ADDED,
            listener.notifications);
        // XXX doesn't check parameters (multiple notifications from this)

        assertEquals(6, model.getVisibleNetCount());
        assertSame(nets[0], model.getVisibleNet(0));
        assertSame(nets[2], model.getVisibleNet(1));
        assertSame(nets[1], model.getVisibleNet(2));
        assertSame(nets[4], model.getVisibleNet(3));
        assertSame(nets[3], model.getVisibleNet(4));
        assertSame(nets[5], model.getVisibleNet(5));
    }

    @Test
    public void removeNet() {
        NetDataModel net1 = new NetDataModel("net1", "net1", new TransitionVector(1));
        NetDataModel net2 = new NetDataModel("net2", "net2", new TransitionVector(1));
        NetDataModel net3 = new NetDataModel("net3", "net3", new TransitionVector(1));
        model.addNet(net1);
        model.addNet(net2);
        model.addNet(net3);
        listener.reset();

        model.removeNet(1);

        assertEquals(WaveformPresentationModelListener.NETS_REMOVED, listener.notifications);
        assertEquals(1, listener.longArg0); // low index
        assertEquals(1, listener.longArg1); // high index
        assertEquals(2, model.getVisibleNetCount());
        assertSame(net1, model.getVisibleNet(0));
        assertSame(net3, model.getVisibleNet(1));
    }

    @Test
    public void removeAllNets() {
        NetDataModel net1 = new NetDataModel("net1", "net1", new TransitionVector(1));
        NetDataModel net2 = new NetDataModel("net2", "net2", new TransitionVector(1));
        NetDataModel net3 = new NetDataModel("net3", "net3", new TransitionVector(1));
        model.addNet(net1);
        model.addNet(net2);
        model.addNet(net3);
        model.addMarker("a_marker", 1000);
        listener.reset();

        model.removeAllNets();

        assertEquals(WaveformPresentationModelListener.NETS_REMOVED, listener.notifications);
        assertEquals(0, listener.longArg0); // Low index
        assertEquals(2, listener.longArg1); // High index
        assertEquals(0, model.getVisibleNetCount());

        // Ensure this didn't remove a marker
        assertEquals(1, model.getMarkerCount());
    }

    // When removing all nets on from an empty model, don't create
    // a notification with a negative index. This was causing an exception
    // that broke file loading.
    @Test
    public void removeAllEmpty() {
        model.removeAllNets();
        assertEquals(0, listener.notifications);
    }

    @Test
    public void clear() {
        NetDataModel net1 = new NetDataModel("net1", "net1", new TransitionVector(1));
        NetDataModel net2 = new NetDataModel("net2", "net2", new TransitionVector(1));

        model.addNet(net1);
        model.addNet(net2);
        listener.reset();

        model.clear();

        assertEquals(WaveformPresentationModelListener.NETS_REMOVED, listener.notifications);
        assertEquals(0, listener.longArg0);
        assertEquals(2, listener.longArg1);
        assertEquals(0, model.getVisibleNetCount());
    }

    @Test
    public void saveRestoreNetSet() {
        NetDataModel net1 = new NetDataModel("net1", "net1", new TransitionVector(1));
        NetDataModel net2 = new NetDataModel("net2", "net2", new TransitionVector(1));
        NetDataModel net3 = new NetDataModel("net3", "net3", new TransitionVector(1));
        NetDataModel net4 = new NetDataModel("net4", "net4", new TransitionVector(1));
        NetDataModel net5 = new NetDataModel("net5", "net5", new TransitionVector(1));
        model.addNet(net1);
        model.addNet(net2);

        model.saveNetSet("set1");
        listener.reset();
        model.removeAllNets();
        model.addNet(net3);
        model.addNet(net4);
        model.addNet(net5);
        model.selectNetSet(0);

        assertEquals(WaveformPresentationModelListener.NETS_ADDED | WaveformPresentationModelListener.NETS_REMOVED,
                listener.notifications);
        assertEquals(0, listener.longArg0); // First index (of added nets)
        assertEquals(1, listener.longArg1); // Last index (not count)
        assertEquals(2, model.getVisibleNetCount());
        assertSame(net1, model.getVisibleNet(0));
        assertSame(net2, model.getVisibleNet(1));
    }

    @Test
    public void updateNetSet() {
        NetDataModel net1 = new NetDataModel("net1", "net1", new TransitionVector(1));
        NetDataModel net2 = new NetDataModel("net2", "net2", new TransitionVector(1));
        NetDataModel net3 = new NetDataModel("net3", "net3", new TransitionVector(1));
        model.addNet(net1);
        model.addNet(net2);
        model.saveNetSet("set2");

        model.addNet(net3);
        model.saveNetSet("set2");
        model.removeAllNets();
        model.selectNetSet(0);

        assertEquals(3, model.getVisibleNetCount());
        assertSame(net1, model.getVisibleNet(0));
        assertSame(net2, model.getVisibleNet(1));
        assertSame(net3, model.getVisibleNet(2));
    }

    @Test
    public void zeroMinorTick() {
        model.setHorizontalScale(100);
        assertEquals(1, model.getMinorTickInterval());
    }

    @Test
    public void setGetValueFormatter() {
        NetDataModel net1 = new NetDataModel("net1", "net1", new TransitionVector(1));
        NetDataModel net2 = new NetDataModel("net2", "net2", new TransitionVector(1));
        model.addNet(net1);
        model.addNet(net2);
        listener.reset();

        DecimalValueFormatter dvf = new DecimalValueFormatter();
        model.setValueFormatter(1, dvf);

        assertEquals(WaveformPresentationModelListener.FORMAT_CHANGED, listener.notifications);
        assertEquals(1, listener.longArg0);
        assertTrue(model.getValueFormatter(1) == dvf);
    }

    // Ensure the minor tick interval is initialized when WaveformPresentationModel
    // is created.
    @Test
    public void minorTickInit() {
        assertNotEquals(0, model.getMinorTickInterval(), 0);
    }

    // Ensure this records multiple listeners properly. We don't test
    // that all notification types will notify all listeners.
    @Test
    public void multipleListeners() {
        WaveformPresentationModelListener listener2 = new WaveformPresentationModelListener();
        model.addListener(listener2);
        model.setHorizontalScale(1);
        assertEquals(WaveformPresentationModelListener.SCALE_CHANGED, listener.notifications);
        assertEquals(WaveformPresentationModelListener.SCALE_CHANGED, listener2.notifications);
    }

    @Test
    public void adjustingCursor() {
        model.setAdjustingCursor(true);
        assertTrue(model.isAdjustingCursor());
        model.setAdjustingCursor(false);
        assertFalse(model.isAdjustingCursor());
    }
}

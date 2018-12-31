
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
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Before;
import org.junit.Test;
import waveview.DecimalValueFormatter;
import waveview.NetDataModel;
import waveview.TransitionVector;
import waveview.WaveformPresentationModel;

public class WaveformPresentationModelTest {
    private final WaveformPresentationModel model = new WaveformPresentationModel();
    private final WaveformPresentationModel.Listener listener = mock(WaveformPresentationModel.Listener.class);

    @Before
    public void setUpTest() {
        model.addListener(listener);
    }

    @Test
    public void scaleChange() {
        model.setHorizontalScale(123.0);

        verify(listener).scaleChanged(123.0);
        verifyNoMoreInteractions(listener);
        assertEquals(123.0, model.getHorizontalScale(), 0.0);
    }

    @Test
    public void cursorChange() {
        model.setCursorPosition(1024);

        verify(listener).cursorChanged(0, 1024);
        verifyNoMoreInteractions(listener);
        assertEquals(1024, model.getCursorPosition());
    }

    @Test
    public void addNet() {
        NetDataModel newNet = new NetDataModel("net1", "net1", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        model.addNet(newNet);

        verify(listener).netsAdded(0, 0);
        verifyNoMoreInteractions(listener);
        assertEquals(1, model.getVisibleNetCount());
        assertSame(newNet, model.getVisibleNet(0));
    }

    @Test
    public void reorderNets() {
        NetDataModel[] nets = new NetDataModel[6];
        for (int i = 0; i < nets.length; i++) {
            String name = "net" + i;
            nets[i] = new NetDataModel(name, name, TransitionVector.Builder.createBuilder(1).getTransitionVector());
            model.addNet(nets[i]);
        }

        clearInvocations(listener);

        // Ensure some elements are below the insertion index to check that
        // it adjusts properly.
        // original: 0 1 2 3 4 5
        // after: 0 2 1 4 3 5
        int[] indices = { 1, 4 };
        model.moveNets(indices, 3);

        verify(listener).netsRemoved(1, 1);
        verify(listener).netsRemoved(4, 4);
        verify(listener).netsAdded(2, 3);
        verifyNoMoreInteractions(listener);
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
        NetDataModel net1 = new NetDataModel("net1", "net1", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel net2 = new NetDataModel("net2", "net2", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel net3 = new NetDataModel("net3", "net3", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        model.addNet(net1);
        model.addNet(net2);
        model.addNet(net3);
        clearInvocations(listener);

        model.removeNet(1);

        verify(listener).netsRemoved(1, 1);
        verifyNoMoreInteractions(listener);
        assertEquals(2, model.getVisibleNetCount());
        assertSame(net1, model.getVisibleNet(0));
        assertSame(net3, model.getVisibleNet(1));
    }

    @Test
    public void removeAllNets() {
        NetDataModel net1 = new NetDataModel("net1", "net1", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel net2 = new NetDataModel("net2", "net2", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel net3 = new NetDataModel("net3", "net3", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        model.addNet(net1);
        model.addNet(net2);
        model.addNet(net3);
        clearInvocations(listener);

        model.removeAllNets();

        verify(listener).netsRemoved(0, 2);
        verifyNoMoreInteractions(listener);
        assertEquals(0, model.getVisibleNetCount());
    }

    // When removing all nets on from an empty model, don't create
    // a notification with a negative index. This was causing an exception
    // that broke file loading.
    @Test
    public void removeAllEmpty() {
        model.removeAllNets();
        verifyZeroInteractions(listener);
    }

    @Test
    public void clear() {
        NetDataModel net1 = new NetDataModel("net1", "net1", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel net2 = new NetDataModel("net2", "net2", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        model.addNet(net1);
        model.addNet(net2);
        clearInvocations(listener);

        model.clear();

        verify(listener).netsRemoved(0, 2);
        verifyNoMoreInteractions(listener);
        assertEquals(0, model.getVisibleNetCount());
    }

    @Test
    public void saveRestoreNetSet() {
        NetDataModel net1 = new NetDataModel("net1", "net1", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel net2 = new NetDataModel("net2", "net2", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel net3 = new NetDataModel("net3", "net3", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel net4 = new NetDataModel("net4", "net4", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel net5 = new NetDataModel("net5", "net5", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        model.addNet(net1);
        model.addNet(net2);
        model.saveNetSet("set1");

        model.removeAllNets();
        model.addNet(net3);
        model.addNet(net4);
        model.addNet(net5);
        clearInvocations(listener);

        model.selectNetSet(0);

        verify(listener).netsRemoved(0, 2);
        verify(listener).netsAdded(0, 1);
        verifyNoMoreInteractions(listener);
        assertEquals(2, model.getVisibleNetCount());
        assertSame(net1, model.getVisibleNet(0));
        assertSame(net2, model.getVisibleNet(1));
    }

    @Test
    public void updateNetSet() {
        NetDataModel net1 = new NetDataModel("net1", "net1", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel net2 = new NetDataModel("net2", "net2", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel net3 = new NetDataModel("net3", "net3", TransitionVector.Builder.createBuilder(1).getTransitionVector());
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
        NetDataModel net1 = new NetDataModel("net1", "net1", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel net2 = new NetDataModel("net2", "net2", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        model.addNet(net1);
        model.addNet(net2);
        clearInvocations(listener);

        DecimalValueFormatter dvf = new DecimalValueFormatter();
        model.setValueFormatter(1, dvf);

        verify(listener).formatChanged(1);
        verifyNoMoreInteractions(listener);
        assertSame(model.getValueFormatter(1), dvf);
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
        WaveformPresentationModel.Listener listener2 = mock(WaveformPresentationModel.Listener.class);
        model.addListener(listener2);
        model.setHorizontalScale(1);
        verify(listener).scaleChanged(1);
        verify(listener2).scaleChanged(1);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void adjustingCursor() {
        model.setAdjustingCursor(true);
        assertTrue(model.isAdjustingCursor());
        verify(listener).cursorChanged(0, 0);
        clearInvocations(listener);

        model.setAdjustingCursor(false);
        assertFalse(model.isAdjustingCursor());
        verify(listener).cursorChanged(0, 0);
    }
}

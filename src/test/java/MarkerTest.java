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
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import org.junit.Before;
import org.junit.Test;
import waveview.NetDataModel;
import waveview.TransitionVector;
import waveview.WaveformPresentationModel;

public class MarkerTest {
    private final WaveformPresentationModel model = new WaveformPresentationModel();
    private WaveformPresentationModel.Listener listener = mock(WaveformPresentationModel.Listener.class);

    @Before
    public void setUpTest() {
        // The scale determines how close the user has to click to a marker to
        // select it.
        model.setHorizontalScale(0.1);
        model.addListener(listener);
    }

    @Test
    public void findMarkerEmpty() {
        assertEquals(0, model.getMarkerAtTime(1000));
    }

    // Ensure no crash when removing marker from empty set
    @Test
    public void removeMarkerEmpty() {
        model.removeMarkerAtTime(1000);
        verifyZeroInteractions(listener);
    }

    @Test
    public void insertMarker() {
        model.addMarker("marker0", 1000);

        verify(listener).markerChanged(1000);
        verifyNoMoreInteractions(listener);
        assertEquals(0, model.getMarkerAtTime(1000));
    }

    @Test
    public void removeMarkerNoMatchBefore() {
        model.addMarker("marker0", 1000);
        clearInvocations(listener);

        model.removeMarkerAtTime(925);  // less than marker time

        verifyZeroInteractions(listener);
        assertEquals(1, model.getMarkerCount()); // Marker should still be present
    }

    @Test
    public void removeMarkerNoMatchAfter() {
        model.addMarker("marker0", 1000);
        clearInvocations(listener);

        model.removeMarkerAtTime(1075); // greater than marker time

        verifyZeroInteractions(listener);
        assertEquals(1, model.getMarkerCount()); // Marker should still be present
    }

    @Test
    public void removeMarkerBeforeSingle() {
        // Marker is both first and last, test edge cases around it
        model.addMarker("marker0", 1000);
        clearInvocations(listener);

        model.removeMarkerAtTime(990);

        verify(listener).markerChanged(1000);
        verifyNoMoreInteractions(listener);
        assertEquals(0, model.getMarkerCount());
    }

    @Test
    public void removeMarkerAfterSingle() {
        // Marker is both first and last, test edge cases around it
        model.addMarker("marker0", 1000);
        clearInvocations(listener);

        model.removeMarkerAtTime(1010);

        verify(listener).markerChanged(1000);
        verifyNoMoreInteractions(listener);
        assertEquals(0, model.getMarkerCount());
    }

    // Remove a marker when there are multiple markers in the list
    @Test
    public void removeMarkerMulti() {
        model.addMarker("marker2", 100);
        model.addMarker("marker3", 200);
        model.addMarker("marker4", 300);
        model.addMarker("marker5", 400);
        clearInvocations(listener);

        model.removeMarkerAtTime(199);

        verify(listener).markerChanged(200);
        verifyNoMoreInteractions(listener);

        // Ensure other markers were unaffected
        assertEquals(3, model.getMarkerCount());
        assertEquals(100, model.getTimestampForMarker(0));
        assertEquals(300, model.getTimestampForMarker(1));
        assertEquals(400, model.getTimestampForMarker(2));
    }

    @Test
    public void removeAllMarkers() {
        model.addMarker("marker2", 100);
        model.addMarker("marker3", 200);
        model.addMarker("marker4", 300);
        model.addMarker("marker5", 400);
        clearInvocations(listener);

        model.removeAllMarkers();

        verify(listener).markerChanged(-1);
        verifyNoMoreInteractions(listener);
        assertEquals(0, model.getMarkerCount());
    }

    // Regression test: when the timescale is 1ns, couldn't remove markers
    @Test
    public void removeMarkerHighZoom() {
        model.setHorizontalScale(0.037);
        model.addMarker("marker1", 14);
        clearInvocations(listener);

        model.removeMarkerAtTime(14);

        verify(listener).markerChanged(14);
        verifyNoMoreInteractions(listener);
        assertEquals(0, model.getMarkerCount());
    }

    @Test
    public void getDescriptionForMarker() {
        model.addMarker("foo", 1000);
        assertEquals("foo", model.getDescriptionForMarker(0));
    }

    // Go to next marker when we are before the first marker
    @Test
    public void nextMarkerBeforeFirst() {
        model.addMarker("marker2", 100);
        model.addMarker("marker3", 200);
        model.setCursorPosition(20);
        clearInvocations(listener);

        model.nextMarker(false);

        verify(listener).cursorChanged(20, 100);
        verifyNoMoreInteractions(listener);
        assertEquals(100, model.getCursorPosition());
        assertEquals(100, model.getSelectionStart());
    }

    // Go to the next marker when we are on the first marker
    @Test
    public void nextMarkerFirst() {
        model.addMarker("marker2", 100);
        model.addMarker("marker3", 200);
        model.setCursorPosition(100);
        clearInvocations(listener);

        model.nextMarker(false);

        verify(listener).cursorChanged(100, 200);
        verifyNoMoreInteractions(listener);
        assertEquals(200, model.getCursorPosition());
        assertEquals(200, model.getSelectionStart());
    }

    // There is no next marker, shouldn't do anything
    @Test
    public void nextMarkerAtLast() {
        model.addMarker("marker1", 100);
        model.addMarker("marker2", 200);
        model.setCursorPosition(200);
        clearInvocations(listener);

        model.nextMarker(false);

        verifyZeroInteractions(listener);
        assertEquals(200, model.getCursorPosition());
    }

    @Test
    public void prevMarkerAfterLast() {
        model.addMarker("marker2", 100);
        model.addMarker("marker3", 200);
        model.setCursorPosition(500);
        clearInvocations(listener);

        model.prevMarker(false);

        verify(listener).cursorChanged(500, 200);
        verifyNoMoreInteractions(listener);
        assertEquals(200, model.getCursorPosition());
        assertEquals(200, model.getSelectionStart());
    }

    @Test
    public void prevMarkerOnLast() {
        model.addMarker("marker2", 100);
        model.addMarker("marker3", 200);
        model.setCursorPosition(200);
        clearInvocations(listener);

        model.prevMarker(false);

        verify(listener).cursorChanged(200, 100);
        verifyNoMoreInteractions(listener);
        assertEquals(100, model.getCursorPosition());
        assertEquals(100, model.getSelectionStart());
    }

    // There is no previous marker, shouldn't do anything
    @Test
    public void prevMarkerAtFirst() {
        model.addMarker("marker1", 100);
        model.addMarker("marker2", 200);
        model.setCursorPosition(100);
        clearInvocations(listener);

        model.prevMarker(false);

        verifyZeroInteractions(listener);
        assertEquals(100, model.getCursorPosition());
    }

    // Extend the selection while navigating to next marker
    @Test
    public void nextMarkerExtendSelection() {
        model.addMarker("marker1", 100);
        model.addMarker("marker2", 200);
        model.setCursorPosition(50);
        clearInvocations(listener);

        model.nextMarker(true);

        verify(listener).cursorChanged(50, 100);
        verifyNoMoreInteractions(listener);
        assertEquals(100, model.getCursorPosition());
        assertEquals(50, model.getSelectionStart());
    }

    // Extend the selection while navigating to previous marker
    @Test
    public void prevMarkerExtendSelection() {
        model.addMarker("marker2", 100);
        model.addMarker("marker3", 200);
        model.setCursorPosition(150);
        clearInvocations(listener);

        model.prevMarker(true);

        verify(listener).cursorChanged(150, 100);
        verifyNoMoreInteractions(listener);
        assertEquals(100, model.getCursorPosition());
        assertEquals(150, model.getSelectionStart());
    }

    @Test
    public void jumpToMarker() {
        model.addMarker("marker", 100);
        model.setCursorPosition(50);
        clearInvocations(listener);

        model.jumpToMarker(0, false);

        verify(listener).cursorChanged(50, 100);
        verifyNoMoreInteractions(listener);
        assertEquals(100, model.getCursorPosition());
        assertEquals(100, model.getSelectionStart());
    }

    @Test
    public void jumpToMarkerExtendSelection() {
        model.addMarker("marker", 100);
        model.setCursorPosition(50);
        clearInvocations(listener);

        model.jumpToMarker(0, true);

        verify(listener).cursorChanged(50, 100);
        verifyNoMoreInteractions(listener);
        assertEquals(100, model.getCursorPosition());
        assertEquals(50, model.getSelectionStart());
    }

    @Test
    public void clearMarkers() {
        model.addMarker("marker0", 1000);
        model.addMarker("marker1", 1200);
        clearInvocations(listener);

        model.clear();

        verify(listener).markerChanged(-1);
        verifyNoMoreInteractions(listener);
        assertEquals(0, model.getMarkerCount());
    }

    // Ensure removeAllNets doesn't affect markers
    @Test
    public void removeAllNets() {
        NetDataModel net1 = new NetDataModel("net1", "net1", new TransitionVector(1));
        model.addNet(net1);
        model.addMarker("a_marker", 1000);
        clearInvocations(listener);

        model.removeAllNets();

        verify(listener, never()).markerChanged(anyInt());
        assertEquals(1, model.getMarkerCount());
    }
}

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
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import org.junit.Before;
import org.junit.Test;

import waveview.WaveformPresentationModel;

public class MarkerTest {
    private final WaveformPresentationModel model = new WaveformPresentationModel();
    private WaveformPresentationModel.Listener listener;

    @Before
    public void setUpTest() {
        // The scale determines how close the user has to click to a marker to
        // select it.
        model.setHorizontalScale(0.1);
        listener = spy(WaveformPresentationModel.Listener.class);
        model.addListener(listener);
    }

    @Test
    public void findMarkerEmpty() {
        assertEquals(0, model.getMarkerAtTime(1000));
    }

    // Ensure no crash when removing marker from empty set
    @Test
    public void removeMarkerEmpty() {
        model.addListener(listener);
        model.removeMarkerAtTime(1000);
        verifyZeroInteractions(listener);
    }

    @Test
    public void insertMarker() {
        model.addMarker("marker0", 1000);
        verify(listener).markerChanged(1000);
        assertEquals(0, model.getMarkerAtTime(1000));
    }

    @Test
    public void removeMarkerNoMatch() {
        model.addMarker("marker0", 1000);
        clearInvocations(listener);

        model.removeMarkerAtTime(925);

        verifyZeroInteractions(listener);
        assertEquals(1, model.getMarkerCount()); // Marker should still be present

        model.removeMarkerAtTime(1075);

        assertEquals(1, model.getMarkerCount()); // Marker should still be present
        verifyZeroInteractions(listener);
    }

    @Test
    public void removeMarkerBeforeSingle() {
        // Marker is both first and last, test edge cases around it
        model.addMarker("marker0", 1000);
        clearInvocations(listener);

        model.removeMarkerAtTime(990);

        assertEquals(0, model.getMarkerCount());
        verify(listener).markerChanged(1000);
    }

    @Test
    public void removeMarkerAfterSingle() {
        // Marker is both first and last, test edge cases around it
        model.addMarker("marker0", 1000);
        clearInvocations(listener);

        model.removeMarkerAtTime(1010);

        assertEquals(0, model.getMarkerCount());
        verify(listener).markerChanged(1000);
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
        assertEquals(0, model.getMarkerCount());
    }

    // Regression test: when the timescale is 1ns, couldn't remove markers
    @Test
    public void removeMarkerHighZoom() {
        model.setHorizontalScale(0.037);
        model.addMarker("marker1", 14);
        model.removeMarkerAtTime(14);
        assertEquals(0, model.getMarkerCount());
    }

    @Test
    public void setDescriptionForMarker() {
        model.addMarker("foo", 1000);
        assertEquals("foo", model.getDescriptionForMarker(0));
        model.setDescriptionForMarker(0, "bar");
        assertEquals("bar", model.getDescriptionForMarker(0));
    }

    @Test
    public void nextMarker() {
        model.addMarker("marker2", 100);
        model.addMarker("marker3", 200);
        model.setCursorPosition(0);

        model.nextMarker(false);
        assertEquals(100, model.getCursorPosition());
        assertEquals(100, model.getSelectionStart());

        model.nextMarker(false);
        assertEquals(200, model.getCursorPosition());
        assertEquals(200, model.getSelectionStart());
    }

    // There is no next marker, shouldn't do anything
    @Test
    public void nextMarkerAtLast() {
        model.addMarker("marker1", 100);
        model.addMarker("marker2", 200);
        model.setCursorPosition(200);

        model.nextMarker(false);
        assertEquals(200, model.getCursorPosition());
    }

    @Test
    public void prevMarker() {
        model.addMarker("marker2", 100);
        model.addMarker("marker3", 200);

        model.setCursorPosition(500);

        model.prevMarker(false);
        assertEquals(200, model.getCursorPosition());
        assertEquals(200, model.getSelectionStart());

        model.prevMarker(false);
        assertEquals(100, model.getCursorPosition());
        assertEquals(100, model.getSelectionStart());
    }

    // There is no previous marker, shouldn't do anything
    @Test
    public void prevMarkerAtFirst() {
        model.addMarker("marker1", 100);
        model.addMarker("marker2", 200);
        model.setCursorPosition(100);

        model.prevMarker(false);
        assertEquals(100, model.getCursorPosition());
    }

    // Extend the selection while navigating to next marker
    @Test
    public void extendSelectionNextMarker() {
        model.addMarker("marker1", 100);
        model.addMarker("marker2", 200);

        model.setCursorPosition(50);
        model.setSelectionStart(50);
        model.nextMarker(true);
        assertEquals(100, model.getCursorPosition());
        assertEquals(50, model.getSelectionStart());
    }

    // Extend the selection while navigating to previous marker
    @Test
    public void extendSelectionPrevMarker() {
        model.addMarker("marker2", 100);
        model.addMarker("marker3", 200);

        model.setCursorPosition(150);
        model.setSelectionStart(150);
        model.prevMarker(true);
        assertEquals(100, model.getCursorPosition());
        assertEquals(150, model.getSelectionStart());
    }

    @Test
    public void clearMarkers() {
        model.addMarker("marker0", 1000);
        model.addMarker("marker1", 1200);

        model.clear();

        assertEquals(0, model.getMarkerCount());
    }
}

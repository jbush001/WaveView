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

import org.junit.Before;
import org.junit.Test;
import waveview.TraceDisplayModel;

public class MarkerTest {
    private final TraceDisplayModel tdm = new TraceDisplayModel();
    private final TraceDisplayModelListener listener = new TraceDisplayModelListener();

    @Before
    public void setUpTest() {
        // The scale determines how close the user has to click to a marker to
        // select it.
        tdm.setHorizontalScale(0.1);
        tdm.addListener(listener);
    }

    @Test
    public void findMarkerEmpty() {
        assertEquals(0, tdm.getMarkerAtTime(1000));
    }

    // Ensure no crash when removing marker from empty set
    @Test
    public void removeMarkerEmpty() {
        tdm.addListener(listener);
        tdm.removeMarkerAtTime(1000);
        assertEquals(0, listener.notifications);
    }

    @Test
    public void insertMarker() {
        tdm.addMarker("marker0", 1000);
        assertEquals(TraceDisplayModelListener.MARKER_CHANGED, listener.notifications);
        assertEquals(1000, listener.longArg0);
        assertEquals(0, tdm.getMarkerAtTime(1000));
    }

    @Test
    public void removeMarkerNoMatch() {
        tdm.addMarker("marker0", 1000);

        listener.reset();
        tdm.removeMarkerAtTime(925);
        assertEquals(1, tdm.getMarkerCount()); // Marker should still be present
        assertEquals(0, listener.notifications);

        tdm.removeMarkerAtTime(1075);
        assertEquals(1, tdm.getMarkerCount()); // Marker should still be present
        assertEquals(0, listener.notifications);
    }

    @Test
    public void removeMarkerBeforeSingle() {
        // Marker is both first and last, test edge cases around it
        tdm.addMarker("marker0", 1000);

        tdm.removeMarkerAtTime(990);
        assertEquals(0, tdm.getMarkerCount());
        assertEquals(TraceDisplayModelListener.MARKER_CHANGED, listener.notifications);
        assertEquals(1000, listener.longArg0);
    }

    @Test
    public void removeMarkerAfterSingle() {
        // Marker is both first and last, test edge cases around it
        tdm.addMarker("marker0", 1000);

        tdm.removeMarkerAtTime(1010);
        assertEquals(0, tdm.getMarkerCount());
        assertEquals(TraceDisplayModelListener.MARKER_CHANGED, listener.notifications);
        assertEquals(1000, listener.longArg0);
    }

    // Remove a marker when there are multiple markers in the list
    @Test
    public void removeMarkerMulti() {
        tdm.addMarker("marker2", 100);
        tdm.addMarker("marker3", 200);
        tdm.addMarker("marker4", 300);
        tdm.addMarker("marker5", 400);
        listener.reset();

        tdm.removeMarkerAtTime(199);
        assertEquals(TraceDisplayModelListener.MARKER_CHANGED, listener.notifications);
        assertEquals(200, listener.longArg0);

        // Ensure other markers were unaffected
        assertEquals(3, tdm.getMarkerCount());
        assertEquals(100, tdm.getTimestampForMarker(0));
        assertEquals(300, tdm.getTimestampForMarker(1));
        assertEquals(400, tdm.getTimestampForMarker(2));
    }

    @Test
    public void removeAllMarkers() {
        tdm.addMarker("marker2", 100);
        tdm.addMarker("marker3", 200);
        tdm.addMarker("marker4", 300);
        tdm.addMarker("marker5", 400);
        listener.reset();

        tdm.removeAllMarkers();

        assertEquals(TraceDisplayModelListener.MARKER_CHANGED, listener.notifications);
        assertEquals(-1, listener.longArg0);
        assertEquals(0, tdm.getMarkerCount());
    }

    // Regression test: when the timescale is 1ns, couldn't remove markers
    @Test
    public void removeMarkerHighZoom() {
        tdm.setHorizontalScale(0.037);
        tdm.addMarker("marker1", 14);
        tdm.removeMarkerAtTime(14);
        assertEquals(0, tdm.getMarkerCount());
    }

    @Test
    public void setDescriptionForMarker() {
        tdm.addMarker("foo", 1000);
        assertEquals("foo", tdm.getDescriptionForMarker(0));
        tdm.setDescriptionForMarker(0, "bar");
        assertEquals("bar", tdm.getDescriptionForMarker(0));
    }

    @Test
    public void nextMarker() {
        tdm.addMarker("marker2", 100);
        tdm.addMarker("marker3", 200);
        tdm.setCursorPosition(0);

        tdm.nextMarker(false);
        assertEquals(100, tdm.getCursorPosition());
        assertEquals(100, tdm.getSelectionStart());

        tdm.nextMarker(false);
        assertEquals(200, tdm.getCursorPosition());
        assertEquals(200, tdm.getSelectionStart());
    }

    // There is no next marker, shouldn't do anything
    @Test
    public void nextMarkerAtLast() {
        tdm.addMarker("marker1", 100);
        tdm.addMarker("marker2", 200);
        tdm.setCursorPosition(200);

        tdm.nextMarker(false);
        assertEquals(200, tdm.getCursorPosition());
    }

    @Test
    public void prevMarker() {
        tdm.addMarker("marker2", 100);
        tdm.addMarker("marker3", 200);

        tdm.setCursorPosition(500);

        tdm.prevMarker(false);
        assertEquals(200, tdm.getCursorPosition());
        assertEquals(200, tdm.getSelectionStart());

        tdm.prevMarker(false);
        assertEquals(100, tdm.getCursorPosition());
        assertEquals(100, tdm.getSelectionStart());
    }

    // There is no previous marker, shouldn't do anything
    @Test
    public void prevMarkerAtFirst() {
        tdm.addMarker("marker1", 100);
        tdm.addMarker("marker2", 200);
        tdm.setCursorPosition(100);

        tdm.prevMarker(false);
        assertEquals(100, tdm.getCursorPosition());
    }

    // Extend the selection while navigating to next marker
    @Test
    public void extendSelectionNextMarker() {
        tdm.addMarker("marker1", 100);
        tdm.addMarker("marker2", 200);

        tdm.setCursorPosition(50);
        tdm.setSelectionStart(50);
        tdm.nextMarker(true);
        assertEquals(100, tdm.getCursorPosition());
        assertEquals(50, tdm.getSelectionStart());
    }

    // Extend the selection while navigating to previous marker
    @Test
    public void extendSelectionPrevMarker() {
        tdm.addMarker("marker2", 100);
        tdm.addMarker("marker3", 200);

        tdm.setCursorPosition(150);
        tdm.setSelectionStart(150);
        tdm.prevMarker(true);
        assertEquals(100, tdm.getCursorPosition());
        assertEquals(150, tdm.getSelectionStart());
    }

    @Test
    public void clearMarkers() {
        tdm.addMarker("marker0", 1000);
        tdm.addMarker("marker1", 1200);

        tdm.clear();

        assertEquals(0, tdm.getMarkerCount());
    }
}

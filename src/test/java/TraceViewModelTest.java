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

public class TraceViewModelTest {
    class TestModelListener implements TraceViewModel.Listener {
        public static final int CURSOR_CHANGED = 1;
        public static final int NETS_ADDED = 2;
        public static final int NETS_REMOVED = 4;
        public static final int SCALE_CHANGED = 8;
        public static final int MARKER_CHANGED = 16;

        public void cursorChanged(long oldTimestamp, long newTimestamp) {
            fNotifications |= CURSOR_CHANGED;
            fLongArg0 = oldTimestamp;
            fLongArg1 = newTimestamp;
        }

        public void netsAdded(int firstIndex, int lastIndex) {
            fNotifications |= NETS_ADDED;
            fLongArg0 = (long) firstIndex;
            fLongArg1 = (long) lastIndex;
        }

        public void netsRemoved(int firstIndex, int lastIndex) {
            fNotifications |= NETS_REMOVED;
            fLongArg0 = (long) firstIndex;
            fLongArg1 = (long) lastIndex;
        }

        public void scaleChanged(double newScale) {
            fNotifications |= SCALE_CHANGED;
            fDoubleArg = newScale;
        }

        public void markerChanged(long timestamp) {
            fNotifications |= MARKER_CHANGED;
            fLongArg0 = timestamp;
        }

        void reset()
        {
            fNotifications = 0;
            fLongArg0 = -1;
            fLongArg1 = -1;
            fDoubleArg = -1;
        }

        public int fNotifications;
        public long fLongArg0;
        public long fLongArg1;
        public double fDoubleArg;
    }


    @Test
    public void testRemoveMarker() {
        TraceViewModel tvm = new TraceViewModel();
        TestModelListener listener = new TestModelListener();
        tvm.addListener(listener);

        assertEquals(0, tvm.getMarkerAtTime(1000)); // Make sure no marker

        // Ensure no crash on empty set
        listener.reset();
        tvm.removeMarkerAtTime(1000);
        assertEquals(0, listener.fNotifications);

        // Insert marker. This is both the first and last marker. Test edge
        // cases around this.
        listener.reset();
        tvm.addMarker("marker0", 1000);
        assertEquals(TestModelListener.MARKER_CHANGED, listener.fNotifications);
        assertEquals(1000, listener.fLongArg0);
        assertEquals(0, tvm.getMarkerAtTime(1000)); // Now there is a marker
        tvm.setHorizontalScale(10.0);   // 50 pixels each direction

        listener.reset();
        tvm.removeMarkerAtTime(925);
        assertEquals(1, tvm.getMarkerCount()); // Marker should still be present
        tvm.removeMarkerAtTime(1075);
        assertEquals(1, tvm.getMarkerCount()); // Marker should still be present
        assertEquals(0, listener.fNotifications);

        // Before marker, should remove
        tvm.removeMarkerAtTime(990);
        assertEquals(0, tvm.getMarkerCount());
        assertEquals(TestModelListener.MARKER_CHANGED, listener.fNotifications);
        assertEquals(1000, listener.fLongArg0);

        // Reinsert the marker
        listener.reset();
        tvm.addMarker("marker1", 1000);
        assertEquals(1, tvm.getMarkerCount());
        assertEquals(TestModelListener.MARKER_CHANGED, listener.fNotifications);
        assertEquals(1000, listener.fLongArg0);

        // Marker is after, remove it
        listener.reset();
        tvm.removeMarkerAtTime(1010);
        assertEquals(0, tvm.getMarkerCount());
        assertEquals(TestModelListener.MARKER_CHANGED, listener.fNotifications);
        assertEquals(1000, listener.fLongArg0);

        // Now create a few markers. Ensure adjacent markers aren't affected by
        // deletions.
        tvm.addMarker("marker2", 100);
        tvm.addMarker("marker3", 200);
        tvm.addMarker("marker4", 300);
        tvm.addMarker("marker5", 400);
        assertEquals(4, tvm.getMarkerCount());
        assertEquals(100, tvm.getTimestampForMarker(0));
        assertEquals("marker2", tvm.getDescriptionForMarker(0));
        assertEquals(200, tvm.getTimestampForMarker(1));
        assertEquals("marker3", tvm.getDescriptionForMarker(1));
        assertEquals(300, tvm.getTimestampForMarker(2));
        assertEquals("marker4", tvm.getDescriptionForMarker(2));
        assertEquals(400, tvm.getTimestampForMarker(3));
        assertEquals("marker5", tvm.getDescriptionForMarker(3));

        listener.reset();
        tvm.removeMarkerAtTime(199);
        assertEquals(TestModelListener.MARKER_CHANGED, listener.fNotifications);
        assertEquals(200, listener.fLongArg0);
        assertEquals(3, tvm.getMarkerCount());
        assertEquals(100, tvm.getTimestampForMarker(0));
        assertEquals(300, tvm.getTimestampForMarker(1));
        assertEquals(400, tvm.getTimestampForMarker(2));

        listener.reset();
        tvm.removeMarkerAtTime(301);
        assertEquals(TestModelListener.MARKER_CHANGED, listener.fNotifications);
        assertEquals(300, listener.fLongArg0);
        assertEquals(2, tvm.getMarkerCount());
        assertEquals(100, tvm.getTimestampForMarker(0));
        assertEquals(400, tvm.getTimestampForMarker(1));

        // Test removeAllMarkers
        listener.reset();
        tvm.removeAllMarkers();
        assertEquals(TestModelListener.MARKER_CHANGED, listener.fNotifications);
        assertEquals(-1, listener.fLongArg0);
        assertEquals(0, tvm.getMarkerCount());
    }

    @Test
    public void testNextPrevMarker() {
        TraceViewModel tvm = new TraceViewModel();

        tvm.addMarker("marker2", 100);
        tvm.addMarker("marker3", 200);
        tvm.addMarker("marker4", 300);
        tvm.addMarker("marker5", 400);
        tvm.setCursorPosition(0);
        tvm.nextMarker(false);
        assertEquals(100, tvm.getCursorPosition());
        assertEquals(100, tvm.getSelectionStart());
        tvm.nextMarker(false);
        assertEquals(200, tvm.getCursorPosition());
        assertEquals(200, tvm.getSelectionStart());
        tvm.nextMarker(false);
        assertEquals(300, tvm.getCursorPosition());
        assertEquals(300, tvm.getSelectionStart());
        tvm.nextMarker(false);
        assertEquals(400, tvm.getCursorPosition());
        assertEquals(400, tvm.getSelectionStart());
        tvm.nextMarker(false);
        assertEquals(400, tvm.getCursorPosition());

        tvm.setCursorPosition(500);
        tvm.prevMarker(false);
        assertEquals(400, tvm.getCursorPosition());
        tvm.prevMarker(false);
        assertEquals(300, tvm.getCursorPosition());
        assertEquals(300, tvm.getSelectionStart());
        tvm.prevMarker(false);
        assertEquals(200, tvm.getCursorPosition());
        assertEquals(200, tvm.getSelectionStart());
        tvm.prevMarker(false);
        assertEquals(100, tvm.getCursorPosition());
        assertEquals(100, tvm.getSelectionStart());
        tvm.prevMarker(false);
        assertEquals(100, tvm.getCursorPosition());

        // Test extending the selection while navigating to marker
        tvm.setCursorPosition(0);
        tvm.setSelectionStart(0);
        tvm.nextMarker(true);
        assertEquals(100, tvm.getCursorPosition());
        assertEquals(0, tvm.getSelectionStart());

        tvm.setCursorPosition(150);
        tvm.setSelectionStart(150);
        tvm.prevMarker(true);
        assertEquals(100, tvm.getCursorPosition());
        assertEquals(150, tvm.getSelectionStart());
    }

    @Test
    public void testScaleChange() {
        TraceViewModel tvm = new TraceViewModel();
        TestModelListener listener = new TestModelListener();
        tvm.addListener(listener);

        tvm.setHorizontalScale(123.0);
        assertEquals(TestModelListener.SCALE_CHANGED, listener.fNotifications);
        assertEquals(123.0, listener.fDoubleArg, 0.0);
        assertEquals(123.0, tvm.getHorizontalScale(), 0.0);
    }

    @Test
    public void testCursor() {
        TraceViewModel tvm = new TraceViewModel();
        TestModelListener listener = new TestModelListener();
        tvm.addListener(listener);

        tvm.setCursorPosition(1024);
        assertEquals(TestModelListener.CURSOR_CHANGED, listener.fNotifications);
        assertEquals(0, listener.fLongArg0);
        assertEquals(1024, listener.fLongArg1);

        listener.reset();
        tvm.setCursorPosition(37);
        assertEquals(TestModelListener.CURSOR_CHANGED, listener.fNotifications);
        assertEquals(1024, listener.fLongArg0);
        assertEquals(37, listener.fLongArg1);
    }

    @Test
    public void testMakeNetsVisible() {
        TraceViewModel tvm = new TraceViewModel();
        TestModelListener listener = new TestModelListener();
        tvm.addListener(listener);

        assertEquals(0, tvm.getVisibleNetCount());

        // Add a net
        tvm.makeNetVisible(7);
        assertEquals(TestModelListener.NETS_ADDED, listener.fNotifications);
        assertEquals(0, listener.fLongArg0);
        assertEquals(0, listener.fLongArg1);
        assertEquals(1, tvm.getVisibleNetCount());
        assertEquals(7, tvm.getVisibleNet(0));

        // Add a second net
        listener.reset();
        tvm.makeNetVisible(9);
        assertEquals(TestModelListener.NETS_ADDED, listener.fNotifications);
        assertEquals(1, listener.fLongArg0);
        assertEquals(1, listener.fLongArg1);
        assertEquals(2, tvm.getVisibleNetCount());
        assertEquals(7, tvm.getVisibleNet(0));
        assertEquals(9, tvm.getVisibleNet(1));

        // Remove first net
        listener.reset();
        tvm.removeNet(0);
        assertEquals(TestModelListener.NETS_REMOVED, listener.fNotifications);
        assertEquals(0, listener.fLongArg0);
        assertEquals(0, listener.fLongArg1);
        assertEquals(1, tvm.getVisibleNetCount());
        assertEquals(9, tvm.getVisibleNet(0));

        // Remove remaining net
        listener.reset();
        tvm.removeNet(0);
        assertEquals(TestModelListener.NETS_REMOVED, listener.fNotifications);
        assertEquals(0, listener.fLongArg0);
        assertEquals(0, listener.fLongArg1);
        assertEquals(0, tvm.getVisibleNetCount());

        // Add a bunch of nets again
        tvm.makeNetVisible(11);
        tvm.makeNetVisible(13);
        tvm.makeNetVisible(17);
        tvm.makeNetVisible(19);
        tvm.makeNetVisible(23);
        tvm.makeNetVisible(27);
        tvm.makeNetVisible(5, 31);  // Note: insert above last

        assertEquals(7, tvm.getVisibleNetCount());

        assertEquals(11, tvm.getVisibleNet(0));
        assertEquals(13, tvm.getVisibleNet(1));
        assertEquals(17, tvm.getVisibleNet(2));
        assertEquals(19, tvm.getVisibleNet(3));
        assertEquals(23, tvm.getVisibleNet(4));
        assertEquals(31, tvm.getVisibleNet(5));
        assertEquals(27, tvm.getVisibleNet(6));

        // Rearrange a set of nets
        int[] indices = { 1, 3, 4, 5};
        listener.reset();
        tvm.moveNets(indices, 2);
        assertEquals(TestModelListener.NETS_REMOVED | TestModelListener.NETS_ADDED, listener.fNotifications);
        // XXX doesn't check parameters (multiple notifications from this)

        assertEquals(7, tvm.getVisibleNetCount());
        assertEquals(11, tvm.getVisibleNet(0));
        assertEquals(13, tvm.getVisibleNet(1));
        assertEquals(19, tvm.getVisibleNet(2));
        assertEquals(23, tvm.getVisibleNet(3));
        assertEquals(31, tvm.getVisibleNet(4));
        assertEquals(17, tvm.getVisibleNet(5));
        assertEquals(27, tvm.getVisibleNet(6));

        // Remove all nets
        listener.reset();
        tvm.removeAllNets();
        assertEquals(TestModelListener.NETS_REMOVED, listener.fNotifications);
        assertEquals(0, listener.fLongArg0);
        assertEquals(6, listener.fLongArg1);
        assertEquals(0, tvm.getVisibleNetCount());
    }

    @Test
    public void testClear() {
        TraceViewModel tvm = new TraceViewModel();

        tvm.addMarker("marker0", 1000);
        tvm.addMarker("marker1", 1200);
        tvm.makeNetVisible(11);
        tvm.makeNetVisible(13);
        tvm.makeNetVisible(17);
        assertEquals(3, tvm.getVisibleNetCount());
        assertEquals(2, tvm.getMarkerCount());

        TestModelListener listener = new TestModelListener();
        tvm.addListener(listener);

        tvm.clear();

        assertEquals(TestModelListener.NETS_REMOVED, listener.fNotifications);
        assertEquals(0, listener.fLongArg0);
        assertEquals(3, listener.fLongArg1);
        assertEquals(0, tvm.getVisibleNetCount());
        assertEquals(0, tvm.getMarkerCount());
    }

    @Test
    public void testNetSet() {
        TraceViewModel tvm = new TraceViewModel();

        // Create first net set
        tvm.makeNetVisible(11);
        tvm.makeNetVisible(13);
        tvm.makeNetVisible(17);
        tvm.saveNetSet("set1");
        assertEquals(1, tvm.getNetSetCount());
        assertEquals("set1", tvm.getNetSetName(0));

        // Create second net set
        tvm.removeAllNets();
        tvm.makeNetVisible(19);
        tvm.makeNetVisible(23);
        tvm.saveNetSet("set2");
        assertEquals(2, tvm.getNetSetCount());

        assertEquals("set1", tvm.getNetSetName(0));
        assertEquals("set2", tvm.getNetSetName(1));

        // Select first net set
        TestModelListener listener = new TestModelListener();
        tvm.addListener(listener);
        tvm.selectNetSet(0);
        assertEquals(TestModelListener.NETS_ADDED | TestModelListener.NETS_REMOVED,
            listener.fNotifications);
        assertEquals(0, listener.fLongArg0); // First index (of added nets)
        assertEquals(2, listener.fLongArg1); // Last index (not count)
        assertEquals(3, tvm.getVisibleNetCount());
        assertEquals(11, tvm.getVisibleNet(0));
        assertEquals(13, tvm.getVisibleNet(1));
        assertEquals(17, tvm.getVisibleNet(2));

        // Select second net set
        listener.reset();
        tvm.selectNetSet(1);
        assertEquals(TestModelListener.NETS_ADDED | TestModelListener.NETS_REMOVED,
            listener.fNotifications);
        assertEquals(0, listener.fLongArg0); // First index
        assertEquals(1, listener.fLongArg1); // last index
        assertEquals(2, tvm.getVisibleNetCount());
        assertEquals(19, tvm.getVisibleNet(0));
        assertEquals(23, tvm.getVisibleNet(1));

        // Update second net set
        tvm.makeNetVisible(31);
        tvm.saveNetSet("set2");
        assertEquals(2, tvm.getNetSetCount());
        assertEquals("set1", tvm.getNetSetName(0));
        assertEquals("set2", tvm.getNetSetName(1));

        // Flip back and forth to reload second net set. Make sure
        // it has been updated
        tvm.selectNetSet(0);
        tvm.selectNetSet(1);
        assertEquals(3, tvm.getVisibleNetCount());
        assertEquals(19, tvm.getVisibleNet(0));
        assertEquals(23, tvm.getVisibleNet(1));
        assertEquals(31, tvm.getVisibleNet(2));
    }

    // When removing all nets on from an empty model, don't create
    // a notification with a negative index. This was causing an exception
    // that broke file loading.
    @Test
    public void testRemoveOnEmpty() {
        TraceViewModel tvm = new TraceViewModel();
        TestModelListener listener = new TestModelListener();
        tvm.addListener(listener);
        tvm.removeAllNets();
        assertEquals(0, listener.fNotifications);
    }

    @Test
    public void testZeroMinorTick() {
        TraceViewModel tvm = new TraceViewModel();
        tvm.setHorizontalScale(0.01);
        assertEquals(1, tvm.getMinorTickInterval());
    }
}

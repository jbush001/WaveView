
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
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import waveview.DecimalValueFormatter;
import waveview.TraceDisplayModel;

public class TraceDisplayModelTest {
    static class TestModelListener implements TraceDisplayModel.Listener {
        public static final int CURSOR_CHANGED = 1;
        public static final int NETS_ADDED = 2;
        public static final int NETS_REMOVED = 4;
        public static final int SCALE_CHANGED = 8;
        public static final int MARKER_CHANGED = 16;
        public static final int FORMAT_CHANGED = 32;

        public int notifications;
        public long longArg0;
        public long longArg1;
        public double doubleArg;

        @Override
        public void cursorChanged(long oldTimestamp, long newTimestamp) {
            notifications |= CURSOR_CHANGED;
            longArg0 = oldTimestamp;
            longArg1 = newTimestamp;
        }

        @Override
        public void netsAdded(int firstIndex, int lastIndex) {
            notifications |= NETS_ADDED;
            longArg0 = (long) firstIndex;
            longArg1 = (long) lastIndex;
        }

        @Override
        public void netsRemoved(int firstIndex, int lastIndex) {
            notifications |= NETS_REMOVED;
            longArg0 = (long) firstIndex;
            longArg1 = (long) lastIndex;
        }

        @Override
        public void scaleChanged(double newScale) {
            notifications |= SCALE_CHANGED;
            doubleArg = newScale;
        }

        @Override
        public void markerChanged(long timestamp) {
            notifications |= MARKER_CHANGED;
            longArg0 = timestamp;
        }

        @Override
        public void formatChanged(int index) {
            notifications |= FORMAT_CHANGED;
            longArg0 = index;
        }

        void reset() {
            notifications = 0;
            longArg0 = -1;
            longArg1 = -1;
            doubleArg = -1;
        }
    }

    /// This also covers a lot of other marker functionality
    @Test
    public void testRemoveMarker() {
        TraceDisplayModel tdm = new TraceDisplayModel();
        TestModelListener listener = new TestModelListener();
        tdm.addListener(listener);

        assertEquals(0, tdm.getMarkerAtTime(1000)); // Make sure no marker

        // Ensure no crash on empty set
        listener.reset();
        tdm.removeMarkerAtTime(1000);
        assertEquals(0, listener.notifications);

        // Insert marker. This is both the first and last marker. Test edge
        // cases around this.
        listener.reset();
        tdm.addMarker("marker0", 1000);
        assertEquals(TestModelListener.MARKER_CHANGED, listener.notifications);
        assertEquals(1000, listener.longArg0);
        assertEquals(0, tdm.getMarkerAtTime(1000)); // Now there is a marker
        tdm.setHorizontalScale(0.1); // 50 pixels each direction

        listener.reset();
        tdm.removeMarkerAtTime(925);
        assertEquals(1, tdm.getMarkerCount()); // Marker should still be present
        tdm.removeMarkerAtTime(1075);
        assertEquals(1, tdm.getMarkerCount()); // Marker should still be present
        assertEquals(0, listener.notifications);

        // Before marker, should remove
        tdm.removeMarkerAtTime(990);
        assertEquals(0, tdm.getMarkerCount());
        assertEquals(TestModelListener.MARKER_CHANGED, listener.notifications);
        assertEquals(1000, listener.longArg0);

        // Reinsert the marker
        listener.reset();
        tdm.addMarker("marker1", 1000);
        assertEquals(1, tdm.getMarkerCount());
        assertEquals(TestModelListener.MARKER_CHANGED, listener.notifications);
        assertEquals(1000, listener.longArg0);

        // Marker is after, remove it
        listener.reset();
        tdm.removeMarkerAtTime(1010);
        assertEquals(0, tdm.getMarkerCount());
        assertEquals(TestModelListener.MARKER_CHANGED, listener.notifications);
        assertEquals(1000, listener.longArg0);

        // Now create a few markers. Ensure adjacent markers aren't affected by
        // deletions.
        tdm.addMarker("marker2", 100);
        tdm.addMarker("marker3", 200);
        tdm.addMarker("marker4", 300);
        tdm.addMarker("marker5", 400);
        assertEquals(4, tdm.getMarkerCount());
        assertEquals(100, tdm.getTimestampForMarker(0));
        assertEquals("marker2", tdm.getDescriptionForMarker(0));
        assertEquals(200, tdm.getTimestampForMarker(1));
        assertEquals("marker3", tdm.getDescriptionForMarker(1));
        assertEquals(300, tdm.getTimestampForMarker(2));
        assertEquals("marker4", tdm.getDescriptionForMarker(2));
        assertEquals(400, tdm.getTimestampForMarker(3));
        assertEquals("marker5", tdm.getDescriptionForMarker(3));

        listener.reset();
        tdm.removeMarkerAtTime(199);
        assertEquals(TestModelListener.MARKER_CHANGED, listener.notifications);
        assertEquals(200, listener.longArg0);
        assertEquals(3, tdm.getMarkerCount());
        assertEquals(100, tdm.getTimestampForMarker(0));
        assertEquals(300, tdm.getTimestampForMarker(1));
        assertEquals(400, tdm.getTimestampForMarker(2));

        listener.reset();
        tdm.removeMarkerAtTime(301);
        assertEquals(TestModelListener.MARKER_CHANGED, listener.notifications);
        assertEquals(300, listener.longArg0);
        assertEquals(2, tdm.getMarkerCount());
        assertEquals(100, tdm.getTimestampForMarker(0));
        assertEquals(400, tdm.getTimestampForMarker(1));

        // Test removeAllMarkers
        listener.reset();
        tdm.removeAllMarkers();
        assertEquals(TestModelListener.MARKER_CHANGED, listener.notifications);
        assertEquals(-1, listener.longArg0);
        assertEquals(0, tdm.getMarkerCount());
    }

    // Regression test: when the timescale is 1ns, couldn't remove markers
    @Test
    public void removeMarkerHighZoom() {
        TraceDisplayModel tdm = new TraceDisplayModel();
        tdm.setHorizontalScale(0.037);
        tdm.addMarker("marker1", 14);
        tdm.removeMarkerAtTime(14);
        assertEquals(0, tdm.getMarkerCount());
    }

    @Test
    public void testSetDescriptionForMarker() {
        TraceDisplayModel tdm = new TraceDisplayModel();
        tdm.addMarker("foo", 1000);
        assertEquals("foo", tdm.getDescriptionForMarker(0));
        tdm.setDescriptionForMarker(0, "bar");
        assertEquals("bar", tdm.getDescriptionForMarker(0));
    }

    @Test
    public void testNextPrevMarker() {
        TraceDisplayModel tdm = new TraceDisplayModel();

        tdm.addMarker("marker2", 100);
        tdm.addMarker("marker3", 200);
        tdm.addMarker("marker4", 300);
        tdm.addMarker("marker5", 400);
        tdm.setCursorPosition(0);
        tdm.nextMarker(false);
        assertEquals(100, tdm.getCursorPosition());
        assertEquals(100, tdm.getSelectionStart());
        tdm.nextMarker(false);
        assertEquals(200, tdm.getCursorPosition());
        assertEquals(200, tdm.getSelectionStart());
        tdm.nextMarker(false);
        assertEquals(300, tdm.getCursorPosition());
        assertEquals(300, tdm.getSelectionStart());
        tdm.nextMarker(false);
        assertEquals(400, tdm.getCursorPosition());
        assertEquals(400, tdm.getSelectionStart());
        tdm.nextMarker(false);
        assertEquals(400, tdm.getCursorPosition());

        tdm.setCursorPosition(500);
        tdm.prevMarker(false);
        assertEquals(400, tdm.getCursorPosition());
        tdm.prevMarker(false);
        assertEquals(300, tdm.getCursorPosition());
        assertEquals(300, tdm.getSelectionStart());
        tdm.prevMarker(false);
        assertEquals(200, tdm.getCursorPosition());
        assertEquals(200, tdm.getSelectionStart());
        tdm.prevMarker(false);
        assertEquals(100, tdm.getCursorPosition());
        assertEquals(100, tdm.getSelectionStart());
        tdm.prevMarker(false);
        assertEquals(100, tdm.getCursorPosition());

        // Test extending the selection while navigating to marker
        tdm.setCursorPosition(0);
        tdm.setSelectionStart(0);
        tdm.nextMarker(true);
        assertEquals(100, tdm.getCursorPosition());
        assertEquals(0, tdm.getSelectionStart());

        tdm.setCursorPosition(150);
        tdm.setSelectionStart(150);
        tdm.prevMarker(true);
        assertEquals(100, tdm.getCursorPosition());
        assertEquals(150, tdm.getSelectionStart());
    }

    @Test
    public void testScaleChange() {
        TraceDisplayModel tdm = new TraceDisplayModel();
        TestModelListener listener = new TestModelListener();
        tdm.addListener(listener);

        tdm.setHorizontalScale(123.0);
        assertEquals(TestModelListener.SCALE_CHANGED, listener.notifications);
        assertEquals(123.0, listener.doubleArg, 0.0);
        assertEquals(123.0, tdm.getHorizontalScale(), 0.0);
    }

    @Test
    public void testCursor() {
        TraceDisplayModel tdm = new TraceDisplayModel();
        TestModelListener listener = new TestModelListener();
        tdm.addListener(listener);

        tdm.setCursorPosition(1024);
        assertEquals(TestModelListener.CURSOR_CHANGED, listener.notifications);
        assertEquals(0, listener.longArg0);
        assertEquals(1024, listener.longArg1);

        listener.reset();
        tdm.setCursorPosition(37);
        assertEquals(TestModelListener.CURSOR_CHANGED, listener.notifications);
        assertEquals(1024, listener.longArg0);
        assertEquals(37, listener.longArg1);
    }

    @Test
    public void testMakeNetsVisible() {
        TraceDisplayModel tdm = new TraceDisplayModel();
        TestModelListener listener = new TestModelListener();
        tdm.addListener(listener);

        assertEquals(0, tdm.getVisibleNetCount());

        // Add a net
        tdm.makeNetVisible(7);
        assertEquals(TestModelListener.NETS_ADDED, listener.notifications);
        assertEquals(0, listener.longArg0);
        assertEquals(0, listener.longArg1);
        assertEquals(1, tdm.getVisibleNetCount());
        assertEquals(7, tdm.getVisibleNet(0));

        // Add a second net
        listener.reset();
        tdm.makeNetVisible(9);
        assertEquals(TestModelListener.NETS_ADDED, listener.notifications);
        assertEquals(1, listener.longArg0);
        assertEquals(1, listener.longArg1);
        assertEquals(2, tdm.getVisibleNetCount());
        assertEquals(7, tdm.getVisibleNet(0));
        assertEquals(9, tdm.getVisibleNet(1));

        // Remove first net
        listener.reset();
        tdm.removeNet(0);
        assertEquals(TestModelListener.NETS_REMOVED, listener.notifications);
        assertEquals(0, listener.longArg0);
        assertEquals(0, listener.longArg1);
        assertEquals(1, tdm.getVisibleNetCount());
        assertEquals(9, tdm.getVisibleNet(0));

        // Remove remaining net
        listener.reset();
        tdm.removeNet(0);
        assertEquals(TestModelListener.NETS_REMOVED, listener.notifications);
        assertEquals(0, listener.longArg0);
        assertEquals(0, listener.longArg1);
        assertEquals(0, tdm.getVisibleNetCount());

        // Add a bunch of nets again
        tdm.makeNetVisible(11);
        tdm.makeNetVisible(13);
        tdm.makeNetVisible(17);
        tdm.makeNetVisible(19);
        tdm.makeNetVisible(23);
        tdm.makeNetVisible(27);
        tdm.makeNetVisible(5, 31); // Note: insert above last

        assertEquals(7, tdm.getVisibleNetCount());

        assertEquals(11, tdm.getVisibleNet(0));
        assertEquals(13, tdm.getVisibleNet(1));
        assertEquals(17, tdm.getVisibleNet(2));
        assertEquals(19, tdm.getVisibleNet(3));
        assertEquals(23, tdm.getVisibleNet(4));
        assertEquals(31, tdm.getVisibleNet(5));
        assertEquals(27, tdm.getVisibleNet(6));

        // Rearrange a set of nets
        int[] indices = { 1, 3, 4, 5 };
        listener.reset();
        tdm.moveNets(indices, 2);
        assertEquals(TestModelListener.NETS_REMOVED | TestModelListener.NETS_ADDED, listener.notifications);
        // XXX doesn't check parameters (multiple notifications from this)

        assertEquals(7, tdm.getVisibleNetCount());
        assertEquals(11, tdm.getVisibleNet(0));
        assertEquals(13, tdm.getVisibleNet(1));
        assertEquals(19, tdm.getVisibleNet(2));
        assertEquals(23, tdm.getVisibleNet(3));
        assertEquals(31, tdm.getVisibleNet(4));
        assertEquals(17, tdm.getVisibleNet(5));
        assertEquals(27, tdm.getVisibleNet(6));

        // Remove all nets
        listener.reset();
        tdm.removeAllNets();
        assertEquals(TestModelListener.NETS_REMOVED, listener.notifications);
        assertEquals(0, listener.longArg0);
        assertEquals(6, listener.longArg1);
        assertEquals(0, tdm.getVisibleNetCount());
    }

    @Test
    public void testClear() {
        TraceDisplayModel tdm = new TraceDisplayModel();

        tdm.addMarker("marker0", 1000);
        tdm.addMarker("marker1", 1200);
        tdm.makeNetVisible(11);
        tdm.makeNetVisible(13);
        tdm.makeNetVisible(17);
        assertEquals(3, tdm.getVisibleNetCount());
        assertEquals(2, tdm.getMarkerCount());

        TestModelListener listener = new TestModelListener();
        tdm.addListener(listener);

        tdm.clear();

        assertEquals(TestModelListener.NETS_REMOVED, listener.notifications);
        assertEquals(0, listener.longArg0);
        assertEquals(3, listener.longArg1);
        assertEquals(0, tdm.getVisibleNetCount());
        assertEquals(0, tdm.getMarkerCount());
    }

    @Test
    public void testNetSet() {
        TraceDisplayModel tdm = new TraceDisplayModel();

        // Create first net set
        tdm.makeNetVisible(11);
        tdm.makeNetVisible(13);
        tdm.makeNetVisible(17);
        tdm.saveNetSet("set1");
        assertEquals(1, tdm.getNetSetCount());
        assertEquals("set1", tdm.getNetSetName(0));

        // Create second net set
        tdm.removeAllNets();
        tdm.makeNetVisible(19);
        tdm.makeNetVisible(23);
        tdm.saveNetSet("set2");
        assertEquals(2, tdm.getNetSetCount());

        assertEquals("set1", tdm.getNetSetName(0));
        assertEquals("set2", tdm.getNetSetName(1));

        // Select first net set
        TestModelListener listener = new TestModelListener();
        tdm.addListener(listener);
        tdm.selectNetSet(0);
        assertEquals(TestModelListener.NETS_ADDED | TestModelListener.NETS_REMOVED, listener.notifications);
        assertEquals(0, listener.longArg0); // First index (of added nets)
        assertEquals(2, listener.longArg1); // Last index (not count)
        assertEquals(3, tdm.getVisibleNetCount());
        assertEquals(11, tdm.getVisibleNet(0));
        assertEquals(13, tdm.getVisibleNet(1));
        assertEquals(17, tdm.getVisibleNet(2));

        // Select second net set
        listener.reset();
        tdm.selectNetSet(1);
        assertEquals(TestModelListener.NETS_ADDED | TestModelListener.NETS_REMOVED, listener.notifications);
        assertEquals(0, listener.longArg0); // First index
        assertEquals(1, listener.longArg1); // last index
        assertEquals(2, tdm.getVisibleNetCount());
        assertEquals(19, tdm.getVisibleNet(0));
        assertEquals(23, tdm.getVisibleNet(1));

        // Update second net set
        tdm.makeNetVisible(31);
        tdm.saveNetSet("set2");
        assertEquals(2, tdm.getNetSetCount());
        assertEquals("set1", tdm.getNetSetName(0));
        assertEquals("set2", tdm.getNetSetName(1));

        // Flip back and forth to reload second net set. Make sure
        // it has been updated
        tdm.selectNetSet(0);
        tdm.selectNetSet(1);
        assertEquals(3, tdm.getVisibleNetCount());
        assertEquals(19, tdm.getVisibleNet(0));
        assertEquals(23, tdm.getVisibleNet(1));
        assertEquals(31, tdm.getVisibleNet(2));
    }

    // When removing all nets on from an empty model, don't create
    // a notification with a negative index. This was causing an exception
    // that broke file loading.
    @Test
    public void testRemoveOnEmpty() {
        TraceDisplayModel tdm = new TraceDisplayModel();
        TestModelListener listener = new TestModelListener();
        tdm.addListener(listener);
        tdm.removeAllNets();
        assertEquals(0, listener.notifications);
    }

    @Test
    public void testZeroMinorTick() {
        TraceDisplayModel tdm = new TraceDisplayModel();
        tdm.setHorizontalScale(100);
        assertEquals(1, tdm.getMinorTickInterval());
    }

    @Test
    public void testSetGetValueFormatter() {
        TraceDisplayModel tdm = new TraceDisplayModel();
        TestModelListener listener = new TestModelListener();
        tdm.addListener(listener);
        tdm.makeNetVisible(1);
        tdm.makeNetVisible(2);
        DecimalValueFormatter dvf = new DecimalValueFormatter();
        listener.reset();
        tdm.setValueFormatter(1, dvf);
        assertEquals(TestModelListener.FORMAT_CHANGED, listener.notifications);
        assertEquals(1, listener.longArg0);
        assertTrue(tdm.getValueFormatter(1) == dvf);
    }

    // Ensure the minor tick interval is initialized when TraceDisplayModel
    // is created.
    @Test
    public void testMinorTickInit() {
        TraceDisplayModel tdm = new TraceDisplayModel();
        assertNotEquals(0, tdm.getMinorTickInterval(), 0);
    }

    // Ensure this records multiple listeners properly. We don't test
    // that all notification types will notify all listeners.
    @Test
    public void testMultipleListeners() {
        TraceDisplayModel tdm = new TraceDisplayModel();
        TestModelListener listener1 = new TestModelListener();
        tdm.addListener(listener1);
        TestModelListener listener2 = new TestModelListener();
        tdm.addListener(listener2);
        tdm.setHorizontalScale(1);
        assertEquals(TestModelListener.SCALE_CHANGED, listener1.notifications);
        assertEquals(TestModelListener.SCALE_CHANGED, listener2.notifications);
    }

    @Test
    public void testAdjustingCursor() {
        TraceDisplayModel tdm = new TraceDisplayModel();
        tdm.setAdjustingCursor(true);
        assertTrue(tdm.isAdjustingCursor());
        tdm.setAdjustingCursor(false);
        assertFalse(tdm.isAdjustingCursor());
    }
}

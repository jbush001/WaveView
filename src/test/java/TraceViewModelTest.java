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

public class TraceViewModelTest
{
    @Test public void testRemoveMarker()
    {
        TraceViewModel tvm = new TraceViewModel();

        assertEquals(0, tvm.getMarkerAtTime(1000)); // Make sure no marker

        // Ensure no crash on empty set
        tvm.removeMarkerAtTime(1000);

        // This is both the first and last marker. Test edge
        // cases around this.
        tvm.addMarker("marker0", 1000);
        assertEquals(0, tvm.getMarkerAtTime(1000)); // Now there is a marker
        tvm.setHorizontalScale(10.0);   // 50 pixels each direction

        tvm.removeMarkerAtTime(925);
        assertEquals(1, tvm.getMarkerCount()); // Marker should still be present
        tvm.removeMarkerAtTime(1075);
        assertEquals(1, tvm.getMarkerCount()); // Marker should still be present

        // Before marker, should remove
        tvm.removeMarkerAtTime(990);
        assertEquals(0, tvm.getMarkerCount());

        // Reinsert the marker
        tvm.addMarker("marker1", 1000);
        assertEquals(1, tvm.getMarkerCount());

        // Marker is after, remove it
        tvm.removeMarkerAtTime(1010);
        assertEquals(0, tvm.getMarkerCount());

        // Now create a few markers. Ensure adjacent markers aren't affected.
        tvm.addMarker("marker2", 100);
        tvm.addMarker("marker3", 200);
        tvm.addMarker("marker4", 300);
        tvm.addMarker("marker5", 400);
        assertEquals(4, tvm.getMarkerCount());
        assertEquals(100, tvm.getTimestampForMarker(0));
        assertEquals(200, tvm.getTimestampForMarker(1));
        assertEquals(300, tvm.getTimestampForMarker(2));
        assertEquals(400, tvm.getTimestampForMarker(3));

        tvm.removeMarkerAtTime(199);
        assertEquals(3, tvm.getMarkerCount());
        assertEquals(100, tvm.getTimestampForMarker(0));
        assertEquals(300, tvm.getTimestampForMarker(1));
        assertEquals(400, tvm.getTimestampForMarker(2));

        tvm.removeMarkerAtTime(301);
        assertEquals(2, tvm.getMarkerCount());
        assertEquals(100, tvm.getTimestampForMarker(0));
        assertEquals(400, tvm.getTimestampForMarker(1));
    }
}

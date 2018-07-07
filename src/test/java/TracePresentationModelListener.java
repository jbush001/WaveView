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

import waveview.TracePresentationModel;

// TraceDisplay listener stand-in, validates that the proper events were received.
class TracePresentationModelListener implements TracePresentationModel.Listener {
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

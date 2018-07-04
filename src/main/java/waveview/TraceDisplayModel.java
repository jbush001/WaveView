//
// Copyright 2011-2012 Jeff Bush
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

package waveview;

import java.util.ArrayList;

///
/// TraceDisplayModel contains visible state for a waveform capture
/// (e.g. Cursor position, scale, visible nets, etc.)
///

public class TraceDisplayModel {
    private final ArrayList<Listener> traceListeners = new ArrayList<>();
    private ArrayList<NetViewModel> visibleNets = new ArrayList<>();
    private final ArrayList<NetSet> netSets = new ArrayList<>();
    private long cursorPosition;
    private long selectionStart;
    private double horizontalScale; // Pixels per time units
    private boolean adjustingCursor;
    private final SortedArrayList<Marker> markers = new SortedArrayList<>();
    private int nextMarkerId = 1;
    private long minorTickInterval;

    public interface Listener {
        void cursorChanged(long oldTimestamp, long newTimestamp);

        void netsAdded(int firstIndex, int lastIndex);

        void netsRemoved(int firstIndex, int lastIndex);

        void scaleChanged(double newScale);

        void markerChanged(long timestamp);

        void formatChanged(int index);
    };

    public TraceDisplayModel() {
        setHorizontalScale(10.0);
    }

    public void clear() {
        int oldSize = visibleNets.size();

        visibleNets.clear();
        markers.clear();
        netSets.clear();
        for (Listener listener : traceListeners) {
            listener.netsRemoved(0, oldSize);
        }
    }

    public void addListener(Listener listener) {
        traceListeners.add(listener);
    }

    // @param scale Pixels per time unit
    public void setHorizontalScale(double scale) {
        horizontalScale = scale;
        minorTickInterval = (int) Math.pow(10, Math.ceil(Math.log10(DrawMetrics.MIN_MINOR_TICK_H_SPACE / scale)));
        if (minorTickInterval <= 0) {
            minorTickInterval = 1;
        }

        for (Listener listener : traceListeners) {
            listener.scaleChanged(scale);
        }
    }

    // @returns Pixels per time unit
    public double getHorizontalScale() {
        return horizontalScale;
    }

    // @returns Duration between horizontal ticks, in time units
    public long getMinorTickInterval() {
        return minorTickInterval;
    }

    public void makeNetVisible(int netId) {
        makeNetVisible(visibleNets.size(), netId);
    }

    public void makeNetVisible(int aboveIndex, int netId) {
        visibleNets.add(aboveIndex, new NetViewModel(netId));
        for (Listener listener : traceListeners) {
            listener.netsAdded(visibleNets.size() - 1, visibleNets.size() - 1);
        }
    }

    public void removeNet(int listIndex) {
        visibleNets.remove(listIndex);
        for (Listener listener : traceListeners) {
            listener.netsRemoved(listIndex, listIndex);
        }
    }

    public void removeAllNets() {
        int oldSize = visibleNets.size();
        visibleNets.clear();
        if (oldSize > 0) {
            for (Listener listener : traceListeners) {
                listener.netsRemoved(0, oldSize - 1);
            }
        }
    }

    public void moveNets(int[] fromIndices, int insertionPoint) {
        NetViewModel[] nets = new NetViewModel[fromIndices.length];
        for (int i = fromIndices.length - 1; i >= 0; i--) {
            nets[i] = visibleNets.get(fromIndices[i]);
            removeNet(fromIndices[i]);
            if (fromIndices[i] < insertionPoint) {
                insertionPoint--;
            }
        }

        for (NetViewModel net : nets) {
            visibleNets.add(insertionPoint++, net);
        }

        for (Listener listener : traceListeners) {
            listener.netsAdded(insertionPoint - fromIndices.length, insertionPoint - 1);
        }
    }

    public int getVisibleNetCount() {
        return visibleNets.size();
    }

    /// Return mapping of visible order to internal index
    /// @param index Index of net in order displayed in net list
    /// @returns netID (as referenced in TraceDataModel)
    public int getVisibleNet(int index) {
        return visibleNets.get(index).index;
    }

    public void setValueFormatter(int listIndex, ValueFormatter formatter) {
        visibleNets.get(listIndex).formatter = formatter;
        for (Listener listener : traceListeners) {
            listener.formatChanged(listIndex);
        }
    }

    public ValueFormatter getValueFormatter(int listIndex) {
        return visibleNets.get(listIndex).formatter;
    }

    public int getNetSetCount() {
        return netSets.size();
    }

    public String getNetSetName(int index) {
        return netSets.get(index).name;
    }

    public void selectNetSet(int index) {
        int oldSize = visibleNets.size();

        visibleNets = new ArrayList<>(netSets.get(index).visibleNets);

        // @todo There is probably a more efficient way to do this
        for (Listener listener : traceListeners) {
            listener.netsRemoved(0, oldSize);
            listener.netsAdded(0, visibleNets.size() - 1);
        }
    }

    /// Saves the current view configuration as a named net set
    public void saveNetSet(String name) {
        NetSet newNetSet = new NetSet(name, visibleNets);

        // Determine if we should save over an existing net set...
        boolean found = false;
        for (int i = 0; i < getNetSetCount(); i++) {
            if (getNetSetName(i).equals(name)) {
                netSets.set(i, newNetSet);
                found = true;
                break;
            }
        }

        if (!found)
            netSets.add(newNetSet);
    }

    public long getCursorPosition() {
        return cursorPosition;
    }

    public void setCursorPosition(long timestamp) {
        long old = cursorPosition;
        cursorPosition = timestamp;
        for (Listener listener : traceListeners) {
            listener.cursorChanged(old, timestamp);
        }
    }

    // This is used to display the timestamp at the top of the cursor when the user
    // is dragging.
    public void setAdjustingCursor(boolean adjust) {
        adjustingCursor = adjust;

        /// @bug This is a hacky way to force everyone to update, but has odd
        /// side effects if it is done in the wrong order. There should most
        /// likely be another way to do this, like, for example, another event
        /// to notify clients that the cursor is in the adjusting state.
        setCursorPosition(cursorPosition);
    }

    public boolean isAdjustingCursor() {
        return adjustingCursor;
    }

    public long getSelectionStart() {
        return selectionStart;
    }

    public void setSelectionStart(long timestamp) {
        selectionStart = timestamp;
    }

    public void removeAllMarkers() {
        markers.clear();
        notifyMarkerChanged(-1);
        nextMarkerId = 1;
    }

    private void notifyMarkerChanged(long timestamp) {
        for (Listener listener : traceListeners) {
            listener.markerChanged(timestamp);
        }
    }

    // XXX clients of this just call getCursorPosition and pass that
    // to timestamp. Should the second parameter be removed?
    // XXX also, if there is another marker that is very close, should
    // we detect that somehow?
    public void addMarker(String description, long timestamp) {
        Marker marker = new Marker();
        marker.id = nextMarkerId++;
        marker.description = description;
        marker.timestamp = timestamp;

        markers.add(marker);
        notifyMarkerChanged(timestamp);
    }

    public int getMarkerAtTime(long timestamp) {
        return markers.findIndex(timestamp);
    }

    /// @returns -1 if no marker was found near this timestamp. The index
    /// into the list of markers otherwise.
    private int findMarkerNear(long timestamp) {
        if (markers.size() == 0) {
            return -1;
        }

        // Because it's hard to click exactly on the marker, allow removing
        // markers a few pixels to the right or left of the current cursor.
        final long MARKER_REMOVE_SLACK = (long) (5.0 / getHorizontalScale());

        int index = markers.findIndex(timestamp);
        long targetTimestamp = markers.get(index).timestamp;

        // The lookup function sometimes rounds to the lower marker, so
        // check both the current marker and the next one.
        if (Math.abs(timestamp - targetTimestamp) <= MARKER_REMOVE_SLACK) {
            return index;
        } else if (index < markers.size() - 1) {
            targetTimestamp = markers.get(index + 1).timestamp;
            if (Math.abs(timestamp - targetTimestamp) <= MARKER_REMOVE_SLACK) {
                return index + 1;
            }
        }

        return -1;
    }

    public void removeMarkerAtTime(long timestamp) {
        int index = findMarkerNear(timestamp);
        if (index != -1) {
            long actualTimestamp = markers.get(index).timestamp;
            markers.remove(index);
            notifyMarkerChanged(actualTimestamp);
        }
    }

    public String getDescriptionForMarker(int index) {
        return markers.get(index).description;
    }

    public void setDescriptionForMarker(int index, String description) {
        markers.get(index).description = description;
    }

    public long getTimestampForMarker(int index) {
        return markers.get(index).timestamp;
    }

    public int getIdForMarker(int index) {
        return markers.get(index).id;
    }

    public int getMarkerCount() {
        return markers.size();
    }

    public void prevMarker(boolean extendSelection) {
        int id = getMarkerAtTime(getCursorPosition()); // Rounds back
        long timestamp = getTimestampForMarker(id);
        if (timestamp >= getCursorPosition() && id > 0) {
            id--;
            timestamp = getTimestampForMarker(id);
        }

        setCursorPosition(timestamp);
        if (!extendSelection) {
            setSelectionStart(timestamp);
        }
    }

    public void nextMarker(boolean extendSelection) {
        int id = getMarkerAtTime(getCursorPosition()); // Rounds back
        if (id < getMarkerCount() - 1) {
            long timestamp = getTimestampForMarker(id);
            if (timestamp <= getCursorPosition()) {
                id++;
                timestamp = getTimestampForMarker(id);
            }

            setCursorPosition(timestamp);
            if (!extendSelection) {
                setSelectionStart(timestamp);
            }
        }
    }

    private static class Marker implements SortedArrayList.Keyed {
        int id;
        String description;
        long timestamp;

        @Override
        public long getKey() {
            return timestamp;
        }
    }

    private static class NetSet {
        final ArrayList<NetViewModel> visibleNets;
        private final String name;

        NetSet(String name, ArrayList<NetViewModel> visibleNets) {
            this.name = name;
            this.visibleNets = new ArrayList<>(visibleNets);
        }
    }

    private static class NetViewModel {
        private int index;
        private ValueFormatter formatter = new HexadecimalValueFormatter();

        NetViewModel(int index) {
            this.index = index;
        }
    }
}

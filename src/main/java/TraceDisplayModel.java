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

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.io.*;
import java.net.*;

///
/// TraceDisplayModel contains visible state for a waveform capture
/// (e.g. Cursor position, scale, visible nets, etc.)
///

public class TraceDisplayModel {
    interface Listener {
        public void cursorChanged(long oldTimestamp, long newTimestamp);
        public void netsAdded(int firstIndex, int lastIndex);
        public void netsRemoved(int firstIndex, int lastIndex);
        public void scaleChanged(double newScale);
        public void markerChanged(long timestamp);
        public void formatChanged(int index);
    };

    public TraceDisplayModel() {
        setHorizontalScale(10.0);
    }

    void clear() {
        int oldSize = fVisibleNets.size();

        fVisibleNets.clear();
        fMarkers.clear();
        fNetSets.clear();
        for (Listener listener : fTraceListeners)
            listener.netsRemoved(0, oldSize);
    }

    public void addListener(Listener listener) {
        fTraceListeners.add(listener);
    }

    // @param scale Nanoseconds per pixel
    public void setHorizontalScale(double scale) {
        fHorizontalScale = scale;
        fMinorTickInterval = (int) Math.pow(10, Math.ceil(Math.log10(
            scale * DrawMetrics.MIN_MINOR_TICK_H_SPACE)));
        if (fMinorTickInterval <= 0)
            fMinorTickInterval = 1;

        for (Listener listener : fTraceListeners)
            listener.scaleChanged(scale);
    }

    // @returns Nanoseconds per pixel
    public double getHorizontalScale() {
        return fHorizontalScale;
    }

    // @returns Duration between horizontal ticks, in nanoseconds
    public long getMinorTickInterval() {
        return fMinorTickInterval;
    }

    public void makeNetVisible(int netId) {
        makeNetVisible(fVisibleNets.size(), netId);
    }

    public void makeNetVisible(int aboveIndex, int netId) {
        fVisibleNets.add(aboveIndex, new NetViewModel(netId, null));
        for (Listener listener : fTraceListeners) {
            listener.netsAdded(fVisibleNets.size() - 1,
                               fVisibleNets.size() - 1);
        }
    }

    public void removeNet(int listIndex) {
        fVisibleNets.remove(listIndex);
        for (Listener listener : fTraceListeners)
            listener.netsRemoved(listIndex, listIndex);
    }

    void removeAllNets() {
        int oldSize = fVisibleNets.size();
        fVisibleNets.clear();
        if (oldSize > 0) {
            for (Listener listener : fTraceListeners)
                listener.netsRemoved(0, oldSize - 1);
        }
    }

    void moveNets(int[] fromIndices, int insertionPoint) {
        NetViewModel[] nets = new NetViewModel[fromIndices.length];
        for (int i = fromIndices.length - 1; i >= 0; i--) {
            nets[i] = fVisibleNets.get(fromIndices[i]);
            removeNet(fromIndices[i]);
            if (fromIndices[i] < insertionPoint)
                insertionPoint--;
        }

        // Add rows at the new location
        for (NetViewModel net : nets)
            fVisibleNets.add(insertionPoint++, net);

        for (Listener listener : fTraceListeners) {
            listener.netsAdded(insertionPoint - fromIndices.length,
                               insertionPoint - 1);
        }
    }

    public int getVisibleNetCount() {
        return fVisibleNets.size();
    }

    /// Return mapping of visible order to internal index
    /// @param index Index of net in order displayed in net list
    /// @returns netID (as referenced in TraceDataModel)
    public int getVisibleNet(int index) {
        return fVisibleNets.get(index).index;
    }

    public void setValueFormatter(int listIndex, ValueFormatter formatter) {
        fVisibleNets.get(listIndex).formatter = formatter;
        for (Listener listener : fTraceListeners)
            listener.formatChanged(listIndex);
    }

    public ValueFormatter getValueFormatter(int listIndex) {
        return fVisibleNets.get(listIndex).formatter;
    }

    public int getNetSetCount() {
        return fNetSets.size();
    }

    public String getNetSetName(int index) {
        return fNetSets.get(index).fName;
    }

    public void selectNetSet(int index) {
        int oldSize = fVisibleNets.size();

        fVisibleNets = new ArrayList<NetViewModel>(fNetSets.get(index).fVisibleNets);

        // @todo There is probably a more efficient way to do this
        for (Listener listener : fTraceListeners) {
            listener.netsRemoved(0, oldSize);
            listener.netsAdded(0, fVisibleNets.size() - 1);
        }
    }

    /// Saves the current view configuration as a named net set
    public void saveNetSet(String name) {
        NetSet newNetSet = new NetSet(name, fVisibleNets);

        // Determine if we should save over an existing net set...
        boolean found = false;
        for (int i = 0; i < getNetSetCount(); i++) {
            if (getNetSetName(i).equals(name)) {
                fNetSets.set(i, newNetSet);
                found = true;
                break;
            }
        }

        if (!found)
            fNetSets.add(newNetSet);
    }

    public long getCursorPosition() {
        return fCursorPosition;
    }

    public void setCursorPosition(long timestamp) {
        long old = fCursorPosition;
        fCursorPosition = timestamp;
        for (Listener listener : fTraceListeners)
            listener.cursorChanged(old, timestamp);
    }

    // This is used to display the timestamp at the top of the cursor when the user is dragging.
    public void setAdjustingCursor(boolean adjust) {
        fAdjustingCursor = adjust;

        /// @bug This is a hacky way to force everyone to update, but has odd side effects if
        /// it is done in the wrong order.  There should most likely be another way to do this,
        /// like, for example, another event to notify clients that the cursor is in the adjusting state.
        setCursorPosition(fCursorPosition);
    }

    public boolean getAdjustingCursor() {
        return fAdjustingCursor;
    }

    public long getSelectionStart() {
        return fSelectionStart;
    }

    public void setSelectionStart(long timestamp) {
        fSelectionStart = timestamp;
    }

    void removeAllMarkers() {
        fMarkers.clear();
        notifyMarkerChanged(-1);
        fNextMarkerId = 1;
    }

    private void notifyMarkerChanged(long timestamp) {
        for (Listener listener : fTraceListeners)
            listener.markerChanged(timestamp);
    }

    // XXX clients of this just call getCursorPosition and pass that
    // to timestamp. Should the second parameter be removed?
    // XXX also, if there is another marker that is very close, should
    // we detect that somehow?
    public void addMarker(String description, long timestamp) {
        Marker marker = new Marker();
        marker.fId = fNextMarkerId++;
        marker.fDescription = description;
        marker.fTimestamp = timestamp;

        fMarkers.add(marker);
        notifyMarkerChanged(timestamp);
    }

    public int getMarkerAtTime(long timestamp) {
        return fMarkers.findIndex(timestamp);
    }

    public void removeMarkerAtTime(long timestamp) {
        if (fMarkers.size() == 0)
            return;

        // Because it's hard to click exactly on the marker, allow removing
        // markers a few pixels to the right or left of the current cursor.
        final int kMarkerRemoveSize = (int)(5.0 * getHorizontalScale());

        int index = fMarkers.findIndex(timestamp);
        long targetTimestamp = fMarkers.get(index).fTimestamp;

        // The lookup function sometimes rounds to the lower marker, so
        // check both the current marker and the next one.
        if (Math.abs(timestamp - targetTimestamp) < kMarkerRemoveSize) {
            fMarkers.remove(index);
            notifyMarkerChanged(targetTimestamp);
        } else if (index < fMarkers.size() - 1) {
            targetTimestamp = fMarkers.get(index + 1).fTimestamp;
            if (Math.abs(timestamp - targetTimestamp) < kMarkerRemoveSize) {
                fMarkers.remove(index + 1);
                notifyMarkerChanged(targetTimestamp);
            }
        }
    }

    public String getDescriptionForMarker(int index) {
        return fMarkers.get(index).fDescription;
    }

    public void setDescriptionForMarker(int index, String description) {
        fMarkers.get(index).fDescription = description;
    }

    public long getTimestampForMarker(int index) {
        return fMarkers.get(index).fTimestamp;
    }

    public int getIdForMarker(int index) {
        return fMarkers.get(index).fId;
    }

    public int getMarkerCount() {
        return fMarkers.size();
    }

    public void prevMarker(boolean extendSelection) {
        int id = getMarkerAtTime(getCursorPosition());    // Rounds back
        long timestamp = getTimestampForMarker(id);
        if (timestamp >= getCursorPosition() && id > 0) {
            id--;
            timestamp = getTimestampForMarker(id);
        }

        setCursorPosition(timestamp);
        if (!extendSelection)
            setSelectionStart(timestamp);
    }

    public void nextMarker(boolean extendSelection) {
        int id = getMarkerAtTime(getCursorPosition());    // Rounds back
        if (id < getMarkerCount() - 1) {
            long timestamp = getTimestampForMarker(id);
            if (timestamp <= getCursorPosition()) {
                id++;
                timestamp = getTimestampForMarker(id);
            }

            setCursorPosition(timestamp);
            if (!extendSelection)
                setSelectionStart(timestamp);
        }
    }

    class Marker implements SortedArrayList.Keyed {
        @Override
        public long getKey() {
            return fTimestamp;
        }

        int fId;
        String fDescription;
        long fTimestamp;
    }

    class NetSet {
        NetSet(String name, ArrayList<NetViewModel> visibleNets) {
            fName = name;
            fVisibleNets = new ArrayList<NetViewModel>(visibleNets);
        }

        private String fName;
        private ArrayList<NetViewModel> fVisibleNets;
    }

    private class NetViewModel {
        public NetViewModel(int _index, ValueFormatter _formatter) {
            index = _index;
            if (_formatter == null)
                formatter = new HexadecimalValueFormatter();
            else
                formatter = _formatter;
        }

        private int index;
        private ValueFormatter formatter;
    }

    private ArrayList<Listener> fTraceListeners = new ArrayList<Listener>();
    private ArrayList<NetViewModel> fVisibleNets = new ArrayList<NetViewModel>();
    private ArrayList<NetSet> fNetSets = new ArrayList<NetSet>();
    private long fCursorPosition;
    private long fSelectionStart;
    private double fHorizontalScale; // Nanoseconds per pixel
    private boolean fAdjustingCursor;
    private SortedArrayList<Marker> fMarkers = new SortedArrayList<Marker>();
    private int fNextMarkerId = 1;
    private long fMinorTickInterval;
};



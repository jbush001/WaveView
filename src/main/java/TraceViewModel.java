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

//
// TraceViewModel contains view state for a waveform capture
//

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.io.*;
import java.net.*;

interface TraceViewModelListener
{
    public void cursorChanged(long oldTimestamp, long newTimestamp);
    public void netsAdded(int firstIndex, int lastIndex);
    public void netsRemoved(int firstIndex, int lastIndex);
    public void scaleChanged(double newScale);
    public void markerChanged(long timestamp);
};

public class TraceViewModel
{
    TraceViewModel()
    {
    }

    void clear()
    {
        int oldSize = fVisibleNets.size();

        fVisibleNets.clear();
        fMarkers.clear();
        fNetSets.clear();
        for (TraceViewModelListener listener : fTraceListeners)
            listener.netsRemoved(0, oldSize);
    }

    public void addListener(TraceViewModelListener listener)
    {
        fTraceListeners.add(listener);
    }

    public void removeListener(TraceViewModelListener listener)
    {
        fTraceListeners.remove(listener);
    }

    public void setHorizontalScale(double scale)
    {
        fHorizontalScale = scale;
        for (TraceViewModelListener listener : fTraceListeners)
            listener.scaleChanged(scale);
    }

    public double getHorizontalScale()
    {
        return fHorizontalScale;
    }

    public void makeNetVisible(int netId)
    {
        makeNetVisible(fVisibleNets.size(), netId);
    }

    public void makeNetVisible(int aboveIndex, int netId)
    {
        fVisibleNets.add(aboveIndex, new NetViewModel(netId, null));
        for (TraceViewModelListener listener : fTraceListeners)
        {
            listener.netsAdded(fVisibleNets.size() - 1,
                fVisibleNets.size() - 1);
        }
    }

    public void removeNet(int listIndex)
    {
        fVisibleNets.remove(listIndex);
        for (TraceViewModelListener listener : fTraceListeners)
            listener.netsRemoved(listIndex, listIndex);
    }

    void removeAllNets()
    {
        int oldSize = fVisibleNets.size();
        fVisibleNets.clear();
        for (TraceViewModelListener listener : fTraceListeners)
            listener.netsRemoved(0, oldSize);
    }

    void moveNets(int[] fromIndices, int insertionPoint)
    {
        NetViewModel[] nets = new NetViewModel[fromIndices.length];
        for (int i = fromIndices.length - 1; i >= 0; i--)
        {
            nets[i] = fVisibleNets.elementAt(fromIndices[i]);
            removeNet(fromIndices[i]);
            if (fromIndices[i] < insertionPoint)
                insertionPoint--;
        }

        // Add rows at the new location
        for (NetViewModel net : nets)
            fVisibleNets.add(insertionPoint++, net);

        for (TraceViewModelListener listener : fTraceListeners)
        {
            listener.netsAdded(insertionPoint - fromIndices.length,
                insertionPoint - 1);
        }
    }

    public int getVisibleNetCount()
    {
        return fVisibleNets.size();
    }

    public int getVisibleNet(int index)
    {
        return fVisibleNets.elementAt(index).index;
    }

    public void setValueFormatter(int listIndex, ValueFormatter formatter)
    {
        fVisibleNets.elementAt(listIndex).formatter = formatter;
    }

    public ValueFormatter getValueFormatter(int listIndex)
    {
        return fVisibleNets.elementAt(listIndex).formatter;
    }

    public int getNetSetCount()
    {
        return fNetSets.size();
    }

    public String getNetSetName(int index)
    {
        return fNetSets.elementAt(index).fName;
    }

    public void selectNetSet(int index)
    {
        int oldSize = fVisibleNets.size();

        fVisibleNets = (Vector<NetViewModel>) fNetSets.elementAt(index).fVisibleNets.clone();

        // There is probably a more efficient way to do this
        for (TraceViewModelListener listener : fTraceListeners)
        {
            listener.netsRemoved(0, oldSize);
            listener.netsAdded(0, fVisibleNets.size());
        }
    }

    /// Saves the current view configuration as a named net set
    public void saveNetSet(String name)
    {
        NetSet newNetSet = new NetSet(name, (Vector<NetViewModel>) fVisibleNets.clone());

        // Determine if we should save over an existing net set...
        boolean found = false;
        for (int i = 0; i < getNetSetCount(); i++)
        {
            if (getNetSetName(i).equals(name))
            {
                fNetSets.set(i, newNetSet);
                found = true;
                break;
            }
        }

        if (!found)
            fNetSets.add(newNetSet);
    }

    public long getCursorPosition()
    {
        return fCursorPosition;
    }

    public void setCursorPosition(long timestamp)
    {
        long old = fCursorPosition;
        fCursorPosition = timestamp;
        for (TraceViewModelListener listener : fTraceListeners)
            listener.cursorChanged(old, timestamp);
    }

    // This is used to display the timestamp at the top of the cursor when the user is dragging.
    public void setAdjustingCursor(boolean adjust)
    {
        fAdjustingCursor = adjust;

        /// @bug This is a hacky way to force everyone to update, but has odd side effects if
        /// it is done in the wrong order.  There should most likely be another way to do this,
        /// like, for example, another event to notify clients that the cursor is in the adjusting state.
        setCursorPosition(fCursorPosition);
    }

    public boolean getAdjustingCursor()
    {
        return fAdjustingCursor;
    }

    public long getSelectionStart()
    {
        return fSelectionStart;
    }

    public void setSelectionStart(long timestamp)
    {
        fSelectionStart = timestamp;
    }

    void removeAllMarkers()
    {
        fMarkers.clear();
        for (TraceViewModelListener listener : fTraceListeners)
            listener.markerChanged(-1);

        fNextMarkerId = 1;
    }

    public void addMarker(String description, long timestamp)
    {
        Marker marker = new Marker();
        marker.fId = fNextMarkerId++;
        marker.fDescription = description;
        marker.fTimestamp = timestamp;

        fMarkers.addSorted(timestamp, marker);

        for (TraceViewModelListener listener : fTraceListeners)
            listener.markerChanged(timestamp);
    }

    public int getMarkerAtTime(long timestamp)
    {
        return fMarkers.lookupValue(timestamp);
    }

    public String getDescriptionForMarker(int index)
    {
        return fMarkers.elementAt(index).fDescription;
    }

    public long getTimestampForMarker(int index)
    {
        return fMarkers.elementAt(index).fTimestamp;
    }

    public int getIdForMarker(int index)
    {
        return fMarkers.elementAt(index).fId;
    }

    public int getMarkerCount()
    {
        return fMarkers.size();
    }

    public void prevMarker(boolean extendSelection)
    {
        int id = getMarkerAtTime(getCursorPosition());    // Rounds back
        long timestamp = getTimestampForMarker(id);
        if (timestamp >= getCursorPosition() && id > 0)
        {
            id--;
            timestamp = getTimestampForMarker(id);
        }

        if (timestamp < getCursorPosition())
        {
            setCursorPosition(timestamp);
            if (!extendSelection)
                setSelectionStart(timestamp);
        }
    }

    public void nextMarker(boolean extendSelection)
    {
        int id = getMarkerAtTime(getCursorPosition());    // Rounds back
        if (id < getMarkerCount() - 1)
        {
            long timestamp = getTimestampForMarker(id);
            if (timestamp <= getCursorPosition())
            {
                id++;
                timestamp = getTimestampForMarker(id );
            }

            if (timestamp > getCursorPosition())
            {
                setCursorPosition(timestamp);
                if (!extendSelection)
                    setSelectionStart(timestamp);
            }
        }
    }

    class Marker implements SortedVector.Keyed
    {
        public long getKey()
        {
            return fTimestamp;
        }

        int fId;
        String fDescription;
        long fTimestamp;
    }

    class NetSet
    {
        NetSet(String name, Vector<NetViewModel> visibleNets)
        {
            fName = name;
            fVisibleNets = visibleNets;
        }

        String fName;
        Vector<NetViewModel> fVisibleNets;
    }

    private class NetViewModel
    {
        public NetViewModel(int _index, ValueFormatter _formatter)
        {
            index = _index;
            if (_formatter == null)
                formatter = new HexadecimalValueFormatter();
            else
                formatter = _formatter;
        }

        int index;
        ValueFormatter formatter;
    }

    private Vector<TraceViewModelListener> fTraceListeners = new Vector<TraceViewModelListener>();
    private Vector<NetViewModel> fVisibleNets = new Vector<NetViewModel>();
    private Vector<NetSet> fNetSets = new Vector<NetSet>();
    private long fCursorPosition;
    private long fSelectionStart;
    private double fHorizontalScale = 10.0;
    private boolean fAdjustingCursor;
    private SortedVector<Marker> fMarkers = new SortedVector<Marker>();
    private int fNextMarkerId = 1;
};



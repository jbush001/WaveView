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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;

///
/// Container for the timescale view, net names, and waveforms.
///
class TracePanel extends JPanel {
    private final WaveformPanel waveformPanel;
    private final NetNameList netNameList;
    private final TraceDisplayModel traceDisplayModel;

    TracePanel(TraceDisplayModel traceDisplayModel, TraceDataModel traceDataModel) {
        super(new BorderLayout());

        this.traceDisplayModel = traceDisplayModel;
        waveformPanel = new WaveformPanel(traceDisplayModel, traceDataModel);
        TimescalePanel timescalePanel = new TimescalePanel(traceDisplayModel, traceDataModel);
        JScrollPane scrollPane = new JScrollPane(waveformPanel);
        scrollPane.setColumnHeaderView(timescalePanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(DrawMetrics.WAVEFORM_HEIGHT);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(DrawMetrics.MIN_MINOR_TICK_H_SPACE);

        // Always keep the horizontal scroll bar. In 99% of cases, we need it,
        // and it simplifies the net name layout if we don't need to worry about
        // the wave view changing size.
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        netNameList = new NetNameList(traceDisplayModel, traceDataModel);

        JViewport netNameViewport = new JViewport();
        netNameViewport.setView(netNameList);
        JPanel netNameBorder = new JPanel(new BorderLayout());
        netNameBorder.setBorder(BorderFactory.createLineBorder(Color.black));
        netNameBorder.add(netNameViewport, BorderLayout.CENTER);

        // Need to make sure the net name view is the same size as the waveform view
        // so scrolling will work correctly (otherwise they will be out of sync).
        JPanel netNameContainer = new JPanel(new BorderLayout());
        netNameContainer.add(Box.createVerticalStrut(timescalePanel.getPreferredSize().height), BorderLayout.NORTH);
        netNameContainer.add(netNameBorder, BorderLayout.CENTER);
        netNameContainer.add(Box.createVerticalStrut(scrollPane.getHorizontalScrollBar().getPreferredSize().height),
                BorderLayout.SOUTH);

        // To allow resizing the net name view, put it in the left half of a split pane
        // rather than setting it as the scroll pane's row header. Add a listener for
        // the vertical scrollbar that also controls the net name view.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, netNameContainer, scrollPane);
        add(splitPane, BorderLayout.CENTER);
        scrollPane.getVerticalScrollBar().addAdjustmentListener((ae) ->
                netNameViewport.setViewPosition(new Point(0, ae.getValue())));

        // Need to repaint when scrolling because values on partially visible
        // nets will be centered.
        scrollPane.getHorizontalScrollBar().addAdjustmentListener((ae) ->
                waveformPanel.repaint());
    }

    void zoomIn() {
        setScaleKeepCentered(traceDisplayModel.getHorizontalScale() * 1.25);
    }

    void zoomOut() {
        setScaleKeepCentered(traceDisplayModel.getHorizontalScale() * 0.8);
    }

    void setScaleKeepCentered(double newScale) {
        Rectangle oldVisibleRect = waveformPanel.getVisibleRect();
        long centerTimestamp = (long) ((oldVisibleRect.x + oldVisibleRect.width / 2)
                / traceDisplayModel.getHorizontalScale());

        traceDisplayModel.setHorizontalScale(newScale);

        // Scroll to new center timestamp
        int centerX = (int) (centerTimestamp * newScale);
        Rectangle newVisibleRect = waveformPanel.getVisibleRect();
        newVisibleRect.x = centerX - newVisibleRect.width / 2;

        waveformPanel.scrollRectToVisible(newVisibleRect);
    }

    void zoomToSelection() {
        // Determine what the new size of the selection window should be.
        double oldScale = traceDisplayModel.getHorizontalScale();
        long selectionStartTimestamp = traceDisplayModel.getSelectionStart();
        long cursorPositionTimestamp = traceDisplayModel.getCursorPosition();
        if (selectionStartTimestamp == cursorPositionTimestamp)
            return; // Will do bad things otherwise

        long lowTimestamp = Math.min(selectionStartTimestamp, cursorPositionTimestamp);
        long highTimestamp = Math.max(selectionStartTimestamp, cursorPositionTimestamp);
        int left = (int) (lowTimestamp * oldScale);
        int right = (int) (highTimestamp * oldScale);
        int selectionWidth = right - left;
        int windowWidth = waveformPanel.getVisibleRect().width;

        double newScale = oldScale * ((double) windowWidth / selectionWidth);
        traceDisplayModel.setHorizontalScale(newScale);

        Rectangle newRect = waveformPanel.getVisibleRect();
        newRect.x = (int) (lowTimestamp * newScale);
        newRect.width = (int) (highTimestamp * newScale) - newRect.x;
        waveformPanel.scrollRectToVisible(newRect);
    }

    int[] getSelectedNets() {
        return netNameList.getSelectedIndices();
    }
}

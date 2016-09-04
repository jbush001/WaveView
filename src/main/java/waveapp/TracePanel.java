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

package waveapp;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.event.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;

///
/// Container for the timescale view, net names, and waveforms.
///
class TracePanel extends JPanel {
    TracePanel(TraceDisplayModel displayModel, TraceDataModel dataModel,
    WaveApp waveApp) {
        super(new BorderLayout());

        fWaveApp = waveApp;
        fTraceDisplayModel = displayModel;
        fTraceDataModel = dataModel;
        fWaveformPanel = new WaveformPanel(displayModel, dataModel);
        fTimescalePanel = new TimescalePanel(displayModel, dataModel);
        fScrollPane = new JScrollPane(fWaveformPanel);
        fScrollPane.setColumnHeaderView(fTimescalePanel);
        fScrollPane.setViewportBorder(BorderFactory.createLineBorder(Color.black));
        fScrollPane.getVerticalScrollBar().setUnitIncrement(DrawMetrics.WAVEFORM_HEIGHT);
        fScrollPane.getHorizontalScrollBar().setUnitIncrement(DrawMetrics.MIN_MINOR_TICK_H_SPACE);

        // Always keep the horizontal scroll bar. In 99% of cases, we need it,
        // and it simplifies the net name layout if we don't need to worry about
        // the wave view changing size.
        fScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            fNetNameList = new NetNameList(displayModel, dataModel);

        JViewport netNameViewport = new JViewport();
        netNameViewport.setView(fNetNameList);
        JPanel netNameBorder = new JPanel(new BorderLayout());
        netNameBorder.setBorder(BorderFactory.createLineBorder(Color.black));
        netNameBorder.add(netNameViewport, BorderLayout.CENTER);

        // Need to make sure the net name view is the same size as the waveform view
        // so scrolling will work correctly (otherwise they will be out of sync).
        JPanel netNameContainer = new JPanel(new BorderLayout());
        netNameContainer.add(Box.createVerticalStrut(fTimescalePanel.getPreferredSize().height),
            BorderLayout.NORTH);
        netNameContainer.add(netNameBorder, BorderLayout.CENTER);
        netNameContainer.add(Box.createVerticalStrut(fScrollPane.getHorizontalScrollBar()
            .getPreferredSize().height), BorderLayout.SOUTH);

        // To allow resizing the net name view,  put it in the left half of a split pane
        // rather than setting it as the scroll pane's row header. Add a listener for
        // the vertical scrollbar that also controls the net name view.
        fSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, netNameContainer, fScrollPane);
        add(fSplitPane, BorderLayout.CENTER);
        fScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent ae) {
                netNameViewport.setViewPosition(new Point(0, ae.getValue()));
            }
        });

        fScrollPane.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent ae) {
                // Need to repaint when scrolling because values on partially visible
                // nets will be centered.
                fWaveformPanel.repaint();
            }
        });
    }

    void zoomIn() {
        setScaleKeepCentered(fTraceDisplayModel.getHorizontalScale() * 1.25);
    }

    void zoomOut() {
        setScaleKeepCentered(fTraceDisplayModel.getHorizontalScale() * 0.8);
    }

    void setScaleKeepCentered(double newScale) {
        Rectangle oldVisibleRect = fWaveformPanel.getVisibleRect();
        long centerTimestamp = (long)((oldVisibleRect.x + oldVisibleRect.width / 2)
            / fTraceDisplayModel.getHorizontalScale());

        fTraceDisplayModel.setHorizontalScale(newScale);

        // Scroll to new center timestamp
        int centerX = (int)(centerTimestamp * newScale);
        Rectangle newVisibleRect = fWaveformPanel.getVisibleRect();
        newVisibleRect.x = centerX - newVisibleRect.width / 2;

        fWaveformPanel.scrollRectToVisible(newVisibleRect);
    }

    void zoomToSelection() {
        // Determine what the new size of the selection window should be.
        double oldScale = fTraceDisplayModel.getHorizontalScale();
        long selectionStartTimestamp = fTraceDisplayModel.getSelectionStart();
        long cursorPositionTimestamp = fTraceDisplayModel.getCursorPosition();
        if (selectionStartTimestamp == cursorPositionTimestamp)
            return;    // Will do bad things otherwise

        long lowTimestamp = Math.min(selectionStartTimestamp, cursorPositionTimestamp);
        long highTimestamp = Math.max(selectionStartTimestamp, cursorPositionTimestamp);
        int left = (int)(lowTimestamp * oldScale);
        int right = (int)(highTimestamp * oldScale);
        int selectionWidth = right - left;
        int windowWidth = fWaveformPanel.getVisibleRect().width;

        double newScale = oldScale * ((double) windowWidth / selectionWidth);
        fTraceDisplayModel.setHorizontalScale(newScale);

        Rectangle newRect = fWaveformPanel.getVisibleRect();
        newRect.x = (int)(lowTimestamp * newScale);
        newRect.width = (int)(highTimestamp * newScale) - newRect.x;
        fWaveformPanel.scrollRectToVisible(newRect);
    }

    int[] getSelectedNets() {
        return fNetNameList.getSelectedIndices();
    }

    private JSplitPane fSplitPane;
    private WaveformPanel fWaveformPanel;
    private NetNameList fNetNameList;
    private JScrollPane fScrollPane;
    private TimescalePanel fTimescalePanel;
    private TraceDisplayModel fTraceDisplayModel;
    private TraceDataModel fTraceDataModel;
    private WaveApp fWaveApp;
}

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
import java.awt.event.*;
import java.util.*;

///
/// This view displays the waveforms.
///

public class WaveformView extends JPanel implements MouseListener,
    MouseMotionListener, TraceViewModel.Listener {

    public WaveformView(TraceViewModel traceViewModel, TraceDataModel traceDataModel) {
        fTraceViewModel = traceViewModel;
        fTraceDataModel = traceDataModel;
        traceViewModel.addListener(this);

        setBackground(AppPreferences.getInstance().kBackgroundColor);
        setFont(new Font("SansSerif", Font.PLAIN, 9));
        computeBounds();
        addMouseListener(this);
        addMouseMotionListener(this);
        setAutoscrolls(true);
    }

    private void computeBounds() {
        Dimension d = getPreferredSize();
        d.width = timestampToCoordinate(fTraceDataModel.getMaxTimestamp());
        d.height = fTraceViewModel.getVisibleNetCount() * DrawMetrics.WAVEFORM_V_SPACING;
        setPreferredSize(d);
        validate();
    }

    @Override
    public void cursorChanged(long oldTimestamp, long newTimestamp) {
        int x2 = timestampToCoordinate(newTimestamp);

        Rectangle visibleRect = getVisibleRect();

        if (x2 < visibleRect.x || x2 > visibleRect.x + visibleRect.width) {
            // Cursor is not visible, scroll to
            visibleRect.x = x2 - 50;
            if (visibleRect.x < 0)
                visibleRect.x = 0;

            visibleRect.width = 100;
            scrollRectToVisible(visibleRect);
        } else {
            int x1 = timestampToCoordinate(oldTimestamp);
            int left = Math.min(x1, x2);
            int right = Math.max(x1, x2);
            repaint(left - 1, 0, right - left + 2, getHeight());
        }
    }

    @Override
    public void netsAdded(int firstIndex, int lastIndex) {
        computeBounds();
        repaint();
    }

    @Override
    public void netsRemoved(int firstIndex, int lastIndex) {
        computeBounds();
        Rectangle visibleRect = getVisibleRect();
        Dimension preferredSize = getPreferredSize();
        if (visibleRect.y + visibleRect.height > preferredSize.height) {
            // XXX hack
            // Need to match behavior of NetNameView, inherited from JList.
            // If the list is scrolled to the bottom and items are removed,
            // scroll up so there's no space at the bottom. If we don't do
            // this, the name view and waveform view will become vertically
            // unaligned.
            visibleRect.y -= (visibleRect.y + visibleRect.height) - preferredSize.height;
            scrollRectToVisible(visibleRect);
        } else if (visibleRect.y > getHeight()) {
            // This happens if all nets are removed. Scroll back to top.
            visibleRect.y = 0;
            scrollRectToVisible(visibleRect);
        }

        repaint();
    }

    @Override
    public void markerChanged(long timestamp) {
        if (timestamp < 0)
            repaint();
        else {
            int x = (int)(timestamp / fTraceViewModel.getHorizontalScale());
            repaint(x - 1, 0, 2, getVisibleRect().height);
        }
    }

    @Override
    public void scaleChanged(double newScale) {
        // Adjust size of canvas
        Dimension d = getPreferredSize();
        d.width = timestampToCoordinate(fTraceDataModel.getMaxTimestamp());
        setPreferredSize(d);
        revalidate();
        repaint();
    }

    @Override
    public void formatChanged(int index) {
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        AppPreferences prefs = AppPreferences.getInstance();
        setBackground(prefs.kBackgroundColor);

        super.paintComponent(g);

        Rectangle visibleRect = getVisibleRect();

        // Draw selection
        if (fTraceViewModel.getCursorPosition() != fTraceViewModel.getSelectionStart()) {
            g.setColor(prefs.kSelectionColor);
            int selectionStart =  timestampToCoordinate(fTraceViewModel.getSelectionStart());
            int selectionEnd =  timestampToCoordinate(fTraceViewModel.getCursorPosition());
            int leftEdge = Math.min(selectionStart, selectionEnd);
            int rightEdge = Math.max(selectionStart, selectionEnd);
            g.fillRect(leftEdge, 0, rightEdge - leftEdge, getHeight());
        }

        drawTimingLines(g, visibleRect);

        drawMarkers(g, visibleRect);

        // Draw nets
        int waveformIndex = visibleRect.y / DrawMetrics.WAVEFORM_V_SPACING;
        if (waveformIndex > 0)
            waveformIndex--;

        while (waveformIndex * DrawMetrics.WAVEFORM_V_SPACING < visibleRect.y + visibleRect.height
                && waveformIndex < fTraceViewModel.getVisibleNetCount()) {
            ValueFormatter formatter = fTraceViewModel.getValueFormatter(waveformIndex);
            int netId = fTraceViewModel.getVisibleNet(waveformIndex);
            if (fTraceDataModel.getNetWidth(netId) > 1) {
                fMultiNetPainter.paint(g, fTraceDataModel, netId,
                    waveformIndex * DrawMetrics.WAVEFORM_V_SPACING + DrawMetrics.WAVEFORM_V_GAP,
                    visibleRect, fTraceViewModel.getHorizontalScale(), formatter);
            } else {
                fSingleNetPainter.paint(g, fTraceDataModel, netId,
                    waveformIndex * DrawMetrics.WAVEFORM_V_SPACING + DrawMetrics.WAVEFORM_V_GAP,
                    visibleRect, fTraceViewModel.getHorizontalScale(), formatter);
            }

            waveformIndex++;
        }

        // Draw the cursor (a vertical line that runs from the top to the
        // bottom of the trace).
        g.setColor(prefs.kCursorColor);
        int cursorX = timestampToCoordinate(fTraceViewModel.getCursorPosition());
        g.drawLine(cursorX, visibleRect.y, cursorX, visibleRect.y + visibleRect.height);
    }

    private void drawMarkers(Graphics g, Rectangle visibleRect) {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setStroke(kDashedStroke);
        g2d.setColor(AppPreferences.getInstance().kMarkerColor);

        long startTime = (long)(visibleRect.x * fTraceViewModel.getHorizontalScale());
        long endTime = (long) ((visibleRect.x + visibleRect.width) * fTraceViewModel.getHorizontalScale());

        // Draw Markers
        int markerIndex = fTraceViewModel.getMarkerAtTime(startTime);
        while (markerIndex < fTraceViewModel.getMarkerCount()) {
            long timestamp = fTraceViewModel.getTimestampForMarker(markerIndex);
            if (timestamp > endTime)
                break;

            int x = (int) (timestamp / fTraceViewModel.getHorizontalScale());
            g2d.drawLine(x, 0, x, visibleRect.y + visibleRect.height);
            markerIndex++;
        }

        g2d.setStroke(kSolidStroke);
    }

    private void drawTimingLines(Graphics g, Rectangle visibleRect) {
        double scale = fTraceViewModel.getHorizontalScale();
        long startTime = (long)(visibleRect.x * scale);
        long endTime = (long) ((visibleRect.x + visibleRect.width) * scale);
        int minorTickInterval = (int) fTraceViewModel.getMinorTickInterval();
        int majorTickInterval = minorTickInterval * DrawMetrics.MINOR_TICKS_PER_MAJOR;
        startTime = ((startTime + majorTickInterval - 1) / majorTickInterval) * majorTickInterval;

        g.setColor(AppPreferences.getInstance().kTimingMarkerColor);
        for (long ts = startTime; ts < endTime; ts += majorTickInterval) {
            int x = (int) (ts / scale);
            g.drawLine(x, visibleRect.y, x, visibleRect.y + visibleRect.height);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // set cursor position
        long timestamp = coordinateToTimestamp(e.getX());
        if (fTraceViewModel.getCursorPosition() != fTraceViewModel.getSelectionStart())
            repaint(); // we already had a selection, clear it

        if (!e.isShiftDown())
            fTraceViewModel.setSelectionStart(timestamp);

        fTraceViewModel.setCursorPosition(timestamp);

        fOldCursor = e.getX();

        /// @bug Be sure to do this after setting the position, otherwise the view will jump back to the
        /// old cursor position first and invoke the auto-scroll-to logic.  This is pretty clunky, and
        /// should probably be rethought.
        fTraceViewModel.setAdjustingCursor(true);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        fTraceViewModel.setAdjustingCursor(false);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        long timestamp = coordinateToTimestamp(e.getX());
        fTraceViewModel.setCursorPosition(timestamp);
        fOldCursor = e.getX();

        // Drag scrolling
        Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1);
        scrollRectToVisible(r);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    private long coordinateToTimestamp(int coordinate) {
        return (long)(coordinate * fTraceViewModel.getHorizontalScale());
    }

    private int timestampToCoordinate(long timestamp) {
        return (int)(timestamp / fTraceViewModel.getHorizontalScale());
    }

    private float kDashDescription[] = { 10.0f };
    private Stroke kDashedStroke = new BasicStroke(1, 0, 0, 10, kDashDescription, 0);
    private Stroke kSolidStroke = new BasicStroke(1);
    private SingleNetPainter fSingleNetPainter = new SingleNetPainter();
    private MultiNetPainter fMultiNetPainter = new MultiNetPainter();
    private TraceViewModel fTraceViewModel;
    private TraceDataModel fTraceDataModel;
    private int fOldCursor;
}

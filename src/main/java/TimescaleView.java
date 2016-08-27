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
import java.awt.*;
import java.util.*;
import java.awt.event.*;

///
/// Draws the ruler with times at the top of the trace view.
///
public class TimescaleView extends JPanel implements TraceViewModel.Listener, ActionListener {
    private static final int TIMESTAMP_DISAPPEAR_INTERVAL = 500;

    public TimescaleView(TraceViewModel viewModel, TraceDataModel dataModel) {
        fTraceViewModel = viewModel;
        fTraceDataModel = dataModel;
        fTraceViewModel.addListener(this);
        setBackground(AppPreferences.getInstance().kBackgroundColor);
        setPreferredSize(new Dimension(200, DrawMetrics.TIMESCALE_HEIGHT));
        setFont(new Font("SansSerif", Font.PLAIN, 9));
        scaleChanged(fTraceViewModel.getHorizontalScale());
        fTimestampDisplayTimer.setRepeats(false);
    }

    private void adjustCanvasSize() {
        scaleChanged(fTraceViewModel.getHorizontalScale());
    }

    @Override
    public void cursorChanged(long oldTimestamp, long newTimestamp) {
        int oldX = (int)(oldTimestamp / fTraceViewModel.getHorizontalScale());
        int newX = (int)(newTimestamp / fTraceViewModel.getHorizontalScale());
        int leftEdge = Math.min(oldX, newX) - DrawMetrics.MAX_TIMESTAMP_LABEL_WIDTH;
        int rightEdge = Math.max(oldX, newX) + DrawMetrics.MAX_TIMESTAMP_LABEL_WIDTH;
        repaint(leftEdge, 0, rightEdge - leftEdge, getHeight());
        if (fTraceViewModel.getAdjustingCursor())
            fShowTimestamp = true;
        else
            fTimestampDisplayTimer.start();
    }

    @Override
    public void markerChanged(long timestamp) {
        if (timestamp < 0)
            repaint();
        else {
            int x = (int)(timestamp / fTraceViewModel.getHorizontalScale());
            repaint(x - (DrawMetrics.MAX_MARKER_LABEL_WIDTH / 2), 0,
                DrawMetrics.MAX_MARKER_LABEL_WIDTH,
                DrawMetrics.TIMESCALE_HEIGHT);
        }
    }

    @Override
    public void netsAdded(int firstIndex, int lastIndex) {
        adjustCanvasSize();
    }

    @Override
    public void netsRemoved(int firstIndex, int lastIndex) {
    }

    @Override
    public void scaleChanged(double newScale) {
        // Make sure minor ticks are large enough
        long minorTickInterval = fTraceViewModel.getMinorTickInterval();
        if (minorTickInterval < 100) {
            fUnitMagnitude = 1;
            fUnit = "ns";
        } else if (minorTickInterval < 100000) {
            fUnitMagnitude = 1000;
            fUnit = "us";
        } else if (minorTickInterval < 100000000) {
            fUnitMagnitude = 1000000;
            fUnit = "ms";
        } else {
            fUnitMagnitude = 1000000000;
            fUnit = "s";
        }

        // XXX if things zoom out a lot more, second will be too small.
        // Not sure the best approach for that.

        Dimension d = getPreferredSize();
        d.width = (int)(fTraceDataModel.getMaxTimestamp() / fTraceViewModel.getHorizontalScale());
        setPreferredSize(d);
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(AppPreferences.getInstance().kTraceColor);

        Rectangle visibleRect = getVisibleRect();
        FontMetrics metrics = g.getFontMetrics();
        long minorTickInterval = fTraceViewModel.getMinorTickInterval();

        // The -100 in start time keeps labels that are to the left of the window from not being drawn
        // (which causes artifacts when scrolling).  It needs to be bigger than the largest label.
        long startTime = (long)((visibleRect.x - 100) * fTraceViewModel.getHorizontalScale());
        long endTime = (long) ((visibleRect.x + visibleRect.width) * fTraceViewModel.getHorizontalScale());
        if (startTime < 0)
            startTime = 0;

        startTime = (startTime / minorTickInterval) * minorTickInterval;    // Round to an event tick boundary
        for (long ts = startTime; ts < endTime; ts += minorTickInterval) {
            int x = (int)(ts / fTraceViewModel.getHorizontalScale());
            if ((ts / minorTickInterval) % DrawMetrics.MINOR_TICKS_PER_MAJOR == 0) {
                g.drawLine(x, 5, x, DrawMetrics.TIMESCALE_HEIGHT);
                g.drawString(Long.toString(ts / fUnitMagnitude) + " " + fUnit, x + 3, DrawMetrics.MINOR_TICK_TOP
                             - metrics.getDescent() - 2);
            } else
                g.drawLine(x, DrawMetrics.MINOR_TICK_TOP, x, DrawMetrics.TIMESCALE_HEIGHT);
        }

        // Draw Markers
        int markerIndex = fTraceViewModel.getMarkerAtTime(startTime);
        while (markerIndex < fTraceViewModel.getMarkerCount()) {
            long timestamp = fTraceViewModel.getTimestampForMarker(markerIndex);
            if (timestamp > endTime)
                break;

            String labelString = "" + fTraceViewModel.getIdForMarker(markerIndex);
            int labelWidth = g.getFontMetrics().stringWidth(labelString);
            int x = (int) (timestamp / fTraceViewModel.getHorizontalScale());
            g.setColor(AppPreferences.getInstance().kBackgroundColor);
            g.fillRect(x - (labelWidth / 2 + DrawMetrics.TIMESTAMP_H_GAP),
                DrawMetrics.TIMESCALE_HEIGHT - 12,
                labelWidth + DrawMetrics.TIMESTAMP_H_GAP * 2, 12);
            g.setColor(AppPreferences.getInstance().kTraceColor);
            g.drawRect(x - (labelWidth / 2 + DrawMetrics.TIMESTAMP_H_GAP),
                DrawMetrics.TIMESCALE_HEIGHT - 12,
                labelWidth + DrawMetrics.TIMESTAMP_H_GAP * 2, 12);
            g.setColor(AppPreferences.getInstance().kTraceColor);
            g.drawString(labelString, x - labelWidth / 2, DrawMetrics.TIMESCALE_HEIGHT - 3);
            markerIndex++;
        }

        if (fShowTimestamp) {
            String timeString = "" + (double) fTraceViewModel.getCursorPosition()
                                / fUnitMagnitude + " " + fUnit;
            int timeWidth = g.getFontMetrics().stringWidth(timeString);
            int cursorX = (int) (fTraceViewModel.getCursorPosition()
                                 / fTraceViewModel.getHorizontalScale());
            int labelLeft = cursorX + timeWidth > visibleRect.x + visibleRect.width
                            ? cursorX - timeWidth : cursorX;

            g.setColor(AppPreferences.getInstance().kBackgroundColor);
            g.fillRect(labelLeft - DrawMetrics.TIMESTAMP_H_GAP, DrawMetrics.TIMESCALE_HEIGHT - 15,
                       timeWidth + DrawMetrics.TIMESTAMP_H_GAP * 2, 15);
            g.setColor(AppPreferences.getInstance().kTraceColor);
            g.drawRect(labelLeft - DrawMetrics.TIMESTAMP_H_GAP, DrawMetrics.TIMESCALE_HEIGHT - 15,
                       timeWidth + DrawMetrics.TIMESTAMP_H_GAP * 2, 15);
            g.setColor(AppPreferences.getInstance().kTraceColor);
            g.drawString(timeString, labelLeft + DrawMetrics.TIMESTAMP_H_GAP,
                         DrawMetrics.TIMESCALE_HEIGHT - DrawMetrics.TIMESTAMP_H_GAP);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        fShowTimestamp = false;
        repaint();
    }

    private boolean fShowTimestamp;
    private int fUnitMagnitude = 1;
    private String fUnit = "s";
    private TraceViewModel fTraceViewModel;
    private TraceDataModel fTraceDataModel;
    private int fOldCursor;
    private javax.swing.Timer fTimestampDisplayTimer = new javax.swing.Timer(TIMESTAMP_DISAPPEAR_INTERVAL, this);
}

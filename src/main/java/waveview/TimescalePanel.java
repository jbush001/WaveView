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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

///
/// Draws the ruler with times at the top of the trace view.
///
class TimescalePanel extends JPanel implements TraceDisplayModel.Listener, ActionListener {
    private static final int TIMESTAMP_DISAPPEAR_INTERVAL = 500;

    TimescalePanel(TraceDisplayModel displayModel, TraceDataModel dataModel) {
        fTraceDisplayModel = displayModel;
        fTraceDataModel = dataModel;
        fTraceDisplayModel.addListener(this);
        setBackground(AppPreferences.getInstance().backgroundColor);
        setPreferredSize(new Dimension(200, DrawMetrics.TIMESCALE_HEIGHT));
        setFont(new Font("SansSerif", Font.PLAIN, 9));
        scaleChanged(fTraceDisplayModel.getHorizontalScale());
        fTimestampDisplayTimer.setRepeats(false);
    }

    private void adjustCanvasSize() {
        scaleChanged(fTraceDisplayModel.getHorizontalScale());
    }

    @Override
    public void cursorChanged(long oldTimestamp, long newTimestamp) {
        int oldX = (int)(oldTimestamp * fTraceDisplayModel.getHorizontalScale());
        int newX = (int)(newTimestamp * fTraceDisplayModel.getHorizontalScale());
        int leftEdge = Math.min(oldX, newX) - DrawMetrics.MAX_TIMESTAMP_LABEL_WIDTH;
        int rightEdge = Math.max(oldX, newX) + DrawMetrics.MAX_TIMESTAMP_LABEL_WIDTH;
        repaint(leftEdge, 0, rightEdge - leftEdge, getHeight());

        // When the cursor moves, draw the current time in the timescale
        // for a little while, then hide it when the timer expires.
        if (fTraceDisplayModel.isAdjustingCursor())
            fShowTimestamp = true;
        else
            fTimestampDisplayTimer.start();
    }

    @Override
    public void markerChanged(long timestamp) {
        if (timestamp < 0)
            repaint();
        else {
            int x = (int)(timestamp * fTraceDisplayModel.getHorizontalScale());
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
    public void netsRemoved(int firstIndex, int lastIndex) {}

    @Override
    public void formatChanged(int index) {}

    @Override
    public void scaleChanged(double newScale) {
        // Make sure minor ticks are large enough

        // Convert to femto seconds, compute unit, then convert back to
        // time units.
        long femtoSecondsPerTimeUnit = (long) Math.pow(10, fTraceDataModel.getTimescale() + 15);
        long minorTickIntervalFs = fTraceDisplayModel.getMinorTickInterval()
            * femtoSecondsPerTimeUnit;
        long unitMagnitudeFs;
        if (minorTickIntervalFs < 100L) {
            unitMagnitudeFs = 1L;
            fUnit = "fs";
        } else if (minorTickIntervalFs < 100000L) {
            unitMagnitudeFs = 1000L;
            fUnit = "ps";
        } else if (minorTickIntervalFs < 100000000L) {
            unitMagnitudeFs = 1000000L;
            fUnit = "ns";
        } else if (minorTickIntervalFs < 100000000000L) {
            unitMagnitudeFs = 1000000000L;
            fUnit = "us";
        } else if (minorTickIntervalFs < 100000000000000L) {
            unitMagnitudeFs = 1000000000000L;
            fUnit = "ms";
        } else {
            unitMagnitudeFs = 1000000000000000L;
            fUnit = "s";
        }

        fUnitMagnitude = unitMagnitudeFs / femtoSecondsPerTimeUnit;

        // XXX if things zoom out a lot more, second will be too small.
        // Not sure the best approach for that.

        Dimension d = getPreferredSize();
        d.width = (int)(fTraceDataModel.getMaxTimestamp() * fTraceDisplayModel.getHorizontalScale());
        setPreferredSize(d);
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(AppPreferences.getInstance().traceColor);

        Rectangle visibleRect = getVisibleRect();
        FontMetrics metrics = g.getFontMetrics();
        long minorTickInterval = fTraceDisplayModel.getMinorTickInterval();

        // The -100 in start time keeps labels that are to the left of the window from not being drawn
        // (which causes artifacts when scrolling).  It needs to be bigger than the largest label.
        long startTime = (long)((visibleRect.x - 100) / fTraceDisplayModel.getHorizontalScale());
        long endTime = (long) ((visibleRect.x + visibleRect.width) / fTraceDisplayModel.getHorizontalScale());
        if (startTime < 0)
            startTime = 0;

        double horizontalScale = fTraceDisplayModel.getHorizontalScale();
        startTime = (startTime / minorTickInterval) * minorTickInterval;    // Round to an event tick boundary
        for (long ts = startTime; ts < endTime; ts += minorTickInterval) {
            int x = (int)(ts * horizontalScale);
            if ((ts / minorTickInterval) % DrawMetrics.MINOR_TICKS_PER_MAJOR == 0) {
                g.drawLine(x, 5, x, DrawMetrics.TIMESCALE_HEIGHT);
                g.drawString(Long.toString(ts / fUnitMagnitude) + " " + fUnit, x + 3,
                             DrawMetrics.MINOR_TICK_TOP - metrics.getDescent() - 2);
            } else
                g.drawLine(x, DrawMetrics.MINOR_TICK_TOP, x, DrawMetrics.TIMESCALE_HEIGHT);
        }

        // Draw Markers
        int markerIndex = fTraceDisplayModel.getMarkerAtTime(startTime);
        while (markerIndex < fTraceDisplayModel.getMarkerCount()) {
            long timestamp = fTraceDisplayModel.getTimestampForMarker(markerIndex);
            if (timestamp > endTime)
                break;

            String labelString = Integer.toString(fTraceDisplayModel.getIdForMarker(markerIndex));
            int labelWidth = g.getFontMetrics().stringWidth(labelString);
            int x = (int) (timestamp * horizontalScale);
            g.setColor(AppPreferences.getInstance().backgroundColor);
            g.fillRect(x - (labelWidth / 2 + DrawMetrics.TIMESTAMP_H_GAP),
                DrawMetrics.TIMESCALE_HEIGHT - 12,
                labelWidth + DrawMetrics.TIMESTAMP_H_GAP * 2, 12);
            g.setColor(AppPreferences.getInstance().traceColor);
            g.drawRect(x - (labelWidth / 2 + DrawMetrics.TIMESTAMP_H_GAP),
                DrawMetrics.TIMESCALE_HEIGHT - 12,
                labelWidth + DrawMetrics.TIMESTAMP_H_GAP * 2, 12);
            g.setColor(AppPreferences.getInstance().traceColor);
            g.drawString(labelString, x - labelWidth / 2, DrawMetrics.TIMESCALE_HEIGHT - 3);
            markerIndex++;
        }

        if (fShowTimestamp) {
            String timeString = Double.toString((double) fTraceDisplayModel.getCursorPosition()
                / fUnitMagnitude) + " " + fUnit;
            int timeWidth = g.getFontMetrics().stringWidth(timeString);
            int cursorX = (int) (fTraceDisplayModel.getCursorPosition()
                                 * horizontalScale);
            int labelLeft = cursorX + timeWidth > visibleRect.x + visibleRect.width
                            ? cursorX - timeWidth : cursorX;

            g.setColor(AppPreferences.getInstance().backgroundColor);
            g.fillRect(labelLeft - DrawMetrics.TIMESTAMP_H_GAP, DrawMetrics.TIMESCALE_HEIGHT - 15,
                       timeWidth + DrawMetrics.TIMESTAMP_H_GAP * 2, 15);
            g.setColor(AppPreferences.getInstance().traceColor);
            g.drawRect(labelLeft - DrawMetrics.TIMESTAMP_H_GAP, DrawMetrics.TIMESCALE_HEIGHT - 15,
                       timeWidth + DrawMetrics.TIMESTAMP_H_GAP * 2, 15);
            g.setColor(AppPreferences.getInstance().traceColor);
            g.drawString(timeString, labelLeft + DrawMetrics.TIMESTAMP_H_GAP,
                         DrawMetrics.TIMESCALE_HEIGHT - DrawMetrics.TIMESTAMP_H_GAP);
        }

        // Draw underline
        g.setColor(AppPreferences.getInstance().traceColor);
        g.drawLine(visibleRect.x, DrawMetrics.TIMESCALE_HEIGHT, visibleRect.x
            + visibleRect.width, DrawMetrics.TIMESCALE_HEIGHT);
    }

    /// This is called when the timer expires. It hides the timestamp displayed above
    /// the cursor.
    @Override
    public void actionPerformed(ActionEvent e) {
        fShowTimestamp = false;
        repaint();
    }

    private boolean fShowTimestamp;
    private long fUnitMagnitude = 1;
    private String fUnit = "s";
    private TraceDisplayModel fTraceDisplayModel;
    private TraceDataModel fTraceDataModel;
    private Timer fTimestampDisplayTimer = new Timer(TIMESTAMP_DISAPPEAR_INTERVAL, this);
}

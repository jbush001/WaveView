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

import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.Timer;

///
/// Draws the ruler with times at the top of the trace view.
///
class TimescalePanel extends JPanel implements TraceDisplayModel.Listener, ActionListener {
    private static final int TIMESTAMP_DISAPPEAR_INTERVAL = 500;

    private boolean showTimestamp;
    private long unitMagnitude = 1;
    private String unitName = "s";
    private TraceDisplayModel traceDisplayModel;
    private TraceDataModel traceDataModel;
    private final Timer timestampDisplayTimer = new Timer(TIMESTAMP_DISAPPEAR_INTERVAL, this);

    TimescalePanel(TraceDisplayModel traceDisplayModel, TraceDataModel traceDataModel) {
        this.traceDisplayModel = traceDisplayModel;
        this.traceDataModel = traceDataModel;
        traceDisplayModel.addListener(this);
        setBackground(AppPreferences.getInstance().backgroundColor);
        setPreferredSize(new Dimension(200, DrawMetrics.TIMESCALE_HEIGHT));
        setFont(new Font("SansSerif", Font.PLAIN, 9));
        scaleChanged(traceDisplayModel.getHorizontalScale());
        timestampDisplayTimer.setRepeats(false);
    }

    private void adjustCanvasSize() {
        scaleChanged(traceDisplayModel.getHorizontalScale());
    }

    @Override
    public void cursorChanged(long oldTimestamp, long newTimestamp) {
        int oldX = (int) (oldTimestamp * traceDisplayModel.getHorizontalScale());
        int newX = (int) (newTimestamp * traceDisplayModel.getHorizontalScale());
        int leftEdge = Math.min(oldX, newX) - DrawMetrics.MAX_TIMESTAMP_LABEL_WIDTH;
        int rightEdge = Math.max(oldX, newX) + DrawMetrics.MAX_TIMESTAMP_LABEL_WIDTH;
        repaint(leftEdge, 0, rightEdge - leftEdge, getHeight());

        // When the cursor moves, draw the current time in the timescale
        // for a little while, then hide it when the timer expires.
        if (traceDisplayModel.isAdjustingCursor()) {
            showTimestamp = true;
        } else {
            timestampDisplayTimer.start();
        }
    }

    @Override
    public void markerChanged(long timestamp) {
        if (timestamp < 0) {
            repaint();
        } else {
            int x = (int) (timestamp * traceDisplayModel.getHorizontalScale());
            repaint(x - (DrawMetrics.MAX_MARKER_LABEL_WIDTH / 2), 0, DrawMetrics.MAX_MARKER_LABEL_WIDTH,
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
    public void formatChanged(int index) {
    }

    @Override
    public void scaleChanged(double newScale) {
        // Make sure minor ticks are large enough

        // Convert to femto seconds, compute unit, then convert back to
        // time units.
        long femtoSecondsPerTimeUnit = (long) Math.pow(10, traceDataModel.getTimescale() + 15);
        long minorTickIntervalFs = traceDisplayModel.getMinorTickInterval() * femtoSecondsPerTimeUnit;
        long unitMagnitudeFs;
        if (minorTickIntervalFs < 100L) {
            unitMagnitudeFs = 1L;
            unitName = "fs";
        } else if (minorTickIntervalFs < 100000L) {
            unitMagnitudeFs = 1000L;
            unitName = "ps";
        } else if (minorTickIntervalFs < 100000000L) {
            unitMagnitudeFs = 1000000L;
            unitName = "ns";
        } else if (minorTickIntervalFs < 100000000000L) {
            unitMagnitudeFs = 1000000000L;
            unitName = "us";
        } else if (minorTickIntervalFs < 100000000000000L) {
            unitMagnitudeFs = 1000000000000L;
            unitName = "ms";
        } else {
            unitMagnitudeFs = 1000000000000000L;
            unitName = "s";
        }

        unitMagnitude = unitMagnitudeFs / femtoSecondsPerTimeUnit;

        // XXX if things zoom out a lot more, second will be too small.
        // Not sure the best approach for that.

        Dimension d = getPreferredSize();
        d.width = (int) (traceDataModel.getMaxTimestamp() * traceDisplayModel.getHorizontalScale());
        setPreferredSize(d);
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Rectangle visibleRect = getVisibleRect();
        long minorTickInterval = traceDisplayModel.getMinorTickInterval();

        // The -100 in start time keeps labels that are to the left of the window from
        // not being drawn
        // (which causes artifacts when scrolling). It needs to be bigger than the
        // largest label.
        long startTime = (long) ((visibleRect.x - 100) / traceDisplayModel.getHorizontalScale());
        long endTime = (long) ((visibleRect.x + visibleRect.width) / traceDisplayModel.getHorizontalScale());
        if (startTime < 0) {
            startTime = 0;
        }

        // Round to an event tick boundary
        startTime = (startTime / minorTickInterval) * minorTickInterval;

        g.setColor(AppPreferences.getInstance().traceColor);
        drawTicks(g, startTime, endTime);
        drawMarkers(g, startTime, endTime);
        if (showTimestamp) {
            drawTimestamp(g);
        }

        drawUnderline(g);
    }

    private void drawTicks(Graphics g, long startTime, long endTime) {
        double horizontalScale = traceDisplayModel.getHorizontalScale();
        long minorTickInterval = traceDisplayModel.getMinorTickInterval();
        FontMetrics metrics = g.getFontMetrics();

        for (long ts = startTime; ts < endTime; ts += minorTickInterval) {
            int x = (int) (ts * horizontalScale);
            if ((ts / minorTickInterval) % DrawMetrics.MINOR_TICKS_PER_MAJOR == 0) {
                g.drawLine(x, 5, x, DrawMetrics.TIMESCALE_HEIGHT);
                g.drawString(Long.toString(ts / unitMagnitude) + " " + unitName, x + 3,
                        DrawMetrics.MINOR_TICK_TOP - metrics.getDescent() - 2);
            } else {
                g.drawLine(x, DrawMetrics.MINOR_TICK_TOP, x, DrawMetrics.TIMESCALE_HEIGHT);
            }
        }
    }

    private void drawMarkers(Graphics g, long startTime, long endTime) {
        double horizontalScale = traceDisplayModel.getHorizontalScale();
        int markerIndex = traceDisplayModel.getMarkerAtTime(startTime);
        while (markerIndex < traceDisplayModel.getMarkerCount()) {
            long timestamp = traceDisplayModel.getTimestampForMarker(markerIndex);
            if (timestamp > endTime) {
                break;
            }

            String labelString = Integer.toString(traceDisplayModel.getIdForMarker(markerIndex));
            int labelWidth = g.getFontMetrics().stringWidth(labelString);
            int x = (int) (timestamp * horizontalScale);
            g.setColor(AppPreferences.getInstance().backgroundColor);
            g.fillRect(x - (labelWidth / 2 + DrawMetrics.TIMESTAMP_H_GAP), DrawMetrics.TIMESCALE_HEIGHT - 12,
                    labelWidth + DrawMetrics.TIMESTAMP_H_GAP * 2, 12);
            g.setColor(AppPreferences.getInstance().traceColor);
            g.drawRect(x - (labelWidth / 2 + DrawMetrics.TIMESTAMP_H_GAP), DrawMetrics.TIMESCALE_HEIGHT - 12,
                    labelWidth + DrawMetrics.TIMESTAMP_H_GAP * 2, 12);
            g.setColor(AppPreferences.getInstance().traceColor);
            g.drawString(labelString, x - labelWidth / 2, DrawMetrics.TIMESCALE_HEIGHT - 3);
            markerIndex++;
        }
    }

    private void drawTimestamp(Graphics g) {
        Rectangle visibleRect = getVisibleRect();
        double horizontalScale = traceDisplayModel.getHorizontalScale();
        String timeString = Double.toString((double) traceDisplayModel.getCursorPosition() / unitMagnitude) + " "
                + unitName;
        int timeWidth = g.getFontMetrics().stringWidth(timeString);
        int cursorX = (int) (traceDisplayModel.getCursorPosition() * horizontalScale);
        int labelLeft = cursorX + timeWidth > visibleRect.x + visibleRect.width ? cursorX - timeWidth : cursorX;

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

    private void drawUnderline(Graphics g) {
        Rectangle visibleRect = getVisibleRect();
        g.setColor(AppPreferences.getInstance().traceColor);
        g.drawLine(visibleRect.x, DrawMetrics.TIMESCALE_HEIGHT, visibleRect.x + visibleRect.width,
                DrawMetrics.TIMESCALE_HEIGHT);
    }

    /// This is called when the timer expires. It hides the timestamp displayed
    /// above
    /// the cursor.
    @Override
    public void actionPerformed(ActionEvent e) {
        showTimestamp = false;
        repaint();
    }
}

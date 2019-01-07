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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.Timer;
import waveview.wavedata.WaveformDataModel;

///
/// Draws the ruler with times at the top of the waveform view.
///
final class TimescaleView extends JPanel implements WaveformPresentationModel.Listener {
    private static final int TIMESTAMP_DISAPPEAR_INTERVAL = 500;

    private boolean showTimestamp;
    private long unitMagnitude = 1;
    private String unitName = "s";
    private final WaveformPresentationModel waveformPresentationModel;
    private final WaveformDataModel waveformDataModel;
    private final Timer timestampDisplayTimer;

    TimescaleView(WaveformPresentationModel waveformPresentationModel, WaveformDataModel waveformDataModel) {
        this.waveformPresentationModel = waveformPresentationModel;
        this.waveformDataModel = waveformDataModel;
        waveformPresentationModel.addListener(this);
        setBackground(AppPreferences.getInstance().backgroundColor);
        setPreferredSize(new Dimension(200, DrawMetrics.TIMESCALE_HEIGHT));
        setFont(new Font("SansSerif", Font.PLAIN, 9));
        scaleChanged(waveformPresentationModel.getHorizontalScale());

        // The timestamp is shown when the user clicks to set the cursor position.
        // This timer makes it disappear after a bit.
        timestampDisplayTimer = new Timer(TIMESTAMP_DISAPPEAR_INTERVAL, e -> {
            showTimestamp = false;
            repaint();
        });

        timestampDisplayTimer.setRepeats(false);
    }

    private void adjustCanvasSize() {
        scaleChanged(waveformPresentationModel.getHorizontalScale());
    }

    @Override
    public void cursorChanged(long oldTimestamp, long newTimestamp) {
        int oldX = (int) (oldTimestamp * waveformPresentationModel.getHorizontalScale());
        int newX = (int) (newTimestamp * waveformPresentationModel.getHorizontalScale());
        int leftEdge = Math.min(oldX, newX) - DrawMetrics.MAX_TIMESTAMP_LABEL_WIDTH;
        int rightEdge = Math.max(oldX, newX) + DrawMetrics.MAX_TIMESTAMP_LABEL_WIDTH;
        repaint(leftEdge, 0, rightEdge - leftEdge, getHeight());

        // When the cursor moves, draw the current time in the timescale
        // for a little while, then hide it when the timer expires.
        if (waveformPresentationModel.isAdjustingCursor()) {
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
            int x = (int) (timestamp * waveformPresentationModel.getHorizontalScale());
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
        long femtoSecondsPerTimeUnit = (long) Math.pow(10, waveformDataModel.getTimescale() + 15);
        long minorTickIntervalFs = waveformPresentationModel.getMinorTickInterval() * femtoSecondsPerTimeUnit;
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
        d.width = (int) (waveformDataModel.getMaxTimestamp() * waveformPresentationModel.getHorizontalScale());
        setPreferredSize(d);
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Rectangle visibleRect = getVisibleRect();
        long minorTickInterval = waveformPresentationModel.getMinorTickInterval();

        // The -100 in start time keeps labels that are to the left of the window from
        // not being drawn
        // (which causes artifacts when scrolling). It needs to be bigger than the
        // largest label.
        long startTime = (long) ((visibleRect.x - 100) / waveformPresentationModel.getHorizontalScale());
        long endTime = (long) ((visibleRect.x + visibleRect.width) / waveformPresentationModel.getHorizontalScale());
        if (startTime < 0) {
            startTime = 0;
        }

        // Round to an event tick boundary
        startTime = (startTime / minorTickInterval) * minorTickInterval;

        g.setColor(AppPreferences.getInstance().waveformColor);
        drawTicks(g, startTime, endTime);
        drawMarkers(g, startTime, endTime);
        if (showTimestamp) {
            drawTimestamp(g);
        }

        drawUnderline(g);
    }

    private void drawTicks(Graphics g, long startTime, long endTime) {
        double horizontalScale = waveformPresentationModel.getHorizontalScale();
        long minorTickInterval = waveformPresentationModel.getMinorTickInterval();
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
        double horizontalScale = waveformPresentationModel.getHorizontalScale();
        int markerIndex = waveformPresentationModel.findMarkerAtOrBeforeTime(startTime);
        while (markerIndex < waveformPresentationModel.getMarkerCount()) {
            long timestamp = waveformPresentationModel.getTimestampForMarker(markerIndex);
            if (timestamp > endTime) {
                break;
            }

            String labelString = Integer.toString(waveformPresentationModel.getIdForMarker(markerIndex));
            int labelWidth = g.getFontMetrics().stringWidth(labelString);
            int x = (int) (timestamp * horizontalScale);
            g.setColor(AppPreferences.getInstance().backgroundColor);
            g.fillRect(x - (labelWidth / 2 + DrawMetrics.TIMESTAMP_H_GAP), DrawMetrics.TIMESCALE_HEIGHT - 12,
                    labelWidth + DrawMetrics.TIMESTAMP_H_GAP * 2, 12);
            g.setColor(AppPreferences.getInstance().waveformColor);
            g.drawRect(x - (labelWidth / 2 + DrawMetrics.TIMESTAMP_H_GAP), DrawMetrics.TIMESCALE_HEIGHT - 12,
                    labelWidth + DrawMetrics.TIMESTAMP_H_GAP * 2, 12);
            g.setColor(AppPreferences.getInstance().waveformColor);
            g.drawString(labelString, x - labelWidth / 2, DrawMetrics.TIMESCALE_HEIGHT - 3);
            markerIndex++;
        }
    }

    private void drawTimestamp(Graphics g) {
        Rectangle visibleRect = getVisibleRect();
        double horizontalScale = waveformPresentationModel.getHorizontalScale();
        String timeString = Double.toString((double) waveformPresentationModel.getCursorPosition() / unitMagnitude) + " "
                + unitName;
        int timeWidth = g.getFontMetrics().stringWidth(timeString);
        int cursorX = (int) (waveformPresentationModel.getCursorPosition() * horizontalScale);
        int labelLeft = cursorX + timeWidth > visibleRect.x + visibleRect.width ? cursorX - timeWidth : cursorX;

        g.setColor(AppPreferences.getInstance().backgroundColor);
        g.fillRect(labelLeft - DrawMetrics.TIMESTAMP_H_GAP, DrawMetrics.TIMESCALE_HEIGHT - 15,
                timeWidth + DrawMetrics.TIMESTAMP_H_GAP * 2, 15);
        g.setColor(AppPreferences.getInstance().waveformColor);
        g.drawRect(labelLeft - DrawMetrics.TIMESTAMP_H_GAP, DrawMetrics.TIMESCALE_HEIGHT - 15,
                timeWidth + DrawMetrics.TIMESTAMP_H_GAP * 2, 15);
        g.setColor(AppPreferences.getInstance().waveformColor);
        g.drawString(timeString, labelLeft + DrawMetrics.TIMESTAMP_H_GAP,
                DrawMetrics.TIMESCALE_HEIGHT - DrawMetrics.TIMESTAMP_H_GAP);
    }

    private void drawUnderline(Graphics g) {
        Rectangle visibleRect = getVisibleRect();
        g.setColor(AppPreferences.getInstance().waveformColor);
        g.drawLine(visibleRect.x, DrawMetrics.TIMESCALE_HEIGHT, visibleRect.x + visibleRect.width,
                DrawMetrics.TIMESCALE_HEIGHT);
    }
}

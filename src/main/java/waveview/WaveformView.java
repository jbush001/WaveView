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

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JPanel;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.WaveformDataModel;

final class WaveformView extends JPanel implements MouseListener, MouseMotionListener, WaveformPresentationModel.Listener {

    private static final float[] DOT_DESCRIPTION = { 2.0f, 4.0f };
    private static final Stroke DOTTED_STROKE = new BasicStroke(1, 0, 0, 10, DOT_DESCRIPTION, 0);
    private static final Stroke SOLID_STROKE = new BasicStroke(1);
    private final SingleBitPainter singleBitPainter = new SingleBitPainter();
    private final MultiBitPainter multiBitPainter = new MultiBitPainter();
    private final WaveformPresentationModel waveformPresentationModel;
    private final WaveformDataModel waveformDataModel;

    WaveformView(WaveformPresentationModel waveformPresentationModel, WaveformDataModel waveformDataModel) {
        this.waveformPresentationModel = waveformPresentationModel;
        this.waveformDataModel = waveformDataModel;
        waveformPresentationModel.addListener(this);

        setBackground(AppPreferences.getInstance().backgroundColor);
        setFont(new Font("SansSerif", Font.PLAIN, 9));
        computeBounds();
        addMouseListener(this);
        addMouseMotionListener(this);
        setAutoscrolls(true);
    }

    private void computeBounds() {
        Dimension d = new Dimension();
        d.width = timestampToXCoordinate(waveformDataModel.getMaxTimestamp());
        d.height = waveformPresentationModel.getVisibleNetCount() * DrawMetrics.WAVEFORM_V_SPACING;
        setPreferredSize(d);
        revalidate();
    }

    @Override
    public void cursorChanged(long oldTimestamp, long newTimestamp) {
        int x2 = timestampToXCoordinate(newTimestamp);

        Rectangle visibleRect = getVisibleRect();

        if (x2 < visibleRect.x || x2 > visibleRect.x + visibleRect.width) {
            // Cursor is not visible, scroll to make it.
            visibleRect.x = x2 - 50;
            if (visibleRect.x < 0) {
                visibleRect.x = 0;
            }

            visibleRect.width = 100;
            scrollRectToVisible(visibleRect);
        } else {
            int x1 = timestampToXCoordinate(oldTimestamp);
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
            // this, the name view and waveform view will become misaligned
            // vertically.
            visibleRect.y -= visibleRect.y + visibleRect.height - preferredSize.height;
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
        if (timestamp < 0) {
            repaint();
        } else {
            repaint(timestampToXCoordinate(timestamp) - 1, 0, 2, getVisibleRect().height);
        }
    }

    @Override
    public void scaleChanged(double newScale) {
        // Adjust size of canvas
        computeBounds();
        repaint();
    }

    @Override
    public void formatChanged(int index) {
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        AppPreferences prefs = AppPreferences.getInstance();
        setBackground(prefs.backgroundColor);

        super.paintComponent(g);

        Rectangle visibleRect = getVisibleRect();

        drawSelection(g);
        drawTimingLines(g, visibleRect);
        drawMarkers(g, visibleRect);
        drawNets(g, visibleRect);
        drawCursor(g, visibleRect);
    }

    private void drawSelection(Graphics g) {
        if (waveformPresentationModel.getCursorPosition() != waveformPresentationModel.getSelectionStart()) {
            g.setColor(AppPreferences.getInstance().selectionColor);
            int selectionStart = timestampToXCoordinate(waveformPresentationModel.getSelectionStart());
            int selectionEnd = timestampToXCoordinate(waveformPresentationModel.getCursorPosition());
            int leftEdge = Math.min(selectionStart, selectionEnd);
            int rightEdge = Math.max(selectionStart, selectionEnd);
            g.fillRect(leftEdge, 0, rightEdge - leftEdge, getHeight());
        }
    }

    private void drawTimingLines(Graphics g, Rectangle visibleRect) {
        Graphics2D g2d = (Graphics2D) g;

        double horizontalScale = waveformPresentationModel.getHorizontalScale();
        long startTime = (long) (visibleRect.x / horizontalScale);
        long endTime = (long) ((visibleRect.x + visibleRect.width) / horizontalScale);
        long minorTickInterval = waveformPresentationModel.getMinorTickInterval();
        long majorTickInterval = minorTickInterval * DrawMetrics.MINOR_TICKS_PER_MAJOR;
        startTime = ((startTime + majorTickInterval - 1) / majorTickInterval) * majorTickInterval;

        g2d.setStroke(DOTTED_STROKE);
        g.setColor(AppPreferences.getInstance().timingMarkerColor);
        for (long ts = startTime; ts < endTime; ts += majorTickInterval) {
            int x = (int) (ts * horizontalScale);
            g.drawLine(x, visibleRect.y, x, visibleRect.y + visibleRect.height);
        }

        g2d.setStroke(SOLID_STROKE);
    }

    private void drawMarkers(Graphics g, Rectangle visibleRect) {
        g.setColor(AppPreferences.getInstance().markerColor);

        long startTime = xCoordinateToTimestamp(visibleRect.x);
        long endTime = xCoordinateToTimestamp(visibleRect.x + visibleRect.width);

        int markerIndex = waveformPresentationModel.findMarkerAtOrBeforeTime(startTime);
        while (markerIndex < waveformPresentationModel.getMarkerCount()) {
            long timestamp = waveformPresentationModel.getTimestampForMarker(markerIndex);
            if (timestamp > endTime) {
                break;
            }

            int x = timestampToXCoordinate(timestamp);
            g.drawLine(x, 0, x, visibleRect.y + visibleRect.height);
            markerIndex++;
        }
    }

    private void drawNets(Graphics g, Rectangle visibleRect) {
        int waveformIndex = visibleRect.y / DrawMetrics.WAVEFORM_V_SPACING;
        if (waveformIndex > 0) {
            waveformIndex--;
        }

        double horizontalScale = waveformPresentationModel.getHorizontalScale();
        while (waveformIndex * DrawMetrics.WAVEFORM_V_SPACING < visibleRect.y + visibleRect.height
                && waveformIndex < waveformPresentationModel.getVisibleNetCount()) {
            ValueFormatter formatter = waveformPresentationModel.getValueFormatter(waveformIndex);
            NetDataModel netDataModel = waveformPresentationModel.getVisibleNet(waveformIndex);
            if (netDataModel.getWidth() > 1) {
                multiBitPainter.paint(g, netDataModel,
                        waveformIndex * DrawMetrics.WAVEFORM_V_SPACING + DrawMetrics.WAVEFORM_V_GAP, visibleRect,
                        horizontalScale, formatter);
            } else {
                singleBitPainter.paint(g, netDataModel,
                        waveformIndex * DrawMetrics.WAVEFORM_V_SPACING + DrawMetrics.WAVEFORM_V_GAP, visibleRect,
                        horizontalScale, formatter);
            }

            waveformIndex++;
        }
    }

    private void drawCursor(Graphics g, Rectangle visibleRect) {
        g.setColor(AppPreferences.getInstance().cursorColor);
        int cursorX = timestampToXCoordinate(waveformPresentationModel.getCursorPosition());
        g.drawLine(cursorX, visibleRect.y, cursorX, visibleRect.y + visibleRect.height);
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
        long timestamp = xCoordinateToTimestamp(e.getX());
        if (waveformPresentationModel.getCursorPosition() != waveformPresentationModel.getSelectionStart()) {
            repaint(); // we already had a selection, clear it
        }

        boolean extendSelection = e.isShiftDown();
        waveformPresentationModel.setCursorPosition(timestamp, extendSelection);

        // Do this after setting cursor position, otherwise it will jump to the old
        // cursor position (as it send a cursor position update as a hacky way to
        // force the timestamp view to update).
        waveformPresentationModel.setAdjustingCursor(true);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        waveformPresentationModel.setAdjustingCursor(false);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        long timestamp = xCoordinateToTimestamp(e.getX());
        boolean extendSelection = e.isShiftDown();
        waveformPresentationModel.setCursorPosition(timestamp, extendSelection);

        Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1);
        scrollRectToVisible(r);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    private long xCoordinateToTimestamp(int coordinate) {
        return (long) (coordinate / waveformPresentationModel.getHorizontalScale());
    }

    private int timestampToXCoordinate(long timestamp) {
        return (int) (timestamp * waveformPresentationModel.getHorizontalScale());
    }
}

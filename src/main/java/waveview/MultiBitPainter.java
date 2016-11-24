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

import java.awt.*;
import java.util.*;

///
/// Delegate that draws the waveform for a single net that has more than one
/// bit in it.
///
class MultiBitPainter implements WaveformPainter {
    @Override
    public void paint(Graphics g, TraceDataModel model, int netId,
                      int topOffset, Rectangle visibleRect, double horizontalScale,
                      ValueFormatter formatter) {
        FontMetrics metrics = g.getFontMetrics();
        int fontBaseline = topOffset + (DrawMetrics.WAVEFORM_HEIGHT + metrics.getHeight()) / 2
            - metrics.getDescent();

        boolean lastValueWasZ = false;
        boolean lastValueWasX = false;
        int lastX = visibleRect.x + visibleRect.width;    // Don't draw before the first segment
        String previousValue = "";
        long firstTimestamp = (long)(visibleRect.x / horizontalScale);

        g.setColor(AppPreferences.getInstance().traceColor);

        Iterator<Transition> i = model.findTransition(netId, firstTimestamp);
        while (true) {
            // Draw the segment to the left of this transition
            Transition transition = i.next();

            boolean isZ = transition.isZ();
            boolean isX = !isZ && transition.isX();

            // Compute the boundaries of this segment
            int x = (int)(transition.getTimestamp() * horizontalScale);

            // Draw transition
            if (x - lastX > DrawMetrics.WAVEFORM_TRANSITION_WIDTH * 2) {
                if (!lastValueWasZ) {
                    if (lastValueWasX) {
                        // Fill in transition twiddle with gray
                        fPolygonXPoints[0] = x - DrawMetrics.WAVEFORM_TRANSITION_WIDTH;
                        fPolygonYPoints[0] = topOffset;
                        fPolygonXPoints[1] = x;
                        fPolygonYPoints[1] = topOffset + DrawMetrics.WAVEFORM_HEIGHT / 2;
                        fPolygonXPoints[2] = x - DrawMetrics.WAVEFORM_TRANSITION_WIDTH;
                        fPolygonYPoints[2] = topOffset + DrawMetrics.WAVEFORM_HEIGHT;
                        g.fillPolygon(fPolygonXPoints, fPolygonYPoints, 3);
                        g.setColor(AppPreferences.getInstance().conflictColor);
                        g.fillPolygon(fPolygonXPoints, fPolygonYPoints, 3);
                        g.setColor(AppPreferences.getInstance().traceColor);
                    }

                    // Draw transition twiddle (left half)
                    g.drawLine(x - DrawMetrics.WAVEFORM_TRANSITION_WIDTH, topOffset, x,
                        topOffset + DrawMetrics.WAVEFORM_HEIGHT / 2);
                    g.drawLine(x - DrawMetrics.WAVEFORM_TRANSITION_WIDTH, topOffset
                        + DrawMetrics.WAVEFORM_HEIGHT, x, topOffset
                        + DrawMetrics.WAVEFORM_HEIGHT / 2);
                }

                if (!isZ) {
                    if (isX) {
                        // Fill in transition with gray
                        fPolygonXPoints[0] = x + DrawMetrics.WAVEFORM_TRANSITION_WIDTH;
                        fPolygonYPoints[0] = topOffset;
                        fPolygonXPoints[1] = x;
                        fPolygonYPoints[1] = topOffset + DrawMetrics.WAVEFORM_HEIGHT / 2;
                        fPolygonXPoints[2] = x + DrawMetrics.WAVEFORM_TRANSITION_WIDTH;
                        fPolygonYPoints[2] = topOffset + DrawMetrics.WAVEFORM_HEIGHT;
                        g.setColor(AppPreferences.getInstance().conflictColor);
                        g.fillPolygon(fPolygonXPoints, fPolygonYPoints, 3);
                        g.setColor(AppPreferences.getInstance().traceColor);
                    }

                    // Draw transition twiddle (right half)
                    g.drawLine(x + DrawMetrics.WAVEFORM_TRANSITION_WIDTH, topOffset, x,
                        topOffset + DrawMetrics.WAVEFORM_HEIGHT / 2);
                    g.drawLine(x + DrawMetrics.WAVEFORM_TRANSITION_WIDTH, topOffset
                        + DrawMetrics.WAVEFORM_HEIGHT, x, topOffset
                        + DrawMetrics.WAVEFORM_HEIGHT / 2);
                }
            } else if (x > lastX) {
                // Draw squished net values
                g.fillRect(x - DrawMetrics.WAVEFORM_TRANSITION_WIDTH, topOffset,
                    DrawMetrics.WAVEFORM_TRANSITION_WIDTH * 2, DrawMetrics.WAVEFORM_HEIGHT);
            }

            drawSpan(g, Math.max(visibleRect.x, lastX + DrawMetrics.WAVEFORM_TRANSITION_WIDTH),
                     Math.min(visibleRect.x + visibleRect.width, x
                         - DrawMetrics.WAVEFORM_TRANSITION_WIDTH),
                     topOffset, previousValue, lastValueWasZ, lastValueWasX, fontBaseline,
                     metrics);

            // Stop drawing when we've gone past the edge of the viewport
            // (trace is no longer visible).
            if (x > visibleRect.x + visibleRect.width)
                break;

            previousValue = formatter.format(transition);
            lastValueWasZ = isZ;
            lastValueWasX = isX;
            lastX = x;
            if (!i.hasNext()) {
                // End of the trace.  Draw remaining span running off to the right...
                drawSpan(g, Math.max(visibleRect.x,lastX + DrawMetrics.WAVEFORM_TRANSITION_WIDTH),
                         visibleRect.x + visibleRect.width, topOffset, previousValue,
                         lastValueWasZ, lastValueWasX, fontBaseline, metrics);
                break;
            }
        }
    }

    private void drawSpan(Graphics g, int left, int right, int top, String label,
                          boolean isZ, boolean isX, int fontBaseline, FontMetrics metrics) {
        if (right <= left)
            return; // You'll end up with single pixel boogers in some cases otherwise

        if (isZ)
            g.drawLine(left, top + DrawMetrics.WAVEFORM_HEIGHT / 2, right, top
                + DrawMetrics.WAVEFORM_HEIGHT / 2);
        else {
            if (isX) {
                g.setColor(AppPreferences.getInstance().conflictColor);
                g.fillRect(left, top, right - left, DrawMetrics.WAVEFORM_HEIGHT);
                g.setColor(AppPreferences.getInstance().traceColor);
            }

            g.drawLine(left, top, right, top);
            g.drawLine(left, top + DrawMetrics.WAVEFORM_HEIGHT, right, top
                + DrawMetrics.WAVEFORM_HEIGHT);

            // Draw text label with values
            int stringWidth = metrics.stringWidth(label);
            int visibleWidth = right - left;
            if (stringWidth < visibleWidth) {
                // Fits, draw it.
                int fontX = (visibleWidth - stringWidth) / 2 + left;
                g.drawString(label, fontX, fontBaseline);
            } else {
                // Try to squeeze in an ellipsis
                String ellipsis = "\u2026";
                stringWidth = metrics.stringWidth(ellipsis);
                if (stringWidth < visibleWidth) {
                    // At least this fits
                    int fontX = (visibleWidth - stringWidth) / 2 + left;
                    g.drawString("\u2026", fontX, fontBaseline);
                }

                // else we draw no label (it won't fit)
            }
        }
    }

    private int[] fPolygonXPoints = new int[3];
    private int[] fPolygonYPoints = new int[3];
}

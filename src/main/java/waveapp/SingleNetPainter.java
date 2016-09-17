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

import java.awt.*;
import java.util.*;

///
/// Delegate that draws the waveform for a single net that has only one
/// bit in it.
///
class SingleNetPainter implements WaveformPainter {
    @Override
    public void paint(Graphics g, TraceDataModel model, int netId,
                      int topOffset, Rectangle visibleRect, double horizontalScale,
                      ValueFormatter formatter) {
        g.setColor(AppPreferences.getInstance().traceColor);

        int lastValue = 0;
        int lastX = visibleRect.x + visibleRect.width;
        long firstTimestamp = (long)(visibleRect.x / horizontalScale);
        Iterator<Transition> i = model.findTransition(netId, firstTimestamp);
        while (true) {
            Transition transition = i.next();

            // Compute the boundaries of this segment
            int x = (int)(transition.getTimestamp() * horizontalScale);
            int value = transition.getBit(0);

            drawSpan(g, lastValue, lastX, x, topOffset);

            // Draw transition line at beginning of interval
            if (lastValue != value) {
                if (lastValue == BitVector.VALUE_Z && value != BitVector.VALUE_X) {
                    if (value == BitVector.VALUE_0) {
                        g.drawLine(x, topOffset + DrawMetrics.WAVEFORM_HEIGHT / 2, x,
                            topOffset + DrawMetrics.WAVEFORM_HEIGHT);
                    } else
                        g.drawLine(x, topOffset + DrawMetrics.WAVEFORM_HEIGHT / 2, x, topOffset);
                } else if (value == BitVector.VALUE_Z && lastValue != BitVector.VALUE_X) {
                    if (lastValue == BitVector.VALUE_0) {
                        g.drawLine(x, topOffset + DrawMetrics.WAVEFORM_HEIGHT, x,
                            topOffset + DrawMetrics.WAVEFORM_HEIGHT / 2);
                    }
                    else
                        g.drawLine(x, topOffset, x, topOffset + DrawMetrics.WAVEFORM_HEIGHT / 2);
                } else
                    g.drawLine(x, topOffset, x, topOffset + DrawMetrics.WAVEFORM_HEIGHT);
            }

            if (x > visibleRect.x + visibleRect.width)
                break;

            lastValue = value;
            lastX = x;
            if (!i.hasNext()) {
                drawSpan(g, lastValue, x, visibleRect.x + visibleRect.width, topOffset);
                break;
            }
        }
    }

    private void drawSpan(Graphics g, int value, int left, int right, int top) {
        if (left >= right)
            return;

        switch (value) {
        case BitVector.VALUE_1:
            g.drawLine(left, top, right, top);
            break;

        case BitVector.VALUE_0:
            g.drawLine(left, top + DrawMetrics.WAVEFORM_HEIGHT, right,
                top + DrawMetrics.WAVEFORM_HEIGHT);
            break;

        case BitVector.VALUE_Z:
            g.drawLine(left, top + DrawMetrics.WAVEFORM_HEIGHT / 2, right, top
                + DrawMetrics.WAVEFORM_HEIGHT / 2);
            break;

        default:
        case BitVector.VALUE_X:
            g.setColor(AppPreferences.getInstance().conflictColor);
            g.fillRect(left, top, right - left, DrawMetrics.WAVEFORM_HEIGHT);
            g.setColor(AppPreferences.getInstance().traceColor);
            g.drawLine(left, top, right, top);
            g.drawLine(left, top + DrawMetrics.WAVEFORM_HEIGHT, right, top
                + DrawMetrics.WAVEFORM_HEIGHT);
            break;
        }
    }
}

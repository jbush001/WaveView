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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Iterator;
import waveview.wavedata.BitValue;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.Transition;

///
/// Delegate that draws the waveform for a single net that has only one
/// bit in it.
///
final class SingleBitPainter implements WaveformPainter {
    @Override
    public void paint(Graphics g, NetDataModel model, int topOffset,
                      Rectangle visibleRect, double horizontalScale,
                      ValueFormatter formatter) {
        g.setColor(AppPreferences.getInstance().waveformColor);

        BitValue lastValue = BitValue.ZERO;
        int lastX = visibleRect.x + visibleRect.width;
        long firstTimestamp = (long)(visibleRect.x / horizontalScale);
        Iterator<Transition> i = model.findTransition(firstTimestamp);
        while (true) {
            Transition transition = i.next();

            // Compute the boundaries of this segment
            int x = (int)(transition.getTimestamp() * horizontalScale);
            BitValue value = transition.getBit(0);

            drawSpan(g, lastValue, lastX, x, topOffset);

            // Draw transition line at beginning of interval
            drawTransition(g, value, lastValue, x, topOffset);

            if (x > visibleRect.x + visibleRect.width) {
                break;
            }

            lastValue = value;
            lastX = x;
            if (!i.hasNext()) {
                drawSpan(g, lastValue, x, visibleRect.x + visibleRect.width,
                         topOffset);
                break;
            }
        }
    }

    private void drawTransition(Graphics g, BitValue value, BitValue lastValue,
                                int x, int topOffset) {
        if (lastValue != value) {
            if (lastValue == BitValue.Z && value != BitValue.X) {
                if (value == BitValue.ZERO) {
                    g.drawLine(x, topOffset + DrawMetrics.WAVEFORM_HEIGHT / 2,
                               x, topOffset + DrawMetrics.WAVEFORM_HEIGHT);
                } else {
                    g.drawLine(x, topOffset + DrawMetrics.WAVEFORM_HEIGHT / 2,
                               x, topOffset);
                }
            } else if (value == BitValue.Z && lastValue != BitValue.X) {
                if (lastValue == BitValue.ZERO) {
                    g.drawLine(x, topOffset + DrawMetrics.WAVEFORM_HEIGHT, x,
                               topOffset + DrawMetrics.WAVEFORM_HEIGHT / 2);
                } else {
                    g.drawLine(x, topOffset, x,
                               topOffset + DrawMetrics.WAVEFORM_HEIGHT / 2);
                }
            } else {
                g.drawLine(x, topOffset, x,
                           topOffset + DrawMetrics.WAVEFORM_HEIGHT);
            }
        }
    }

    private void drawSpan(Graphics g, BitValue value, int left, int right,
                          int top) {
        if (left >= right)
            return;

        switch (value) {
        case ONE:
            g.drawLine(left, top, right, top);
            break;
        case ZERO:
            g.drawLine(left, top + DrawMetrics.WAVEFORM_HEIGHT, right,
                       top + DrawMetrics.WAVEFORM_HEIGHT);
            break;
        case Z:
            g.drawLine(left, top + DrawMetrics.WAVEFORM_HEIGHT / 2, right,
                       top + DrawMetrics.WAVEFORM_HEIGHT / 2);
            break;
        case X:
        default:
            g.setColor(AppPreferences.getInstance().conflictColor);
            g.fillRect(left, top, right - left, DrawMetrics.WAVEFORM_HEIGHT);
            g.setColor(AppPreferences.getInstance().waveformColor);
            g.drawLine(left, top, right, top);
            g.drawLine(left, top + DrawMetrics.WAVEFORM_HEIGHT, right,
                       top + DrawMetrics.WAVEFORM_HEIGHT);
            break;
        }
    }
}

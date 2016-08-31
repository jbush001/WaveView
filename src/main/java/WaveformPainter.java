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

import java.awt.*;

///
/// Delegate used to draw a waveform for a single net
/// This is subclassed for single and multi-bit nets.
///

interface WaveformPainter {
    /// @param g Graphics context to draw onto
    /// @param model Waveform information will be pulled from this model
    /// @param netId Identifier of net to paint
    /// @param y Vertical offset of top of waveform
    /// @param visibleRect This is used to constrain what subset of the
    ///         wave is drawn.
    /// @param horizontalScale nanoseconds per pixel.
    /// @param formatter Used to convert the BitVector to a readable string
    ///        that is drawn on top of the trace waveform.
    void paint(Graphics g, TraceDataModel model, int netId,
               int y, Rectangle visibleRect, double horizontalScale,
               ValueFormatter formatter);
}

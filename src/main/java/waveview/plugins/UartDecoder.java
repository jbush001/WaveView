//
// Copyright 2019 Jeff Bush
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

package waveview.plugins;

import java.util.Iterator;
import waveview.wavedata.BitValue;
import waveview.wavedata.BitVector;
import waveview.wavedata.Decoder;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.Transition;
import waveview.wavedata.TransitionVector;

// XXX This doesn't have a way of signaling an error, so it makes a best
// effort to decode the value.
public class UartDecoder extends Decoder {
    private NetDataModel data;
    private static final BitVector Z = new BitVector("zzzzzzzz", 2);
    private static final int BITS_PER_BYTE = 8;
    private int baudRate;

    @Override
    public String[] getInputNames() {
        return new String[] {"data"};
    }

    @Override
    public String[] getParamNames() {
        return new String[] {"Baud rate"};
    }

    @Override
    public void setParam(int param, String value) {
        baudRate = Integer.parseInt(value);
    }

    @Override
    public void setInput(int index, NetDataModel data) {
        this.data = data;
    }

    class DataCursor {
        final Iterator<Transition> dataIterator;
        long segmentEnd;
        BitValue currentValue;
        BitValue nextValue;

        DataCursor(TransitionVector vec) {
            dataIterator = data.findTransition(0);
            Transition t = dataIterator.next();
            nextValue = t.getBit(0);
            nextSegment();
        }

        long nextFallingEdge(long timestamp) {
            while (timestamp > segmentEnd || currentValue != BitValue.ONE) {
                if (!dataIterator.hasNext()) {
                    return -1;
                }

                nextSegment();
            }

            long segmentBegin = segmentEnd;
            while (currentValue != BitValue.ZERO) {
                if (!dataIterator.hasNext()) {
                    return -1;
                }

                segmentBegin = segmentEnd;
                nextSegment();
            }

            return segmentBegin;
        }

        // Timestamp must always be >= the last timestamp that was
        // return from nextFallingEdge and the last one that was passed
        // to this function.
        BitValue getValueAt(long timestamp) {
            while (timestamp > segmentEnd && dataIterator.hasNext()) {
                nextSegment();
            }

            return currentValue;
        }

        private void nextSegment() {
            // The iterator is always at the end of the current segment
            currentValue = nextValue;
            Transition t = dataIterator.next();
            nextValue = t.getBit(0);
            segmentEnd = t.getTimestamp();
        }
    }

    @Override
    public TransitionVector decode() {
        int timescale = getTimescale();
        double timeUnitsPerSecond = Math.pow(10, -timescale);
        double timeUnitsPerBit = timeUnitsPerSecond / baudRate;

        TransitionVector.Builder outputBuilder =
            TransitionVector.Builder.createBuilder(BITS_PER_BYTE)
            .appendTransition(0, Z);
        BitVector value = new BitVector(8);
        DataCursor cursor = new DataCursor(data.getTransitionVector());
        long currentTime = 0;
        while (true) {
            long byteStart = cursor.nextFallingEdge(currentTime);
            if (byteStart < 0) {
                break;
            }

            // Sample in middle of bit
            for (int i = 0; i < 8; i++) {
                long sampleTime = byteStart + (long) (((double) i + 1.5)
                    * timeUnitsPerBit);
                value.setBit(i, cursor.getValueAt(sampleTime));
            }

            outputBuilder.appendTransition(byteStart, value);
            currentTime = byteStart + (long)(timeUnitsPerBit * 9.5);
            outputBuilder.appendTransition(currentTime, Z);
        }

        return outputBuilder.getTransitionVector();
    }
}

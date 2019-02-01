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

import waveview.wavedata.BitValue;
import waveview.wavedata.BitVector;
import waveview.wavedata.Decoder;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.SignalCursor;
import waveview.wavedata.TransitionVector;

// XXX This doesn't have a way of signaling an error, so it makes a best
// effort to decode the value.
public class UartDecoder extends Decoder {
    private static final BitVector Z = new BitVector("zzzzzzzz", 2);
    private static final int BITS_PER_BYTE = 8;
    private NetDataModel data;
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

    @Override
    public TransitionVector decode() {
        int timescale = getTimescale();
        double timeUnitsPerSecond = Math.pow(10, -timescale);
        double timeUnitsPerBit = timeUnitsPerSecond / baudRate;
        TransitionVector.Builder outputBuilder =
            TransitionVector.Builder.createBuilder(BITS_PER_BYTE)
            .appendTransition(0, Z);
        BitVector value = new BitVector(8);
        SignalCursor cursor = new SignalCursor(data.getTransitionVector());
        long currentTime = 0;
        int loopDetect = 0;
        while (true) {
            long byteStart = cursor.nextEdge(currentTime, BitValue.ZERO);
            if (byteStart < 0) {
                break;
            }

            // Sample in middle of each bit
            for (int i = 0; i < 8; i++) {
                long sampleTime = byteStart + (long) (((double) i + 1.5)
                    * timeUnitsPerBit);
                value.setBit(i, cursor.getValueAt(sampleTime));
            }

            outputBuilder.appendTransition(byteStart, value);
            outputBuilder.appendTransition(byteStart + (long)(timeUnitsPerBit * 10), Z);
            currentTime = byteStart + (long)(timeUnitsPerBit * 9.5);
        }

        return outputBuilder.getTransitionVector();
    }
}

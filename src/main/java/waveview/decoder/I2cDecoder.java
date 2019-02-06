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

package waveview.decoder;

import waveview.wavedata.BitValue;
import waveview.wavedata.BitVector;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.TransitionVector;

class I2cDecoder extends Decoder {
    private NetDataModel sda;
    private NetDataModel sdc;
    private int maxMessageWidth = 11;

    @Override
    public String[] getInputNames() {
        return new String[] {"sda", "sclk"};
    }

    @Override
    public void setInput(int index, NetDataModel data) {
        switch (index) {
            case 0:
                sda = data;
                break;
            case 1:
                sdc = data;
                break;
            default:
                throw new IllegalArgumentException("Invalid input index for I2cDecoder");
        }
    }

    @Override
    public TransitionVector decode() {
        long timestamp = 0;
        BitVector Z = new BitVector(maxMessageWidth);
        for (int i = 0; i < maxMessageWidth; i++) {
            Z.setBit(i, BitValue.Z);
        }

        System.out.println(Z);
        BitVector value = new BitVector(maxMessageWidth);
        TransitionVector.Builder outputBuilder =
            TransitionVector.Builder.createBuilder(maxMessageWidth)
            .appendTransition(0, Z);
        SignalCursor sdcCursor = new SignalCursor(sdc.getTransitionVector());
        SignalCursor sdaCursor = new SignalCursor(sda.getTransitionVector());

        while (true) {
            // Wait for start transition.
            timestamp = sdaCursor.nextEdge(timestamp, BitValue.ZERO);
            if (timestamp < 0) {
                break;
            }

            if (sdcCursor.getValueAt(timestamp) != BitValue.ONE) {
                // Not valid start condition
                continue;
            }

            long startTime = timestamp;

            // Read data bits
            int dataBitIndex = 0;
            while (timestamp < Long.MAX_VALUE) {
                long clockHigh = sdcCursor.nextEdge(timestamp, BitValue.ONE);
                if (clockHigh < 0) {
                    break;
                }

                long clockLow = sdcCursor.nextEdge(clockHigh, BitValue.ZERO);
                if (clockLow < 0) {
                    clockLow = Long.MAX_VALUE;
                }

                long nextData = sdaCursor.nextEdge(clockHigh, BitValue.ONE);
                if (nextData < 0) {
                    break;
                }

                if (nextData > clockHigh && nextData < clockLow) {
                    // stop condition
                    break;
                }

                BitValue dataValue = sdaCursor.getValueAt(clockHigh);
                if (dataBitIndex < maxMessageWidth) {
                    value.setBit(maxMessageWidth - dataBitIndex++ - 1, dataValue);
                } // otherwise ignore extra data bits (XXX should this be indicated visually?)

                timestamp = clockLow;
            }

            outputBuilder.appendTransition(startTime, value);
            outputBuilder.appendTransition(timestamp, Z);
        }

        return outputBuilder.getTransitionVector();
    }

    @Override
    public String[] getParamNames() {
        return new String[] {"max message width"};
    }

    @Override
    public void setParam(int param, String value) {
        assert param == 0;

        maxMessageWidth = Integer.parseInt(value);
    }
}

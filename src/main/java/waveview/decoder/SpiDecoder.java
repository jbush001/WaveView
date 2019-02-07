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

///
/// Serial Peripheral Interface (SPI) protocol decoder
/// https://en.wikipedia.org/wiki/Serial_Peripheral_Interface
///
public class SpiDecoder extends Decoder {
    private static final BitVector Z = new BitVector("zzzzzzzz", 2);
    private static final int BITS_PER_BYTE = 8;
    private static final BitValue SS_ACTIVE = BitValue.ZERO;
    private NetDataModel ss;
    private NetDataModel sclk;
    private NetDataModel data;
    private BitValue clockPolarity;
    private int bitCount;
    private final BitVector currentByte = new BitVector(BITS_PER_BYTE);
    private long byteStartTime;

    @Override
    public String[] getInputNames() {
        return new String[] {"ss", "sclk", "data"};
    }

    @Override
    public String[] getParamNames() {
        return new String[] {"SPI mode (0-3)"};
    }

    @Override
    public void setParam(int param, String value) {
        assert param == 0;
        switch (value) {
            case "0":
            case "2":
                clockPolarity = BitValue.ONE;
                break;
            case "1":
            case "3":
                clockPolarity = BitValue.ZERO;
                break;

            default:
                throw new IllegalArgumentException("invalid SPI mode (must be 0-3)");
        }
    }

    @Override
    public void setInput(int index, NetDataModel data) {
        if (data.getWidth() != 1) {
            throw new IllegalArgumentException("Invalid net " + data.getFullName()
                + ": must be 1 bit wide");
        }

        switch (index) {
            case 0:
                ss = data;
                break;
            case 1:
                sclk = data;
                break;
            case 2:
                this.data = data;
                break;
            default:
                throw new IllegalArgumentException("invalid input index");
        }
    }

    @Override
    public TransitionVector decode() {
        TransitionVector.Builder outputBuilder =
            TransitionVector.Builder.createBuilder(BITS_PER_BYTE)
            .appendTransition(0, Z);
        SignalCursor ssCursor = new SignalCursor(ss.getTransitionVector());
        SignalCursor dataCursor = new SignalCursor(data.getTransitionVector());
        SignalCursor clockCursor = new SignalCursor(sclk.getTransitionVector());
        long currentTime = ssCursor.nextLevel(0, SS_ACTIVE);

        while (true) {
            currentTime = clockCursor.nextEdge(currentTime, clockPolarity);
            if (currentTime < 0) {
                break;
            }

            if (ssCursor.getValueAt(currentTime) != SS_ACTIVE) {
                // SS is deasserted, scan for when it is active again.
                currentTime = ssCursor.nextLevel(currentTime, SS_ACTIVE);
                if (currentTime < 0) {
                    break;
                }

                continue;
            }

            currentByte.setBit(BITS_PER_BYTE - bitCount++ - 1,
                dataCursor.getValueAt(currentTime));
            if (bitCount == 1) {
                byteStartTime = currentTime;
            } else if (bitCount == BITS_PER_BYTE) {
                outputBuilder.appendTransition(byteStartTime, currentByte);
                outputBuilder.appendTransition(currentTime, Z);
                bitCount = 0;
            }
        }

        return outputBuilder.getTransitionVector();
    }
}

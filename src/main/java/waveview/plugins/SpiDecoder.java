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

import java.lang.IllegalArgumentException;
import java.util.Iterator;
import waveview.wavedata.BitValue;
import waveview.wavedata.BitVector;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.Transition;
import waveview.wavedata.TransitionVector;
import waveview.wavedata.Decoder;

public class SpiDecoder extends Decoder {
    private static final BitVector Z = new BitVector("zzzzzzzz", 2);
    private NetDataModel ss;
    private NetDataModel sclk;
    private NetDataModel data;
    private static final int BITS_PER_BYTE = 8;
    private ValueBuilder valueBuilder = new ValueBuilder();
    private boolean samplePolarity;

    public SpiDecoder() {
        super("SPI");
    }

    static class ValueBuilder {
        private int bitCount;
        private BitVector currentByte = new BitVector(BITS_PER_BYTE);
        private long byteStartTime;
        private TransitionVector.Builder outputBuilder =
            TransitionVector.Builder.createBuilder(BITS_PER_BYTE)
            .appendTransition(0, Z);

        void clockEdge(long timestamp, TransitionVector dataLine) {
            Transition dataVal = dataLine.findTransition(timestamp).next();
            currentByte.setBit(BITS_PER_BYTE - bitCount++ - 1, dataVal.getBit(0));
            if (bitCount == 1) {
                byteStartTime = timestamp;
            } else if (bitCount == BITS_PER_BYTE) {
                outputBuilder.appendTransition(byteStartTime, currentByte);
                outputBuilder.appendTransition(timestamp, Z);
                bitCount = 0;
            }
        }

        TransitionVector getResult() {
            return outputBuilder.getTransitionVector();
        }
    }

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
                samplePolarity = true;
                break;
            case "1":
            case "3":
                samplePolarity = false;
                break;

            default:
                throw new IllegalArgumentException("invalid SPI mode (must be 0-3)");
        }
    }

    @Override
    public void setInput(int index, NetDataModel data) {
        if (data.getWidth() != 1) {
            throw new IllegalArgumentException("Invalid signal " + data.getFullName()
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
                throw new IllegalArgumentException("invalid signal index");
        }
    }

    @Override
    public TransitionVector decode() {
        Iterator<Transition> ssIterator = ss.findTransition(0);
        while (ssIterator.hasNext()) {
            // Find timestamp where SS is asserted
            Transition ssTransition = ssIterator.next();
            if (ssTransition.getBit(0) == BitValue.ONE) {
                // Inactive
                continue;
            }

            // Find timestamp where SS is deasserted
            long startTime = ssTransition.getTimestamp();
            long endTime;
            while (true) {
                if (!ssIterator.hasNext()) {
                    endTime = Long.MAX_VALUE;
                    break;
                }

                ssTransition = ssIterator.next();
                if (ssTransition.getBit(0) == BitValue.ONE) {
                    endTime = ssTransition.getTimestamp();
                    break;
                }
            }

            decodeActiveSegment(startTime + 1, endTime);
        }

        return valueBuilder.getResult();
    }

    private void decodeActiveSegment(long startTime, long endTime) {
        Iterator<Transition> clockIterator = sclk.findTransition(startTime);
        Transition initialTransition = clockIterator.next();
        boolean lastClock = initialTransition.getBit(0) == BitValue.ONE;

        while (clockIterator.hasNext()) {
            Transition currentTransition = clockIterator.next();
            if (currentTransition.getTimestamp() > endTime) {
                break;
            }

            boolean currentClock = currentTransition.getBit(0) == BitValue.ONE;
            if (currentClock != lastClock) {
                if (currentClock == samplePolarity) {
                    valueBuilder.clockEdge(currentTransition.getTimestamp(),
                        data.getTransitionVector());
                }

                lastClock = currentClock;
            }
        }
    }
}

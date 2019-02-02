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

import waveview.wavedata.NetDataModel;
import waveview.wavedata.TransitionVector;

public abstract class Decoder {
    private int timescale;

    public static Decoder createDecoder(String name) {
        switch (name) {
            case "SPI":
                return new SpiDecoder();
            case "UART":
                return new UartDecoder();
            default:
                throw new IllegalArgumentException("unknown decoder name");
        }
    }

    public static String[] getDecoderList() {
        return new String[] {"SPI", "UART"};
    }

    public abstract String[] getInputNames();
    public abstract void setInput(int index, NetDataModel data);
    public abstract TransitionVector decode();
    public abstract String[] getParamNames();
    public abstract void setParam(int param, String value);
    public void setTimescale(int timescale) {
        this.timescale = timescale;
    }

    protected int getTimescale() {
        return timescale;
    }
}

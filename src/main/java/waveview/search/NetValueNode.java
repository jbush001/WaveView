//
// Copyright 2011-2019 Jeff Bush
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

package waveview.search;

import java.util.Iterator;
import waveview.BitVector;
import waveview.NetDataModel;
import waveview.Transition;
import waveview.WaveformDataModel;

final class NetValueNode extends ValueNode {
    private final NetDataModel netDataModel;

    NetValueNode(NetDataModel netDataModel) {
        this.netDataModel = netDataModel;
    }

    @Override
    BitVector evaluate(WaveformDataModel model, long timestamp) {
        Iterator<Transition> i = netDataModel.findTransition(timestamp);
        Transition t = i.next();
        BitVector value = new BitVector(t);
        if (timestamp >= t.getTimestamp()) {
            backwardHint = t.getTimestamp() - 1;
        } else {
            backwardHint = Long.MIN_VALUE;
        }

        if (i.hasNext()) {
            t = i.next();
            forwardHint = t.getTimestamp();
        } else {
            forwardHint = Long.MAX_VALUE;
        }

        return value;
    }

    @Override
    public String toString() {
        return netDataModel.getFullName();
    }
}

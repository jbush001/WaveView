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
import waveview.wavedata.BitVector;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.Transition;

final class NetValueNode extends ValueNode {
    private final NetDataModel netDataModel;
    private final int lowIndex;
    private final int highIndex;

    NetValueNode(NetDataModel netDataModel) {
        this.netDataModel = netDataModel;
        lowIndex = -1;
        highIndex = -1;
    }

    NetValueNode(NetDataModel netDataModel, int lowIndex, int highIndex) {
        this.netDataModel = netDataModel;
        this.lowIndex = lowIndex;
        this.highIndex = highIndex;
    }

    @Override
    BitVector evaluate(long timestamp, SearchHint hint) {
        Iterator<Transition> i = netDataModel.findTransition(timestamp);
        Transition t = i.next();
        BitVector value = new BitVector(t);
        if (timestamp >= t.getTimestamp()) {
            hint.backward = t.getTimestamp() - 1;
        } else {
            hint.backward = Long.MIN_VALUE;
        }

        if (i.hasNext()) {
            t = i.next();
            hint.forward = t.getTimestamp();
        } else {
            hint.forward = Long.MAX_VALUE;
        }

        if (this.lowIndex != -1) {
            return value.slice(lowIndex, highIndex);
        }

        return value;
    }

    @Override
    public String toString() {
        if (this.lowIndex == -1) {
            return netDataModel.getFullName();
        } else {
            return netDataModel.getFullName() + "[" + highIndex + ":" + lowIndex + "]";
        }
    }
}

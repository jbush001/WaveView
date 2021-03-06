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

import waveview.wavedata.BitVector;

/// Represents a constant binary value (literal) in an expression.
final class ConstValueNode extends ValueNode {
    private final BitVector value;

    ConstValueNode(BitVector constValue) {
        value = new BitVector(constValue);
    }

    @Override
    BitVector evaluate(long timestamp, SearchHint hint) {
        hint.backward = Long.MIN_VALUE;
        hint.forward = Long.MAX_VALUE;
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

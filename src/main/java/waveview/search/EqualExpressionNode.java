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

final class EqualExpressionNode extends ComparisonExpressionNode {
    EqualExpressionNode(ValueNode left, ValueNode right) { super(left, right); }

    @Override
    protected boolean doCompare(BitVector value1, BitVector value2) {
        return value1.compare(value2) == 0;
    }

    @Override
    public String toString() {
        return "(eq " + leftChild + " " + rightChild + ")";
    }
}

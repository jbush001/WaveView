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
import waveview.wavedata.WaveformDataModel;

abstract class ComparisonExpressionNode extends BooleanExpressionNode {
    protected final ValueNode leftChild;
    protected final ValueNode rightChild;

    protected ComparisonExpressionNode(ValueNode leftNode, ValueNode rightNode) {
        this.leftChild = leftNode;
        this.rightChild = rightNode;
    }

    @Override
    boolean evaluate(WaveformDataModel model, long timestamp) {
        BitVector leftValue = leftChild.evaluate(model, timestamp);
        BitVector rightValue = rightChild.evaluate(model, timestamp);
        boolean result = doCompare(leftValue, rightValue);
        backwardHint = Math.max(leftChild.backwardHint, rightChild.backwardHint);
        forwardHint = Math.min(leftChild.forwardHint, rightChild.forwardHint);
        return result;
    }

    protected abstract boolean doCompare(BitVector value1, BitVector value2);
}

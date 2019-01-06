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

import waveview.WaveformDataModel;

abstract class LogicalExpressionNode extends BooleanExpressionNode {
    protected final BooleanExpressionNode leftChild;
    protected final BooleanExpressionNode rightChild;

    LogicalExpressionNode(BooleanExpressionNode leftChild, BooleanExpressionNode rightChild) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
    }

    @Override
    boolean evaluate(WaveformDataModel model, long timestamp) {
        boolean leftResult = leftChild.evaluate(model, timestamp);
        boolean rightResult = rightChild.evaluate(model, timestamp);

        forwardHint = nextForwardHint(leftResult, rightResult, leftChild.forwardHint,
                rightChild.forwardHint);
        backwardHint = nextBackwardHint(leftResult, rightResult, leftChild.backwardHint,
                rightChild.backwardHint);

        return compareResults(leftResult, rightResult);
    }

    protected abstract boolean compareResults(boolean value1, boolean value2);

    protected abstract long nextForwardHint(boolean leftResult, boolean rightResult,
            long nextLeftTimestamp, long nextRightTimestamp);
    protected abstract long nextBackwardHint(boolean leftResult, boolean rightResult,
            long nextLeftTimestamp, long nextRightTimestamp);
}

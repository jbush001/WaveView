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

final class AndExpressionNode extends LogicalExpressionNode {
    AndExpressionNode(BooleanExpressionNode left, BooleanExpressionNode right) {
        super(left, right);
    }

    @Override
    protected boolean compareResults(boolean value1, boolean value2) {
        return value1 && value2;
    }

    @Override
    protected long nextForwardHint(boolean leftResult, boolean rightResult, long nextLeftTimestamp,
            long nextRightTimestamp) {
        if (leftResult && rightResult) {
            // Both expressions are true. Either expression changing
            // could make it false.
            return Math.min(nextLeftTimestamp, nextRightTimestamp);
        } else if (leftResult) {
            // Currently false. It can only become true when right result
            // becomes true.
            return nextRightTimestamp;
        } else if (rightResult) {
            // Currently false. It can only become true when left result
            // becomes true.
            return nextLeftTimestamp;
        } else {
            // Both expressions are false. Both must change before this
            // may become true.
            return Math.max(nextLeftTimestamp, nextRightTimestamp);
        }
    }

    // Mirror of above
    @Override
    protected long nextBackwardHint(boolean leftResult, boolean rightResult, long nextLeftTimestamp,
            long nextRightTimestamp) {
        if (leftResult && rightResult) {
            return Math.max(nextLeftTimestamp, nextRightTimestamp);
        } else if (leftResult) {
            return nextRightTimestamp;
        } else if (rightResult) {
            return nextLeftTimestamp;
        } else {
            return Math.min(nextLeftTimestamp, nextRightTimestamp);
        }
    }

    @Override
    public String toString() {
        return "(and " + leftChild + " " + rightChild + ")";
    }
}

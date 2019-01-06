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

final class OrExpressionNode extends LogicalExpressionNode {
    OrExpressionNode(BooleanExpressionNode left, BooleanExpressionNode right) {
        super(left, right);
    }

    @Override
    protected boolean compareResults(boolean value1, boolean value2) {
        return value1 || value2;
    }

    @Override
    protected long nextForwardHint(boolean leftResult, boolean rightResult, long nextLeftTimestamp,
            long nextRightTimestamp) {
        if (leftResult && rightResult) {
            // Both expressions are true. The only way for this to become
            // false is if both change to false.
            return Math.max(nextLeftTimestamp, nextRightTimestamp);
        } else if (leftResult) {
            // Currently true. It can only become false when left result
            // becomes false.
            return nextLeftTimestamp;
        } else if (rightResult) {
            // Currently true. It can only become false when right result
            // becomes false.
            return nextRightTimestamp;
        } else {
            // Both expressions are false. May become true if either subexpression
            // changes.
            return Math.min(nextLeftTimestamp, nextRightTimestamp);
        }
    }

    // Mirror of above
    @Override
    protected long nextBackwardHint(boolean leftResult, boolean rightResult, long nextLeftTimestamp,
            long nextRightTimestamp) {
        if (leftResult && rightResult) {
            return Math.min(nextLeftTimestamp, nextRightTimestamp);
        } else if (leftResult) {
            return nextLeftTimestamp;
        } else if (rightResult) {
            return nextRightTimestamp;
        } else {
            return Math.max(nextLeftTimestamp, nextRightTimestamp);
        }
    }

    @Override
    public String toString() {
        return "(or " + leftChild + " " + rightChild + ")";
    }
}

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

final class AndExpressionNode extends BooleanExpressionNode {
    protected final BooleanExpressionNode leftChild;
    protected final BooleanExpressionNode rightChild;

    AndExpressionNode(BooleanExpressionNode leftChild, BooleanExpressionNode rightChild) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
    }

    @Override
    boolean evaluate(long timestamp) {
        boolean leftResult = leftChild.evaluate(timestamp);
        boolean rightResult = rightChild.evaluate(timestamp);

        if (leftResult && rightResult) {
            // Both expressions are true. Either expression changing
            // could make it false, so choose the nearest event.
            forwardHint = Math.min(leftChild.forwardHint, rightChild.forwardHint);
            backwardHint = Math.max(leftChild.backwardHint, rightChild.backwardHint);
        } else if (leftResult) {
            // Currently false. It can only become true when right result
            // becomes true.
            forwardHint = rightChild.forwardHint;
            backwardHint = rightChild.backwardHint;
        } else if (rightResult) {
            // Currently false. It can only become true when left result
            // becomes true.
            forwardHint = leftChild.forwardHint;
            backwardHint = leftChild.backwardHint;
        } else {
            // Both expressions are false. Both must change before this
            // may become true, so choose the farthest event.
            forwardHint = Math.max(leftChild.forwardHint, rightChild.forwardHint);
            backwardHint = Math.min(leftChild.backwardHint, rightChild.backwardHint);
        }

        return leftResult && rightResult;
    }

    @Override
    public String toString() {
        return "(and " + leftChild + " " + rightChild + ")";
    }
}

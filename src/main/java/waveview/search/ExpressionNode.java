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

class ExpressionNode {
    // These are set as a side effect of evaluating the node at a specific time.
    // They contain the next timestamp where the value of the expression may change.
    // It is guaranteed that no transition will occur sooner than this value.
    long forwardHint;
    long backwardHint;

    protected ExpressionNode() {}
}

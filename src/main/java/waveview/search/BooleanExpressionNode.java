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

abstract class BooleanExpressionNode extends ExpressionNode {
    /// Determine if this subexpression is true at the passed timestamp.
    /// @param timestamp Timestamp and which to evaluate. If timestamp
    /// is at a transition, the value after the transition will be used
    /// @param outHint This will be filled in with the nearest possible
    ///    forward and backward transitions.
    /// @return
    /// - true if the value at the timestamp makes this expression true
    /// - false if the value at the timestamp makes this expression true
    abstract boolean evaluate(long timestamp, SearchHint outHint);
}

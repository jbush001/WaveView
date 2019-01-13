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

abstract class ValueNode extends ExpressionNode {
    /// Determine a signal value at the passed timestamp.
    /// @param timestamp Timestamp and which to evaluate. If timestamp
    /// is at a transition, the value after the transition will be used
    /// @param hint This will be filled in with the nearest possible
    ///    forward and backward transitions.
    /// @return A BitVector with the value.
    ///
    abstract BitVector evaluate(long timestamp, SearchHint hint);
}

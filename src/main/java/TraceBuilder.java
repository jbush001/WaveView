//
// Copyright 2011-2012 Jeff Bush
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

///
/// Loader calls this to copy information from a trace into the model.
///

interface TraceBuilder {
    /// Adds another module to the fully qualified path of any nets
    /// that are subsequently added via newNet.
    void enterModule(String name);

    /// Pops up to the previous module pushed by enterModule.
    void exitModule();

    /// Create a new net
    /// @param shortName Name of the signal. It's called 'short' to distinguish
    ///   it from a dotted name that includes the full hiearchy of containing modules.
    /// @param cloneId If two nets (with different names) are connected together,
    ///   the should share the same transition data. The clone ID will contain the
    ///   ID that was returned from newNet when the original signal was added
    /// @param width Number of bits in this signal.
    /// @returns a unique integer identifier for the newly created net
    int newNet(String shortName, int cloneId, int width);

    /// Return the width of a net that was previously added by newNet
    /// @bug This is a hack.  Clean up.
    int getNetWidth(int netId);

    /// Add a new transition
    /// @param netId Identifier of the net for which the transition takes place. This
    ///    is the value that was returned by newNet.
    /// @param timestamp timestamp of the transition, in nanoseconds from start.
    /// @param values New values the signal will take after the transition.
    void appendTransition(int netId, long timestamp, BitVector values);

    /// Called when all nets and transitions have been added. No other methods
    /// in TraceBuilder will be called after this.
    void loadFinished();
}

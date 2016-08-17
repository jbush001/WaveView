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

//
// This is a builder pattern that the loader calls into to populate
// information from a trace into the model.
//
interface TraceBuilder
{
    void enterModule(String name);
    void exitModule();
    int newNet(String shortName, int cloneId, int width);
    int getNetWidth(int netId);     /// @bug This is a hack.  Clean up.
    void appendTransition(int id, long timestamp, BitVector values);
    void loadFinished();
}
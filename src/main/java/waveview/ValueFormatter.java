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

package waveview;

import waveview.wavedata.BitVector;

///
/// This is the base class for classes that convert a binary representation
/// of 4 valued logic into human readable strings. The idea behind making
/// this a class was to allow dynamically loading classes to decode custom
/// logic types.
///
public interface ValueFormatter {
    String format(BitVector values);
}

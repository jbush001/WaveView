
//
// Copyright 2016 Jeff Bush
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

import org.mockito.ArgumentMatcher;
import waveview.BitVector;

///
/// Matcher for Mockito mock objects that take a BitVector parameter
///
class BitVectorMatcher implements ArgumentMatcher<BitVector> {
    private final BitVector expected;

    public BitVectorMatcher(String expected) {
        this.expected = new BitVector(expected, 2);
    }

    @Override
    public boolean matches(BitVector check) {
        return check.compare(expected) == 0;
    }

    @Override
    public String toString() {
        return expected.toString(2);
    }
}

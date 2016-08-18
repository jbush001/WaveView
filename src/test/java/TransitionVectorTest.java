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

import static org.junit.Assert.*;
import org.junit.*;

public class TransitionVectorTest
{
    @Test public void testFindTransition()
    {
        TransitionVector vec = new TransitionVector(8);
        vec.appendTransition(100, new BitVector("00000001", 2));
        vec.appendTransition(110, new BitVector("00000010", 2));
        vec.appendTransition(111, new BitVector("00001000", 2));
        vec.appendTransition(112, new BitVector("00010000", 2));
        vec.appendTransition(115, new BitVector("00010000", 2));

        // XXX failing test
        // assertEquals(vec.findTransition(99).current().getTimestamp(), 100);

        assertEquals(vec.findTransition(100).current().getTimestamp(), 100);
        assertEquals(vec.findTransition(101).current().getTimestamp(), 100);
        assertEquals(vec.findTransition(105).current().getTimestamp(), 100);
        assertEquals(vec.findTransition(109).current().getTimestamp(), 100);
        assertEquals(vec.findTransition(110).current().getTimestamp(), 110);
        assertEquals(vec.findTransition(111).current().getTimestamp(), 111);
        assertEquals(vec.findTransition(112).current().getTimestamp(), 112);
        assertEquals(vec.findTransition(113).current().getTimestamp(), 112);
        assertEquals(vec.findTransition(116).current().getTimestamp(), 115);
        assertEquals(vec.findTransition(20000).current().getTimestamp(), 115);
    }
}

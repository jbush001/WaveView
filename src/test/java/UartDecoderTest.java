//
// Copyright 2019 Jeff Bush
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import org.junit.Test;
import waveview.wavedata.BitVector;
import waveview.wavedata.Decoder;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.Transition;
import waveview.wavedata.TransitionVector;

public class UartDecoderTest {
    @Test
    public void decode() {
        TransitionVector data = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2)) // stop polarity

            // First byte (0xb1)
            .appendTransition(40, new BitVector("0", 2)) // start bit
            .appendTransition(49, new BitVector("1", 2)) // lsb
            .appendTransition(57, new BitVector("0", 2))
            .appendTransition(66, new BitVector("0", 2))
            .appendTransition(75, new BitVector("0", 2))
            .appendTransition(83, new BitVector("1", 2))
            .appendTransition(92, new BitVector("1", 2))
            .appendTransition(101, new BitVector("0", 2))
            .appendTransition(109, new BitVector("1", 2)) // msb
            .appendTransition(118, new BitVector("1", 2)) // stop bit

            // Second byte (0x6a)
            .appendTransition(200, new BitVector("0", 2)) // start bit
            .appendTransition(209, new BitVector("0", 2)) // lsb
            .appendTransition(217, new BitVector("1", 2))
            .appendTransition(226, new BitVector("0", 2))
            .appendTransition(235, new BitVector("1", 2))
            .appendTransition(243, new BitVector("0", 2))
            .appendTransition(252, new BitVector("1", 2))
            .appendTransition(261, new BitVector("1", 2))
            .appendTransition(269, new BitVector("0", 2)) // msb
            .appendTransition(278, new BitVector("1", 2)) // stop bit
            .getTransitionVector();

        Decoder decoder = Decoder.createDecoder("UART");
        decoder.setTimescale(-6);
        decoder.setParam(0, "115200");
        decoder.setInput(0, new NetDataModel("data", "data", data));
        TransitionVector results = decoder.decode();

        Iterator<Transition> dataIterator = results.findTransition(0);
        Transition t = dataIterator.next();
        assertEquals(0, t.getTimestamp());
        assertEquals("ZZ", t.toString(16));

        t = dataIterator.next();
        assertEquals(40, t.getTimestamp());
        assertEquals("B1", t.toString(16));

        t = dataIterator.next();
        assertEquals(122, t.getTimestamp());
        assertEquals("ZZ", t.toString(16));

        t = dataIterator.next();
        assertEquals(200, t.getTimestamp());
        assertEquals("6A", t.toString(16));

        t = dataIterator.next();
        assertEquals(282, t.getTimestamp());
        assertEquals("ZZ", t.toString(16));
    }

    @Test
    public void getDecoderList() {
        assertTrue(Arrays.binarySearch(Decoder.getDecoderList(), "UART") >= 0);
    }
}

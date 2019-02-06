
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import org.junit.Test;
import waveview.decoder.Decoder;
import waveview.wavedata.BitVector;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.Transition;
import waveview.wavedata.TransitionVector;

public class UartDecoderTest {
    final BitVector ZERO = new BitVector("0", 2);
    final BitVector ONE = new BitVector("1", 2);;

    @Test
    public void decode() {
        TransitionVector data = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, ONE) // stop polarity

            // First byte (0xb1)
            .appendTransition(40, ZERO) // start bit
            .appendTransition(49, ONE) // lsb
            .appendTransition(57, ZERO)
            .appendTransition(83, ONE)
            .appendTransition(92, ONE)
            .appendTransition(101, ZERO)
            .appendTransition(109, ONE) // msb & stop bit

            // Second byte (0x6a)
            .appendTransition(200, ZERO) // start bit & lsb
            .appendTransition(217, ONE)
            .appendTransition(226, ZERO)
            .appendTransition(235, ONE)
            .appendTransition(243, ZERO)
            .appendTransition(252, ONE)
            .appendTransition(269, ZERO) // msb
            .appendTransition(278, ONE) // stop bit
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
        assertEquals(126, t.getTimestamp());
        assertEquals("ZZ", t.toString(16));

        t = dataIterator.next();
        assertEquals(200, t.getTimestamp());
        assertEquals("6A", t.toString(16));

        t = dataIterator.next();
        assertEquals(286, t.getTimestamp());
        assertEquals("ZZ", t.toString(16));
    }

    @Test
    public void getDecoderList() {
        assertTrue(Arrays.binarySearch(Decoder.getDecoderList(), "UART") >= 0);
    }

    @Test
    public void getInputNames() {
        Decoder decoder = Decoder.createDecoder("UART");
        String[] expect = {"data"};
        assertArrayEquals(expect, decoder.getInputNames());
    }

    @Test
    public void getParamNames() {
        Decoder decoder = Decoder.createDecoder("UART");
        String[] expect = {"Baud rate"};
        assertArrayEquals(expect, decoder.getParamNames());
    }
}

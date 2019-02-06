
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
import static org.junit.Assert.assertFalse;

import java.util.Iterator;
import org.junit.Test;
import waveview.decoder.Decoder;
import waveview.wavedata.BitVector;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.Transition;
import waveview.wavedata.TransitionVector;

public class I2cDecoderTest {
    @Test
    public void decodeWrite() {
        final BitVector ONE = new BitVector("1", 2);
        final BitVector ZERO = new BitVector("0", 2);

        TransitionVector sdc = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, ONE)
            .appendTransition(15, ZERO)
            .appendTransition(25, ONE)  // pulse 0
            .appendTransition(35, ZERO)
            .appendTransition(45, ONE)  // pulse 1
            .appendTransition(55, ZERO)
            .appendTransition(65, ONE)  // pulse 2
            .appendTransition(75, ZERO)
            .appendTransition(85, ONE)  // pulse 3
            .appendTransition(95, ZERO)
            .appendTransition(105, ONE)  // pulse 4
            .appendTransition(115, ZERO)
            .appendTransition(125, ONE)  // pulse 5
            .appendTransition(135, ZERO)
            .appendTransition(145, ONE)  // pulse 6
            .appendTransition(155, ZERO)
            .appendTransition(165, ONE)  // pulse 7
            .appendTransition(175, ZERO)
            .appendTransition(185, ONE)
            .getTransitionVector();

        // 10010011
        TransitionVector sda = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, ONE)
            .appendTransition(10, ZERO)  // start bit
            .appendTransition(20, ONE)  // 7
            .appendTransition(40, ZERO)  // 6-5
            .appendTransition(80, ONE)  // 4
            .appendTransition(100, ZERO)  // 3-2
            .appendTransition(140, ONE)  // 1-0
            .appendTransition(180, ZERO)
            .appendTransition(190, ONE)  // stop bit
            .getTransitionVector();
        Decoder decoder = Decoder.createDecoder("I2C");
        decoder.setInput(0, new NetDataModel("sda", "sda", sda));
        decoder.setInput(1, new NetDataModel("sdc", "sdc", sdc));
        decoder.setParam(0, "8");
        TransitionVector results = decoder.decode();

        Iterator<Transition> dataIterator = results.findTransition(0);
        Transition t = dataIterator.next();
        assertEquals(0, t.getTimestamp());
        assertEquals("zzzzzzzz", t.toString(2));

        t = dataIterator.next();
        assertEquals(10, t.getTimestamp());
        assertEquals("10010011", t.toString(2));

        t = dataIterator.next();
        assertEquals(175, t.getTimestamp());
        assertEquals("zzzzzzzz", t.toString(2));

        assertFalse(dataIterator.hasNext());
    }
}

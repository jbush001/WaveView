
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import waveview.plugins.SpiDecoder;
import waveview.wavedata.BitVector;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.Transition;
import waveview.wavedata.TransitionVector;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

public class SpiDecoderTest {
    final BitVector ZERO = new BitVector("0", 2);
    final BitVector ONE = new BitVector("1", 2);

    final TransitionVector data = TransitionVector.Builder.createBuilder(1)
        // 0xda
        .appendTransition(20, new BitVector("1", 2))
        .appendTransition(30, new BitVector("1", 2))
        .appendTransition(40, new BitVector("0", 2))
        .appendTransition(50, new BitVector("1", 2))
        .appendTransition(60, new BitVector("1", 2))
        .appendTransition(70, new BitVector("0", 2))
        .appendTransition(80, new BitVector("1", 2))
        .appendTransition(90, new BitVector("0", 2))

        // 0xA3
        .appendTransition(100, new BitVector("1", 2))
        .appendTransition(110, new BitVector("0", 2))
        .appendTransition(120, new BitVector("1", 2))
        .appendTransition(130, new BitVector("0", 2))
        .appendTransition(140, new BitVector("0", 2))
        .appendTransition(150, new BitVector("0", 2))
        .appendTransition(160, new BitVector("1", 2))
        .appendTransition(170, new BitVector("1", 2))
        .getTransitionVector();

    TransitionVector sclk;

    @Before
    public void initClocks() {
        TransitionVector.Builder sclkBuilder =
            TransitionVector.Builder.createBuilder(1);
        for (int i = 0; i < 20; i++) {
            sclkBuilder.appendTransition(i * 10, ONE);
            sclkBuilder.appendTransition(i * 10 + 5, ZERO);
        }

        sclk = sclkBuilder.getTransitionVector();
    }

    // Ensure this properly ignores clocks when slave select is deasserted.
    @Test
    public void selectDeassert() {
        TransitionVector ss = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2))
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(100, new BitVector("1", 2))
            .getTransitionVector();

        SpiDecoder decoder = new SpiDecoder();
        decoder.setParam(0, "0");
        decoder.setInput(0, new NetDataModel("ss", "ss", ss));
        decoder.setInput(1, new NetDataModel("sclk", "sclk", sclk));
        decoder.setInput(2, new NetDataModel("data", "data", data));
        TransitionVector results = decoder.decode();

        Iterator<Transition> dataIterator = results.findTransition(0);
        Transition t = dataIterator.next();
        assertEquals(0, t.getTimestamp());
        assertEquals("ZZ", t.toString(16));

        t = dataIterator.next();
        assertEquals(20, t.getTimestamp());
        assertEquals("DA", t.toString(16));

        t = dataIterator.next();
        assertEquals(90, t.getTimestamp());
        assertEquals("ZZ", t.toString(16));

        assertFalse(dataIterator.hasNext());
    }

    // Test decoding multiple bytes in a row
    @Test
    public void decodeMultiple() {
        TransitionVector ss = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2))
            .appendTransition(15, new BitVector("0", 2))
            .getTransitionVector();

        SpiDecoder decoder = new SpiDecoder();
        decoder.setParam(0, "0");
        decoder.setInput(0, new NetDataModel("ss", "ss", ss));
        decoder.setInput(1, new NetDataModel("sclk", "sclk", sclk));
        decoder.setInput(2, new NetDataModel("data", "data", data));
        TransitionVector results = decoder.decode();

        Iterator<Transition> dataIterator = results.findTransition(0);
        Transition t = dataIterator.next();
        assertEquals(0, t.getTimestamp());
        assertEquals("ZZ", t.toString(16));

        t = dataIterator.next();
        assertEquals(20, t.getTimestamp());
        assertEquals("DA", t.toString(16));

        t = dataIterator.next();
        assertEquals(90, t.getTimestamp());
        assertEquals("ZZ", t.toString(16));

        t = dataIterator.next();
        assertEquals(100, t.getTimestamp());
        assertEquals("A3", t.toString(16));

        t = dataIterator.next();
        assertEquals(170, t.getTimestamp());
        assertEquals("ZZ", t.toString(16));

        assertFalse(dataIterator.hasNext());
    }

    // Pass an invalid mode parameter, ensure it throws an exception.
    @Test
    public void invalidMode() {
        SpiDecoder decoder = new SpiDecoder();
        try {
            decoder.setParam(0, "4");
            fail("didn't throw exception");
        } catch (IllegalArgumentException exc) {
            // Expected
            assertEquals("invalid SPI mode (must be 0-3)", exc.getMessage());
        }
    }

    // Pass nets that are the wrong width as inputs.
    @Test
    public void invalidNet() {
        SpiDecoder decoder = new SpiDecoder();
        TransitionVector ss = TransitionVector.Builder.createBuilder(2)
            .getTransitionVector();
        NetDataModel dataModel = new NetDataModel("foo", "foo", ss);

        try {
            decoder.setInput(0, dataModel);
            fail("didn't throw exception");
        } catch (IllegalArgumentException exc) {
            // Expected
            assertEquals("Invalid net foo: must be 1 bit wide", exc.getMessage());
        }

        try {
            decoder.setInput(1, dataModel);
            fail("didn't throw exception");
        } catch (IllegalArgumentException exc) {
            // Expected
            assertEquals("Invalid net foo: must be 1 bit wide", exc.getMessage());
        }

        try {
            decoder.setInput(2, dataModel);
            fail("didn't throw exception");
        } catch (IllegalArgumentException exc) {
            // Expected
            assertEquals("Invalid net foo: must be 1 bit wide", exc.getMessage());
        }

        try {
            decoder.setInput(3, dataModel);
            fail("didn't throw exception");
        } catch (IllegalArgumentException exc) {
            // Expected
            assertEquals("Invalid net foo: must be 1 bit wide", exc.getMessage());
        }
    }

    @Test
    public void getInputNames() {
        SpiDecoder decoder = new SpiDecoder();
        String[] expect = {"ss", "sclk", "data"};
        assertArrayEquals(expect, decoder.getInputNames());
    }

    @Test
    public void getParamNames() {
        SpiDecoder decoder = new SpiDecoder();
        String[] expect = {"SPI mode (0-3)"};
        assertArrayEquals(expect, decoder.getParamNames());
    }

    // There is no guarantee that a transition will actually change
    // the value of a signal. Ensure the decoder doesn't falsely
    // trigger a clock transition in these cases.
    @Test
    public void redundantClockTransitions() {
        final TransitionVector badclk = TransitionVector.Builder.createBuilder(1)
            .appendTransition(15, new BitVector("0", 2))
            .appendTransition(20, new BitVector("1", 2))
            .appendTransition(22, new BitVector("1", 2))
            .appendTransition(25, new BitVector("0", 2))
            .appendTransition(27, new BitVector("0", 2))
            .appendTransition(30, new BitVector("1", 2))
            .appendTransition(35, new BitVector("0", 2))
            .appendTransition(40, new BitVector("1", 2))
            .appendTransition(45, new BitVector("0", 2))
            .appendTransition(50, new BitVector("1", 2))
            .appendTransition(55, new BitVector("0", 2))
            .appendTransition(60, new BitVector("1", 2))
            .appendTransition(65, new BitVector("0", 2))
            .appendTransition(70, new BitVector("1", 2))
            .appendTransition(75, new BitVector("0", 2))
            .appendTransition(80, new BitVector("1", 2))
            .appendTransition(85, new BitVector("0", 2))
            .appendTransition(90, new BitVector("1", 2))
            .getTransitionVector();

        final TransitionVector ss = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("0", 2))
            .getTransitionVector();

        SpiDecoder decoder = new SpiDecoder();
        decoder.setParam(0, "0");
        decoder.setInput(0, new NetDataModel("ss", "ss", ss));
        decoder.setInput(1, new NetDataModel("sclk", "sclk", badclk));
        decoder.setInput(2, new NetDataModel("data", "data", data));
        TransitionVector results = decoder.decode();
        Iterator<Transition> dataIterator = results.findTransition(0);
        dataIterator.next();    // Skip Z portion at beginning
        Transition t = dataIterator.next();
        assertEquals("DA", t.toString(16));
    }

    // Mode 1 has the opposite clock phase.
    @Test
    public void mode1() {
        TransitionVector.Builder sclknBuilder =
            TransitionVector.Builder.createBuilder(1);
        for (int i = 0; i < 20; i++) {
            sclknBuilder.appendTransition(i * 10, ZERO);
            sclknBuilder.appendTransition(i * 10 + 5, ONE);
        }

        TransitionVector sclkn = sclknBuilder.getTransitionVector();
        SpiDecoder decoder = new SpiDecoder();

        TransitionVector ss = TransitionVector.Builder.createBuilder(1)
            .appendTransition(0, new BitVector("1", 2))
            .appendTransition(15, new BitVector("0", 2))
            .getTransitionVector();

        decoder.setParam(0, "1");
        decoder.setInput(0, new NetDataModel("ss", "ss", ss));
        decoder.setInput(1, new NetDataModel("sclk", "sclk", sclkn));
        decoder.setInput(2, new NetDataModel("data", "data", data));
        TransitionVector results = decoder.decode();

        Iterator<Transition> dataIterator = results.findTransition(0);
        dataIterator.next();    // Skip Z
        Transition t = dataIterator.next();
        assertEquals(20, t.getTimestamp());
        assertEquals("DA", t.toString(16));
    }
}

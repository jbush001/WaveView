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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import waveview.wavedata.BitValue;

public class BitValueTest {
    @Test
    public void fromChar() {
        assertEquals(BitValue.ZERO, BitValue.fromChar('0'));
        assertEquals(BitValue.ONE, BitValue.fromChar('1'));
        assertEquals(BitValue.X, BitValue.fromChar('x'));
        assertEquals(BitValue.X, BitValue.fromChar('X'));
        assertEquals(BitValue.Z, BitValue.fromChar('z'));
        assertEquals(BitValue.Z, BitValue.fromChar('Z'));
    }

    @Test
    public void toChar() {
        assertEquals('0', BitValue.ZERO.toChar());
        assertEquals('1', BitValue.ONE.toChar());
        assertEquals('x', BitValue.X.toChar());
        assertEquals('z', BitValue.Z.toChar());
    }

    @Test
    public void fromInt() {
        assertEquals(BitValue.ZERO, BitValue.fromInt(0));
        assertEquals(BitValue.ONE, BitValue.fromInt(1));
        assertEquals(BitValue.ONE, BitValue.fromInt(213));
    }

    @Test
    public void toInt() {
        assertEquals(1, BitValue.ONE.toInt());
        assertEquals(0, BitValue.ZERO.toInt());
        assertEquals(0, BitValue.X.toInt());
        assertEquals(0, BitValue.Z.toInt());
    }

    @Test
    public void fromOrdinal() {
        assertEquals(BitValue.ZERO,
                     BitValue.fromOrdinal(BitValue.ZERO.ordinal()));
        assertEquals(BitValue.ONE,
                     BitValue.fromOrdinal(BitValue.ONE.ordinal()));
        assertEquals(BitValue.X, BitValue.fromOrdinal(BitValue.X.ordinal()));
        assertEquals(BitValue.Z, BitValue.fromOrdinal(BitValue.Z.ordinal()));
    }

    @Test
    public void compare() {
        assertEquals(0, BitValue.ZERO.compare(BitValue.ZERO));
        assertEquals(1, BitValue.ONE.compare(BitValue.ZERO));
        assertEquals(0, BitValue.X.compare(BitValue.ZERO));
        assertEquals(0, BitValue.Z.compare(BitValue.ZERO));

        assertEquals(-1, BitValue.ZERO.compare(BitValue.ONE));
        assertEquals(0, BitValue.ONE.compare(BitValue.ONE));
        assertEquals(0, BitValue.X.compare(BitValue.ONE));
        assertEquals(0, BitValue.Z.compare(BitValue.ONE));

        assertEquals(0, BitValue.ZERO.compare(BitValue.X));
        assertEquals(0, BitValue.ONE.compare(BitValue.X));
        assertEquals(0, BitValue.X.compare(BitValue.X));
        assertEquals(0, BitValue.Z.compare(BitValue.X));

        assertEquals(0, BitValue.ZERO.compare(BitValue.Z));
        assertEquals(0, BitValue.ONE.compare(BitValue.Z));
        assertEquals(0, BitValue.X.compare(BitValue.Z));
        assertEquals(0, BitValue.Z.compare(BitValue.Z));
    }
}
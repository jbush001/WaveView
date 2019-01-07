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

package waveview.wavedata;

import java.math.BigInteger;

public class BitVector {
    // Index 0 is least significant position
    private BitValue[] values;
    public static final BitVector ZERO = new BitVector("0", 2);

    public BitVector() {}

    public BitVector(String string, int radix) throws NumberFormatException {
        parseString(string, radix);
    }

    public BitVector(int width) {
        values = new BitValue[width];
    }

    public BitVector(BitVector from) {
        assign(from);
    }

    public final void assign(BitVector from) {
        if (from.values == null) {
            values = null;
        } else {
            if (values == null || values.length != from.values.length) {
                values = new BitValue[from.values.length];
            }

            System.arraycopy(from.values, 0, values, 0, from.values.length);
        }
    }

    /// @param index bit number, where 0 is least significant
    /// @returns Value of bit at position
    public BitValue getBit(int index) {
        return values[index];
    }

    /// @param index bit number, where 0 is least significant
    /// @param value of bit at position
    public void setBit(int index, BitValue value) {
        values[index] = value;
    }

    /// @returns total bits in this vector (which may contain leading zeroes)
    public int getWidth() {
        return values.length;
    }

    /// @note This will set all bits in the vector to zero as a side effect.
    public void setWidth(int width) {
        values = new BitValue[width];
    }

    /// @returns true if this is all Zs
    public boolean isZ() {
        for (BitValue value : values) {
            if (value != BitValue.Z) {
                return false;
            }
        }

        return true;
    }

    /// @returns true if this contains any Z or X values in any positions
    public boolean isX() {
        for (BitValue val : values) {
            if (val == BitValue.Z || val == BitValue.X) {
                return true;
            }
        }

        return false;
    }

    /// @param radix May be 2, 10, or 16
    public void parseString(String string, int radix) throws NumberFormatException {
        switch (radix) {
            case 2:
                parseBinaryValue(string);
                break;
            case 10:
                parseDecimalValue(string);
                break;
            case 16:
                parseHexadecimalValue(string);
                break;
            default:
                throw new NumberFormatException("bad radix passed to parseString");
        }
    }

    /// @returns 1 if this is greater than the other bit vector, -1 if it is
    /// less than, 0 if equal
    /// @bug Ignores X and Z values, should have a rule for those.
    public int compare(BitVector other) {
        int myWidth = getWidth();
        int otherWidth = other.getWidth();
        if (otherWidth > myWidth) {
            // The other one is wider than me. Check if its leading digits
            // have any ones. If so, it is bigger
            for (int i = otherWidth - 1; i >= myWidth; i--) {
                if (other.values[i] == BitValue.ONE) {
                    return -1;
                }
            }
        } else if (myWidth > otherWidth) {
            // I am wider than the other number. Check if my leading digits
            // have any ones. If so, I am bigger.
            for (int i = myWidth - 1; i >= otherWidth; i--) {
                if (values[i] == BitValue.ONE) {
                    return 1;
                }
            }
        }

        int index = Math.min(myWidth, otherWidth) - 1;

        // Compare remaining digits
        while (index >= 0) {
            int comp = values[index].compare(other.values[index]);
            if (comp != 0) {
                return comp;
            }

            index--;
        }

        return 0;
    }

    /// @returns int representation of BitVector. Zs and Xs are
    /// treated as zeroes. This is truncated to 32 bits.
    public int intValue() {
        int value = 0;

        for (int index = getWidth() - 1; index >= 0; index--) {
            value <<= 1;
            value |= getBit(index).toInt();
        }

        return value;
    }

    /// @param radix may be 2, 10, or 16
    /// @returns A string representation of this BitVector with the given radix
    public String toString(int radix) {
        if (values == null) {
            return "0";
        }

        switch (radix) {
            case 2:
                return toBinaryString();
            case 10:
                return toDecimalString();
            case 16:
                return toHexString();
            default:
                throw new NumberFormatException("bad radix");
        }
    }

    @Override
    public String toString() {
        return toString(2);
    }

    private void parseBinaryValue(String string) throws NumberFormatException {
        if (values == null || values.length != string.length()) {
            values = new BitValue[string.length()];
        }

        int length = string.length();
        for (int index = 0; index < length; index++) {
            values[length - index - 1] = BitValue.fromChar(string.charAt(index));
        }
    }

    private void parseDecimalValue(String string) throws NumberFormatException {
        BigInteger bigint = new BigInteger(string);
        byte[] bytes = bigint.toByteArray();
        int totalBits = bytes.length * 8;

        if (values == null || values.length != totalBits) {
            values = new BitValue[totalBits];
        }

        for (int i = 0; i < totalBits; i++) {
            values[i] = BitValue.fromInt(bytes[bytes.length - (i / 8) - 1] & (1 << (i % 8)));
        }
    }

    private void parseHexadecimalValue(String string) throws NumberFormatException {
        if (values == null || values.length != string.length() * 4) {
            values = new BitValue[string.length() * 4];
        }

        int length = string.length();
        for (int index = 0; index < length; index++) {
            char c = string.charAt(length - index - 1);
            if (c >= '0' && c <= '9') {
                int digitVal = c - '0';
                for (int offset = 0; offset < 4; offset++) {
                    values[(index + 1) * 4 - offset - 1] = BitValue.fromInt(digitVal & (8 >> offset));
                }
            } else if (c >= 'a' && c <= 'f') {
                int digitVal = c - 'a' + 10;
                for (int offset = 0; offset < 4; offset++) {
                    values[(index + 1) * 4 - offset - 1] = BitValue.fromInt(digitVal & (8 >> offset));
                }
            } else if (c >= 'A' && c <= 'F') {
                int digitVal = c - 'A' + 10;
                for (int offset = 0; offset < 4; offset++) {
                    values[(index + 1) * 4 - offset - 1] = BitValue.fromInt(digitVal & (8 >> offset));
                }
            } else if (c == 'X' || c == 'x') {
                for (int offset = 0; offset < 4; offset++) {
                    values[(index + 1) * 4 - offset - 1] = BitValue.X;
                }
            } else if (c == 'Z' || c == 'z') {
                for (int offset = 0; offset < 4; offset++) {
                    values[(index + 1) * 4 - offset - 1] = BitValue.Z;
                }
            } else {
                throw new NumberFormatException("number format exception parsing " + string);
            }
        }
    }

    private String toBinaryString() {
        StringBuilder result = new StringBuilder();

        for (int index = getWidth() - 1; index >= 0; index--) {
            result.append(getBit(index).toChar());
        }

        return result.toString();
    }

    private String toDecimalString() {
        assert values != null;

        // Add one leading byte that is always zero so this will
        // be treated as unsigned.
        byte[] bytes = new byte[(values.length + 7) / 8 + 1];

        for (int i = 0; i < values.length; i++) {
            if (values[i] == BitValue.ONE) {
                bytes[bytes.length - (i / 8) - 1] |= (byte) (1 << (i % 8));
            }
        }

        return new BigInteger(bytes).toString();
    }

    private char bitsToHexDigit(int offset, int count) {
        int value = 0;

        for (int i = count - 1; i >= 0; i--) {
            value <<= 1;
            switch (getBit(i + offset)) {
                case ZERO:
                    break;
                case ONE:
                    value |= 1;
                    break;
                case X:
                    return 'X';
                case Z: // @bug should only be Z if all bits are Z
                    return 'Z';
            }
        }

        return "0123456789ABCDEF".charAt(value);
    }

    private String toHexString() {
        int lowBit = getWidth();
        StringBuilder result = new StringBuilder();

        // Partial first digit
        int partial = getWidth() % 4;
        if (partial > 0) {
            lowBit -= partial;
            result.append(bitsToHexDigit(lowBit, partial));
        }

        // Full hex digits
        while (lowBit >= 4) {
            lowBit -= 4;
            result.append(bitsToHexDigit(lowBit, 4));
        }

        return result.toString();
    }
}

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

import java.math.BigInteger;

///
/// Array of arbitrary lengthed four-valued logic values
///
public class BitVector {
    public static final byte VALUE_0 = 0;
    public static final byte VALUE_1 = 1;
    public static final byte VALUE_X = 2;
    public static final byte VALUE_Z = 3;

    // Index 0 is least significant value
    private byte[] values;

    public BitVector() {
    }

    public BitVector(String string, int radix) throws NumberFormatException {
        parseString(string, radix);
    }

    public BitVector(int width) {
        values = new byte[width];
    }

    public BitVector(BitVector from) {
        assign(from);
    }

    public void assign(BitVector from) {
        if (from.values == null) {
            values = null;
        } else {
            if (values == null || values.length != from.values.length) {
                values = new byte[from.values.length];
            }

            System.arraycopy(from.values, 0, values, 0, from.values.length);
        }
    }

    /// @param index bit number, where 0 is least significant
    /// @returns Value of bit at position, one of VALUE_0, VALUE_1, VALUE_X, VALUE_Z
    public int getBit(int index) {
        return values[index];
    }

    /// @param index bit number, where 0 is least significant
    /// @parma value of bit at position, one of VALUE_0, VALUE_1, VALUE_X, VALUE_Z
    public void setBit(int index, int value) {
        if (value > VALUE_Z || value < 0) {
            throw new NumberFormatException("invalid bit value");
        }

        values[index] = (byte) value;
    }

    /// @returns total number of bits in this vector (which may contain some number
    /// of leading zeroes)
    public int getWidth() {
        return values.length;
    }

    /// @param width number of bits
    /// @note This will set the value of the the bit vector to zero.
    public void setWidth(int width) {
        values = new byte[width];
    }

    /// @returns true if this is all Zs
    public boolean isZ() {
        for (int i = 0; i < values.length; i++) {
            if (values[i] != VALUE_Z) {
                return false;
            }
        }

        return true;
    }

    /// @returns true if this contains any Z or X values in any positions
    public boolean isX() {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == VALUE_Z || values[i] == VALUE_X) {
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
                if (other.values[i] == VALUE_1) {
                    return -1;
                }
            }
        } else if (myWidth > otherWidth) {
            // I am wider than the other number. Check if my leading digits
            // have any ones. If so, I am bigger.
            for (int i = myWidth - 1; i >= otherWidth; i--) {
                if (values[i] == VALUE_1) {
                    return 1;
                }
            }
        }

        int index = Math.min(myWidth, otherWidth) - 1;

        // Compare remaining digits
        while (index >= 0) {
            switch (other.values[index]) {
            case VALUE_0:
                if (values[index] == VALUE_1) {
                    return 1;
                }

                // If my value is Z or X, ignore
                break;
            case VALUE_1:
                if (values[index] == VALUE_0) {
                    return -1;
                }

                // If my value is Z or X, ignore
                break;
            case VALUE_X:
            case VALUE_Z:
                // Ignore...
                break;
            }

            index--;
        }

        return 0;
    }

    /// @returns int representation of BitVector. Zs and Xs are
    /// treated as zeroes. This is limited to 32 bits.
    public int intValue() {
        int value = 0;

        for (int index = getWidth() - 1; index >= 0; index--) {
            value <<= 1;
            if (getBit(index) == VALUE_1) {
                value |= 1;
            }
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
            values = new byte[string.length()];
        }

        int length = string.length();
        for (int index = 0; index < length; index++) {
            char c = string.charAt(index);
            if (c == '0') {
                values[length - index - 1] = VALUE_0;
            } else if (c == '1') {
                values[length - index - 1] = VALUE_1;
            } else if (c == 'x' || c == 'X') {
                values[length - index - 1] = VALUE_X;
            } else if (c == 'z' || c == 'Z') {
                values[length - index - 1] = VALUE_Z;
            } else {
                throw new NumberFormatException("number format exception parsing " + string);
            }
        }
    }

    private void parseDecimalValue(String string) throws NumberFormatException {
        BigInteger bigint = new BigInteger(string);
        byte[] bytes = bigint.toByteArray();
        int totalBits = bytes.length * 8;

        if (values == null || values.length != totalBits) {
            values = new byte[totalBits];
        }

        for (int i = 0; i < totalBits; i++) {
            values[i] = (bytes[bytes.length - (i / 8) - 1] & (1 << (i % 8))) == 0 ? VALUE_0 : VALUE_1;
        }
    }

    private void parseHexadecimalValue(String string) throws NumberFormatException {
        if (values == null || values.length != string.length() * 4) {
            values = new byte[string.length() * 4];
        }

        int length = string.length();
        for (int index = 0; index < length; index++) {
            char c = string.charAt(length - index - 1);
            if (c >= '0' && c <= '9') {
                int digitVal = c - '0';
                for (int offset = 0; offset < 4; offset++) {
                    values[(index + 1) * 4 - offset - 1] = (digitVal & (8 >> offset)) == 0 ? VALUE_0 : VALUE_1;
                }
            } else if (c >= 'a' && c <= 'f') {
                int digitVal = c - 'a' + 10;
                for (int offset = 0; offset < 4; offset++) {
                    values[(index + 1) * 4 - offset - 1] = (digitVal & (8 >> offset)) == 0 ? VALUE_0 : VALUE_1;
                }
            } else if (c >= 'A' && c <= 'F') {
                int digitVal = c - 'A' + 10;
                for (int offset = 0; offset < 4; offset++) {
                    values[(index + 1) * 4 - offset - 1] = (digitVal & (8 >> offset)) == 0 ? VALUE_0 : VALUE_1;
                }
            } else if (c == 'X' || c == 'x') {
                for (int offset = 0; offset < 4; offset++) {
                    values[(index + 1) * 4 - offset - 1] = VALUE_X;
                }
            } else if (c == 'Z' || c == 'z') {
                for (int offset = 0; offset < 4; offset++) {
                    values[(index + 1) * 4 - offset - 1] = VALUE_Z;
                }
            } else {
                throw new NumberFormatException("number format exception parsing " + string);
            }
        }
    }

    private String toBinaryString() {
        StringBuilder result = new StringBuilder();

        for (int index = getWidth() - 1; index >= 0; index--) {
            switch (getBit(index)) {
            case VALUE_0:
                result.append('0');
                break;
            case VALUE_1:
                result.append('1');
                break;
            case VALUE_Z:
                result.append('z');
                break;
            case VALUE_X:
            default:
                result.append('x');
                break;
            }
        }

        return result.toString();
    }

    /// @bug will crash if the bit vector hasn't been assigned.
    private String toDecimalString() {
        // We add one leading byte that is always zero so this will
        // be treated as unsigned.
        byte[] bytes = new byte[(values.length + 7) / 8 + 1];

        for (int i = 0; i < values.length; i++) {
            if (values[i] == VALUE_1) {
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
            case VALUE_0:
                break;
            case VALUE_1:
                value |= 1;
                break;
            case VALUE_X:
                return 'X';
            case VALUE_Z: // @bug should only be Z if all bits are Z
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

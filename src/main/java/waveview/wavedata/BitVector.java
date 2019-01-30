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
import java.nio.ByteBuffer;

public class BitVector {
    // If a bit in zx_flag is 1, the corresponding bit in the value array
    // represents either z (0) or x (1). Otherwise, the value bit represents
    // a 0 or 1.
    // Index 0 is least significant position
    // All code in this class guarantees that no bits with an index greater than
    // width will be nonzero.
    private long[] zxflags;
    private long[] values;
    private int width; // Number of valid bits

    public static final BitVector ZERO = new BitVector("0", 2);

    public BitVector() {}

    public BitVector(String string, int radix) throws NumberFormatException {
        parseString(string, radix);
    }

    public BitVector(int width) {
        setWidth(width);
    }

    public BitVector(BitVector from) {
        assign(from);
    }

    public final void assign(BitVector from) {
        if (from.values == null) {
            values = null;
            zxflags = null;
            width = 0;
        } else {
            values = from.values.clone();
            zxflags = from.zxflags.clone();
            width = from.width;
        }
    }

    /// @param index bit number, where 0 is least significant
    /// @returns Value of bit at position
    public BitValue getBit(int index) {
        assert index < width;

        int wordIndex = index / Long.SIZE;
        int bitOffset = index % Long.SIZE;
        long bitMask = 1L << bitOffset;
        if ((zxflags[wordIndex] & bitMask) == 0) {
            return (values[wordIndex] & bitMask) == 0 ? BitValue.ZERO : BitValue.ONE;
        } else {
            return (values[wordIndex] & bitMask) == 0 ? BitValue.Z : BitValue.X;
        }
    }

    /// @param index bit number, where 0 is least significant
    /// @param value of bit at position
    public void setBit(int index, BitValue value) {
        assert index < width;

        int wordIndex = index / Long.SIZE;
        int bitOffset = index % Long.SIZE;
        long bitMask = 1L << bitOffset;
        if (value == BitValue.X || value == BitValue.Z) {
            zxflags[wordIndex] |= bitMask;
            if (value == BitValue.X) {
                values[wordIndex] |= bitMask;
            } else {
                values[wordIndex] &= ~bitMask;
            }
        } else {
            zxflags[wordIndex] &= ~bitMask;
            if (value == BitValue.ONE) {
                values[wordIndex] |= bitMask;
            } else {
                values[wordIndex] &= ~bitMask;
            }
        }
    }

    /// @returns total bits in this vector (which may contain leading zeroes)
    public int getWidth() {
        return width;
    }

    /// @note This will set all bits in the vector to zero as a side effect.
    public void setWidth(int width) {
        int numWords = (width + Long.SIZE - 1) / Long.SIZE;
        values = new long[numWords];
        zxflags = new long[numWords];
        this.width = width;
    }

    /// @returns true if this is all Zs
    public boolean isZ() {
        int numWords = width / Long.SIZE;
        for (int i = 0; i < numWords; i++) {
            if (zxflags[i] != -1 || values[i] != 0) {
                return false;
            }
        }

        int remainingBits = width % Long.SIZE;
        return (remainingBits == 0
            || (zxflags[numWords] == (1 << remainingBits) - 1
            && values[numWords] == 0));
    }

    /// @returns true if this contains an X or Z values in any position.
    public boolean isX() {
        for (long zxval : zxflags) {
            if (zxval != 0) {
                return true;
            }
        }

        return false;
    }

    /// @returns int representation of BitVector. Zs and Xs are
    /// treated as zeroes. This is truncated to 32 bits.
    public int intValue() {
        return (int)((values[0] & ~zxflags[0]) & 0xffffffff);
    }

    public BitVector slice(int lowBit, int highBit) {
        if (lowBit < 0 || highBit >= width || lowBit > highBit) {
            throw new IllegalArgumentException("invalid bit slice range "
                + lowBit + ":" + highBit);
        }

        int destBits = highBit - lowBit + 1;
        int destWords = (destBits + Long.SIZE - 1) / Long.SIZE;
        int sourceIndex = lowBit / Long.SIZE;
        int rightShift = lowBit % Long.SIZE;
        int leftShift = Long.SIZE - rightShift;
        long[] newValues = new long[destWords];
        long[] newZxFlags = new long[destWords];

        for (int i = 0; i < destWords; i++) {
            newValues[i] = values[sourceIndex + i] >>> rightShift;
            newZxFlags[i] = zxflags[sourceIndex + i] >>> rightShift;
            if (leftShift != 0 && sourceIndex + i + 1 < values.length) {
                newValues[i] |= values[sourceIndex + i + 1] << leftShift;
                newZxFlags[i] |= zxflags[sourceIndex + i + 1] << leftShift;
            }
        }

        int leadingBits = destBits % Long.SIZE;
        if (leadingBits != 0) {
            int mask = (1 << leadingBits) - 1;
            newValues[destWords - 1] &= mask;
            newZxFlags[destWords - 1] &= mask;
        }

        BitVector newVec = new BitVector();
        newVec.values = newValues;
        newVec.zxflags = newZxFlags;
        newVec.width = highBit - lowBit + 1;
        return newVec;
    }

    /// @param radix May be 2, 10, or 16
    public void parseString(String string, int radix) throws NumberFormatException {
        switch (radix) {
            case 2:
                parsePowerOfTwo(string, 1);
                break;
            case 8:
                parsePowerOfTwo(string, 3);
                break;
            case 10:
                parseDecimal(string);
                break;
            case 16:
                parsePowerOfTwo(string, 4);
                break;
            default:
                throw new NumberFormatException("bad radix passed to parseString");
        }
    }

    private void parseDecimal(String string) throws NumberFormatException {
        BigInteger bigint = new BigInteger(string);
        byte[] bytes = bigint.toByteArray();
        setWidth(bytes.length * Byte.SIZE);
        for (int i = 0; i < bytes.length; i++) {
            long byteVal = (((long) bytes[i]) & 0xff);
            int destByteIndex = bytes.length - i - 1;
            int shiftAmount = (destByteIndex % 8) * 8;
            int destWordIndex = destByteIndex / 8;
            values[destWordIndex] |= byteVal << shiftAmount;
        }
    }

    private void parsePowerOfTwo(String string, int bitsPerChar) throws NumberFormatException {
        int radix = 1 << bitsPerChar;
        long mask = (long) radix - 1;
        int length = string.length();
        setWidth(length * bitsPerChar);
        int bitOffset = 0;
        int wordIndex = 0;
        for (int index = 0; index < length; index++) {
            long zxdigit = 0;
            long valdigit = 0;
            char c = string.charAt(length - index - 1);
            if (c == 'x' || c == 'X') {
                zxdigit = mask;
                valdigit = mask;
            } else if (c == 'z' || c == 'Z') {
                zxdigit = mask;
            } else {
                valdigit = (long) Character.digit(c, radix);
                if (valdigit < 0) {
                    throw new NumberFormatException("number format exception parsing "
                        + string);
                }
            }

            zxflags[wordIndex] |= zxdigit << bitOffset;
            values[wordIndex] |= valdigit << bitOffset;
            bitOffset += bitsPerChar;
            int spillover = bitOffset - Long.SIZE;
            if (spillover > 0) {
                // This digit spans a word boundary (which only happens with
                // octal values).
                int rshift = bitsPerChar - spillover;
                zxflags[wordIndex + 1] |= zxdigit >> rshift;
                values[wordIndex + 1] |= valdigit >> rshift;
            }

            if (bitOffset >= Long.SIZE) {
                bitOffset -= Long.SIZE;
                wordIndex++;
            }
        }
    }

    @Override
    public String toString() {
        return toString(2);
    }

    /// @param radix may be 2, 10, or 16
    /// @returns A string representation of this BitVector with the given radix
    public String toString(int radix) {
        if (values == null) {
            return "0";
        }

        switch (radix) {
            case 2:
                return toPowerOfTwoString(1);
            case 8:
                return toPowerOfTwoString(3);
            case 10:
                return toDecimalString();
            case 16:
                return toPowerOfTwoString(4);
            default:
                throw new NumberFormatException("bad radix");
        }
    }

    private String toDecimalString() {
        assert values != null;

        // Add one leading byte that is always zero so this will
        // be treated as unsigned.
        ByteBuffer buffer = ByteBuffer.allocate((values.length + 1) * 8);
        buffer.putLong(0);   // Ensure unsigned by having leading zero
        for (int i = 0; i < values.length; i++) {
            int sourceIndex = values.length - i - 1;
            buffer.putLong(values[sourceIndex] & ~zxflags[sourceIndex]);
        }

        return new BigInteger(buffer.array()).toString();
    }

    private String toPowerOfTwoString(int bitsPerChar) {
        char zChar = bitsPerChar == 1 ? 'z' : 'Z';
        char xChar = bitsPerChar == 1 ? 'x' : 'X';
        int lowBit = (((width + bitsPerChar - 1) / bitsPerChar) - 1) * bitsPerChar;
        long digitMask = (1L << bitsPerChar) - 1;
        int wordIndex = lowBit / Long.SIZE;
        int bitOffset = lowBit % Long.SIZE;
        StringBuilder result = new StringBuilder();

        while (wordIndex >= 0) {
            int digitVal = (int)((values[wordIndex] >>> bitOffset) & digitMask);
            int zxflag = (int)((zxflags[wordIndex] >>> bitOffset) & digitMask);
            int spillover = bitOffset + bitsPerChar - Long.SIZE;
            if (spillover > 0 && wordIndex < values.length - 1) {
                // This digit spans a word boundary (which only happens with
                // octal values).
                int lshift = bitsPerChar - spillover;
                digitVal |= (int)((values[wordIndex + 1] << lshift) & digitMask);
                zxflag |= (int)((zxflags[wordIndex + 1] << lshift) & digitMask);
            }

            if (zxflag == digitMask && digitVal == 0) {
                result.append(zChar);
            } else if (zxflag != 0) {
                result.append(xChar);
            } else {
                result.append("0123456789ABCDEF".charAt(digitVal));
            }

            bitOffset -= bitsPerChar;
            if (bitOffset < 0) {
                bitOffset += Long.SIZE;
                wordIndex--;
            }
        }

        return result.toString();
    }

    /// @returns 1 if this is greater than the other bit vector, -1 if it is
    /// less than, 0 if equal
    /// @bug Ignores X and Z values, should have a rule for those.
    public int compare(BitVector other) {
        int myWidth = values.length;
        int otherWidth = other.values.length;
        if (otherWidth > myWidth) {
            // The other one is wider than me. Check if its leading digits
            // have any ones. If so, it is bigger
            for (int i = otherWidth - 1; i >= myWidth; i--) {
                if ((other.values[i] & ~other.zxflags[i]) != 0) {
                    return -1;
                }
            }
        } else if (myWidth > otherWidth) {
            // I am wider than the other number. Check if my leading digits
            // have any ones. If so, I am bigger.
            for (int i = myWidth - 1; i >= otherWidth; i--) {
                if ((values[i] & ~zxflags[i]) != 0) {
                    return 1;
                }
            }
        }

        int index = Math.min(myWidth, otherWidth) - 1;

        // Compare remaining digits
        while (index >= 0) {
            // Z and X match everything
            long ignoremask = ~(zxflags[index] | other.zxflags[index]);
            long a = values[index] & ignoremask;
            long b = other.values[index] & ignoremask;
            if (a != b) {
                return (a > b) ^ (a < 0) ^ (b < 0) ? 1 : -1;
            }

            index--;
        }

        return 0;
    }
}

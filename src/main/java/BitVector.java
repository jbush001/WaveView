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

import java.math.BigInteger;
import java.lang.*;

//
// Array of arbitrary lengthed four-valued logic values
//

class BitVector
{
    public static final byte VALUE_0 = 0;
    public static final byte VALUE_1 = 1;
    public static final byte VALUE_X = 2;
    public static final byte VALUE_Z = 3;

    public BitVector()
    {
    }

    public BitVector(String string, int radix) throws NumberFormatException
    {
        parseString(string, radix);
    }

    public BitVector(int width)
    {
        fValues = new byte[width];
    }

    /// @param index bit number, where 0 is least significant
    /// @returns Value of bit at position, one of VALUE_0, VALUE_1, VALUE_X, VALUE_Z
    public int getBit(int index)
    {
        return fValues[index];
    }

    /// @param index bit number, where 0 is least significant
    /// @parma value of bit at position, one of VALUE_0, VALUE_1, VALUE_X, VALUE_Z
    public void setBit(int index, int value)
    {
        fValues[index] = (byte) value;
    }

    /// @returns total number of bits in this vector (which may contain some number
    /// of leading zeroes)
    public int getWidth()
    {
        return fValues.length;
    }

    /// @param width number of bits
    /// @note This will set the value of the the bit vector to zero.
    void setWidth(int width)
    {
        fValues = new byte[width];
    }

    /// @returns true if this is all Zs
    public boolean isZ()
    {
        for (int i = 0; i < fValues.length; i++)
        {
            if (fValues[i] != BitVector.VALUE_Z)
                return false;
        }

        return true;
    }

    /// @returns True if this contains any Z or X values in any positions
    public boolean isX()
    {
        for (int i = 0; i < fValues.length; i++)
        {
            if (fValues[i] == BitVector.VALUE_Z || fValues[i] == BitVector.VALUE_X)
                return true;
        }

        return false;
    }

    /// @param radix May be 16 or 2 (hex or binary)
    /// @todo make this more generic
    /// @bug Doesn't support decimal
    public void parseString(String string, int radix) throws NumberFormatException
    {
        if (fValues == null || fValues.length != string.length())
            fValues = new byte[string.length()];

        switch (radix)
        {
            case 2:
                parseBinaryValue(string);
                break;

            case 16:
                parseHexadecimalValue(string);
                break;

            default:
                throw new NumberFormatException("bad radix passed to parseString");
        }
    }

    private void parseBinaryValue(String string) throws NumberFormatException
    {
        if (string.length() != fValues.length)
            fValues = new byte[string.length()];

        int length = string.length();
        for (int index = 0; index < length; index++)
        {
            char c = string.charAt(index);
            if (c == '0')
                fValues[length - index - 1] = BitVector.VALUE_0;
            else if (c == '1')
                fValues[length - index - 1] = BitVector.VALUE_1;
            else if (c == 'x' || c == 'X')
                fValues[length - index - 1] = BitVector.VALUE_X;
            else if (c == 'z' || c == 'Z')
                fValues[length - index - 1] = BitVector.VALUE_Z;
            else
                throw new NumberFormatException("number format exception parsing " + string);
        }
    }

    private void parseHexadecimalValue(String string) throws NumberFormatException
    {
        if (string.length() * 4 != fValues.length)
            fValues = new byte[string.length() * 4];

        int length = string.length();
        for (int index = 0; index < length; index++)
        {
            char c = string.charAt(length - index - 1);
            if (c >= '0' && c <= '9')
            {
                int digitVal = c - '0';
                for (int offset = 0; offset < 4; offset++)
                {
                    fValues[(index + 1) * 4 - offset - 1] = (digitVal & (8 >> offset)) != 0 ? BitVector.VALUE_1
                        : BitVector.VALUE_0;
                }
            }
            else if (c >= 'a' && c <= 'f')
            {
                int digitVal = c - 'a' + 10;
                for (int offset = 0; offset < 4; offset++)
                {
                    fValues[(index + 1) * 4 - offset - 1] = (digitVal & (8 >> offset)) != 0 ? BitVector.VALUE_1
                        : BitVector.VALUE_0;
                }
            }
            else if (c >= 'A' && c <= 'F')
            {
                int digitVal = c - 'A' + 10;
                for (int offset = 0; offset < 4; offset++)
                {
                    fValues[(index + 1) * 4 - offset - 1] = (digitVal & (8 >> offset)) != 0 ? BitVector.VALUE_1
                        : BitVector.VALUE_0;
                }
            }
            else if (c == 'X' || c == 'x')
            {
                for (int offset = 0; offset < 4; offset++)
                    fValues[(index + 1) * 4 - offset - 1] = BitVector.VALUE_X;
            }
            else if (c == 'Z' || c == 'z')
            {
                for (int offset = 0; offset < 4; offset++)
                    fValues[(index + 1) * 4 - offset - 1] = BitVector.VALUE_Z;
            }
            else
                throw new NumberFormatException("number format exception parsing " + string);
        }
    }

    /// @returns 1 if this is greater than the other bit vector, -1 if it is
    /// less than, 0 if equal
    /// @bug This is probably not completely correct for X and Z values...
    public int compare(BitVector other)
    {
        int myWidth = getWidth();
        int otherWidth = other.getWidth();
        if (otherWidth > myWidth)
        {
            // The other one is wider than me.  Check if its leading digits
            // have any ones.  If so, it is bigger
            for (int i = otherWidth - 1; i >= myWidth; i--)
                if (other.fValues[i] == VALUE_1)
                    return -1;
        }
        else if (myWidth > otherWidth)
        {
            // I am wider than the other number.  Check if my leading digits
            // have any ones.  If so, I am bigger.
            for (int i = myWidth - 1; i >= otherWidth; i--)
                if (fValues[i] == VALUE_1)
                    return 1;
        }

        int index = Math.min(myWidth, otherWidth) - 1;

        // Now compare remaining digits directly.
        while (index >= 0)
        {
            switch (other.fValues[index])
            {
                case VALUE_0:
                    if (fValues[index] == VALUE_1)
                        return 1;

                    // If my value is Z or X, ignore
                    break;

                case VALUE_1:
                    if (fValues[index] == VALUE_0)
                        return -1;

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
    int intValue()
    {
        int value = 0;

        for (int index = getWidth() - 1; index >= 0; index--)
        {
            value <<= 1;
            if (getBit(index) == BitVector.VALUE_1)
                value |= 1;
        }

        return value;
    }

    /// @returns A string representation of this BitVector with the given radix,
    ///  which may be 2, 10, or 16.
    /// @bug Radix 10 numbers are limited to 32 bits (the others are unlimited)
    String toString(int radix)
    {
        switch (radix)
        {
            case 2:
                return toBinaryString();

            case 10:
                return Integer.toString(intValue());

            case 16:
                return toHexString();

            default:
                return "bad radix";
        }
    }

    private String toBinaryString()
    {
        StringBuffer result = new StringBuffer();

        for (int index = getWidth() - 1; index >= 0; index--)
        {
            switch (getBit(index))
            {
                case BitVector.VALUE_0:
                    result.append('0');
                    break;
                case BitVector.VALUE_1:
                    result.append('1');
                    break;
                case BitVector.VALUE_X:
                    result.append('x');
                    break;
                case BitVector.VALUE_Z:
                    result.append('z');
                    break;
            }
        }

        return result.toString();
    }

    private char bitsToHexDigit(int offset, int count)
    {
        int value = 0;

        for (int i = count - 1; i >= 0; i--)
        {
            value <<= 1;
            switch (getBit(i + offset))
            {
                case BitVector.VALUE_0:
                    break;
                case BitVector.VALUE_1:
                    value |= 1;
                    break;
                case BitVector.VALUE_X:
                    return 'X';
                case BitVector.VALUE_Z:    // XXX bug: should only be Z if all bits are Z
                    return 'Z';
            }
        }

        return "0123456789ABCDEF".charAt(value);
    }

    private String toHexString()
    {
        int lowBit = getWidth();
        StringBuffer result = new StringBuffer();

        // Partial first digit
        int partial = getWidth() % 4;
        if (partial > 0)
        {
            lowBit -= partial;
            result.append(bitsToHexDigit(lowBit, partial));
        }

        // Full hex digits
        while (lowBit >= 4)
        {
            lowBit -= 4;
            result.append(bitsToHexDigit(lowBit, 4));
        }

        return result.toString();
    }

    // Bit 0 is least significant value
    private byte[] fValues;
}
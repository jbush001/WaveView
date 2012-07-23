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

class BitVector
{
	public static final byte VALUE_0 = 0;
	public static final byte VALUE_1 = 1;
	public static final byte VALUE_X = 2;
	public static final byte VALUE_Z = 3;
	
	public BitVector()
	{
	}
	
	public BitVector(String string, int radix)
	{
		parseString(string, radix);
	}

	public BitVector(int width)
	{
		fValues = new byte[width];
	}
	
	public int getValue(int index)
	{
		return fValues[index];
	}

	public void setValue(int index, int value)
	{
		fValues[index] = (byte) value;
	}

	public int getWidth()
	{
		return fValues.length;
	}

	void setWidth(int width)
	{
		fValues = new byte[width];
	}

	public boolean isZ()
	{
		for (int i = 0; i < fValues.length; i++)
		{
			if (fValues[i] != BitVector.VALUE_Z)
				return false;
		}
		
		return true;
	}

	public boolean isX()
	{
		for (int i = 0; i < fValues.length; i++)
		{
			if (fValues[i] == BitVector.VALUE_Z || fValues[i] == BitVector.VALUE_X)
				return true;
		}
		
		return false;
	}
	
	// XXX make this more generic
	public void parseString(String string, int radix)
	{
		if (fValues == null || fValues.length != string.length())
			fValues = new byte[string.length()];

		switch (radix)
		{
			case 2:
				parseBinaryValue(string);
				break;

			// XXX no support for decimal

			case 16:
				parseHexadecimalValue(string);
				break;
				
			default:
				System.out.println("bad radix passed to parseString");
		}
	}

	private void parseBinaryValue(String string) 
	{
		if (string.length() != fValues.length)
			fValues = new byte[string.length()];
		
		for (int index = 0; index < string.length(); index++)
		{
			char c = string.charAt(index);
			if (c == '0')
				fValues[index] = BitVector.VALUE_0;
			else if (c == '1')
				fValues[index] = BitVector.VALUE_1;
			else if (c == 'x' || c == 'X')
				fValues[index] = BitVector.VALUE_X;
			else if (c == 'z' || c == 'Z')
				fValues[index] = BitVector.VALUE_Z;
			else
				System.out.println("number format exception parsing " + string);
		}
	}
	
	private void parseHexadecimalValue(String string)
	{
		if (string.length() * 4 != fValues.length)
			fValues = new byte[string.length() * 4];
		
		for (int index = 0; index < string.length(); index++)
		{
			char c = string.charAt(index);
			if (c >= '0' && c <= '9')
			{
				int digitVal = c - '0';
				for (int offset = 0; offset < 4; offset++)
				{
					fValues[index * 4 + offset] = (digitVal & (8 >> offset)) != 0 ? BitVector.VALUE_1 
						: BitVector.VALUE_0;
				}
			}
			else if (c >= 'a' && c <= 'f')
			{
				int digitVal = c - 'a' + 10;
				for (int offset = 0; offset < 4; offset++)
				{
					fValues[index * 4 + offset] = (digitVal & (8 >> offset)) != 0 ? BitVector.VALUE_1 
						: BitVector.VALUE_0;
				}
			}
			else if (c >= 'A' && c <= 'F')
			{
				int digitVal = c - 'A' + 10;
				for (int offset = 0; offset < 4; offset++)
				{
					fValues[index * 4 + offset] = (digitVal & (8 >> offset)) != 0 ? BitVector.VALUE_1 
						: BitVector.VALUE_0;
				}
			}
			else if (c == 'X' || c == 'x')
			{
				for (int offset = 0; offset < 4; offset++)
					fValues[index * 4 + offset] = BitVector.VALUE_X;
			}
			else if (c == 'Z' || c == 'z')
			{
				for (int offset = 0; offset < 4; offset++)
					fValues[index * 4 + offset] = BitVector.VALUE_Z;
			}
			else
				System.out.println("number format exception parsing " + string);
		}
	}
	
	// Return 1 if this is greater than the other bit vector, -1 if it is
	// less than, 0 if equal
	// @bug This is probably not completely correct for X and Z values...
	public int compare(BitVector other)
	{
		int myIndex = 0;
		int otherIndex = 0;
 		
		int difference = getWidth() - other.getWidth();
 		if (difference < 0)
 		{
 			// The other one is wider than me.  Check if its leading digits
 			// have any ones.  If so, it is bigger
 			for (int i = 0; i < -difference; i++, otherIndex++)
 				if (other.fValues[otherIndex] == VALUE_1)
 					return -1;
 		}
 		else if (difference > 0)
		{
			// I am wider than the other number.  Check if my leading digits
			// have any ones.  If so, I am bigger.
			for (int i = 0; i < difference; i++, myIndex++)
				if (fValues[myIndex] == VALUE_1)
					return -1;
		}

		// Now compare remaining digits directly.
		while (myIndex < getWidth())
		{
			switch (other.fValues[otherIndex])
			{
				case VALUE_0:
					if (fValues[myIndex] == VALUE_1)
						return 1;
					
					// If my value is Z or X, ignore
					break;
					
				case VALUE_1:
					if (fValues[myIndex] == VALUE_0)
						return -1;
					
					// If my value is Z or X, ignore
					break;

				case VALUE_X:
				case VALUE_Z:
					// Ignore...
					break;
			}
			
			myIndex++;
			otherIndex++;
		}

		return 0;
	}
	
	String toString(int radix)
	{
		switch (radix)
		{
			case 2:
				return toBinaryString();

			case 10:
				return Integer.toString(intValue());	// XXX only up to 32 bits

			case 16:
				return toHexString();

			default:
				return "bad radix";
		}
	}
	
	String toBinaryString()
	{
		StringBuffer result = new StringBuffer();
		
		for (int index = 0; index < getWidth(); index++)
		{
			switch (getValue(index))
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
	
	// Returns an integer.  Note that this is limited to 32 bits.
	int intValue()
	{
		int value = 0;

		for (int index = 0; index < getWidth(); index++)
		{
			value <<= 1;
			if (getValue(index) == BitVector.VALUE_1)
				value |= 1;
		}
		
		return value;
	}
	
	private char bitsToHexDigit(int offset, int count)
	{
		int value = 0;
	
		for (int i = 0; i < count; i++)
		{
			value <<= 1;
			switch (getValue(i + offset))
			{
				case BitVector.VALUE_0:
					break;
				case BitVector.VALUE_1:
					value |= 1;
					break;
				case BitVector.VALUE_X:
					return 'X';
				case BitVector.VALUE_Z:	// XXX bug: should only be Z if all bits are Z
					return 'Z';
			}
		}

		return "0123456789ABCDEF".charAt(value);
	}

	private String toHexString()
	{
		int index = 0;
		StringBuffer result = new StringBuffer();

		// Partial first digit
		int partial = getWidth() % 4;
		if (partial > 0)
		{
			result.append(bitsToHexDigit(0, partial));
			index += partial;
		}

		// Full hex digits
		while (index < getWidth())
		{
			result.append(bitsToHexDigit(index, 4));
			index += 4;
		}

		return result.toString();
	}

	// Index 0 is the most significant bit of this signal
	// Thi sis usually the opposite of the way RTL numbers the signals.otyn['fvglh;,jk
	private byte[] fValues;
}
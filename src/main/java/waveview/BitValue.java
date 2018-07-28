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

public enum BitValue {
    ZERO,
    ONE,
    X,
    Z;

    private static final BitValue[] ORDINAL_TABLE = BitValue.values();

    public static BitValue fromChar(char c) {
        switch (c) {
            case '0':
                return ZERO;
            case '1':
                return ONE;
            case 'x':
            case 'X':
                return X;
            case 'z':
            case 'Z':
                return Z;
            default:
                throw new NumberFormatException("unknown digit " + c);
        }
    }

    public char toChar() {
        switch (this) {
            case ZERO:
                return '0';
            case ONE:
                return '1';
            case X:
                return 'x';
            case Z:
                return 'z';
        }

        return ' '; // Unreachable?
    }

    public static BitValue fromInt(int i) {
        if (i == 0) {
            return ZERO;
        } else {
            return ONE;
        }
    }

    public int toInt() {
        return (this == ONE) ? 1 : 0;
    }

    public static BitValue fromOrdinal(int ord) {
        return ORDINAL_TABLE[ord];
    }

    public int compare(BitValue other) {
        if (this == ONE && other == ZERO) {
            return 1;
        } else if (this == ZERO && other == ONE) {
            return -1;
        } else {
            // Either these are equal, or there is an X and Z, which match everything.
            return 0;
        }
    }
}

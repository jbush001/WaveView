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
    VALUE_0,
    VALUE_1,
    VALUE_X,
    VALUE_Z;

    private static final BitValue[] ORDINAL_TABLE = BitValue.values();

    public static BitValue fromChar(char c) {
        switch (c) {
        case '0':
            return VALUE_0;
        case '1':
            return VALUE_1;
        case 'x':
        case 'X':
            return VALUE_X;
        case 'z':
        case 'Z':
            return VALUE_Z;
        default:
            throw new NumberFormatException("unknown digit " + c);
        }
    }

    public static BitValue fromInt(int i) {
        if (i == 0) {
            return VALUE_0;
        } else {
            return VALUE_1;
        }
    }

    public static BitValue fromOrdinal(int ord) {
        return ORDINAL_TABLE[ord];
    }

    public int toInt() {
        return (this == VALUE_1) ? 1 : 0;
    }

    public int compare(BitValue other) {
        if (this == VALUE_1 && other == VALUE_0) {
            return 1;
        } else if (this == VALUE_0 && other == VALUE_1) {
            return -1;
        } else {
            // Either these are equal, or there is an X and Z, which match everything.
            return 0;
        }
    }

    public char toChar() {
        switch (this) {
        case VALUE_0:
            return '0';
        case VALUE_1:
            return '1';
        case VALUE_X:
            return 'x';
        case VALUE_Z:
            return 'z';
        }

        return ' '; // Unreachable?
    }
}

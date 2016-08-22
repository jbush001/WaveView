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

import java.util.*;

///
/// Converts a bitvector to a set of strings from an enumeration. Useful for
/// encoding state variables.
///

/// @bug Inconsistent naming. Some are XXXAtIndex, some are XXXByIndex
public class IdentifierValueFormatter implements ValueFormatter {
    public class Mapping {
        Mapping(int _value, String _name) {
            value = _value;
            name = _name;
        }

        int value;
        String name;
    };

    public IdentifierValueFormatter() {
    }

    void addMapping(int value, String name) {
        fMappings.add(new Mapping(value, name));
    }

    void setNameAtIndex(int index, String name) {
        fMappings.elementAt(index).name = name;
    }

    void setValueAtIndex(int index, int value) {
        fMappings.elementAt(index).value = value;
    }

    int getValueByIndex(int index) {
        return fMappings.elementAt(index).value;
    }

    String getNameByIndex(int index) {
        return fMappings.elementAt(index).name;
    }

    int getMappingCount() {
        return fMappings.size();
    }

    @Override
    public String format(BitVector bits) {
        int mapIndex = bits.intValue();
        for (Mapping m : fMappings) {
            if (m.value == mapIndex)
                return m.name;
        }

        // If this doesn't have a mapping, print the raw value
        return "??? (" + Integer.toString(mapIndex) + ")";
    }

    private Vector<Mapping> fMappings = new Vector<Mapping>();
}

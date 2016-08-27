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

public class EnumValueFormatter implements ValueFormatter {
    public class Mapping {
        Mapping(int _value, String _name) {
            value = _value;
            name = _name;
        }

        int value;
        String name;
    };

    public EnumValueFormatter() {
    }

    void addMapping(int value, String name) {
        fMappings.add(new Mapping(value, name));
    }

    void setName(int index, String name) {
        fMappings.get(index).name = name;
    }

    void setValue(int index, int value) {
        fMappings.get(index).value = value;
    }

    int getValue(int index) {
        return fMappings.get(index).value;
    }

    String getName(int index) {
        return fMappings.get(index).name;
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

    private ArrayList<Mapping> fMappings = new ArrayList<Mapping>();
}

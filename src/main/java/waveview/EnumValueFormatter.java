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

import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;

///
/// Converts a bitvector to one of a set of strings from an enumeration.
/// Useful for encoding state variables.
///

public class EnumValueFormatter implements ValueFormatter {
    private final Map<Integer, String> mappings = new HashMap<>();
    private final File mappingFile;

    public EnumValueFormatter(File mappingFile) throws IOException {
        this.mappingFile = mappingFile;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(mappingFile), "UTF8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(" ", 0);
                if (tokens.length >= 2) {
                    mappings.put(Integer.parseInt(tokens[0]), tokens[1]);
                }
            }
        }
    }

    public File getFile() {
        return mappingFile;
    }

    @Override
    public String format(BitVector bits) {
        int mapIndex = bits.intValue();
        if (mappings.containsKey(mapIndex))
            return mappings.get(mapIndex);
        else
            return "??? (" + mapIndex + ")";
    }
}

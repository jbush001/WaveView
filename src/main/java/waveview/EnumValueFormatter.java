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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;
import waveview.wavedata.BitVector;

///
/// Converts a bitvector to one of a set of strings from an enumeration.
/// Useful for encoding state variables.
///

public final class EnumValueFormatter implements ValueFormatter {
    private final Map<Integer, String> mappings = new HashMap<>();
    private final File mappingFile;

    public static class FormatException extends IOException {
        public FormatException(String message) {
            super(message);
        }
    }

    public EnumValueFormatter(File mappingFile) throws IOException {
        this.mappingFile = mappingFile;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(Files.newInputStream(mappingFile.toPath()), "UTF8"))) {
            int lineNum = 0;
            String line;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] tokens = line.split(" ", 0);
                if (tokens.length != 2) {
                    throw new FormatException("Line " + lineNum + " parse error");
                }

                try {
                    mappings.put(Integer.parseInt(tokens[0]), tokens[1]);
                } catch (NumberFormatException exc) {
                    throw new FormatException("Line " + lineNum + " invalid number format");
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

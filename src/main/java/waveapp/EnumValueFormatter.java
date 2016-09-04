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

package waveapp;

import java.util.*;
import java.io.*;

///
/// Converts a bitvector to one of a set of strings from an enumeration.
/// Useful for encoding state variables.
///

public class EnumValueFormatter implements ValueFormatter {
    public void loadFromFile(File file) throws IOException {
        fFile = file;

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                String [] tokens = line.split(" ");
                if (tokens.length >= 2)
                    fMappings.put(Integer.parseInt(tokens[0]), tokens[1]);
            }
        } finally {
            if (br != null)
                br.close();
        }
    }

    public File getFile() {
        return fFile;
    }

    @Override
    public String format(BitVector bits) {
        int mapIndex = bits.intValue();
        if (fMappings.containsKey(mapIndex))
            return fMappings.get(mapIndex);
        else
            return "??? (" + mapIndex + ")";
    }

    private HashMap<Integer, String> fMappings = new HashMap<Integer, String>();
    private File fFile;
}

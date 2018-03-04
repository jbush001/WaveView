//
// Copyright 2017 Jeff Bush
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

import java.util.ArrayList;

public class RecentFiles {
    private static final int MAX_FILES = 10;

    public void add(String path) {
        int index = fFiles.indexOf(path);
        if (index == -1) {
            // Need to add a new entry. Discard the oldest entry if
            // necessary
            if (fFiles.size() == MAX_FILES)
                fFiles.remove(MAX_FILES - 1);
        } else {
            // Existing entry. Remove from old location. It will be
            // added to the beginning of the list.
            fFiles.remove(index);
        }

        fFiles.add(0, path);
    }

    public String pack() {
        StringBuilder packed = new StringBuilder();
        for (String file : fFiles) {
            if (packed.length() > 0)
                packed.append(';');

            packed.append(file);
        }

        return packed.toString();
    }

    public void unpack(String packed) {
        for (String path : packed.split(";"))
            fFiles.add(path);
    }

    public ArrayList<String> getList() {
        return fFiles;
    }

    private ArrayList<String> fFiles = new ArrayList<>();
}

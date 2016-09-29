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

import java.util.prefs.*;
import java.util.*;
import java.awt.*;
import java.io.*;

///
/// Global application preferences
///

class AppPreferences {
    private static final int MAX_RECENT_FILES = 10;

    static AppPreferences getInstance() {
        if (fInstance == null)
            fInstance = new AppPreferences();

        return fInstance;
    }

    Color traceColor;
    Color conflictColor;
    Color selectionColor;
    Color cursorColor;
    Color backgroundColor;
    Color timingMarkerColor;
    Color markerColor;
    Color listSelectionBgColor;
    Color listSelectionFgColor;
    Color valueColor;

    void setInitialTraceDirectory(File file) {
        fPrefs.put("initialTraceDirectory", file.toString());
    }

    File getInitialTraceDirectory() {
        return new File(fPrefs.get("initialTraceDirectory", ""));
    }

    void addFileToRecents(String path) {
        // check if this is already in the recent files list
        for (String recentFile : fRecentFiles) {
            if (recentFile.equals(path))
                return;    // Skip
        }

        // Remove oldest file from list if necessary
        if (fRecentFiles.size() >= MAX_RECENT_FILES)
            fRecentFiles.remove(0);

        fRecentFiles.add(path);

        // Write out to preferences file
        StringBuffer recentList = new StringBuffer();
        for (String recentFile : fRecentFiles) {
            if (recentList.length() > 0)
                recentList.append(';');

            recentList.append(recentFile);
        }

        fPrefs.put("recentFiles", recentList.toString());
    }

    ArrayList<String> getRecentFileList() {
        return fRecentFiles;
    }

    void writeColors() {
        writeColor("traceColor", traceColor);
        writeColor("conflictColor", conflictColor);
        writeColor("selectionColor", selectionColor);
        writeColor("cursorColor", cursorColor);
        writeColor("backgroundColor", backgroundColor);
        writeColor("timingMarkerColor", timingMarkerColor);
        writeColor("markerColor", markerColor);
        writeColor("listSelectionBgColor", listSelectionBgColor);
        writeColor("listSelectionFgColor", listSelectionFgColor);
        writeColor("valueColor", valueColor);
    }

    private AppPreferences() {
        readPreferences();
    }

    void readPreferences() {
        String recentList = fPrefs.get("recentFiles", "");
        String[] paths = recentList.split(";");
        for (String path : paths) {
            if (path.length() > 0)
                fRecentFiles.add(path);
        }

        traceColor = readColor("traceColor", Color.black);
        conflictColor = readColor("conflictColor", new Color(255, 200, 200));
        selectionColor = readColor("selectionColor", new Color(230, 230, 230));
        cursorColor = readColor("cursorColor", Color.red);
        backgroundColor = readColor("backgroundColor", Color.white);
        timingMarkerColor = readColor("timingMarkerColor", new Color(230, 230, 230));
        markerColor = readColor("markerColor", Color.green);
        listSelectionBgColor = readColor("listSelectionBgColor", Color.blue);
        listSelectionFgColor = readColor("listSelectionFgColor", Color.white);
        valueColor = readColor("valueColor", Color.blue);
    }

    private Color readColor(String name, Color def) {
        int components = fPrefs.getInt(name, -1);
        if (components == -1)
            return def;

        return new Color((components >> 16) & 0xff, (components >> 8) & 0xff,
                         components & 0xff);
    }

    private void writeColor(String name, Color color) {
        int packed = color.getBlue() | (color.getGreen() << 8) | (color.getRed() << 16);
        fPrefs.putInt(name, packed);
    }

    private Preferences fPrefs = Preferences.userNodeForPackage(MainWindow.class);
    private static AppPreferences fInstance;
    private ArrayList<String> fRecentFiles = new ArrayList<String>();
}

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


import java.util.prefs.*;
import java.util.*;
import java.awt.*;
import java.io.*;

///
/// Global application preferences
///

class AppPreferences {
    private static final int kMaxRecentFiles = 10;

    public static AppPreferences getInstance() {
        if (fInstance == null)
            fInstance = new AppPreferences();

        return fInstance;
    }

    public Color kTraceColor;
    public Color kConflictColor;
    public Color kSelectionColor;
    public Color kCursorColor;
    public Color kBackgroundColor;
    public Color kTimingMarkerColor;
    public Color kMarkerColor;
    public Color kListSelectionBgColor;
    public Color kListSelectionFgColor;
    public Color kValueColor;

    public void setInitialTraceDirectory(File file) {
        fPrefs.put("initialTraceDirectory", file.toString());
    }

    public File getInitialTraceDirectory() {
        return new File(fPrefs.get("initialTraceDirectory", ""));
    }

    public void addFileToRecents(String path) {
        // check if this is already in the recent files list
        for (String recentFile : fRecentFiles) {
            if (recentFile.equals(path))
                return;    // Skip
        }

        // Remove oldest file from list if necessary
        if (fRecentFiles.size() >= kMaxRecentFiles)
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

    public ArrayList<String> getRecentFileList() {
        return fRecentFiles;
    }

    public void writeColors() {
        writeColor("traceColor", kTraceColor);
        writeColor("conflictColor", kConflictColor);
        writeColor("selectionColor", kSelectionColor);
        writeColor("cursorColor", kCursorColor);
        writeColor("backgroundColor", kBackgroundColor);
        writeColor("timingMarkerColor", kTimingMarkerColor);
        writeColor("markerColor", kMarkerColor);
        writeColor("listSelectionBgColor", kListSelectionBgColor);
        writeColor("listSelectionFgColor", kListSelectionFgColor);
        writeColor("valueColor", kValueColor);
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

        kTraceColor = readColor("traceColor", Color.black);
        kConflictColor = readColor("conflictColor", new Color(255, 200, 200));
        kSelectionColor = readColor("selectionColor", new Color(230, 230, 230));
        kCursorColor = readColor("cursorColor", Color.red);
        kBackgroundColor = readColor("backgroundColor", Color.white);
        kTimingMarkerColor = readColor("timingMarkerColor", new Color(230, 230, 230));
        kMarkerColor = readColor("markerColor", Color.green);
        kListSelectionBgColor = readColor("listSelectionBgColor", Color.blue);
        kListSelectionFgColor = readColor("listSelectionFgColor", Color.white);
        kValueColor = readColor("valueColor", Color.blue);
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

    private Preferences fPrefs = Preferences.userNodeForPackage(WaveApp.class);
    private static AppPreferences fInstance;
    private ArrayList<String> fRecentFiles = new ArrayList<String>();
}

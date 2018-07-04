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

import java.awt.Color;
import java.io.File;
import java.util.prefs.Preferences;

///
/// Global application preferences
///

class AppPreferences {
    private static AppPreferences instance;
    private final Preferences prefs = Preferences.userNodeForPackage(MainWindow.class);

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

    static AppPreferences getInstance() {
        if (instance == null) {
            instance = new AppPreferences();
        }

        return instance;
    }

    private AppPreferences() {
        readColors();
    }

    // The initial trace directory is the one the file chooser will open by
    // default when opened when the user tries to open a trace.
    void setInitialTraceDirectory(File file) {
        prefs.put("initialTraceDirectory", file.toString());
    }

    File getInitialTraceDirectory() {
        return new File(prefs.get("initialTraceDirectory", ""));
    }

    // The initial enum directory is the one the file chooser will open by
    // default when the user attempts to open an enum mapping file
    void setInitialEnumDirectory(File file) {
        prefs.put("initialEnumDirectory", file.toString());
    }

    File getInitialEnumDirectory() {
        return new File(prefs.get("initialEnumDirectory", ""));
    }

    void setRecentList(String files) {
        prefs.put("recentFiles", files);
    }

    String getRecentList() {
        return prefs.get("recentFiles", "");
    }

    void readColors() {
        traceColor = readColor("traceColor", Color.black);
        conflictColor = readColor("conflictColor", new Color(255, 200, 200));
        selectionColor = readColor("selectionColor", new Color(230, 230, 230));
        cursorColor = readColor("cursorColor", Color.red);
        backgroundColor = readColor("backgroundColor", Color.white);
        timingMarkerColor = readColor("timingMarkerColor", new Color(200, 200, 200));
        markerColor = readColor("markerColor", Color.green);
        listSelectionBgColor = readColor("listSelectionBgColor", Color.blue);
        listSelectionFgColor = readColor("listSelectionFgColor", Color.white);
        valueColor = readColor("valueColor", Color.blue);
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

    private Color readColor(String name, Color def) {
        int components = prefs.getInt(name, -1);
        if (components == -1)
            return def;

        return new Color((components >> 16) & 0xff, (components >> 8) & 0xff, components & 0xff);
    }

    private void writeColor(String name, Color color) {
        int packed = color.getBlue() | (color.getGreen() << 8) | (color.getRed() << 16);
        prefs.putInt(name, packed);
    }
}

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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.event.*;
import javax.swing.text.*;

class ColorPreferencePane extends PreferencePane {
    ColorPreferencePane() {
        super(new FlowLayout());
        AppPreferences prefs = AppPreferences.getInstance();

        fTraceColorButton = new ColorButton("Trace", prefs.traceColor);
        add(fTraceColorButton);
        fConflictColorButton = new ColorButton("Conflict", prefs.conflictColor);
        add(fConflictColorButton);
        fSelectionColorButton = new ColorButton("Selection", prefs.selectionColor);
        add(fSelectionColorButton);
        fCursorColorButton = new ColorButton("Cursor", prefs.cursorColor);
        add(fCursorColorButton);
        fBackgroundColorButton = new ColorButton("Background", prefs.backgroundColor);
        add(fBackgroundColorButton);
        fTimingMarkerColorButton = new ColorButton("Timing Mark", prefs.timingMarkerColor);
        add(fTimingMarkerColorButton);
        fMarkerColorButton = new ColorButton("Marker", prefs.markerColor);
        add(fMarkerColorButton);
        fListSelectionBgColorButton = new ColorButton("List Selection Background",
                prefs.listSelectionBgColor);
        add(fListSelectionBgColorButton);
        fListSelectionFgColorButton = new ColorButton("List Selection Foreground",
                prefs.listSelectionFgColor);
        add(fListSelectionFgColorButton);
        fValueColorButton = new ColorButton("Value", prefs.valueColor);
        add(fValueColorButton);

        setPreferredSize(new Dimension(275, 500));
    }

    @Override
    void loadSettings() {}

    @Override
    void saveSettings() {
        AppPreferences prefs = AppPreferences.getInstance();
        prefs.traceColor = fTraceColorButton.getColor();
        prefs.conflictColor = fConflictColorButton.getColor();
        prefs.selectionColor = fSelectionColorButton.getColor();
        prefs.cursorColor = fCursorColorButton.getColor();
        prefs.backgroundColor = fBackgroundColorButton.getColor();
        prefs.timingMarkerColor = fTimingMarkerColorButton.getColor();
        prefs.markerColor = fMarkerColorButton.getColor();
        prefs.listSelectionBgColor = fListSelectionBgColorButton.getColor();
        prefs.listSelectionFgColor = fListSelectionFgColorButton.getColor();
        prefs.valueColor = fValueColorButton.getColor();
        prefs.writeColors();
    }

    private ColorButton fTraceColorButton;
    private ColorButton fConflictColorButton;
    private ColorButton fSelectionColorButton;
    private ColorButton fCursorColorButton;
    private ColorButton fBackgroundColorButton;
    private ColorButton fTimingMarkerColorButton;
    private ColorButton fMarkerColorButton;
    private ColorButton fListSelectionBgColorButton;
    private ColorButton fListSelectionFgColorButton;
    private ColorButton fValueColorButton;
}

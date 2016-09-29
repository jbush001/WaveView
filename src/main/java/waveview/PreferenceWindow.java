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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.datatransfer.*;
import javax.swing.text.*;

class PreferenceWindow extends JDialog {
    PreferenceWindow(JFrame parent) {
        super(parent, "Preferences", true);

        setPreferredSize(new Dimension(500, 400));

        Container contentPane = new Container();
        contentPane.setLayout(new BorderLayout());

        JPanel bodyArea = new JPanel();
        bodyArea.setLayout(new GridLayout(10, 1));
        AppPreferences prefs = AppPreferences.getInstance();
        fTraceColorButton = new ColorButton("Trace", prefs.traceColor);
        bodyArea.add(fTraceColorButton);
        fConflictColorButton = new ColorButton("Conflict", prefs.conflictColor);
        bodyArea.add(fConflictColorButton);
        fSelectionColorButton = new ColorButton("Selection", prefs.selectionColor);
        bodyArea.add(fSelectionColorButton);
        fCursorColorButton = new ColorButton("Cursor", prefs.cursorColor);
        bodyArea.add(fCursorColorButton);
        fBackgroundColorButton = new ColorButton("Background", prefs.backgroundColor);
        bodyArea.add(fBackgroundColorButton);
        fTimingMarkerColorButton = new ColorButton("Timing Mark", prefs.timingMarkerColor);
        bodyArea.add(fTimingMarkerColorButton);
        fMarkerColorButton = new ColorButton("Marker", prefs.markerColor);
        bodyArea.add(fMarkerColorButton);
        fListSelectionBgColorButton = new ColorButton("List Selection Background",
                prefs.listSelectionBgColor);
        bodyArea.add(fListSelectionBgColorButton);
        fListSelectionFgColorButton = new ColorButton("List Selection Foreground",
                prefs.listSelectionFgColor);
        bodyArea.add(fListSelectionFgColorButton);
        fValueColorButton = new ColorButton("Value", prefs.valueColor);
        bodyArea.add(fValueColorButton);
        contentPane.add(bodyArea, BorderLayout.CENTER);

        Container okCancelContainer = new Container();
        okCancelContainer.setLayout(new FlowLayout());
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                ok();
            }
        });

        okCancelContainer.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                cancel();
            }
        });
        okCancelContainer.add(cancelButton);

        contentPane.add(okCancelContainer, BorderLayout.SOUTH);
        setContentPane(contentPane);
        pack();
    }

    protected void cancel() {
        dispose();
    }

    protected void ok() {
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
        dispose();
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

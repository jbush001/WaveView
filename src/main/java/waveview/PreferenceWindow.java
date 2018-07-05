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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

class PreferenceWindow extends JDialog {
    private final ColorButton traceColorButton;
    private final ColorButton conflictColorButton;
    private final ColorButton selectionColorButton;
    private final ColorButton cursorColorButton;
    private final ColorButton backgroundColorButton;
    private final ColorButton timingMarkerColorButton;
    private final ColorButton markerColorButton;
    private final ColorButton listSelectionBgColorButton;
    private final ColorButton listSelectionFgColorButton;
    private final ColorButton valueColorButton;

    PreferenceWindow(JFrame parent) {
        super(parent, "Preferences", true);

        setPreferredSize(new Dimension(500, 400));

        Container contentPane = new Container();
        contentPane.setLayout(new BorderLayout());

        JPanel bodyArea = new JPanel();
        bodyArea.setLayout(new GridLayout(10, 1));
        AppPreferences prefs = AppPreferences.getInstance();
        traceColorButton = new ColorButton("Trace", prefs.traceColor);
        bodyArea.add(traceColorButton);
        conflictColorButton = new ColorButton("Conflict", prefs.conflictColor);
        bodyArea.add(conflictColorButton);
        selectionColorButton = new ColorButton("Selection", prefs.selectionColor);
        bodyArea.add(selectionColorButton);
        cursorColorButton = new ColorButton("Cursor", prefs.cursorColor);
        bodyArea.add(cursorColorButton);
        backgroundColorButton = new ColorButton("Background", prefs.backgroundColor);
        bodyArea.add(backgroundColorButton);
        timingMarkerColorButton = new ColorButton("Timing Mark", prefs.timingMarkerColor);
        bodyArea.add(timingMarkerColorButton);
        markerColorButton = new ColorButton("Marker", prefs.markerColor);
        bodyArea.add(markerColorButton);
        listSelectionBgColorButton = new ColorButton("List Selection Background", prefs.listSelectionBgColor);
        bodyArea.add(listSelectionBgColorButton);
        listSelectionFgColorButton = new ColorButton("List Selection Foreground", prefs.listSelectionFgColor);
        bodyArea.add(listSelectionFgColorButton);
        valueColorButton = new ColorButton("Value", prefs.valueColor);
        bodyArea.add(valueColorButton);
        contentPane.add(bodyArea, BorderLayout.CENTER);

        Container okCancelContainer = new Container();
        okCancelContainer.setLayout(new FlowLayout());
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> ok());

        okCancelContainer.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> cancel());
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
        prefs.traceColor = traceColorButton.getColor();
        prefs.conflictColor = conflictColorButton.getColor();
        prefs.selectionColor = selectionColorButton.getColor();
        prefs.cursorColor = cursorColorButton.getColor();
        prefs.backgroundColor = backgroundColorButton.getColor();
        prefs.timingMarkerColor = timingMarkerColorButton.getColor();
        prefs.markerColor = markerColorButton.getColor();
        prefs.listSelectionBgColor = listSelectionBgColorButton.getColor();
        prefs.listSelectionFgColor = listSelectionFgColorButton.getColor();
        prefs.valueColor = valueColorButton.getColor();
        prefs.writeColors();
        dispose();
    }
}

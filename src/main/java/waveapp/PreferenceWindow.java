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
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.datatransfer.*;
import javax.swing.text.*;

class PreferenceWindow extends JFrame {
    PreferenceWindow() {
        super("Preferences");

        setPreferredSize(new Dimension(500, 400));

        JPanel contentPane = new JPanel();

        contentPane.setLayout(new BorderLayout());
        fTabbedPane = new JTabbedPane();
        fTabbedPane.addTab("Colors", null, new ColorPreferencePane(), "");
        fTabbedPane.setOpaque(true);
        contentPane.add(fTabbedPane, BorderLayout.CENTER);

        Container okCancelContainer = new Container();
        okCancelContainer.setLayout(new FlowLayout());
        JButton okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                ok();
            }
        });

        okCancelContainer.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
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
        setVisible(false);
    }

    protected void ok() {
        for (int i = 0; i < fTabbedPane.getTabCount(); i++)
            ((PreferencePane) fTabbedPane.getComponentAt(i)).saveSettings();

        setVisible(false);
    }

    JTabbedPane fTabbedPane;
}

//
// Copyright 2011-2016 Jeff Bush
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
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;

class FindPanel extends JPanel implements ActionListener {
    FindPanel(MainWindow app, String initialSearch) {
        fMainWindow = app;

        setLayout(new BorderLayout());

        JLabel findLabel = new JLabel("Find:");
        fTextArea = new JTextArea(5, 30);
        fTextArea.setLineWrap(true);
        fTextArea.setText(initialSearch);
        fTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                invalidateText();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                invalidateText();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                invalidateText();
            }
        });

        fHighlighter = fTextArea.getHighlighter();
        fHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.yellow);

        JButton prevButton = new JButton("Prev");
        prevButton.addActionListener(this);
        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(this);

        JPanel findContainer = new JPanel();
        findContainer.setLayout(new FlowLayout(FlowLayout.LEFT));
        findContainer.add(findLabel);
        findContainer.add(new JScrollPane(fTextArea));
        add(findContainer, BorderLayout.CENTER);

        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonContainer.add(prevButton);
        buttonContainer.add(nextButton);
        add(buttonContainer, BorderLayout.SOUTH);
    }

    /// Called when the user changes the search string.
    void invalidateText() {
        // The next time the user hits next/prev, need to regenerate the Search.
        fNeedsSearchUpdate = true;

        // When the user begins editing, remove the error highlights
        // so they don't leave boogers all over the place.
        fHighlighter.removeAllHighlights();
    }

    /// If the user changed the search string, try to parse it and generate
    /// a new Search object. If the search string is invalid, highlight the
    /// incorrect portion and pop up an error message.
    private void checkUpdateSearch() {
        if (fNeedsSearchUpdate) {
            try {
                fMainWindow.setSearch(fTextArea.getText());
            } catch (Search.ParseException exc) {
                // Highlight error
                fHighlighter.removeAllHighlights();
                try {
                    fHighlighter.addHighlight(exc.getStartOffset(), exc.getEndOffset() + 1,
                                              fHighlightPainter);
                } catch (BadLocationException ble) {
                    System.out.println("execption " + ble);
                }

                /// @todo Should this be displayed in the window somewhere?
                JOptionPane.showMessageDialog(null, exc.getMessage(), "Error parsing expression",
                                              JOptionPane.ERROR_MESSAGE);
            }

            fNeedsSearchUpdate = false;
        }
    }

    /// Handle button presses
    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("Prev")) {
            checkUpdateSearch();
            fMainWindow.findPrev(false);
        } else if (cmd.equals("Next")) {
            checkUpdateSearch();
            fMainWindow.findNext(false);
        }
    }

    private JTextArea fTextArea;
    private MainWindow fMainWindow;
    private boolean fNeedsSearchUpdate = true;
    private transient Highlighter fHighlighter;
    private transient Highlighter.HighlightPainter fHighlightPainter;
}


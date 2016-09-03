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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.text.*;

class FindPanel extends JPanel implements ActionListener {
    FindPanel(WaveApp app, String initialQuery) {
        fWaveApp = app;

        setLayout(new BorderLayout());

        JLabel findLabel = new JLabel("Find:");
        fTextArea = new JTextArea(5, 30);
        fTextArea.setLineWrap(true);
        fTextArea.setText(initialQuery);
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

    /// Called when the user alters the current query string.
    void invalidateText() {
        // The next time the user hits next/prev, need to regenerate the query.
        fNeedsQueryUpdate = true;

        // When the user beings editing, remove the error highlights
        // so they don't leave boogers all over the place.
        fHighlighter.removeAllHighlights();
    }

    /// If the query has been updated, try to parse it and generate a new query
    /// object. If the query string is invalid, highlight the incorrect portion
    /// and pop up an error message.
    private void checkUpdateQuery() {
        if (fNeedsQueryUpdate) {
            try {
                fWaveApp.setQuery(fTextArea.getText());
            } catch (Query.ParseException exc) {
                // Highlight error
                fHighlighter.removeAllHighlights();
                try {
                    fHighlighter.addHighlight(exc.getStartOffset(), exc.getEndOffset() + 1,
                                              fHighlightPainter);
                } catch (BadLocationException ble) {
                    // Ignore
                }

                /// @todo Should this be displayed in the window somewhere?
                JOptionPane.showMessageDialog(null, exc.getMessage(), "Error parsing query",
                                              JOptionPane.ERROR_MESSAGE);
            }

            fNeedsQueryUpdate = false;
        }
    }

    /// Handle button presses
    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("Prev")) {
            checkUpdateQuery();
            fWaveApp.findPrev();
        } else if (cmd.equals("Next")) {
            checkUpdateQuery();
            fWaveApp.findNext();
        }
    }

    private JTextArea fTextArea;
    private JScrollPane fScrollPane;
    private WaveApp fWaveApp;
    private boolean fNeedsQueryUpdate = true;
    private Highlighter fHighlighter;
    private Highlighter.HighlightPainter fHighlightPainter;
}


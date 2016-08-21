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
    public FindPanel(WaveApp app) {
        fWaveApp = app;

        setLayout(new BorderLayout());

        JLabel findLabel = new JLabel("Find:");
        fTextArea = new JTextArea(5, 30);
        fTextArea.setLineWrap(true);
        fTextArea.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                invalidateText();
            }

            public void removeUpdate(DocumentEvent e) {
                invalidateText();
            }

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
        findContainer.add(fTextArea);
        add(findContainer, BorderLayout.CENTER);

        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonContainer.add(prevButton);
        buttonContainer.add(nextButton);
        add(buttonContainer, BorderLayout.SOUTH);
    }

    public void setInitialQuery(String string) {
        fTextArea.setText(string);
    }

    void invalidateText() {
        // The next time the user hits next/prev, need to regenerate the query.
        fNeedsQueryUpdate = true;

        // When the user beings editing, remove the error highlights
        // so they don't leave boogers all over the place.
        fHighlighter.removeAllHighlights();
    }

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
                JOptionPane.showMessageDialog(null, exc.toString(),"Error parsing query",
                                              JOptionPane.ERROR_MESSAGE);
            }

            fNeedsQueryUpdate = false;
        }
    }

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


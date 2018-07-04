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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

class FindPanel extends JPanel implements ActionListener {
    private JTextArea textArea;
    private MainWindow mainWindow;
    private boolean needsSearchUpdate = true;
    private transient Highlighter highlighter;
    private transient Highlighter.HighlightPainter highlightPainter;

    FindPanel(MainWindow mainWindow, String initialSearch) {
        this.mainWindow = mainWindow;

        setLayout(new BorderLayout());

        JLabel findLabel = new JLabel("Find:");
        textArea = new JTextArea(5, 30);
        textArea.setLineWrap(true);
        textArea.setText(initialSearch);
        textArea.getDocument().addDocumentListener(new DocumentListener() {
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

        highlighter = textArea.getHighlighter();
        highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.yellow);

        JButton prevButton = new JButton("Prev");
        prevButton.addActionListener(this);
        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(this);

        JPanel findContainer = new JPanel();
        findContainer.setLayout(new FlowLayout(FlowLayout.LEFT));
        findContainer.add(findLabel);
        findContainer.add(new JScrollPane(textArea));
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
        needsSearchUpdate = true;

        // When the user begins editing, remove the error highlights
        // so they don't leave boogers all over the place.
        highlighter.removeAllHighlights();
    }

    /// If the user changed the search string, try to parse it and generate
    /// a new Search object. If the search string is invalid, highlight the
    /// incorrect portion and pop up an error message.
    private void checkUpdateSearch() {
        if (needsSearchUpdate) {
            try {
                mainWindow.setSearch(textArea.getText());
            } catch (Search.ParseException exc) {
                // Highlight error
                highlighter.removeAllHighlights();
                try {
                    highlighter.addHighlight(exc.getStartOffset(), exc.getEndOffset() + 1, highlightPainter);
                } catch (BadLocationException ble) {
                    System.out.println("execption " + ble);
                }

                /// @todo Should this be displayed in the window somewhere?
                JOptionPane.showMessageDialog(null, exc.getMessage(), "Error parsing expression",
                        JOptionPane.ERROR_MESSAGE);
            }

            needsSearchUpdate = false;
        }
    }

    /// Handle button presses
    @Override
    public void actionPerformed(ActionEvent e) {
        checkUpdateSearch();
        switch (e.getActionCommand()) {
        case "Prev":
            mainWindow.findPrev(false);
            break;
        case "Next":
            mainWindow.findNext(false);
            break;
        default:
            System.out.println("FindPanel: unknown action " + e.getActionCommand());
        }
    }
}

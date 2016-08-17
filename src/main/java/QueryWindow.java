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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.datatransfer.*;
import javax.swing.text.*;
import javax.swing.table.*;

public class QueryWindow extends JFrame
{
    public QueryWindow()
    {
        super("Query");
        setPreferredSize(new Dimension(300, 250));

        fTextArea = new JTextArea();
        fTextArea.setLineWrap(true);
        fScrollPane = new JScrollPane(fTextArea);
        setContentPane(fScrollPane);
        pack();
    }

    public void setInitialQuery(String string)
    {
        fTextArea.setText(string);
    }

    private JTextArea fTextArea;
    private JScrollPane fScrollPane;
}


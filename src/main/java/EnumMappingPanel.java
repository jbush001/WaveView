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
import javax.swing.event.*;
import javax.swing.table.*;

///
/// This window allows editing number -> enum identifier mappings.
/// Usually used for displaying human readable names to state machine
/// states.
/// @bug Should check for duplicate values and display an error
/// @bug Need to put values in order
/// @bug When enum is updated, the waveform will still display old values
///   until it is redrawn. There's no notifier to tell TraceDisplayModel
///   listeners that enum mappings have changed.
///
class EnumMappingPanel extends JPanel {
    EnumMappingPanel(EnumValueFormatter formatter) {
        super(new BorderLayout());

        fTable = new JTable(new EnumMappingTableModel(formatter));
        JScrollPane scroller = new JScrollPane(fTable);
        fTable.setFillsViewportHeight(true);
        add(scroller);
    }

    private JTable fTable;
}


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
/// This is a window that allows editing a table of number -> enum identifier
/// mappings.  Usually used for displaying human readable names to state machine
/// states.
/// @bug Should check for duplicate values
/// @bug Need to put values in order
/// @bug When enum is updated, the waveform will still display old values
///   until it is redrawn. There's no notifier to tell TraceDisplayModel
///   listeners that enum mappings have changed.
///
class EnumMappingPanel extends JPanel {
    public EnumMappingPanel(EnumValueFormatter formatter) {
        super(new BorderLayout());

        fTable = new JTable(new MappingTableModel(formatter));
        JScrollPane scroller = new JScrollPane(fTable);
        fTable.setFillsViewportHeight(true);
        add(scroller);
    }

    class MappingTableModel extends AbstractTableModel {
        public MappingTableModel(EnumValueFormatter formatter) {
            fFormatter = formatter;
        }

        @Override
        public String getColumnName(int col) {
            if (col == 0)
                return "value";
            else
                return "name";
        }

        @Override
        public int getRowCount() {
            return fFormatter.getMappingCount() + 1;    // Bottom row always adds
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= fFormatter.getMappingCount())
                return "";

            if (col == 0)
                return fFormatter.getValue(row);
            else
                return fFormatter.getName(row);
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return true;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (row == fFormatter.getMappingCount())
                fFormatter.addMapping(0, "");    // Create a new row

            if (col == 0)
                fFormatter.setValue(row, Integer.parseInt((String) value));
            else
                fFormatter.setName(row, (String) value);
        }

        private EnumValueFormatter fFormatter;
    }

    private JTable fTable;
}


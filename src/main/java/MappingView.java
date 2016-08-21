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
/// This is a window that allows editing a table of number -> identifier
/// mappings.  Usually used for displaying human readable names to state machine
/// states.
/// @bug Should check for duplicate values
///
class MappingView extends JPanel
{
    public MappingView(IdentifierValueFormatter formatter)
    {
        super(new BorderLayout());

        fTable = new JTable(new MappingTableModel(formatter));
        JScrollPane scroller = new JScrollPane(fTable);
        fTable.setFillsViewportHeight(true);
        add(scroller);
    }

    class MappingTableModel extends AbstractTableModel
    {
        private static final int kNumColumns = 2;

        public MappingTableModel(IdentifierValueFormatter formatter)
        {
            fFormatter = formatter;
        }

        public String getColumnName(int col)
        {
            if (col == 0)
                return "value";
            else
                return "name";
        }

        public int getRowCount()
        {
            return fFormatter.getMappingCount() + 1;    // Bottom row always adds
        }

        public int getColumnCount()
        {
            return 2;
        }

        public Object getValueAt(int row, int col)
        {
            if (row >= fFormatter.getMappingCount())
                return "";

            if (col == 0)
                return fFormatter.getValueByIndex(row);
            else
                return fFormatter.getNameByIndex(row);
        }

        public boolean isCellEditable(int row, int col)
        {
            return true;
        }

        public void setValueAt(Object value, int row, int col)
        {
            if (row == fFormatter.getMappingCount())
                fFormatter.addMapping(0, "");    // Create a new row

            if (col == 0)
                fFormatter.setValueAtIndex(row, Integer.parseInt((String) value));
            else
                fFormatter.setNameAtIndex(row, (String) value);
        }

        IdentifierValueFormatter fFormatter;
    }

    JTable fTable;
}


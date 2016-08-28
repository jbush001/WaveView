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

import javax.swing.table.*;

///
/// Used by EnumMappingPanel to display and edit mapping for a net.
///

class EnumMappingTableModel extends AbstractTableModel {
    public EnumMappingTableModel(EnumValueFormatter formatter) {
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
            return Integer.toString(fFormatter.getValue(row));
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
            fFormatter.addMapping(0, "");    // Create a new row at bottom

        if (col == 0)
            fFormatter.setValue(row, Integer.parseInt((String) value));
        else
            fFormatter.setName(row, (String) value);
    }

    private EnumValueFormatter fFormatter;
}

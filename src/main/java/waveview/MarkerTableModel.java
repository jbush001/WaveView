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

import javax.swing.table.AbstractTableModel;

///
/// Used by MarkerListPanel to display all markers
///

public class MarkerTableModel extends AbstractTableModel {
    private static final int NUM_COLUMNS = 3;
    private static final String COLUMN_NAMES[] = { "ID", "Timestamp", "Comment" };
    private final TraceDisplayModel traceDisplayModel;

    public MarkerTableModel(TraceDisplayModel traceDisplayModel) {
        this.traceDisplayModel = traceDisplayModel;
    }

    @Override
    public int getColumnCount() {
        return NUM_COLUMNS;
    }

    @Override
    public String getColumnName(int col) {
        return COLUMN_NAMES[col];
    }

    @Override
    public int getRowCount() {
        return traceDisplayModel.getMarkerCount();
    }

    @Override
    public Object getValueAt(int row, int col) {
        switch (col) {
        case 0:
            return Integer.toString(traceDisplayModel.getIdForMarker(row));
        case 1:
            // XXX add suffix with units here.
            return Long.toString(traceDisplayModel.getTimestampForMarker(row));
        case 2:
            return traceDisplayModel.getDescriptionForMarker(row);
        default:
            return "";
        }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col == 2; // Can edit description
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        traceDisplayModel.setDescriptionForMarker(row, (String) value);
    }
}

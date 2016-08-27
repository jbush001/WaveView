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

///
/// Displays a list of all markers and their timestamps.  The user can click on them
/// to jump to that point in the trace.
/// @todo Add a way to remove entries directly from this list
///
public class MarkerListPanel extends JPanel implements ActionListener, TraceDisplayModel.Listener {
    public MarkerListPanel(TraceDisplayModel traceModel) {
        setLayout(new GridLayout(1, 1));

        fTraceDisplayModel = traceModel;
        fTraceDisplayModel.addListener(this);

        fTableModel = new MarkerTableModel(fTraceDisplayModel);
        fTable = new JTable(fTableModel);

        fTable.getColumnModel().getColumn(0).setMaxWidth(35);
        fTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    select(e.isShiftDown());
            }
        });

        JScrollPane panel = new JScrollPane(fTable);
        add(panel);
    }

    private void select(boolean extendSelection) {
        long timestamp = fTraceDisplayModel.getTimestampForMarker(fTable.getSelectedRow());

        fTraceDisplayModel.setCursorPosition(timestamp);
        if (!extendSelection)
            fTraceDisplayModel.setSelectionStart(timestamp);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    @Override
    public void cursorChanged(long oldTimestamp, long newTimestamp) {
    }

    @Override
    public void netsAdded(int firstIndex, int lastIndex) {
    }

    @Override
    public void netsRemoved(int firstIndex, int lastIndex) {
    }

    @Override
    public void scaleChanged(double newScale) {
    }

    @Override
    public void formatChanged(int index) {
    }

    @Override
    public void markerChanged(long timestamp) {
        fTableModel.fireTableDataChanged();
        // Redraw list
        repaint();
    }

    private TraceDisplayModel fTraceDisplayModel;
    private MarkerTableModel fTableModel;
    private JTable fTable;
}

class MarkerTableModel extends AbstractTableModel {
    private static final int NUM_COLUMNS = 3;

    public MarkerTableModel(TraceDisplayModel model) {
        fTraceDisplayModel = model;
    }

    @Override
    public int getColumnCount() {
        return NUM_COLUMNS;
    }

    @Override
    public String getColumnName(int col) {
        return kColumnNames[col];
    }

    @Override
    public int getRowCount() {
        return fTraceDisplayModel.getMarkerCount();
    }

    @Override
    public Object getValueAt(int row, int col) {
        switch (col) {
        case 0:
            return "" + fTraceDisplayModel.getIdForMarker(row);
        case 1:
            return "" + fTraceDisplayModel.getTimestampForMarker(row) + " ns";
        case 2:
            return "" + fTraceDisplayModel.getDescriptionForMarker(row);
        }

        return "";
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col == 2;    // Can edit description
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        fTraceDisplayModel.setDescriptionForMarker(row, (String) value);
    }

    private String kColumnNames[] = { "ID", "Timestamp", "Comment" };
    private TraceDisplayModel fTraceDisplayModel;
}


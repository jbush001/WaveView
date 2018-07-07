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

package waveview;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

///
/// Displays a list of all markers and their timestamps.  The user can click
/// on them to jump to that point in the trace.
/// @todo Add a way to remove entries from this list
///
class MarkerListPanel extends JPanel implements ActionListener, TracePresentationModel.Listener {
    private final TracePresentationModel tracePresentationModel;
    private final MarkerTableModel tableModel;
    private final JTable table;

    MarkerListPanel(TracePresentationModel tracePresentationModel) {
        setLayout(new GridLayout(1, 1));

        this.tracePresentationModel = tracePresentationModel;
        tracePresentationModel.addListener(this);

        tableModel = new MarkerTableModel(tracePresentationModel);
        table = new JTable(tableModel);

        table.getColumnModel().getColumn(0).setMaxWidth(35);

        // Double clicking an item in the list will cause the trace
        // view to jump to that location. Holding shift will select
        // the area between the old cursor and new location (consistent
        // with click selection in the trace view)
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    select(e.isShiftDown());
                }
            }
        });

        JScrollPane panel = new JScrollPane(table);
        add(panel);
    }

    private void select(boolean extendSelection) {
        long timestamp = tracePresentationModel.getTimestampForMarker(table.getSelectedRow());
        tracePresentationModel.setCursorPosition(timestamp);
        if (!extendSelection) {
            tracePresentationModel.setSelectionStart(timestamp);
        }
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
        tableModel.fireTableDataChanged();
        repaint();
    }
}

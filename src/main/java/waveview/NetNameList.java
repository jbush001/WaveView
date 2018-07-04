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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.TransferHandler;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

///
/// Displays names of nets next to waveforms, along with value at cursor
///
class NetNameList extends JList<Integer> implements TraceDisplayModel.Listener, ActionListener {

    private TraceDisplayModel traceDisplayModel;
    private TraceDataModel traceDataModel;
    private JPopupMenu popupMenu;

    class NetNameRenderer extends JPanel implements ListCellRenderer<Integer> {
        private int currentNet;
        private boolean currentNetIsSelected;
        private int labelBaseline = -1;
        private int valueBaseline;
        private final Font labelFont = new Font("SansSerif", Font.BOLD, 10);
        private final Font valueFont = new Font("SansSerif", Font.PLAIN, 8);

        NetNameRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Integer> list, Integer value, int index,
                boolean isSelected, boolean cellHasFocus) {
            currentNet = value.intValue();
            currentNetIsSelected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            AppPreferences prefs = AppPreferences.getInstance();
            setBackground(prefs.backgroundColor);

            super.paintComponent(g);

            if (currentNet == -1) {
                return;
            }

            if (labelBaseline == -1) {
                // Initialize this stuff once, then cache results
                FontMetrics labelMetrics = g.getFontMetrics(labelFont);
                FontMetrics valueMetrics = g.getFontMetrics(valueFont);
                labelBaseline = labelMetrics.getAscent();
                valueBaseline = labelBaseline + labelMetrics.getDescent() + labelMetrics.getLeading()
                        + valueMetrics.getAscent();
                int totalHeight = valueBaseline + valueMetrics.getDescent();
                int border = (DrawMetrics.WAVEFORM_V_SPACING - totalHeight) / 2;
                labelBaseline += border;
                valueBaseline += border;
            }

            if (currentNetIsSelected) {
                g.setColor(prefs.listSelectionBgColor);
            } else {
                g.setColor(prefs.backgroundColor);
            }

            g.fillRect(0, 0, getWidth(), DrawMetrics.WAVEFORM_V_SPACING);
            g.setFont(labelFont);
            g.setColor(currentNetIsSelected ? prefs.listSelectionFgColor : prefs.traceColor);

            int netId = traceDisplayModel.getVisibleNet(currentNet);
            String name = traceDataModel.getShortNetName(netId);
            g.drawString(name, 1, labelBaseline);

            g.setColor(currentNetIsSelected ? prefs.listSelectionFgColor : prefs.valueColor);
            g.setFont(valueFont);

            Transition t = traceDataModel.findTransition(netId, traceDisplayModel.getCursorPosition()).next();
            g.drawString(traceDisplayModel.getValueFormatter(currentNet).format(t), 1, valueBaseline);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            return traceDataModel.getFullNetName(traceDisplayModel.getVisibleNet(currentNet));
        }
    }

    class ListModelAdapter implements ListModel<Integer>, TraceDisplayModel.Listener {
        ListModelAdapter() {
            traceDisplayModel.addListener(this);
        }

        @Override
        public void addListDataListener(ListDataListener l) {
            fListeners.add(l);
        }

        @Override
        public void removeListDataListener(ListDataListener l) {
            fListeners.remove(l);
        }

        @Override
        public Integer getElementAt(int index) {
            return Integer.valueOf(index);
        }

        @Override
        public int getSize() {
            return traceDisplayModel.getVisibleNetCount();
        }

        @Override
        public void cursorChanged(long oldTimestamp, long newTimestamp) {
        }

        @Override
        public void netsAdded(int firstIndex, int lastIndex) {
            for (ListDataListener l : fListeners) {
                l.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, firstIndex, lastIndex));
            }
        }

        @Override
        public void netsRemoved(int firstIndex, int lastIndex) {
            for (ListDataListener l : fListeners) {
                l.intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, firstIndex, lastIndex));
            }
        }

        @Override
        public void markerChanged(long timestamp) {
        }

        @Override
        public void scaleChanged(double newScale) {
        }

        @Override
        public void formatChanged(int index) {
        }

        private ArrayList<ListDataListener> fListeners = new ArrayList<ListDataListener>();
    }

    /// Handles lists of signals dropped onto this view
    class NetTransferHandler extends TransferHandler {
        private boolean isLocalDrop;
        private int[] localIndices;

        @Override
        public int getSourceActions(JComponent component) {
            return MOVE;
        }

        @Override
        public Transferable createTransferable(JComponent component) {
            localIndices = ((JList) component).getSelectedIndices();
            isLocalDrop = true;
            return new StringSelection("");
        }

        @Override
        public void exportDone(JComponent component, Transferable transfer, int action) {
            isLocalDrop = false;
            // XXX do nothing
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            return true;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            String data;
            try {
                data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            } catch (Exception exc) {
                System.out.println(exc.toString());
                return false;
            }

            JList.DropLocation location = (JList.DropLocation) support.getDropLocation();
            int insertionPoint = location.getIndex();
            if (isLocalDrop) {
                traceDisplayModel.moveNets(localIndices, insertionPoint);
            } else {
                // Drag from another window (for example, net tree)
                String[] values = data.split("\n", 0);
                for (String value : values)
                    traceDisplayModel.makeNetVisible(insertionPoint++, traceDataModel.findNet(value));
            }

            // @todo Deal with selection changes. Should probably just clear the selection.

            return true;
        }
    }

    NetNameList(TraceDisplayModel traceDisplayModel, TraceDataModel traceDataModel) {
        this.traceDisplayModel = traceDisplayModel;
        this.traceDataModel = traceDataModel;
        traceDisplayModel.addListener(this);
        setModel(new ListModelAdapter());
        setCellRenderer(new NetNameRenderer());
        computeBounds();
        setFixedCellHeight(DrawMetrics.WAVEFORM_V_SPACING);
        setDragEnabled(true);
        setDropMode(DropMode.ON_OR_INSERT);
        setTransferHandler(new NetTransferHandler());
        setBackground(AppPreferences.getInstance().backgroundColor);

        popupMenu = new JPopupMenu();
        JMenuItem item = new JMenuItem("Remove Net");
        item.addActionListener(this);
        popupMenu.add(item);
        item = new JMenu("Format");
        JMenuItem subItem = new JMenuItem("Hex");
        item.add(subItem);
        subItem.addActionListener(this);
        subItem = new JMenuItem("Binary");
        subItem.addActionListener(this);
        item.add(subItem);
        subItem = new JMenuItem("Decimal");
        subItem.addActionListener(this);
        item.add(subItem);
        subItem = new JMenuItem("ASCII");
        subItem.addActionListener(this);
        item.add(subItem);
        subItem = new JMenuItem("Enum");
        subItem.addActionListener(this);
        item.add(subItem);
        popupMenu.add(item);
    }

    @Override
    protected void processMouseEvent(MouseEvent event) {
        if (event.isPopupTrigger()) {
            popupMenu.show(event.getComponent(), event.getX(), event.getY());
        } else {
            super.processMouseEvent(event);
        }
    }

    private void computeBounds() {
        Dimension d = getPreferredSize();
        d.width = 200;
        d.height = traceDisplayModel.getVisibleNetCount() * DrawMetrics.WAVEFORM_V_SPACING;
        setPreferredSize(d);
        validate();
        repaint();
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        /// @bug Eat key events so up/down arrows don't change selection
        /// If we allow arrows to change selection, the net name view scrolls
        /// without moving the other views. This seems like a bug in the swing
        /// components.
    }

    @Override
    public void scaleChanged(double newScale) {
    }

    @Override
    public void netsAdded(int firstIndex, int lastIndex) {
        computeBounds();
    }

    @Override
    public void netsRemoved(int firstIndex, int lastIndex) {
        computeBounds();
    }

    @Override
    public void markerChanged(long timestamp) {
    }

    @Override
    public void cursorChanged(long oldTimestamp, long newTimestamp) {
        repaint();
    }

    @Override
    public void formatChanged(int index) {
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int[] indices = getSelectedIndices();
        ValueFormatter formatter = null;

        switch (e.getActionCommand()) {
        case "Remove Net":
            removeNets(indices);
            break;
        case "Enum":
            formatter = createEnumFormatter();
            break;
        case "Hex":
            formatter = new HexadecimalValueFormatter();
            break;
        case "Binary":
            formatter = new BinaryValueFormatter();
            break;
        case "Decimal":
            formatter = new DecimalValueFormatter();
            break;
        case "ASCII":
            formatter = new ASCIIValueFormatter();
            break;
        default:
            System.out.println("NetNameList: unknown action " + e.getActionCommand());
            break;
        }

        if (formatter != null) {
            for (int i = 0; i < indices.length; i++) {
                traceDisplayModel.setValueFormatter(indices[i], formatter);
            }
        }
    }

    void removeNets(int[] indices) {
        for (int i = indices.length - 1; i >= 0; i--) {
            traceDisplayModel.removeNet(indices[i]);
        }

        clearSelection();
    }

    ValueFormatter createEnumFormatter() {
        ValueFormatter formatter = null;
        JFileChooser chooser = new JFileChooser(AppPreferences.getInstance().getInitialEnumDirectory());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int returnValue = chooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            AppPreferences.getInstance().setInitialEnumDirectory(chooser.getSelectedFile().getParentFile());
            try {
                formatter = new EnumValueFormatter();
                ((EnumValueFormatter) formatter).loadFromFile(chooser.getSelectedFile());
            } catch (Exception exc) {
                JOptionPane.showMessageDialog(this, "Error opening enum mapping file");
                formatter = null;
            }
        }

        return formatter;
    }
}

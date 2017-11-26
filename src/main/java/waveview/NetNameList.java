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

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.datatransfer.*;

///
/// Displays names of nets next to waveforms, along with value at cursor
///
class NetNameList extends JList<Integer> implements TraceDisplayModel.Listener,
    ActionListener {

    class NetNameRenderer extends JPanel implements ListCellRenderer<Integer> {
        private int fCurrentNet;
        private boolean fCurrentNetIsSelected;
        private int fLabelBaseline = -1;
        private int fValueBaseline;
        private Font fLabelFont = new Font("SansSerif", Font.BOLD, 10);
        private Font fValueFont = new Font("SansSerif", Font.PLAIN, 8);

        NetNameRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(
            JList<? extends Integer> list,
            Integer value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
            fCurrentNet = value.intValue();
            fCurrentNetIsSelected = isSelected;

            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            AppPreferences prefs = AppPreferences.getInstance();
            setBackground(prefs.backgroundColor);

            super.paintComponent(g);

            if (fCurrentNet == -1)
                return;

            if (fLabelBaseline == -1) {
                // Initialize this stuff once, then cache results
                FontMetrics labelMetrics = g.getFontMetrics(fLabelFont);
                FontMetrics valueMetrics = g.getFontMetrics(fValueFont);
                fLabelBaseline = labelMetrics.getAscent();
                fValueBaseline = fLabelBaseline + labelMetrics.getDescent() + labelMetrics.getLeading()
                                 + valueMetrics.getAscent();
                int totalHeight = fValueBaseline + valueMetrics.getDescent();
                int border = (DrawMetrics.WAVEFORM_V_SPACING - totalHeight) / 2;
                fLabelBaseline += border;
                fValueBaseline += border;
            }

            if (fCurrentNetIsSelected)
                g.setColor(prefs.listSelectionBgColor);
            else
                g.setColor(prefs.backgroundColor);

            g.fillRect(0, 0, getWidth(), DrawMetrics.WAVEFORM_V_SPACING);
            g.setFont(fLabelFont);
            g.setColor(fCurrentNetIsSelected ? prefs.listSelectionFgColor
                       : prefs.traceColor);

            int netId = fTraceDisplayModel.getVisibleNet(fCurrentNet);
            String name = fTraceDataModel.getShortNetName(netId);
            g.drawString(name, 1, fLabelBaseline);

            g.setColor(fCurrentNetIsSelected ? prefs.listSelectionFgColor
                       : prefs.valueColor);
            g.setFont(fValueFont);

            Transition t = fTraceDataModel.findTransition(netId,
                           fTraceDisplayModel.getCursorPosition()).next();
            g.drawString(fTraceDisplayModel.getValueFormatter(fCurrentNet).format(t),
                         1, fValueBaseline);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            return fTraceDataModel.getFullNetName(fTraceDisplayModel.getVisibleNet(fCurrentNet));
        }
    }

    class ListModelAdapter implements ListModel<Integer>, TraceDisplayModel.Listener {
        ListModelAdapter() {
            fTraceDisplayModel.addListener(this);
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
            return fTraceDisplayModel.getVisibleNetCount();
        }

        @Override
        public void cursorChanged(long oldTimestamp, long newTimestamp) {}

        @Override
        public void netsAdded(int firstIndex, int lastIndex) {
            for (ListDataListener l : fListeners) {
                l.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED,
                    firstIndex, lastIndex));
            }
        }

        @Override
        public void netsRemoved(int firstIndex, int lastIndex) {
            for (ListDataListener l : fListeners) {
                l.intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED,
                    firstIndex, lastIndex));
            }
        }

        @Override
        public void markerChanged(long timestamp) {}

        @Override
        public void scaleChanged(double newScale) {}

        @Override
        public void formatChanged(int index) {}

        private ArrayList<ListDataListener> fListeners = new ArrayList<ListDataListener>();
    }

    /// Handles lists of signals dropped onto this view
    class NetTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent component) {
            return MOVE;
        }

        @Override
        public Transferable createTransferable(JComponent component) {
            fLocalIndices = ((JList) component).getSelectedIndices();
            fIsLocalDrop = true;
            return new StringSelection("");
        }

        @Override
        public void exportDone(JComponent component, Transferable transfer, int action) {
            fIsLocalDrop = false;
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
            if (fIsLocalDrop) {
                fTraceDisplayModel.moveNets(fLocalIndices, insertionPoint);
            } else {
                // Drag from another window (for example, net tree)
                String[] values = data.split("\n");
                for (String value : values)
                    fTraceDisplayModel.makeNetVisible(insertionPoint++, fTraceDataModel.findNet(value));
            }

            // @todo Deal with selection changes.  Should probably just clear the selection.

            return true;
        }

        private boolean fIsLocalDrop;
        private int[] fLocalIndices;
    }

    NetNameList(TraceDisplayModel displayModel, TraceDataModel dataModel) {
        fTraceDisplayModel = displayModel;
        fTraceDataModel = dataModel;
        displayModel.addListener(this);
        setModel(new ListModelAdapter());
        setCellRenderer(new NetNameRenderer());
        computeBounds();
        setFixedCellHeight(DrawMetrics.WAVEFORM_V_SPACING);
        setDragEnabled(true);
        setDropMode(DropMode.ON_OR_INSERT);
        setTransferHandler(new NetTransferHandler());
        setBackground(AppPreferences.getInstance().backgroundColor);

        fPopupMenu = new JPopupMenu();
        JMenuItem item = new JMenuItem("Remove Net");
        item.addActionListener(this);
        fPopupMenu.add(item);
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
        fPopupMenu.add(item);
    }

    @Override
    protected void processMouseEvent(MouseEvent event) {
        if (event.isPopupTrigger())
            fPopupMenu.show(event.getComponent(), event.getX(), event.getY());
        else
            super.processMouseEvent(event);
    }

    private void computeBounds() {
        Dimension d = getPreferredSize();
        d.width = 200;
        d.height = fTraceDisplayModel.getVisibleNetCount() * DrawMetrics.WAVEFORM_V_SPACING;
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
    public void scaleChanged(double newScale) {}

    @Override
    public void netsAdded(int firstIndex, int lastIndex) {
        computeBounds();
    }

    @Override
    public void netsRemoved(int firstIndex, int lastIndex) {
        computeBounds();
    }

    @Override
    public void markerChanged(long timestamp) {}

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

        if (e.getActionCommand().equals("Remove Net")) {
            for (int i = indices.length - 1; i >= 0; i--)
                fTraceDisplayModel.removeNet(indices[i]);

            clearSelection();
        } else {
            if (e.getActionCommand().equals("Enum")) {
                JFileChooser chooser = new JFileChooser(AppPreferences.getInstance().getInitialEnumDirectory());
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setMultiSelectionEnabled(false);
                int returnValue = chooser.showOpenDialog(this);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    AppPreferences.getInstance().setInitialEnumDirectory(
                        chooser.getSelectedFile().getParentFile());
                    try {
                        EnumValueFormatter formatter = new EnumValueFormatter();
                        formatter.loadFromFile(chooser.getSelectedFile());
                        fTraceDisplayModel.setValueFormatter(indices[0], formatter);
                    } catch (Exception exc) {
                        JOptionPane.showMessageDialog(this, "Error opening enum mapping file");
                    }
                }
            } else {
                ValueFormatter formatter = null;
                if (e.getActionCommand().equals("Hex"))
                    formatter = new HexadecimalValueFormatter();
                else if (e.getActionCommand().equals("Binary"))
                    formatter = new BinaryValueFormatter();
                else if (e.getActionCommand().equals("Decimal"))
                    formatter = new DecimalValueFormatter();
                else if (e.getActionCommand().equals("ASCII"))
                    formatter = new ASCIIValueFormatter();

                if (formatter != null) {
                    for (int i = 0; i < indices.length; i++)
                        fTraceDisplayModel.setValueFormatter(indices[i], formatter);
                }
            }
        }
    }

    private TraceDisplayModel fTraceDisplayModel;
    private TraceDataModel fTraceDataModel;
    private JPopupMenu fPopupMenu;
}

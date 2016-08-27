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
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.event.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;

///
/// Container for the timescale view, net names, and waveforms.
///
public class TraceView extends JPanel implements ActionListener {
    public TraceView(TraceViewModel viewModel, TraceDataModel dataModel,
    WaveApp waveApp) {
        super(new BorderLayout());

        fWaveApp = waveApp;
        fTraceViewModel = viewModel;
        fTraceDataModel = dataModel;
        fWaveformView = new WaveformView(viewModel, dataModel);
        fTimescaleView = new TimescaleView(viewModel, dataModel);
        fScrollPane = new JScrollPane(fWaveformView);
        fScrollPane.setColumnHeaderView(fTimescaleView);
        fScrollPane.setViewportBorder(BorderFactory.createLineBorder(Color.black));

        // Always keep the horizontal scroll bar. In 99% of cases, we need it,
        // and it simplifies the net name layout if we don't need to worry about
        // the wave view changing size.
        fScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            fNetNameView = new NetNameView(viewModel, dataModel);

        JViewport netNameViewport = new JViewport();
        netNameViewport.setView(fNetNameView);
        JPanel netNameBorder = new JPanel(new BorderLayout());
        netNameBorder.setBorder(BorderFactory.createLineBorder(Color.black));
        netNameBorder.add(netNameViewport, BorderLayout.CENTER);

        // Need to make sure the net name view is the same size as the waveform view
        // so scrolling will work correctly (otherwise they will be out of sync).
        JPanel netNameContainer = new JPanel(new BorderLayout());
        netNameContainer.add(Box.createVerticalStrut(fTimescaleView.getPreferredSize().height),
            BorderLayout.NORTH);
        netNameContainer.add(netNameBorder, BorderLayout.CENTER);
        netNameContainer.add(Box.createVerticalStrut(fScrollPane.getHorizontalScrollBar()
            .getPreferredSize().height), BorderLayout.SOUTH);

        // To allow resizing the net name view,  put it in the left half of a split pane
        // rather than setting it as the scroll pane's row header. Add a listener for
        // the vertical scrollbar that also controls the net name view.
        fSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, netNameContainer, fScrollPane);
        add(fSplitPane, BorderLayout.CENTER);
        fScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent ae) {
                netNameViewport.setViewPosition(new Point(0, ae.getValue()));
            }
        });

        fScrollPane.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent ae) {
                // Need to repaint when scrolling because values on partially visible
                // nets will be centered.
                fWaveformView.repaint();
            }
        });

        // Add net context menu
        // @todo should this be moved to NetNameView?
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
        subItem = new JMenuItem("Custom Formatter...");
        subItem.addActionListener(this);
        item.add(subItem);
        fPopupMenu.add(item);

        fNetNameView.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent event) {
                if (event.isPopupTrigger())
                    fPopupMenu.show(event.getComponent(), event.getX(), event.getY());
            }
        });

        fTraceViewModel.setHorizontalScale(10.0);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int[] indices = fNetNameView.getSelectedIndices();

        if (e.getActionCommand().equals("Remove Net")) {
            for (int i = indices.length - 1; i >= 0; i--)
                fTraceViewModel.removeNet(indices[i]);

            fNetNameView.clearSelection();
        } else {
            if (e.getActionCommand().equals("Enum")) {
                if (indices.length > 0) {
                    ValueFormatter formatter = fTraceViewModel.getValueFormatter(indices[0]);
                    if (!(formatter instanceof EnumValueFormatter)) {
                        formatter = new EnumValueFormatter();
                        fTraceViewModel.setValueFormatter(indices[0], formatter);
                    }

                    JFrame frame = new JFrame("Mapping for " + fTraceDataModel.getShortNetName(fTraceViewModel.getVisibleNet(indices[0])));
                    JPanel contentPane = new MappingView((EnumValueFormatter) formatter);
                    contentPane.setOpaque(true);
                    frame.setContentPane(contentPane);
                    frame.pack();
                    frame.setVisible(true);
                }
            } else if (e.getActionCommand().equals("Custom Formatter...")) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setMultiSelectionEnabled(false);
                int returnValue = chooser.showOpenDialog(this);

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    try {
//                         String jarFileName = chooser.getSelectedFile().getCanonicalPath();
//                         URL[] urls = { (new File(jarFileName)).toURL() };
//                         URLClassLoader loader = new URLClassLoader(urls);
//                         Class c = loader.loadClass("CustomFormatter");
//                         ValueFormatter formatter = (ValueFormatter) c.newInstance();
//                         for (int i = 0; i < indices.length; i++)
//                         {
//                             NetModel net = fTraceViewModel.getVisibleNet(indices[i]);
//                             net.setCustomValueFormatterPath(jarFileName);
//                             net.setValueFormatter(formatter);
//                         }
                    } catch (Exception exc) {
                        JOptionPane.showMessageDialog(this, "Error opening configuration file");
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
                        fTraceViewModel.setValueFormatter(indices[i], formatter);
                }
            }
        }

        fWaveformView.repaint();
        fNetNameView.repaint();
    }

    public void zoomIn() {
        setScaleKeepCentered(fTraceViewModel.getHorizontalScale() / 1.25);
    }

    public void zoomOut() {
        setScaleKeepCentered(fTraceViewModel.getHorizontalScale() * 1.25);
    }

    public void setScaleKeepCentered(double newScale) {
        Rectangle oldVisibleRect = fWaveformView.getVisibleRect();
        long centerTimestamp = (long)((oldVisibleRect.x + oldVisibleRect.width / 2) * fTraceViewModel.getHorizontalScale());

        fTraceViewModel.setHorizontalScale(newScale);

        // Scroll to new center timestamp
        int centerX = (int)(centerTimestamp / newScale);
        Rectangle newVisibleRect = fWaveformView.getVisibleRect();
        newVisibleRect.x = centerX - newVisibleRect.width / 2;

        fWaveformView.scrollRectToVisible(newVisibleRect);
    }

    public void zoomToSelection() {
        // Determine what the new size of the selection window should be.
        long selectionStartTimestamp = fTraceViewModel.getSelectionStart();
        long cursorPositionTimestamp = fTraceViewModel.getCursorPosition();
        if (selectionStartTimestamp == cursorPositionTimestamp)
            return;    // Will do bad things otherwise

        long lowTimestamp = Math.min(selectionStartTimestamp, cursorPositionTimestamp);
        long highTimestamp = Math.max(selectionStartTimestamp, cursorPositionTimestamp);
        int left = (int)(lowTimestamp / fTraceViewModel.getHorizontalScale());
        int right = (int)(highTimestamp / fTraceViewModel.getHorizontalScale());

        int newSize = right - left;
        int currentSize = fWaveformView.getVisibleRect().width;

        double newScale = ((double) newSize / currentSize) * fTraceViewModel.getHorizontalScale();

        fTraceViewModel.setHorizontalScale(newScale);

        Rectangle newRect = fWaveformView.getVisibleRect();
        newRect.x = (int)(lowTimestamp / fTraceViewModel.getHorizontalScale());
        newRect.width = (int)(highTimestamp / fTraceViewModel.getHorizontalScale()) - newRect.x;
        fWaveformView.scrollRectToVisible(newRect);
    }

    public int[] getSelectedNets() {
        return fNetNameView.getSelectedIndices();
    }

    private JSplitPane fSplitPane;
    private WaveformView fWaveformView;
    private NetNameView fNetNameView;
    private JScrollPane fScrollPane;
    private TimescaleView fTimescaleView;
    private JPopupMenu fPopupMenu;
    private TraceViewModel fTraceViewModel;
    private TraceDataModel fTraceDataModel;
    private WaveApp fWaveApp;
}

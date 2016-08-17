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

///
/// Displays searchable lists of all nets in a design. Can be dragged onto
/// the visible net view to see them.
///
public class NetSearchView extends JPanel implements ActionListener
{
    class NetTreeTransferHandler extends TransferHandler
    {
        public int getSourceActions(JComponent component)
        {
            return MOVE;
        }

        public void buildNetListRecursive(Object node, StringBuffer indexList)
        {
             if (fTree.getModel().isLeaf(node))
             {
                 indexList.append(fTraceDataModel.getFullNetName(fTraceDataModel.getNetFromTreeObject(node)));
                 indexList.append('\n');
                 return;
             }

             for (int i = 0; i < fTree.getModel().getChildCount(node); i++)
                 buildNetListRecursive(fTree.getModel().getChild(node, i), indexList);
        }

        public Transferable createTransferable(JComponent component)
        {
            StringBuffer indexList = new StringBuffer();
            for (TreePath selectedPath : fTree.getSelectionPaths())
            {
                buildNetListRecursive(selectedPath.getLastPathComponent(),
                    indexList);
            }

            return new StringSelection(indexList.toString());
        }

        public void exportDone(JComponent component, Transferable transfer, int action)
        {
            // XXX do nothing
        }

        public boolean canImport(TransferHandler.TransferSupport support)
        {
            return false;
        }

        public boolean importData(TransferHandler.TransferSupport support)
        {
            return false;
        }
    }

    class ListModelAdapter implements ListModel, TraceViewModelListener, DocumentListener
    {
        public ListModelAdapter(TraceViewModel model)
        {
            fTraceViewModel = model;
            model.addListener(this);
            setPattern("");
        }

        public void setPattern(String pattern)
        {
            if (pattern.equals(""))
            {
                fMatches.clear();
                for (int index = 0; index < fTraceDataModel.getTotalNetCount(); index++)
                    fMatches.add(fTraceDataModel.getFullNetName(index));
            }
            else
            {
                fMatches.clear();
                for (int index = 0; index < fTraceDataModel.getTotalNetCount(); index++)
                {
                    String name = fTraceDataModel.getFullNetName(index);
                    if (name.indexOf(pattern) != -1)
                        fMatches.add(name);
                }
            }

            if (fListener != null)
                fListener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, fMatches.size()));
        }

        private void filter(Document doc)
        {
            try
            {
                setPattern(doc.getText(0, doc.getEndPosition().getOffset()).trim());
            }
            catch (Exception exc)
            {
                System.out.println("caught exception " + exc);
            }
        }

        public void insertUpdate(DocumentEvent event)
        {
            filter(event.getDocument());
        }

        public void removeUpdate(DocumentEvent event)
        {
            filter(event.getDocument());
        }

        public void changedUpdate(DocumentEvent ev)
        {
        }

        public void addListDataListener(ListDataListener l)
        {
            fListener = l;
        }

        public Object getElementAt(int index)
        {
            return fMatches.elementAt(index);
        }

        public int getSize()
        {
            return fMatches.size();
        }

        public void removeListDataListener(ListDataListener l)
        {
            fListener = null;
        }

        public void cursorChanged(long oldTimestamp, long newTimestamp)
        {
        }

        public void netsAdded(int firstIndex, int lastIndex)
        {
            fListener.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, firstIndex, lastIndex));
        }

        public void netsRemoved(int firstIndex, int lastIndex)
        {
            fListener.intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, firstIndex, lastIndex));
        }

        public void markerChanged(long timestamp)
        {
        }

        public void scaleChanged(double newScale)
        {
        }

        private ListDataListener fListener;
        private TraceViewModel fTraceViewModel;
        private Vector<String> fMatches = new Vector<String>();
    }

    public NetSearchView(TraceViewModel viewModel, TraceDataModel dataModel)
    {
        super(new BorderLayout());
        setPreferredSize(new Dimension(275, 500));

        fTraceViewModel = viewModel;
        fTraceDataModel = dataModel;

        JTabbedPane tabView = new JTabbedPane();
        add(tabView);

        // Set up net hieararchy view
        JPanel treeTab = new JPanel();
        tabView.addTab("Tree", null, treeTab, "tree view");
        treeTab.setLayout(new BorderLayout());
        fTree = new JTree(fTraceDataModel.getNetTree());
        fTree.setDragEnabled(true);
        fTree.setTransferHandler(new NetTreeTransferHandler());
        JScrollPane scroller = new JScrollPane(fTree);
        treeTab.add(scroller, BorderLayout.CENTER);

        // Set up net search view
        JPanel searchTab = new JPanel();
        tabView.addTab("Search", null, searchTab, "search");
        searchTab.setLayout(new BorderLayout());
        JTextField searchField = new JTextField();
        searchTab.add(searchField, BorderLayout.NORTH);
        ListModelAdapter adapter = new ListModelAdapter(fTraceViewModel);
        JList netList = new JList(adapter);
        searchField.getDocument().addDocumentListener(adapter);
        netList.setDragEnabled(true);
        JScrollPane listScroller = new JScrollPane(netList);
        searchTab.add(listScroller, BorderLayout.CENTER);
        listScroller.add(new JTextArea());
    }

    public void actionPerformed(ActionEvent e)
    {
    }

    private JTree fTree;
    private TraceViewModel fTraceViewModel;
    private TraceDataModel fTraceDataModel;
}

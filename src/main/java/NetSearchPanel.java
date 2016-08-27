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
public class NetSearchPanel extends JPanel implements ActionListener {

    /// Allow user to drag signals out of this view and drop
    /// in currently displayed nets
    class NetTreeTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent component) {
            return MOVE;
        }

        private void buildNetListRecursive(Object node, StringBuffer indexList) {
            if (fTree.getModel().isLeaf(node)) {
                indexList.append(fTraceDataModel.getFullNetName(fTraceDataModel.getNetFromTreeObject(node)));
                indexList.append('\n');
                return;
            }

            for (int i = 0; i < fTree.getModel().getChildCount(node); i++)
                buildNetListRecursive(fTree.getModel().getChild(node, i), indexList);
        }

        @Override
        public Transferable createTransferable(JComponent component) {
            StringBuffer indexList = new StringBuffer();
            for (TreePath selectedPath : fTree.getSelectionPaths()) {
                buildNetListRecursive(selectedPath.getLastPathComponent(),
                                      indexList);
            }

            return new StringSelection(indexList.toString());
        }

        @Override
        public void exportDone(JComponent component, Transferable transfer, int action) {
            // XXX do nothing
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            return false;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            return false;
        }
    }

    public NetSearchPanel(TraceDisplayModel displayModel, TraceDataModel dataModel) {
        super(new BorderLayout());
        setPreferredSize(new Dimension(275, 500));

        fTraceDisplayModel = displayModel;
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
        NetSearchListModelAdapter adapter = new NetSearchListModelAdapter(
            fTraceDataModel);
        JList<String> netList = new JList<String>(adapter);
        searchField.getDocument().addDocumentListener(adapter);
        netList.setDragEnabled(true);
        JScrollPane listScroller = new JScrollPane(netList);
        searchTab.add(listScroller, BorderLayout.CENTER);
        listScroller.add(new JTextArea());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    }

    private JTree fTree;
    private TraceDisplayModel fTraceDisplayModel;
    private TraceDataModel fTraceDataModel;
}

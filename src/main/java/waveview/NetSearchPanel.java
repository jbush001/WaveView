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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

///
/// Displays searchable lists of all nets in a design. Can be dragged onto
/// the visible net view to see them.
///
class NetSearchPanel extends JPanel {
    private final JTree tree;
    private final TraceDataModel traceDataModel;
    private final ImageIcon netIcon;
    private final ImageIcon moduleIcon;

    /// Allow user to drag signals out of this view and drop in currently
    /// displayed nets
    class NetTreeTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent component) {
            return MOVE;
        }

        private void buildNetListRecursive(Object node, StringBuilder indexList) {
            if (tree.getModel().isLeaf(node)) {
                indexList.append(traceDataModel.getNetFromTreeObject(node).getFullName());
                indexList.append('\n');
                return;
            }

            for (int i = 0; i < tree.getModel().getChildCount(node); i++) {
                buildNetListRecursive(tree.getModel().getChild(node, i), indexList);
            }
        }

        @Override
        public Transferable createTransferable(JComponent component) {
            StringBuilder indexList = new StringBuilder();
            for (TreePath selectedPath : tree.getSelectionPaths()) {
                buildNetListRecursive(selectedPath.getLastPathComponent(), indexList);
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

    NetSearchPanel(TraceDataModel traceDataModel) {
        super(new BorderLayout());
        setPreferredSize(new Dimension(275, 500));

        this.traceDataModel = traceDataModel;

        JTabbedPane tabView = new JTabbedPane();
        add(tabView);

        // Set up net hieararchy view
        JPanel treeTab = new JPanel();
        tabView.addTab("Tree", null, treeTab, "tree view");
        treeTab.setLayout(new BorderLayout());
        tree = new JTree(traceDataModel.getNetTree());
        tree.setCellRenderer(new NetTreeCellRenderer());
        tree.setDragEnabled(true);
        tree.setTransferHandler(new NetTreeTransferHandler());
        JScrollPane scroller = new JScrollPane(tree);
        treeTab.add(scroller, BorderLayout.CENTER);

        netIcon = loadResourceIcon("tree-net.png");
        moduleIcon = loadResourceIcon("tree-module.png");

        // Set up net search view
        JPanel searchTab = new JPanel();
        tabView.addTab("Search", null, searchTab, "search");
        searchTab.setLayout(new BorderLayout());
        JTextField searchField = new JTextField();
        searchTab.add(searchField, BorderLayout.NORTH);
        NetSearchListModelAdapter adapter = new NetSearchListModelAdapter(traceDataModel);
        JList<String> netList = new JList<>(adapter);
        searchField.getDocument().addDocumentListener(adapter);
        netList.setDragEnabled(true);
        JScrollPane listScroller = new JScrollPane(netList);
        searchTab.add(listScroller, BorderLayout.CENTER);
        listScroller.add(new JTextArea());
    }

    class NetTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {

            NetTreeModel.Node node = (NetTreeModel.Node) value;
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (node.isLeaf())
                setIcon(netIcon);
            else
                setIcon(moduleIcon);

            return this;
        }
    }

    private ImageIcon loadResourceIcon(String name) {
        return new ImageIcon(this.getClass().getClassLoader().getResource(name));
    }
}

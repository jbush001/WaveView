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
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import waveview.wavedata.NetTreeNode;
import waveview.wavedata.WaveformDataModel;

///
/// Displays searchable lists of all nets in a design. Can be dragged onto
/// the visible net view to see them.
///
final class NetSearchView extends JPanel {
    private JTree tree;
    private final WaveformDataModel waveformDataModel;
    private final ImageIcon netIcon;
    private final ImageIcon moduleIcon;

    /// Allow user to drag signals out of this view and drop in currently
    /// displayed nets
    class NetTreeTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent component) {
            return MOVE;
        }

        private void buildNetListRecursive(NetTreeNode node, StringBuilder indexList) {
            TreeModel model = tree.getModel();
            if (model.isLeaf(node)) {
                indexList.append(node.getNetDataModel().getFullName());
                indexList.append('\n');
                return;
            }

            for (int i = 0; i < model.getChildCount(node); i++) {
                buildNetListRecursive((NetTreeNode) model.getChild(node, i), indexList);
            }
        }

        @Override
        public Transferable createTransferable(JComponent component) {
            StringBuilder indexList = new StringBuilder();
            for (TreePath selectedPath : tree.getSelectionPaths()) {
                buildNetListRecursive((NetTreeNode) selectedPath.getLastPathComponent(), indexList);
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

    NetSearchView(WaveformDataModel waveformDataModel) {
        super(new BorderLayout());
        setPreferredSize(new Dimension(275, 500));

        this.waveformDataModel = waveformDataModel;

        JTabbedPane tabView = new JTabbedPane();
        add(tabView);
        JPanel treeTab = createTreeTab();
        tabView.addTab("Tree", null, treeTab, "tree view");
        JPanel searchTab = createSearchTab();
        tabView.addTab("Search", null, searchTab, "search");

        netIcon = loadResourceIcon("tree-net.png");
        moduleIcon = loadResourceIcon("tree-module.png");
    }

    private static class TreeModelWrapper implements TreeModel {
        private final NetTreeNode root;

        TreeModelWrapper(NetTreeNode root) {
            this.root = root;
        }

        // Tree model methods. Listeners are unimplemented because the tree is
        // immutable.
        @Override
        public void addTreeModelListener(TreeModelListener l) {}

        @Override
        public void removeTreeModelListener(TreeModelListener l) {}

        @Override
        public Object getChild(Object parent, int index) {
            return ((NetTreeNode) parent).getChild(index);
        }

        @Override
        public int getChildCount(Object parent) {
            return ((NetTreeNode) parent).getChildCount();
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            return ((NetTreeNode) parent).getIndexOfChild((NetTreeNode) child);
        }

        @Override
        public Object getRoot() {
            return root;
        }

        @Override
        public boolean isLeaf(Object node) {
            return ((NetTreeNode) node).isLeaf();
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
            throw new UnsupportedOperationException();
        }
    }

    JPanel createTreeTab() {
        tree = new JTree(new TreeModelWrapper(waveformDataModel.getNetTree()));

        JPanel treeTab = new JPanel();
        treeTab.setLayout(new BorderLayout());
        tree.setCellRenderer(new NetTreeCellRenderer());
        tree.setDragEnabled(true);
        tree.setTransferHandler(new NetTreeTransferHandler());
        JScrollPane scroller = new JScrollPane(tree);
        treeTab.add(scroller, BorderLayout.CENTER);
        return treeTab;
    }

    JPanel createSearchTab() {
        JPanel searchTab = new JPanel();
        searchTab.setLayout(new BorderLayout());
        JTextField searchField = new JTextField();
        searchTab.add(searchField, BorderLayout.NORTH);
        NetSearchListModelAdapter adapter = new NetSearchListModelAdapter(waveformDataModel);
        JList<String> netList = new JList<>(adapter);
        searchField.getDocument().addDocumentListener(adapter);
        netList.setDragEnabled(true);
        JScrollPane listScroller = new JScrollPane(netList);
        searchTab.add(listScroller, BorderLayout.CENTER);
        listScroller.add(new JTextArea());
        return searchTab;
    }

    private class NetTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (((NetTreeNode) value).isLeaf()) {
                setIcon(netIcon);
            } else {
                setIcon(moduleIcon);
            }

            return this;
        }
    }

    private ImageIcon loadResourceIcon(String name) {
        return new ImageIcon(this.getClass().getClassLoader().getResource(name));
    }
}

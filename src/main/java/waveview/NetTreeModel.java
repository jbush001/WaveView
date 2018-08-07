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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

///
/// Maintains module/net hieararchy, where leaf nodes are nets and interior nodes
/// are modules.
///
public final class NetTreeModel implements TreeModel {
    private Node root;

    public class Builder {
        private final Deque<Node> nodeStack = new ArrayDeque<>();

        public void enterScope(String name) {
            if (nodeStack.isEmpty() && root != null) {
                // If you call $dumpvars more than once with iverilog, it will pop the root
                // node off and re-push it. Handle this case here.
                nodeStack.addLast(root);
                return;
            }

            Node node = new Node(name);
            if (root == null) {
                root = node;
            } else {
                nodeStack.peekLast().children.add(node);
            }

            nodeStack.addLast(node);
        }

        public void leaveScope() {
            nodeStack.removeLast();
        }

        public void addNet(NetDataModel netDataModel) {
            nodeStack.peekLast().children.add(new Node(netDataModel));
        }
    }

    static class Node {
        private final String name;
        private final NetDataModel netDataModel;
        private final List<Node> children = new ArrayList<>();

        // Interior nodes (modules/interfaces) only
        Node(String name) {
            this.name = name;
            netDataModel = null;
        }

        // Leaf nodes (nets) only
        Node(NetDataModel netDataModel) {
            name = netDataModel.getShortName();
            this.netDataModel = netDataModel;
        }

        @Override
        public String toString() {
            return name;
        }

        boolean isLeaf() {
            return children.isEmpty();
        }
    }

    public Builder startBuilding() {
        return new Builder();
    }

    public void clear() {
        root = null;
    }

    public NetDataModel getNetFromTreeObject(Object o) {
        return ((Node) o).netDataModel;
    }

    // Tree model methods. Listeners are unimplemented because the tree is
    // immutable.
    @Override
    public void addTreeModelListener(TreeModelListener l) {
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
    }

    @Override
    public Object getChild(Object parent, int index) {
        return ((Node) parent).children.get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        Node n = (Node) parent;
        if (n.isLeaf()) {
            return 0;
        } else {
            return n.children.size();
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return ((Node) parent).children.indexOf(child);
    }

    @Override
    public Object getRoot() {
        return root;
    }

    @Override
    public boolean isLeaf(Object node) {
        return ((Node) node).isLeaf();
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        throw new UnsupportedOperationException();
    }
}

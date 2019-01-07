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

package waveview.wavedata;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

///
/// Maintains module/net hieararchy, where leaf nodes are nets and interior nodes
/// are modules.
///
public final class NetTreeModel {
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

    public static class Node {
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

        public boolean isLeaf() {
            return children.isEmpty();
        }
    }

    public Builder startBuilding() {
        return new Builder();
    }

    public void clear() {
        root = null;
    }

    public NetDataModel getNetFromTreeObject(Node o) {
        return o.netDataModel;
    }

    public Node getChild(Node parent, int index) {
        return parent.children.get(index);
    }

    public int getChildCount(Node parent) {
        if (parent.isLeaf()) {
            return 0;
        } else {
            return parent.children.size();
        }
    }

    public int getIndexOfChild(Node parent, Node child) {
        return parent.children.indexOf(child);
    }

    public Node getRoot() {
        return root;
    }

    public boolean isLeaf(Object node) {
        return ((Node) node).isLeaf();
    }
}

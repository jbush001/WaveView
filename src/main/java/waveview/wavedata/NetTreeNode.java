
//
// Copyright 2011-2019 Jeff Bush
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

/// This is used to represent the hierarchy of modules and nets.
public final class NetTreeNode {
    private final String name;
    private final NetDataModel netDataModel;
    private final List<NetTreeNode> children = new ArrayList<>();

    // Builds a tree of NetTreeNodes
    public static final class Builder {
        private final Deque<NetTreeNode> nodeStack = new ArrayDeque<>();
        private NetTreeNode root;

        public void enterScope(String name) {
            if (nodeStack.isEmpty() && root != null) {
                // If you call $dumpvars more than once with iverilog, it will
                // pop the root node off and re-push it. Handle this case here.
                nodeStack.addLast(root);
                return;
            }

            NetTreeNode node = new NetTreeNode(name);
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
            nodeStack.peekLast().children.add(new NetTreeNode(netDataModel));
        }

        public NetTreeNode getRoot() {
            return root;
        }
    }

    // Interior nodes (modules/interfaces) only
    private NetTreeNode(String name) {
        this.name = name;
        netDataModel = null;
    }

    // Leaf nodes (nets) only
    private NetTreeNode(NetDataModel netDataModel) {
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

    public NetTreeNode getChild(int index) {
        return children.get(index);
    }

    public int getChildCount() {
        if (isLeaf()) {
            return 0;
        } else {
            return children.size();
        }
    }

    public int getIndexOfChild(NetTreeNode child) {
        return children.indexOf(child);
    }

    public NetDataModel getNetDataModel() {
        return netDataModel;
    }
}

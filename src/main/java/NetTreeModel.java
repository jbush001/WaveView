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
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import javax.swing.tree.*;

///
/// Maintains module/net hieararchy, where leaf nodes are nets and interior nodes
/// are modules.
///
public class NetTreeModel implements TreeModel {
    public NetTreeModel() {
    }

    public void clear() {
        fRoot = null;
    }

    public void enterScope(String name) {
        if (fNodeStack.empty() && fRoot != null) {
            // If you call $dumpvars more than once with iverilog, it will pop the root
            // node off and re-push it.  Handle this case here.
            fNodeStack.push(fRoot);
            return;
        }

        NetTreeNode node = new NetTreeNode(name);
        if (fRoot == null)
            fRoot = node;
        else
            fNodeStack.peek().fChildren.add(node);

        fNodeStack.push(node);
    }

    public void leaveScope() {
        fNodeStack.pop();
    }

    public void addNet(String name, int netId) {
        fNodeStack.peek().fChildren.add(new NetTreeNode(name, netId));
    }

    public int getNetFromTreeObject(Object o) {
        return ((NetTreeNode)o).fNet;
    }

    // Tree model methods
    @Override
    public void addTreeModelListener(TreeModelListener l) {
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
    }

    @Override
    public Object getChild(Object parent, int index) {
        return ((NetTreeNode) parent).fChildren.elementAt(index);
    }

    @Override
    public int getChildCount(Object parent) {
        return ((NetTreeNode) parent).fChildren.size();
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return ((NetTreeNode) parent).fChildren.indexOf(child);
    }

    @Override
    public Object getRoot() {
        return fRoot;
    }

    @Override
    public boolean isLeaf(Object node) {
        return ((NetTreeNode) node).fNet != -1;
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // XXX not implemented
    }

    private class NetTreeNode {
        // Interior nodes only
        NetTreeNode(String name) {
            fName = name;
        }

        // Leaf nodes only
        NetTreeNode(String name, int net) {
            fName = name;
            fNet = net;
        }

        public String toString() {
            return fName;
        }

        private Vector<NetTreeNode> fChildren = new Vector<NetTreeNode>();
        private String fName;
        private int fNet = -1;
    };

    private NetTreeNode fRoot;
    private Stack<NetTreeNode> fNodeStack = new Stack<NetTreeNode>();
}
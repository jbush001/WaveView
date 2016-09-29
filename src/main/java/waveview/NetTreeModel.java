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
import java.util.*;
import javax.swing.tree.*;

///
/// Maintains module/net hieararchy, where leaf nodes are nets and interior nodes
/// are modules.
///
public class NetTreeModel implements TreeModel {
    public NetTreeModel() {}

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

        Node node = new Node(name);
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
        fNodeStack.peek().fChildren.add(new Node(name, netId));
    }

    public int getNetFromTreeObject(Object o) {
        return ((Node)o).fNet;
    }

    // Tree model methods. Listeners are unimplemented because the tree is
    // immutable.
    @Override
    public void addTreeModelListener(TreeModelListener l) {}

    @Override
    public void removeTreeModelListener(TreeModelListener l) {}

    @Override
    public Object getChild(Object parent, int index) {
        return ((Node) parent).fChildren.get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        Node n = (Node) parent;
        if (n.isLeaf())
            return 0;
        else
            return n.fChildren.size();
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return ((Node) parent).fChildren.indexOf(child);
    }

    @Override
    public Object getRoot() {
        return fRoot;
    }

    @Override
    public boolean isLeaf(Object node) {
        return ((Node) node).isLeaf();
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        throw new UnsupportedOperationException();
    }

    static class Node {
        // Interior nodes only
        Node(String name) {
            fName = name;
            fChildren = new ArrayList<Node>();
        }

        // Leaf nodes only
        Node(String name, int net) {
            fName = name;
            fNet = net;
        }

        @Override
        public String toString() {
            return fName;
        }

        boolean isLeaf() {
            return fChildren == null;
        }

        private ArrayList<Node> fChildren;
        private String fName;
        private int fNet = -1;
    };

    private Node fRoot;
    private Stack<Node> fNodeStack = new Stack<Node>();
}

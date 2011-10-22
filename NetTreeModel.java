import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import javax.swing.tree.*;

///
/// Maintains module/net hieararchy, where leaf nodes are nets and interior nodes
/// are modules.
///
public class NetTreeModel implements TreeModel
{
	public NetTreeModel()
	{
	}

	public void clear()
	{
		fRoot = null;
	}

	public void enterScope(String name)
	{
		if (fNodeStack.empty() && fRoot != null)
		{
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
	
	public void leaveScope()
	{
		fNodeStack.pop();
	}
	
	public void addNet(String name, int netId)
	{
		fNodeStack.peek().fChildren.add(new NetTreeNode(name, netId));
	}

	public int getNetFromTreeObject(Object o)
	{
		return ((NetTreeNode)o).fNet;
	}

	// Tree model methods
	public void addTreeModelListener(TreeModelListener l)
	{
	}
	
	public Object getChild(Object parent, int index)
	{
		return ((NetTreeNode) parent).fChildren.elementAt(index);
	}
	
	public int getChildCount(Object parent)
	{
		return ((NetTreeNode) parent).fChildren.size();
	}
	
	public int getIndexOfChild(Object parent, Object child)
	{
		return ((NetTreeNode) parent).fChildren.indexOf(child);
	}
	
	public Object getRoot()
	{
		return fRoot;
	}
	
	public boolean isLeaf(Object node)
	{
		return ((NetTreeNode) node).fNet != -1;
	}
	
	public void removeTreeModelListener(TreeModelListener l)
	{
	}
	
	public void valueForPathChanged(TreePath path, Object newValue) 	
	{
		// XXX not implemented
	}

	private class NetTreeNode
	{
		// Interior nodes only
		NetTreeNode(String name)
		{
			fName = name;
		}
		
		// Leaf nodes only
		NetTreeNode(String name, int net)
		{
			fName = name;
			fNet = net;
		}
	
		public String toString()
		{
			return fName;
		}
	
		Vector<NetTreeNode> fChildren = new Vector<NetTreeNode>();
		String fName;
		int fNet = -1;
	};

	private NetTreeNode fRoot;
	private Stack<NetTreeNode> fNodeStack = new Stack<NetTreeNode>();
}
//
// This is the leftmost scroll pane that displays the names of the nets next to the waveforms
//

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.datatransfer.*;

class NetNameView extends JList implements TraceViewModelListener
{
	private static final int kCellHeight = 26;
	private static final int kDragThreshold = 15;

	class NetNameRenderer extends JPanel implements ListCellRenderer
	{
		private int fCurrentNet;
		private boolean fCurrentNetIsSelected;
		private TraceViewModel fTraceViewModel;	// XXX get this from the enclosing class
		private int fLabelBaseline = -1;
		private int fValueBaseline;
		private Font fLabelFont = new Font("SansSerif", Font.BOLD, 10);
		private Font fValueFont = new Font("SansSerif", Font.PLAIN, 8);
	
		NetNameRenderer(TraceViewModel viewModel, TraceDataModel dataModel)
		{
			fTraceViewModel = viewModel;
			fTraceDataModel = dataModel;
			setOpaque(true);
		}

		public Component getListCellRendererComponent(
			 JList list,
			 Object value,
			 int index,
			 boolean isSelected,
			 boolean cellHasFocus)
		{
			fCurrentNet = ((Integer) value).intValue();
			fCurrentNetIsSelected = isSelected;
			
			return this;
		}

		protected void paintComponent(Graphics g)
		{
			AppPreferences prefs = AppPreferences.getInstance();
			setBackground(prefs.kBackgroundColor);

			super.paintComponent(g);
			
			if (fCurrentNet == -1)
				return;

			if (fLabelBaseline == -1)
			{
				// Initialize this stuff once, then cache results
				FontMetrics labelMetrics = g.getFontMetrics(fLabelFont);
				FontMetrics valueMetrics = g.getFontMetrics(fValueFont);
				fLabelBaseline = labelMetrics.getAscent();
				fValueBaseline = fLabelBaseline + labelMetrics.getDescent() + labelMetrics.getLeading() 
					+ valueMetrics.getAscent();
				int totalHeight = fValueBaseline + valueMetrics.getDescent();
				int border = (kCellHeight - totalHeight) / 2;
				fLabelBaseline += border;
				fValueBaseline += border;
			}

			if (fCurrentNetIsSelected)
				g.setColor(prefs.kListSelectionBgColor);
			else
				g.setColor(prefs.kBackgroundColor);

			g.fillRect(0, 0, getWidth(), kCellHeight);
			g.setFont(fLabelFont);
			g.setColor(fCurrentNetIsSelected ? prefs.kListSelectionFgColor 
				: prefs.kTraceColor);

			int netId = fTraceViewModel.getVisibleNet(fCurrentNet);
			String name = fTraceDataModel.getShortNetName(netId);
			g.drawString(name, 1, fLabelBaseline);		

			g.setColor(fCurrentNetIsSelected ? prefs.kListSelectionFgColor 
				: prefs.kValueColor);
			g.setFont(fValueFont);
			
			Transition t = fTraceDataModel.findTransition(netId, 
				fTraceViewModel.getCursorPosition()).next();
			g.drawString(fTraceViewModel.getValueFormatter(fCurrentNet).format(t), 
				1, fValueBaseline);	
		}

		public String getToolTipText(MouseEvent event)
		{
			return fTraceDataModel.getFullNetName(fTraceViewModel.getVisibleNet(fCurrentNet));
		}
	}

	class ListModelAdapter implements ListModel, TraceViewModelListener
	{
		public ListModelAdapter(TraceViewModel model)
		{
			fTraceViewModel = model;
			model.addListener(this);
		}
	
		public void addListDataListener(ListDataListener l)
		{
			fListener = l;
		}
		
		public Object getElementAt(int index)
		{
			return new Integer(index);
		}
		
		public int getSize()
		{
			return fTraceViewModel.getVisibleNetCount();
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
	}

	class NetTransferHandler extends TransferHandler
	{
		public int getSourceActions(JComponent component)
		{
			return MOVE;
		}

		public Transferable createTransferable(JComponent component)
		{
			fLocalIndices = ((JList) component).getSelectedIndices();
			fIsLocalDrop = true;
			return new StringSelection("");
		}

		public void exportDone(JComponent component, Transferable transfer, int action)
		{
			fIsLocalDrop = false;
			// XXX do nothing
		}
		
		public boolean canImport(TransferHandler.TransferSupport support)
		{
			return true;
		}
		
		public boolean importData(TransferHandler.TransferSupport support)
		{
			String data;
			try
			{
				data = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
			}
			catch (Exception exc)
			{
				System.out.println(exc.toString());
				return false;
			}

			JList.DropLocation location = (JList.DropLocation) support.getDropLocation();
			int insertionPoint = location.getIndex();
			if (fIsLocalDrop)
			{
				fTraceViewModel.moveNets(fLocalIndices, insertionPoint);
			}
			else
			{
				// Drag from another window (for example, net tree)
				String[] values = data.split("\n");
				for (String value : values)
					fTraceViewModel.makeNetVisible(insertionPoint++, fTraceDataModel.findNet(value));
			}

			// @todo Deal with selection changes.  Should probably just clear the selection.

			return true;
		}
		
		boolean fIsLocalDrop = false;
		int[] fLocalIndices;
	}

	public NetNameView(TraceViewModel viewModel, TraceDataModel dataModel)
	{
		fTraceViewModel = viewModel;
		fTraceDataModel = dataModel;
		viewModel.addListener(this);
		setModel(new ListModelAdapter(viewModel));
		setCellRenderer(new NetNameRenderer(fTraceViewModel, fTraceDataModel));
		computeBounds();
		setFixedCellHeight(kCellHeight);
		setDragEnabled(true);
		setDropMode(DropMode.ON_OR_INSERT);
		setTransferHandler(new NetTransferHandler());
	}

	private void computeBounds()
	{
		Dimension d = getPreferredSize();
		d.width = 200;
		d.height = fTraceViewModel.getVisibleNetCount() * kCellHeight;
		setPreferredSize(d);
		validate();
		repaint();
	}
	
	protected void processKeyEvent(KeyEvent e)
	{
		/// @bug Eat key events so up/down arrows don't change selection
		/// If we allow arrows to change selection, the net name view scrolls without moving the other views.
		/// That really seems like a bug in the swing components.
	}

	public void scaleChanged(double newScale)
	{
	}

	public void netsAdded(int firstIndex, int lastIndex)
	{
		computeBounds();
	}

	public void netsRemoved(int firstIndex, int lastIndex)
	{
		computeBounds();
	}

	public void markerChanged(long timestamp)
	{
	}
	
	public void cursorChanged(long oldTimestamp, long newTimestamp)
	{
		repaint();
	}
	
	private TraceViewModel fTraceViewModel;
	private TraceDataModel fTraceDataModel;
}

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.datatransfer.*;
import javax.swing.text.*;
import javax.swing.table.*;

///
/// Displays a list of all markers and their timestamps.  The user can click on them
/// to jump to that point in the trace.
///
public class MarkerListView extends JPanel implements ActionListener, TraceViewModelListener
{
	public MarkerListView(TraceViewModel traceModel)
	{
		setLayout(new GridLayout(1, 1));
	
		fTraceViewModel = traceModel;
		fTraceViewModel.addListener(this);

		fTableModel = new MarkerTableModel(fTraceViewModel);
		fTable = new JTable(fTableModel);
		fTable.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2)
					select(e.isShiftDown());
			}
		});

		JScrollPane panel = new JScrollPane(fTable);
		add(panel);
	}

	public void select(boolean extendSelection)
	{
		long timestamp = fTraceViewModel.getTimestampForMarker(fTable.getSelectedRow());

		fTraceViewModel.setCursorPosition(timestamp);
		if (!extendSelection)
			fTraceViewModel.setSelectionStart(timestamp);
	}

	public void actionPerformed(ActionEvent e)
	{
	}

	public void cursorChanged(long oldTimestamp, long newTimestamp)
	{
	}
	
	public void netsAdded(int firstIndex, int lastIndex)
	{
	}
	
	public void netsRemoved(int firstIndex, int lastIndex)
	{
	}
	
	public void scaleChanged(double newScale)
	{
	}
	
	public void markerChanged(long timestamp)
	{
		fTableModel.fireTableDataChanged();
		// Redraw list
		repaint();
	}

	private TraceViewModel fTraceViewModel;
	private MarkerTableModel fTableModel;
	private JTable fTable;
}

class MarkerTableModel extends AbstractTableModel
{
	private static final int kNumColumns = 3;
	
	public MarkerTableModel(TraceViewModel model)
	{
		fTraceViewModel = model;
	}
	
	public int getColumnCount() 
	{ 
		return kNumColumns; 
	}
	
	public String getColumnName(int col)
	{
		return kColumnNames[col];
	}
	
	public int getRowCount() 
	{ 
		return fTraceViewModel.getMarkerCount(); 
	}

	public Object getValueAt(int row, int col) 
	{
		switch (col)
		{
			case 0:
				return "" + fTraceViewModel.getIdForMarker(row);
			case 1:
				return "" + fTraceViewModel.getTimestampForMarker(row);
			case 2:
				return "" + fTraceViewModel.getDescriptionForMarker(row);
		}

		return "";
	}
	
	private String kColumnNames[] = { "ID", "Timestamp", "Comment" };
	private TraceViewModel fTraceViewModel;
}


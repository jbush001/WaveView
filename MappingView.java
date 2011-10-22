import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.event.*;
import javax.swing.table.*;

///
/// This is a window that allows editing a table of number -> identifier
/// mappings.  Usually used for displaying human readable names to state machine
/// states.
///
class MappingView extends JPanel
{
	public MappingView(IdentifierValueFormatter formatter)
	{
		super(new BorderLayout());

		fTable = new JTable(new MappingTableModel(formatter));
		JScrollPane scroller = new JScrollPane(fTable);
		fTable.setFillsViewportHeight(true);
		add(scroller);
	}

	class MappingTableModel extends AbstractTableModel
	{
		private static final int kNumColumns = 2;
		
		public MappingTableModel(IdentifierValueFormatter formatter)
		{
			fFormatter = formatter;
		}
		
		public String getColumnName(int col) 
		{
			if (col == 0)
				return "value";
			else
				return "name";
		}

		public int getRowCount() 
		{ 
			return fFormatter.getMappingCount() + 1;	// Bottom row always adds
		}

		public int getColumnCount() 
		{ 
			return 2; 
		}

		public Object getValueAt(int row, int col) 
		{
			if (row >= fFormatter.getMappingCount())
				return "";
				
			if (col == 0)
				return fFormatter.getValueByIndex(row);
			else
				return fFormatter.getNameByIndex(row);
		}
		
		public boolean isCellEditable(int row, int col)
		{ 
			return true; 
		}
		
		public void setValueAt(Object value, int row, int col) 
		{
			if (row == fFormatter.getMappingCount())
				fFormatter.addMapping(0, "");	// Create a new row
		
			if (col == 0)
				fFormatter.setValueAtIndex(row, Integer.parseInt((String) value));
			else
				fFormatter.setNameAtIndex(row, (String) value);
		}

		IdentifierValueFormatter fFormatter;
	}

	JTable fTable;
}


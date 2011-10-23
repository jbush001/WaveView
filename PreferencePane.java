import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.event.*;
import javax.swing.text.*;

abstract class PreferencePane extends JPanel
{
	public PreferencePane(LayoutManager manager)
	{
		super(manager);
	}
	
	abstract public void loadSettings();
	abstract public void saveSettings();
}

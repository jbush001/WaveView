import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.event.*;
import javax.swing.text.*;

class ColorPreferencePane extends PreferencePane
{
	public ColorPreferencePane()
	{
		super(new FlowLayout());
		AppPreferences prefs = AppPreferences.getInstance();
		
		fTraceColorButton = new ColorButton("Trace", prefs.kTraceColor);
		add(fTraceColorButton);
		fConflictColorButton = new ColorButton("Conflict", prefs.kConflictColor);
		add(fConflictColorButton);
		fSelectionColorButton = new ColorButton("Selection", prefs.kSelectionColor);
		add(fSelectionColorButton);
		fCursorColorButton = new ColorButton("Cursor", prefs.kCursorColor);
		add(fCursorColorButton);
		fBackgroundColorButton = new ColorButton("Background", prefs.kBackgroundColor);
		add(fBackgroundColorButton);
		fTimingMarkerColorButton = new ColorButton("Timing Mark", prefs.kTimingMarkerColor);
		add(fTimingMarkerColorButton);
		fMarkerColorButton = new ColorButton("Marker", prefs.kMarkerColor);
		add(fMarkerColorButton);
		fListSelectionBgColorButton = new ColorButton("List Selection Background", 
			prefs.kListSelectionBgColor);
		add(fListSelectionBgColorButton);
		fListSelectionFgColorButton = new ColorButton("List Selection Foreground", 
			prefs.kListSelectionFgColor);
		add(fListSelectionFgColorButton);
		fValueColorButton = new ColorButton("Value", prefs.kValueColor);
		add(fValueColorButton);
		
		setPreferredSize(new Dimension(275, 500));
	}
	
	public void loadSettings()
	{
	}
	
	public void saveSettings()
	{
		AppPreferences prefs = AppPreferences.getInstance();
		prefs.kTraceColor = fTraceColorButton.getColor();
		prefs.kConflictColor = fConflictColorButton.getColor();
		prefs.kSelectionColor = fSelectionColorButton.getColor();
		prefs.kCursorColor = fCursorColorButton.getColor();
		prefs.kBackgroundColor = fBackgroundColorButton.getColor();
		prefs.kTimingMarkerColor = fTimingMarkerColorButton.getColor();
		prefs.kMarkerColor = fMarkerColorButton.getColor();
		prefs.kListSelectionBgColor = fListSelectionBgColorButton.getColor();
		prefs.kListSelectionFgColor = fListSelectionFgColorButton.getColor();
		prefs.kValueColor = fValueColorButton.getColor();
	}

	private ColorButton fTraceColorButton;
	private ColorButton fConflictColorButton;
	private ColorButton fSelectionColorButton;
	private ColorButton fCursorColorButton;
	private ColorButton fBackgroundColorButton;
	private ColorButton fTimingMarkerColorButton;
	private ColorButton fMarkerColorButton;
	private ColorButton fListSelectionBgColorButton;
	private ColorButton fListSelectionFgColorButton;
	private ColorButton fValueColorButton;
}

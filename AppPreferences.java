//
// Global application preferences
//

import java.util.prefs.*;
import java.util.*;
import java.awt.*;
import java.io.*;

class AppPreferences
{
	private static final int kMaxRecentFiles = 10;

	public static AppPreferences getInstance()
	{
		if (fInstance == null)
			fInstance = new AppPreferences();
	
		return fInstance;
	}

	public Color kTraceColor;
	public Color kConflictColor;
	public Color kSelectionColor;
	public Color kCursorColor;
	public Color kBackgroundColor;
	public Color kTimingMarkerColor;
	public Color kMarkerColor;
	public Color kListSelectionBgColor;
	public Color kListSelectionFgColor;
	public Color kValueColor;

	public void setInitialTraceDirectory(File file)
	{
		fPrefs.put("initialTraceDirectory", file.toString());
	}
	
	public File getInitialTraceDirectory()
	{
		return new File(fPrefs.get("initialTraceDirectory", ""));
	}

	public void addFileToRecents(String path)
	{
		// check if this is already in the recent files list
		for (String recentFile : fRecentFiles)
		{
			if (recentFile.equals(path))
				return;	// Skip
		}

		// Remove oldest file from list if necessary
		if (fRecentFiles.size() >= kMaxRecentFiles)
			fRecentFiles.removeElementAt(0);
		
		fRecentFiles.addElement(path);

		// Write out to preferences file
		StringBuffer recentList = new StringBuffer();
		for (String recentFile : fRecentFiles)
		{
			if (recentList.length() > 0)
				recentList.append(';');
				
			recentList.append(recentFile);
		}

		fPrefs.put("recentFiles", recentList.toString());
	}

	public Vector<String> getRecentFileList()
	{
		return fRecentFiles;
	}

	public void writeColors()
	{
		writeColor("traceColor", kTraceColor);
		writeColor("conflictColor", kConflictColor);
		writeColor("selectionColor", kSelectionColor);
		writeColor("cursorColor", kCursorColor);
		writeColor("backgroundColor", kBackgroundColor);
		writeColor("timingMarkerColor", kTimingMarkerColor);
		writeColor("markerColor", kMarkerColor);
		writeColor("listSelectionBgColor", kListSelectionBgColor);
		writeColor("listSelectionFgColor", kListSelectionFgColor);
		writeColor("valueColor", kValueColor);
	}

	private AppPreferences()
	{
		readPreferences();
	}

	void readPreferences()
	{
		String recentList = fPrefs.get("recentFiles", "");
		String[] paths = recentList.split(";");
		for (String path : paths)
		{
			if (path.length() > 0)
				fRecentFiles.addElement(path);
		}
		
		kTraceColor = readColor("traceColor", Color.black);
		kConflictColor = readColor("conflictColor", new Color(255, 200, 200));
		kSelectionColor = readColor("selectionColor", new Color(230, 230, 230));
		kCursorColor = readColor("cursorColor", Color.red);
		kBackgroundColor = readColor("backgroundColor", Color.white);
		System.out.println("read background color = " + kBackgroundColor);
		kTimingMarkerColor = readColor("timingMarkerColor", Color.gray);
		kMarkerColor = readColor("markerColor", Color.green);
		kListSelectionBgColor = readColor("listSelectionBgColor", Color.blue);
		kListSelectionFgColor = readColor("listSelectionFgColor", Color.white);
		kValueColor = readColor("valueColor", Color.blue);
	}
	
	private Color readColor(String name, Color def)
	{
		int components = fPrefs.getInt(name, -1);
		if (components == -1)
			return def;
			
		return new Color((components >> 16) & 0xff, (components >> 8) & 0xff, 
			components & 0xff);
	}
	
	private void writeColor(String name, Color color)
	{
		int packed = color.getBlue() | (color.getGreen() << 8) | (color.getRed() << 16);
		fPrefs.putInt(name, packed);	
	}
	
	private Preferences fPrefs = Preferences.userNodeForPackage(WaveApp.class);
	private static AppPreferences fInstance;
	private Vector<String> fRecentFiles = new Vector<String>();
}

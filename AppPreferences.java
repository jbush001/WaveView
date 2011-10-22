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

	void addFileToRecents(String path)
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

	private AppPreferences()
	{
		readPreferences();

		kTraceColor = Color.black;
		kConflictColor = new Color(255, 200, 200);
		kSelectionColor = new Color(230, 230, 230);
		kCursorColor = Color.red;
		kBackgroundColor = Color.white;
		kTimingMarkerColor = Color.gray;
		kMarkerColor = Color.green;
		kListSelectionBgColor = Color.blue;
		kListSelectionFgColor = Color.white;
		kValueColor = Color.blue;
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
	}
	
	private Preferences fPrefs = Preferences.userNodeForPackage(WaveApp.class);
	private static AppPreferences fInstance = new AppPreferences();
	private Vector<String> fRecentFiles = new Vector<String>();
}

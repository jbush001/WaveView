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
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.text.*;
import java.io.*;

public class WaveApp extends JPanel implements ActionListener
{
    public WaveApp(TraceViewModel viewModel, TraceDataModel dataModel)
    {
        super(new BorderLayout());

        fTraceViewModel = viewModel;
        fTraceDataModel = dataModel;

        JToolBar toolBar = new JToolBar();
        JButton button = new JButton(loadResourceIcon("zoom-in.png"));
        button.setActionCommand("zoomin");
        button.addActionListener(this);
        button.setToolTipText("Zoom In");
        toolBar.add(button);

        button = new JButton(loadResourceIcon("zoom-out.png"));
        button.setActionCommand("zoomout");
        button.addActionListener(this);
        button.setToolTipText("Zoom Out");
        toolBar.add(button);

        button = new JButton(loadResourceIcon("zoom-selection.png"));
        button.setActionCommand("zoomselection");
        button.addActionListener(this);
        button.setToolTipText("Zoom to selected region");
        toolBar.add(button);

        button = new JButton(loadResourceIcon("net-search.png"));
        button.setActionCommand("addnet");
        button.addActionListener(this);
        button.setToolTipText("Add nets");
        toolBar.add(button);
        add(toolBar, BorderLayout.PAGE_START);

        fTraceView = new TraceView(viewModel, dataModel, this);
        add(fTraceView, BorderLayout.CENTER);

        setPreferredSize(new Dimension(900,600));
    }

    public ImageIcon loadResourceIcon(String name)
    {
        return new ImageIcon(this.getClass().getClassLoader().getResource(name));
    }

    public void actionPerformed(ActionEvent e)
    {
        String cmd = e.getActionCommand();
        if (cmd.equals("zoomin"))
            fTraceView.zoomIn();
        else if (cmd.equals("zoomout"))
            fTraceView.zoomOut();
        else if (cmd.equals("zoomselection"))
            fTraceView.zoomToSelection();
        else if (cmd.equals("addnet"))
        {
            if (fSearchPane == null)
            {
                /// @bug This is a hack.  It makes sure the search
                /// panel is created after the file is loaded.
                /// It may not work correctly after re-loading a new file.
                fSearchPane = new NetSearchView(fTraceViewModel, fTraceDataModel);
                add(fSearchPane, BorderLayout.WEST);

                /// @bug Bigger hack: for some reason it doesn't show up
                /// unless I do this.
                fSearchPane.setVisible(false);
                fSearchPane.setVisible(true);
            }
            else
            {
                // Hide or show the search panel
                fSearchPane.setVisible(!fSearchPane.isVisible());
            }
        }
        else if (cmd.equals("opentrace"))
        {
            JFileChooser chooser = new JFileChooser(AppPreferences.getInstance().getInitialTraceDirectory());
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setMultiSelectionEnabled(false);
            int returnValue = chooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION)
            {
                try
                {
                    openTraceFile(chooser.getSelectedFile());
                    addFileToRecents(chooser.getSelectedFile().getCanonicalPath());
                    AppPreferences.getInstance().setInitialTraceDirectory(chooser.getSelectedFile().getParentFile());
                }
                catch (Exception exc)
                {
                    JOptionPane.showMessageDialog(this, "Error opening configuration file");
                }
            }
        }
        else if (cmd.equals("exit"))
        {
            saveConfig();
            System.exit(0);
        }
        else if (cmd.equals("removeAllMarkers"))
            fTraceViewModel.removeAllMarkers();
        else if (cmd.equals("removeAllNets"))
            fTraceViewModel.removeAllNets();
        else if (cmd.equals("insertMarker"))
        {
            String description = (String) JOptionPane.showInputDialog(
                SwingUtilities.getWindowAncestor(this), "Description for this marker", "New Marker",
                JOptionPane.PLAIN_MESSAGE, null, null, null);
            fTraceViewModel.addMarker(description, fTraceViewModel.getCursorPosition());
        }
        else if (cmd.equals("showmarkerlist"))
        {
            if (fMarkersWindow == null)
            {
                fMarkersWindow = new JFrame("Markers");
                MarkerListView contentPane = new MarkerListView(fTraceViewModel);
                contentPane.setOpaque(true);
                fMarkersWindow.setPreferredSize(new Dimension(400, 300));
                fMarkersWindow.setContentPane(contentPane);
                fMarkersWindow.pack();
            }

            fMarkersWindow.setVisible(true);
        }
        else if (cmd.equals("nextMarker"))
            fTraceViewModel.nextMarker((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
        else if (cmd.equals("prevMarker"))
            fTraceViewModel.prevMarker((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
        else if (cmd.equals("removeMarker"))
            fTraceViewModel.removeMarkerAtTime(fTraceViewModel.getCursorPosition());
        else if (cmd.equals("findbyvalue"))
            showQueryDialog();
        else if (cmd.equals("findnext"))
            findNext();
        else if (cmd.equals("findprev"))
            findPrev();
        else if (cmd.equals("saveNetSet"))
        {
            String name = (String) JOptionPane.showInputDialog(
                SwingUtilities.getWindowAncestor(this), "Net Set Name", "Save Net Set",
                JOptionPane.PLAIN_MESSAGE, null, null, null);
            if (!name.equals(""))
            {
                fTraceViewModel.saveNetSet(name);
                buildNetMenu();
            }
        }
        else if (cmd.length() > 7  && cmd.substring(0, 7).equals("netSet_"))
        {
            int index = Integer.parseInt(cmd.substring(7));
            fTraceViewModel.selectNetSet(index);
        }
        else if (cmd.length() > 5 && cmd.substring(0, 5).equals("open "))
        {
            try
            {
                openTraceFile(cmd.substring(5));
            }
            catch (Exception exc)
            {
                /// @todo Pop up an error dialog here
                System.out.println("caught " + exc + " trying to open file");
            }
        }
        else if (cmd.equals("prefs"))
        {
            if (fPrefsWindow == null)
                fPrefsWindow = new PreferenceWindow();

            fPrefsWindow.setVisible(true);
        }
    }

    void openTraceFile(String path) throws Exception
    {
        openTraceFile(new File(path));
    }

    void openTraceFile(File file) throws Exception
    {
        if (fConfigFileName != null)
            saveConfig();

        /// @todo Determine the type dynamically
        TraceLoader loader = new VCDLoader();
        fTraceViewModel.clear();

        System.gc();
        long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long startTime = System.currentTimeMillis();
        loader.load(new FileInputStream(file), fTraceDataModel.startBuilding());
        long endTime = System.currentTimeMillis();
        System.gc();
        long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        System.out.println("took " + (endTime - startTime) +  " ms to load file");
        System.out.println("" + (endMem - startMem) +  " bytes of memory used");
        fFrame.setTitle("Waveform Viewer [" + file.getName() + "]");
        setConfigurationFile(createConfigFileName(file));
        buildNetMenu();
    }

    void showQueryDialog()
    {
        // The initial query string is formed by the selected nets and
        // their values at the cursor position.
        StringBuffer initialQuery = new StringBuffer();
        boolean first = true;
        long cursorPosition = fTraceViewModel.getCursorPosition();

        for (int index : fTraceView.getSelectedNets())
        {
            int netId = fTraceViewModel.getVisibleNet(index);
            if (first)
                first = false;
            else
                initialQuery.append(" and ");

            initialQuery.append(fTraceDataModel.getFullNetName(netId));
            Transition t = fTraceDataModel.findTransition(netId,
                cursorPosition).next();
            initialQuery.append(" = ");
            initialQuery.append(fTraceViewModel.getValueFormatter(index)
                .format(t));
        }

        // XXX make a proper find window with a textarea
        String query = (String) JOptionPane.showInputDialog(
            SwingUtilities.getWindowAncestor(this), "Enter search string",
            initialQuery);
        if (query != null)
            doFind(query);
    }

    void doFind(String queryString)
    {
        try
        {
            fCurrentQuery = new Query(fTraceDataModel, queryString);
            findNext();
        }
        catch (Query.QueryParseException exc)
        {
            JOptionPane.showMessageDialog(null, exc.toString(),"Error parsing query",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    void findNext()
    {
        if (fCurrentQuery != null)
        {
            long newTimestamp = fCurrentQuery.getNextMatch(fTraceViewModel.getCursorPosition());
            fTraceViewModel.setSelectionStart(newTimestamp);
            fTraceViewModel.setCursorPosition(newTimestamp);
        }
    }

    void findPrev()
    {
        if (fCurrentQuery != null)
        {
            long newTimestamp = fCurrentQuery.getPreviousMatch(fTraceViewModel.getCursorPosition());
            fTraceViewModel.setSelectionStart(newTimestamp);
            fTraceViewModel.setCursorPosition(newTimestamp);
        }
    }

    void setConfigurationFile(String configFileName)
    {
        fTraceSettingsFile = new TraceSettingsFile(configFileName,
            fTraceDataModel, fTraceViewModel);
        fConfigFileName = configFileName ;
        if ((new File(configFileName)).exists())
            fTraceSettingsFile.readConfigurationFile();
    }

    void saveConfig()
    {
        if (fTraceSettingsFile != null)
            fTraceSettingsFile.writeConfigurationFile();
    }

    static String createConfigFileName(File file) throws IOException
    {
        String path = file.getCanonicalPath();

        // Find leaf file name
        int index = path.lastIndexOf('/');
        String dirPath;
        String nodeName;
        if (index == -1)
        {
            dirPath = "";
            nodeName = path;
        }
        else
        {
            dirPath = path.substring(0, index + 1);
            nodeName = path.substring(index + 1);
        }

        nodeName = "." + nodeName + ".traceconfig";

        return dirPath + nodeName;
    }

    void buildNetMenu()
    {
        fNetMenu.removeAll();

        JMenuItem item = new JMenuItem("Add nets...");
        item.setActionCommand("addnet");
        item.addActionListener(this);
        fNetMenu.add(item);

        item = new JMenuItem("Remove all nets");
        item.setActionCommand("removeAllNets");
        item.addActionListener(this);
        fNetMenu.add(item);

        item = new JMenuItem("Save Net Set");
        item.setActionCommand("saveNetSet");
        item.addActionListener(this);
        fNetMenu.add(item);

        if (fTraceViewModel.getNetSetCount() > 0)
        {
            fNetMenu.addSeparator();
            for (int i = 0; i < fTraceViewModel.getNetSetCount(); i++)
            {
                item = new JMenuItem(fTraceViewModel.getNetSetName(i));
                item.setActionCommand("netSet_" + i);
                item.addActionListener(this);
                fNetMenu.add(item);
            }
        }
    }

    void addFileToRecents(String path)
    {
        AppPreferences.getInstance().addFileToRecents(path);
        buildRecentFilesMenu();
    }

    void buildRecentFilesMenu()
    {
        fRecentFilesMenu.removeAll();
        for (String path : AppPreferences.getInstance().getRecentFileList())
        {
            JMenuItem item = new JMenuItem(path);
            item.setActionCommand("open " + path);
            item.addActionListener(this);
            fRecentFilesMenu.add(item);
        }
    }

    void buildMenus()
    {
        JMenuItem item;
        JMenuBar menuBar = new JMenuBar();
        fFrame.setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        item = new JMenuItem("Open Trace...");
        item.setActionCommand("opentrace");
        item.addActionListener(this);
        fileMenu.add(item);

        fRecentFilesMenu = new JMenu("Open Recent");
        fileMenu.add(fRecentFilesMenu);

        buildRecentFilesMenu();

        item = new JMenuItem("Preferences...");
        item.setActionCommand("prefs");
        item.addActionListener(this);
        fileMenu.add(item);

        item = new JMenuItem("Exit");
        item.setActionCommand("exit");
        item.addActionListener(this);
        fileMenu.add(item);

        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        item = new JMenuItem("Find...");
        item.setActionCommand("findbyvalue");
        item.addActionListener(this);
        editMenu.add(item);

        item = new JMenuItem("Find next");
        item.setActionCommand("findnext");
        item.addActionListener(this);
        editMenu.add(item);

        item = new JMenuItem("Find prev");
        item.setActionCommand("findprev");
        item.addActionListener(this);
        editMenu.add(item);

        // XXX go to timestamp

        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);
        item = new JMenuItem("Zoom In");
        item.setActionCommand("zoomin");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.META_DOWN_MASK));
        item.addActionListener(this);
        viewMenu.add(item);

        item = new JMenuItem("Zoom Out");
        item.setActionCommand("zoomout");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.META_DOWN_MASK));
        item.addActionListener(this);
        viewMenu.add(item);

        item = new JMenuItem("Zoom To Selection");
        item.setActionCommand("zoomselection");
        item.addActionListener(this);
        viewMenu.add(item);

        fNetMenu = new JMenu("Net");
        menuBar.add(fNetMenu);
        buildNetMenu();

        JMenu markerMenu = new JMenu("Marker");
        menuBar.add(markerMenu);
        item = new JMenuItem("Insert Marker...");
        item.setActionCommand("insertMarker");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.META_DOWN_MASK));
        item.addActionListener(this);
        markerMenu.add(item);

        item = new JMenuItem("Next Marker");
        item.setActionCommand("nextMarker");
        item.addActionListener(this);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.META_DOWN_MASK));
        markerMenu.add(item);

        item = new JMenuItem("Prev Marker");
        item.setActionCommand("prevMarker");
        item.addActionListener(this);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.META_DOWN_MASK));
        markerMenu.add(item);

        item = new JMenuItem("Remove Marker");
        item.setActionCommand("removeMarker");
        item.addActionListener(this);
        markerMenu.add(item);

        item = new JMenuItem("Remove all markers");
        item.setActionCommand("removeAllMarkers");
        item.addActionListener(this);
        markerMenu.add(item);

        item = new JMenuItem("Show marker list");
        item.setActionCommand("showmarkerlist");
        item.addActionListener(this);
        markerMenu.add(item);
    }

    private TraceView fTraceView;
    private TraceViewModel fTraceViewModel;
    private TraceDataModel fTraceDataModel;
    private String fConfigFileName;
    private Query fCurrentQuery;
    private JFrame fAddNetsWindow;
    private JFrame fMarkersWindow;
    private JFrame fPrefsWindow;
    private JMenu fNetMenu;
    private JFrame fFrame;
    private JMenu fRecentFilesMenu;
    private TraceSettingsFile fTraceSettingsFile;
    private NetSearchView fSearchPane;

    private static void createAndShowGUI(String[] args)
    {
        TraceViewModel viewModel = new TraceViewModel();
        TraceDataModel dataModel = new TraceDataModel();
        final WaveApp contentPane = new WaveApp(viewModel, dataModel);
        JFrame frame = new JFrame("Waveform Viewer");
        contentPane.fFrame = frame;
        contentPane.buildMenus();

        if (args.length > 0)
        {
            try
            {
                contentPane.openTraceFile(args[0]);
                contentPane.addFileToRecents((new File(args[0])).getCanonicalPath());
            }
            catch (Exception exc)
            {
                System.out.println("caught exception " + exc);
                exc.printStackTrace();
            }
        }

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                contentPane.saveConfig();
                System.exit(0);
            }
        });

        contentPane.setOpaque(true);
        frame.setContentPane(contentPane);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args)
    {
        final String[] _args = args;
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() { createAndShowGUI(_args); }
        });
    }
}


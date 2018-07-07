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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ProgressMonitor;

/// @todo Add menu item to jump to specific timestamp
public class MainWindow extends JPanel implements ActionListener {
    private final TracePanel tracePanel;
    private final TracePresentationModel tracePresentationModel = new TracePresentationModel();
    private final TraceDataModel traceDataModel = new TraceDataModel();
    private Search currentSearch;
    private JMenu netMenu;
    private JFrame frame;
    private JMenu recentFilesMenu;
    private TraceSettingsFile traceSettingsFile;
    private File currentTraceFile;
    private NetSearchPanel netSearchPane;
    private final RecentFiles recentFiles = new RecentFiles();

    public MainWindow() {
        super(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        add(toolBar, BorderLayout.PAGE_START);
        toolBar.add(createButton("net-search.png", "Add Nets", "addnet"));
        toolBar.add(createButton("zoom-in.png", "Zoom In", "zoomin"));
        toolBar.add(createButton("zoom-out.png", "Zoom Out", "zoomout"));
        toolBar.add(createButton("zoom-selection.png", "Zoom to selected region", "zoomselection"));
        toolBar.add(createButton("add-marker.png", "Insert Marker", "insertMarker"));
        toolBar.add(createButton("remove-marker.png", "Remove Marker", "removeMarker"));

        tracePanel = new TracePanel(tracePresentationModel, traceDataModel);
        add(tracePanel, BorderLayout.CENTER);

        recentFiles.unpack(AppPreferences.getInstance().getRecentList());

        setPreferredSize(new Dimension(900, 600));
    }

    private JButton createButton(String iconName, String toolTipText, String command) {
        JButton button = new JButton(loadResourceIcon(iconName));
        button.setActionCommand(command);
        button.addActionListener(this);
        button.setToolTipText(toolTipText);
        return button;
    }

    private ImageIcon loadResourceIcon(String name) {
        return new ImageIcon(this.getClass().getClassLoader().getResource(name));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        switch (cmd) {
        case "zoomin":
            tracePanel.zoomIn();
            break;
        case "zoomout":
            tracePanel.zoomOut();
            break;
        case "zoomselection":
            tracePanel.zoomToSelection();
            break;
        case "addnet":
            addNet();
            break;
        case "opentrace":
            openTrace();
            break;
        case "reloadtrace":
            loadTraceFile(currentTraceFile);
            break;
        case "quit":
            frame.dispose();
            break;
        case "removeAllMarkers":
            tracePresentationModel.removeAllMarkers();
            break;
        case "removeAllNets":
            tracePresentationModel.removeAllNets();
            break;
        case "insertMarker":
            insertMarker();
            break;
        case "showmarkerlist":
            showMarkerList();
            break;
        case "nextMarker":
            nextMarker(e);
            break;
        case "prevMarker":
            prevMarker(e);
            break;
        case "removeMarker":
            removeMarker();
            break;
        case "findbyvalue":
            showFindDialog();
            break;
        case "findnext":
            findNext((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
            break;
        case "findprev":
            findPrev((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
            break;
        case "saveNetSet":
            saveNetSet();
            break;
        case "prefs":
            showPrefs();
            break;
        default:
            if (cmd.length() > 7 && cmd.substring(0, 7).equals("netSet_")) {
                int index = Integer.parseInt(cmd.substring(7));
                tracePresentationModel.selectNetSet(index);
            } else if (cmd.length() > 5 && cmd.substring(0, 5).equals("open ")) {
                // Load from recents menu
                loadTraceFile(cmd.substring(5));
            }
        }
    }

    private void addNet() {
        if (netSearchPane == null) {
            /// @bug This is a hack. It makes sure the search
            /// panel is created after the file is loaded.
            netSearchPane = new NetSearchPanel(traceDataModel);
            add(netSearchPane, BorderLayout.WEST);

            /// @bug Bigger hack: for some reason it doesn't show up
            /// unless I do this.
            netSearchPane.setVisible(false);
            netSearchPane.setVisible(true);
        } else {
            // Hide or show the search panel
            netSearchPane.setVisible(!netSearchPane.isVisible());
        }
    }

    private void insertMarker() {
        String description = (String) JOptionPane.showInputDialog(frame, "Description for this marker", "New Marker",
                JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (description != null) {
            tracePresentationModel.addMarker(description, tracePresentationModel.getCursorPosition());
        }
    }

    private void showMarkerList() {
        JDialog markersWindow = new JDialog(frame, "Markers", true);
        markersWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        MarkerListPanel contentPane = new MarkerListPanel(tracePresentationModel);
        contentPane.setOpaque(true);
        markersWindow.setPreferredSize(new Dimension(400, 300));
        markersWindow.setContentPane(contentPane);
        markersWindow.pack();
        markersWindow.setLocationRelativeTo(this);
        markersWindow.setVisible(true);
    }

    private void nextMarker(ActionEvent e) {
        tracePresentationModel.nextMarker((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
    }

    private void prevMarker(ActionEvent e) {
        tracePresentationModel.prevMarker((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
    }

    private void removeMarker() {
        tracePresentationModel.removeMarkerAtTime(tracePresentationModel.getCursorPosition());
    }

    private void openTrace() {
        JFileChooser chooser = new JFileChooser(AppPreferences.getInstance().getInitialTraceDirectory());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int returnValue = chooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            AppPreferences.getInstance().setInitialTraceDirectory(chooser.getSelectedFile().getParentFile());
            loadTraceFile(chooser.getSelectedFile());
        }
    }

    private void saveNetSet() {
        String name = (String) JOptionPane.showInputDialog(frame, "Net Set Name", "Save Net Set",
                JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (!name.equals("")) {
            tracePresentationModel.saveNetSet(name);
            buildNetMenu();
        }
    }

    private void showPrefs() {
        JDialog prefsWindow = new PreferenceWindow(frame);
        prefsWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        prefsWindow.setLocationRelativeTo(this);
        prefsWindow.setVisible(true);
    }

    public void loadTraceFile(String path) {
        loadTraceFile(new File(path));
    }

    private void loadTraceFile(File file) {
        saveTraceSettings();
        ProgressMonitor monitor = new ProgressMonitor(MainWindow.this, "Loading...", "", 0, 100);
        new TraceLoadWorker(file, monitor, (newModel, errorMessage)
                -> handleLoadFinished(file, newModel, errorMessage)).execute();
    }

    private void handleLoadFinished(File file, TraceDataModel newModel, String errorMessage) {
        if (errorMessage != null) {
            JOptionPane.showMessageDialog(MainWindow.this, "Error opening waveform file: "
                + errorMessage);
            return;
        }

        currentSearch = null;
        tracePresentationModel.clear();

        // XXX hack
        // Because the load ran on a separate thread, and I didn't want to add locking
        // everywhere, this loaded into a new copy of a data model. However, there are
        // references to the trace data model scattered all over the place. Rather
        // than try to update all pointers to the new model, this just copies data from
        // the new object to the old one. Since this runs on the main window thread now,
        // this is safe.
        traceDataModel.copyFrom(newModel);

        frame.setTitle("Waveform Viewer [" + file.getName() + "]");

        try {
            recentFiles.add(file.getCanonicalPath());
            AppPreferences.getInstance().setRecentList(recentFiles.pack());

            File settingsFile = TraceSettingsFile.settingsFileName(file);
            traceSettingsFile = new TraceSettingsFile(settingsFile, traceDataModel, tracePresentationModel);
            if (settingsFile.exists()) {
                traceSettingsFile.read();
            }
        } catch (Exception exc) {
            System.out.println("caught exception while reading settings " + exc);
            // XXX Display an error dialog?
        }

        buildRecentFilesMenu();
        buildNetMenu();
        destroyNetSearchPane();
        currentTraceFile = file;
    }

    // XXX hack
    // The net search pane holds onto the old tree model, which has been
    // replaced. Delete it so it will be re-created attached to the new one.
    private void destroyNetSearchPane() {
        if (netSearchPane != null) {
            if (netSearchPane.isVisible()) {
                netSearchPane.setVisible(false);
            }

            remove(netSearchPane);
            netSearchPane = null;
        }
    }

    private void showFindDialog() {
        String initialSearch = generateSearchFromSelection();
        FindPanel findPanel = new FindPanel(this, initialSearch);
        JDialog findFrame = new JDialog(this.frame, "Find", true);
        findFrame.getContentPane().add(findPanel);
        findFrame.setSize(new Dimension(450, 150));
        findFrame.setResizable(false);
        findFrame.setLocationRelativeTo(this);
        findFrame.setVisible(true);
    }

    /// The initial search string is formed by the selected nets and
    /// their values at the cursor position.
    private String generateSearchFromSelection() {
        StringBuilder searchExpr = new StringBuilder();
        boolean first = true;
        long cursorPosition = tracePresentationModel.getCursorPosition();

        for (int index : tracePanel.getSelectedNets()) {
            NetDataModel netDataModel = tracePresentationModel.getVisibleNet(index);
            if (first) {
                first = false;
            } else {
                searchExpr.append(" and ");
            }

            searchExpr.append(netDataModel.getFullName());
            Transition t = netDataModel.findTransition(cursorPosition).next();

            searchExpr.append(" = 'h");
            searchExpr.append(t.toString(16));
        }

        return searchExpr.toString();
    }

    void setSearch(String searchString) throws Search.ParseException {
        currentSearch = new Search(traceDataModel, searchString);
    }

    void findNext(boolean extendSelection) {
        if (currentSearch != null) {
            long newTimestamp = currentSearch.getNextMatch(tracePresentationModel.getCursorPosition());
            if (newTimestamp >= 0) {
                if (!extendSelection) {
                    tracePresentationModel.setSelectionStart(newTimestamp);
                }

                tracePresentationModel.setCursorPosition(newTimestamp);
            }
        }
    }

    void findPrev(boolean extendSelection) {
        if (currentSearch != null) {
            long newTimestamp = currentSearch.getPreviousMatch(tracePresentationModel.getCursorPosition());
            if (newTimestamp >= 0) {
                if (!extendSelection) {
                    tracePresentationModel.setSelectionStart(newTimestamp);
                }

                tracePresentationModel.setCursorPosition(newTimestamp);
            }
        }
    }

    private void saveTraceSettings() {
        try {
            if (traceSettingsFile != null)
                traceSettingsFile.write();
        } catch (Exception exc) {
            System.out.println("Error saving configuration file " + exc);
        }
    }

    private void buildNetMenu() {
        netMenu.removeAll();

        JMenuItem item = new JMenuItem("Add nets...");
        item.setActionCommand("addnet");
        item.addActionListener(this);
        netMenu.add(item);

        item = new JMenuItem("Remove all nets");
        item.setActionCommand("removeAllNets");
        item.addActionListener(this);
        netMenu.add(item);

        item = new JMenuItem("Save Net Set");
        item.setActionCommand("saveNetSet");
        item.addActionListener(this);
        netMenu.add(item);

        if (tracePresentationModel.getNetSetCount() > 0) {
            netMenu.addSeparator();
            for (int i = 0; i < tracePresentationModel.getNetSetCount(); i++) {
                item = new JMenuItem(tracePresentationModel.getNetSetName(i));
                item.setActionCommand("netSet_" + i);
                item.addActionListener(this);
                netMenu.add(item);
            }
        }
    }

    private void buildMenus() {
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        fileMenu.add(createMenuItem("Open Trace...", "opentrace",
                KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.META_DOWN_MASK)));

        recentFilesMenu = new JMenu("Open Recent");
        fileMenu.add(recentFilesMenu);
        buildRecentFilesMenu();

        fileMenu.add(createMenuItem("Reload Trace", "reloadtrace", null));
        fileMenu.add(createMenuItem("Preferences...", "prefs", null));
        fileMenu.add(createMenuItem("Quit", "quit", KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.META_DOWN_MASK)));

        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        editMenu.add(createMenuItem("Find...", "findbyvalue", KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.META_DOWN_MASK)));
        editMenu.add(createMenuItem("Find next", "findnext", KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.META_DOWN_MASK)));
        editMenu.add(createMenuItem("Find prev", "findprev",
                KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)));

        // XXX go to timestamp

        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);
        viewMenu.add(createMenuItem("Zoom In", "zoomin", KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.META_DOWN_MASK)));
        viewMenu.add(createMenuItem("Zoom Out", "zoomout",
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.META_DOWN_MASK)));
        viewMenu.add(createMenuItem("Zoom To Selection", "zoomselection", null));

        netMenu = new JMenu("Net");
        menuBar.add(netMenu);
        buildNetMenu();

        JMenu markerMenu = new JMenu("Marker");
        menuBar.add(markerMenu);
        markerMenu.add(createMenuItem("Insert Marker...", "insertMarker",
                KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.META_DOWN_MASK)));
        markerMenu.add(createMenuItem("Next Marker", "nextMarker",
                KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.META_DOWN_MASK)));
        markerMenu.add(createMenuItem("Prev Marker", "prevMarker",
                KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.META_DOWN_MASK)));
        markerMenu.add(createMenuItem("Remove Marker", "removeMarker", null));
        markerMenu.add(createMenuItem("Remove all markers", "removeAllMarkers", null));
        markerMenu.add(createMenuItem("Show marker list", "showmarkerlist", null));
    }

    JMenuItem createMenuItem(String text, String actionCommand,
            KeyStroke accelerator) {
        JMenuItem item = new JMenuItem(text);
        item.setActionCommand(actionCommand);
        item.addActionListener(this);
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }

        return item;
    }

    private void buildRecentFilesMenu() {
        recentFilesMenu.removeAll();
        for (String path : recentFiles.getList()) {
            JMenuItem item = new JMenuItem(path);
            item.setActionCommand("open " + path);
            item.addActionListener(this);
            recentFilesMenu.add(item);
        }
    }

    private static void createAndShowGUI(String[] args) {
        final MainWindow contentPane = new MainWindow();
        JFrame frame = new JFrame("Waveform Viewer");
        contentPane.frame = frame;
        contentPane.buildMenus();

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                contentPane.saveTraceSettings();
            }
        });

        contentPane.setOpaque(true);
        frame.setContentPane(contentPane);
        frame.pack();
        frame.setVisible(true);

        if (args.length > 0) {
            contentPane.loadTraceFile(args[0]);
        }
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> createAndShowGUI(args));
    }
}

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
    private final WaveformContainerView waveformContainer;
    private final WaveformPresentationModel waveformPresentationModel = new WaveformPresentationModel();
    private final WaveformDataModel waveformDataModel = new WaveformDataModel();
    private Search currentSearch;
    private JMenu netMenu;
    private JFrame frame;
    private JMenu recentFilesMenu;
    private WaveformSettingsFile waveformSettingsFile;
    private File currentWaveformFile;
    private NetSearchView netSearchPane;
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

        waveformContainer = new WaveformContainerView(waveformPresentationModel, waveformDataModel);
        add(waveformContainer, BorderLayout.CENTER);

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
            waveformContainer.zoomIn();
            break;
        case "zoomout":
            waveformContainer.zoomOut();
            break;
        case "zoomselection":
            waveformContainer.zoomToSelection();
            break;
        case "addnet":
            addNet();
            break;
        case "openwaveform":
            openWaveform();
            break;
        case "reloadwaveform":
            loadWaveformFile(currentWaveformFile);
            break;
        case "quit":
            frame.dispose();
            break;
        case "removeAllMarkers":
            waveformPresentationModel.removeAllMarkers();
            break;
        case "removeAllNets":
            waveformPresentationModel.removeAllNets();
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
                waveformPresentationModel.selectNetSet(index);
            } else if (cmd.length() > 5 && cmd.substring(0, 5).equals("open ")) {
                // Load from recents menu
                loadWaveformFile(cmd.substring(5));
            }
            break;
        }
    }

    private void addNet() {
        if (netSearchPane == null) {
            /// @bug This is a hack. It makes sure the search
            /// panel is created after the file is loaded.
            netSearchPane = new NetSearchView(waveformDataModel);
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
            waveformPresentationModel.addMarker(description, waveformPresentationModel.getCursorPosition());
        }
    }

    private void showMarkerList() {
        JDialog markersWindow = new JDialog(frame, "Markers", true);
        markersWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        MarkerListView contentPane = new MarkerListView(waveformPresentationModel);
        contentPane.setOpaque(true);
        markersWindow.setPreferredSize(new Dimension(400, 300));
        markersWindow.setContentPane(contentPane);
        markersWindow.pack();
        markersWindow.setLocationRelativeTo(this);
        markersWindow.setVisible(true);
    }

    private void nextMarker(ActionEvent e) {
        waveformPresentationModel.nextMarker((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
    }

    private void prevMarker(ActionEvent e) {
        waveformPresentationModel.prevMarker((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
    }

    private void removeMarker() {
        waveformPresentationModel.removeMarkerAtTime(waveformPresentationModel.getCursorPosition());
    }

    private void openWaveform() {
        JFileChooser chooser = new JFileChooser(AppPreferences.getInstance().getInitialWaveformDirectory());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        int returnValue = chooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            AppPreferences.getInstance().setInitialWaveformDirectory(chooser.getSelectedFile().getParentFile());
            loadWaveformFile(chooser.getSelectedFile());
        }
    }

    private void saveNetSet() {
        String name = (String) JOptionPane.showInputDialog(frame, "Net Set Name", "Save Net Set",
                JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (!name.equals("")) {
            waveformPresentationModel.saveNetSet(name);
            buildNetMenu();
        }
    }

    private void showPrefs() {
        JDialog prefsWindow = new PreferenceWindow(frame);
        prefsWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        prefsWindow.setLocationRelativeTo(this);
        prefsWindow.setVisible(true);
    }

    public void loadWaveformFile(String path) {
        loadWaveformFile(new File(path));
    }

    private void loadWaveformFile(File file) {
        saveWaveformSettings();
        ProgressMonitor monitor = new ProgressMonitor(MainWindow.this, "Loading...", "", 0, 100);
        new WaveformLoadWorker(file, monitor, (newModel, errorMessage)
                -> handleLoadFinished(file, newModel, errorMessage)).execute();
    }

    private void handleLoadFinished(File file, WaveformDataModel newModel, String errorMessage) {
        if (errorMessage != null) {
            JOptionPane.showMessageDialog(MainWindow.this, "Error opening waveform file: "
                + errorMessage);
            return;
        }

        currentSearch = null;
        waveformPresentationModel.clear();

        // XXX hack
        // Because the load ran on a separate thread, and I didn't want to add locking
        // everywhere, this loaded into a new copy of a data model. However, there are
        // references to the waveform data model scattered all over the place. Rather
        // than try to update all pointers to the new model, this just copies data from
        // the new object to the old one. Since this runs on the main window thread now,
        // this is safe.
        waveformDataModel.copyFrom(newModel);

        frame.setTitle("Waveform Viewer [" + file.getName() + "]");

        try {
            recentFiles.add(file.getCanonicalPath());
            AppPreferences.getInstance().setRecentList(recentFiles.pack());

            File settingsFile = WaveformSettingsFile.settingsFileName(file);
            waveformSettingsFile = new WaveformSettingsFile(settingsFile, waveformDataModel, waveformPresentationModel);
            if (settingsFile.exists()) {
                waveformSettingsFile.read();
            }
        } catch (Exception exc) {
            System.out.println("caught exception while reading settings " + exc);
            // XXX Display an error dialog?
        }

        buildRecentFilesMenu();
        buildNetMenu();
        destroyNetSearchPane();
        currentWaveformFile = file;
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
        FindView findPanel = new FindView(this, initialSearch);
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
        int[] selectedIndices = waveformContainer.getSelectedNets();
        NetDataModel[] nets = new NetDataModel[selectedIndices.length];
        int netIndex = 0;
        for (int visibleIndex : selectedIndices) {
            nets[netIndex++] = waveformPresentationModel.getVisibleNet(visibleIndex);
        }

        long cursorPosition = waveformPresentationModel.getCursorPosition();
        return Search.generateSearch(nets, cursorPosition);
    }

    void setSearch(String searchString) throws Search.ParseException {
        currentSearch = new Search(waveformDataModel, searchString);
    }

    void findNext(boolean extendSelection) {
        if (currentSearch != null) {
            long newTimestamp = currentSearch.getNextMatch(waveformPresentationModel.getCursorPosition());
            if (newTimestamp >= 0) {
                waveformPresentationModel.setCursorPosition(newTimestamp, extendSelection);
            }
        }
    }

    void findPrev(boolean extendSelection) {
        if (currentSearch != null) {
            long newTimestamp = currentSearch.getPreviousMatch(waveformPresentationModel.getCursorPosition());
            if (newTimestamp >= 0) {
                waveformPresentationModel.setCursorPosition(newTimestamp, extendSelection);
            }
        }
    }

    private void saveWaveformSettings() {
        try {
            if (waveformSettingsFile != null)
                waveformSettingsFile.write();
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

        if (waveformPresentationModel.getNetSetCount() > 0) {
            netMenu.addSeparator();
            for (int i = 0; i < waveformPresentationModel.getNetSetCount(); i++) {
                item = new JMenuItem(waveformPresentationModel.getNetSetName(i));
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
        fileMenu.add(createMenuItem("Open Waveform...", "openwaveform",
                KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.META_DOWN_MASK)));

        recentFilesMenu = new JMenu("Open Recent");
        fileMenu.add(recentFilesMenu);
        buildRecentFilesMenu();

        fileMenu.add(createMenuItem("Reload Waveform", "reloadwaveform", null));
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

    private JMenuItem createMenuItem(String text, String actionCommand,
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
                contentPane.saveWaveformSettings();
            }
        });

        contentPane.setOpaque(true);
        frame.setContentPane(contentPane);
        frame.pack();
        frame.setVisible(true);

        if (args.length > 0) {
            contentPane.loadWaveformFile(args[0]);
        }
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> createAndShowGUI(args));
    }
}

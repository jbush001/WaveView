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

/// @todo Add menu item to jump to specific timestamp
public class WaveApp extends JPanel implements ActionListener {
    public WaveApp() {
        super(new BorderLayout());

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

        fTraceView = new TraceView(fTraceViewModel, fTraceDataModel, this);
        add(fTraceView, BorderLayout.CENTER);

        setPreferredSize(new Dimension(900,600));
    }

    private ImageIcon loadResourceIcon(String name) {
        return new ImageIcon(this.getClass().getClassLoader().getResource(name));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (cmd.equals("zoomin"))
            fTraceView.zoomIn();
        else if (cmd.equals("zoomout"))
            fTraceView.zoomOut();
        else if (cmd.equals("zoomselection"))
            fTraceView.zoomToSelection();
        else if (cmd.equals("addnet")) {
            if (fNetSearchPane == null) {
                /// @bug This is a hack.  It makes sure the search
                /// panel is created after the file is loaded.
                fNetSearchPane = new NetSearchView(fTraceViewModel, fTraceDataModel);
                add(fNetSearchPane, BorderLayout.WEST);

                /// @bug Bigger hack: for some reason it doesn't show up
                /// unless I do this.
                fNetSearchPane.setVisible(false);
                fNetSearchPane.setVisible(true);
            } else {
                // Hide or show the search panel
                fNetSearchPane.setVisible(!fNetSearchPane.isVisible());
            }
        } else if (cmd.equals("opentrace")) {
            JFileChooser chooser = new JFileChooser(AppPreferences.getInstance().getInitialTraceDirectory());
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            chooser.setMultiSelectionEnabled(false);
            int returnValue = chooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                AppPreferences.getInstance().setInitialTraceDirectory(chooser.getSelectedFile().getParentFile());
                loadTraceFile(chooser.getSelectedFile());
            }
        } else if (cmd.equals("quit")) {
            saveConfig();
            System.exit(0);
        } else if (cmd.equals("removeAllMarkers"))
            fTraceViewModel.removeAllMarkers();
        else if (cmd.equals("removeAllNets"))
            fTraceViewModel.removeAllNets();
        else if (cmd.equals("insertMarker")) {
            String description = (String) JOptionPane.showInputDialog(
                                     SwingUtilities.getWindowAncestor(this), "Description for this marker", "New Marker",
                                     JOptionPane.PLAIN_MESSAGE, null, null, null);
            fTraceViewModel.addMarker(description, fTraceViewModel.getCursorPosition());
        } else if (cmd.equals("showmarkerlist")) {
            if (fMarkersWindow == null) {
                fMarkersWindow = new JFrame("Markers");
                MarkerListView contentPane = new MarkerListView(fTraceViewModel);
                contentPane.setOpaque(true);
                fMarkersWindow.setPreferredSize(new Dimension(400, 300));
                fMarkersWindow.setContentPane(contentPane);
                fMarkersWindow.pack();
            }

            fMarkersWindow.setLocationRelativeTo(this);
            fMarkersWindow.setVisible(true);
        } else if (cmd.equals("nextMarker"))
            fTraceViewModel.nextMarker((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
        else if (cmd.equals("prevMarker"))
            fTraceViewModel.prevMarker((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0);
        else if (cmd.equals("removeMarker"))
            fTraceViewModel.removeMarkerAtTime(fTraceViewModel.getCursorPosition());
        else if (cmd.equals("findbyvalue"))
            showFindDialog();
        else if (cmd.equals("findnext"))
            findNext();
        else if (cmd.equals("findprev"))
            findPrev();
        else if (cmd.equals("saveNetSet")) {
            String name = (String) JOptionPane.showInputDialog(
                              SwingUtilities.getWindowAncestor(this), "Net Set Name", "Save Net Set",
                              JOptionPane.PLAIN_MESSAGE, null, null, null);
            if (!name.equals("")) {
                fTraceViewModel.saveNetSet(name);
                buildNetMenu();
            }
        } else if (cmd.length() > 7  && cmd.substring(0, 7).equals("netSet_")) {
            int index = Integer.parseInt(cmd.substring(7));
            fTraceViewModel.selectNetSet(index);
        } else if (cmd.length() > 5 && cmd.substring(0, 5).equals("open ")) {
            // Load from recents menu
            loadTraceFile(cmd.substring(5));
        } else if (cmd.equals("prefs")) {
            if (fPrefsWindow == null)
                fPrefsWindow = new PreferenceWindow();

            fPrefsWindow.setLocationRelativeTo(this);
            fPrefsWindow.setVisible(true);
        }
    }

    private void loadTraceFile(String path) {
        loadTraceFile(new File(path));
    }

    class TraceLoadWorker extends SwingWorker<Void, Void> {
        TraceLoadWorker(File file, ProgressMonitor monitor) {
            fFile = file;
            fProgressMonitor = monitor;
        }

        @Override
        public Void doInBackground() {
            try {
                System.out.println("Loading " + fFile.getCanonicalPath());

                /// @todo Determine the loader type dynamically
                TraceLoader loader = new VCDLoader();
                fNewModel = new TraceDataModel();
                TraceLoader.ProgressListener progressListener = new TraceLoader.ProgressListener() {
                    public boolean updateProgress(final int percentRead) {
                        // Accessing the component from a different thread, technically
                        // a no no, but probably okay.
                        if (fProgressMonitor.isCanceled())
                            return false;

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                fProgressMonitor.setProgress(percentRead);
                            }
                        });

                        return true;
                    }
                };

                loader.load(fFile, fNewModel.startBuilding(), progressListener);
            } catch (Exception exc) {
                fErrorMessage = exc.getMessage();
            }

            return null;
        }

        // Executed on main thread
        @Override
        protected void done() {
            System.out.println("finished loading trace file");
            fProgressMonitor.close();
            if (fErrorMessage != null) {
                JOptionPane.showMessageDialog(WaveApp.this, "Error opening waveform file: "
                                              + fErrorMessage);
            } else {
                fCurrentQuery = null;

                // XXX hack
                // Because this is running a separate thread, and I don't want to add locking
                // everywhere, we load into a new copy of a data model. However, there are
                // references to the trace data model scattered all over the place. Rather
                // than try to update all pointers to the new model, I just copy data from
                // the new object to the old one. Since I'm in the main window thread now,
                // this is safe.
                fTraceDataModel.copyFrom(fNewModel);

                fTraceViewModel.clear();
                fFrame.setTitle("Waveform Viewer [" + fFile.getName() + "]");

                try {
                    AppPreferences.getInstance().addFileToRecents(fFile.getCanonicalPath());

                    fConfigFile = createConfigFileName(fFile);
                    fTraceSettingsFile = new TraceSettingsFile(fConfigFile,
                            fTraceDataModel, fTraceViewModel);
                    if (fConfigFile.exists())
                        fTraceSettingsFile.read();
                } catch (Exception exc) {
                    exc.printStackTrace();
                }

                buildRecentFilesMenu();
                buildNetMenu();

                // XXX hack
                // The net search pane holds onto the old tree model, which has been
                // replaced. Delete it so it will be re-created attached to the new one.
                if (fNetSearchPane != null)
                {
                    if (fNetSearchPane.isVisible())
                        fNetSearchPane.setVisible(false);

                    remove(fNetSearchPane);
                    fNetSearchPane = null;
                }
            }
        }

        private File fFile;
        private ProgressMonitor fProgressMonitor;
        private TraceDataModel fNewModel;
        private String fErrorMessage;
    }

    private void loadTraceFile(File file) {
        saveConfig();
        ProgressMonitor monitor = new ProgressMonitor(WaveApp.this, "Loading...", "", 0, 100);
        (new TraceLoadWorker(file, monitor)).execute();
    }

    private void showFindDialog() {
        // The initial query string is formed by the selected nets and
        // their values at the cursor position.
        StringBuffer initialQuery = new StringBuffer();
        boolean first = true;
        long cursorPosition = fTraceViewModel.getCursorPosition();

        for (int index : fTraceView.getSelectedNets()) {
            int netId = fTraceViewModel.getVisibleNet(index);
            if (first)
                first = false;
            else
                initialQuery.append(" & ");

            initialQuery.append(fTraceDataModel.getFullNetName(netId));
            Transition t = fTraceDataModel.findTransition(netId,
                           cursorPosition).next();

            initialQuery.append(" = 'h");
            initialQuery.append(t.toString(16));
        }

        FindPanel findPanel = new FindPanel(this);
        findPanel.setInitialQuery(initialQuery.toString());
        JDialog frame = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Find", true);
        frame.getContentPane().add(findPanel);
        frame.setSize(new Dimension(450, 150));
        frame.setResizable(false);
        frame.setLocationRelativeTo(this);
        frame.setVisible(true);
    }

    void setQuery(String query) throws Query.ParseException {
        fCurrentQuery = new Query(fTraceDataModel, query);
    }

    void findNext() {
        if (fCurrentQuery != null) {
            long newTimestamp = fCurrentQuery.getNextMatch(fTraceViewModel.getCursorPosition());
            if (newTimestamp >= 0) {
                fTraceViewModel.setSelectionStart(newTimestamp);
                fTraceViewModel.setCursorPosition(newTimestamp);
            }
        }
    }

    void findPrev() {
        if (fCurrentQuery != null) {
            long newTimestamp = fCurrentQuery.getPreviousMatch(fTraceViewModel.getCursorPosition());
            if (newTimestamp >= 0) {
                fTraceViewModel.setSelectionStart(newTimestamp);
                fTraceViewModel.setCursorPosition(newTimestamp);
            }
        }
    }

    private void saveConfig() {
        try {
            if (fTraceSettingsFile != null)
                fTraceSettingsFile.write();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private static File createConfigFileName(File file) throws IOException {
        String path = file.getCanonicalPath();

        // Find leaf file name
        int index = path.lastIndexOf('/');
        String dirPath;
        String nodeName;
        if (index == -1) {
            dirPath = "";
            nodeName = path;
        } else {
            dirPath = path.substring(0, index + 1);
            nodeName = path.substring(index + 1);
        }

        nodeName = "." + nodeName + ".traceconfig";

        return new File(dirPath + nodeName);
    }

    private void buildNetMenu() {
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

        if (fTraceViewModel.getNetSetCount() > 0) {
            fNetMenu.addSeparator();
            for (int i = 0; i < fTraceViewModel.getNetSetCount(); i++) {
                item = new JMenuItem(fTraceViewModel.getNetSetName(i));
                item.setActionCommand("netSet_" + i);
                item.addActionListener(this);
                fNetMenu.add(item);
            }
        }
    }

    private void buildRecentFilesMenu() {
        fRecentFilesMenu.removeAll();
        for (String path : AppPreferences.getInstance().getRecentFileList()) {
            JMenuItem item = new JMenuItem(path);
            item.setActionCommand("open " + path);
            item.addActionListener(this);
            fRecentFilesMenu.add(item);
        }
    }

    private void buildMenus() {
        JMenuItem item;
        JMenuBar menuBar = new JMenuBar();
        fFrame.setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        item = new JMenuItem("Open Trace...");
        item.setActionCommand("opentrace");
        item.addActionListener(this);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.META_DOWN_MASK));
        fileMenu.add(item);

        fRecentFilesMenu = new JMenu("Open Recent");
        fileMenu.add(fRecentFilesMenu);

        buildRecentFilesMenu();

        item = new JMenuItem("Preferences...");
        item.setActionCommand("prefs");
        item.addActionListener(this);
        fileMenu.add(item);

        item = new JMenuItem("Quit");
        item.setActionCommand("quit");
        item.addActionListener(this);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.META_DOWN_MASK));
        fileMenu.add(item);

        JMenu editMenu = new JMenu("Edit");
        menuBar.add(editMenu);
        item = new JMenuItem("Find...");
        item.setActionCommand("findbyvalue");
        item.addActionListener(this);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.META_DOWN_MASK));
        editMenu.add(item);

        item = new JMenuItem("Find next");
        item.setActionCommand("findnext");
        item.addActionListener(this);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.META_DOWN_MASK));
        editMenu.add(item);

        item = new JMenuItem("Find prev");
        item.setActionCommand("findprev");
        item.addActionListener(this);
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.META_DOWN_MASK
                            | KeyEvent.SHIFT_DOWN_MASK));
        editMenu.add(item);

        // XXX go to timestamp

        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);
        item = new JMenuItem("Zoom In");
        item.setActionCommand("zoomin");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.META_DOWN_MASK));
        item.addActionListener(this);
        viewMenu.add(item);

        item = new JMenuItem("Zoom Out");
        item.setActionCommand("zoomout");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.META_DOWN_MASK));
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
    private TraceViewModel fTraceViewModel = new TraceViewModel();
    private TraceDataModel fTraceDataModel = new TraceDataModel();
    private File fConfigFile;
    private Query fCurrentQuery;
    private JFrame fAddNetsWindow;
    private JFrame fMarkersWindow;
    private JFrame fPrefsWindow;
    private JMenu fNetMenu;
    private JFrame fFrame;
    private JMenu fRecentFilesMenu;
    private TraceSettingsFile fTraceSettingsFile;
    private NetSearchView fNetSearchPane;

    private static void createAndShowGUI(String[] args) {
        final WaveApp contentPane = new WaveApp();
        JFrame frame = new JFrame("Waveform Viewer");
        contentPane.fFrame = frame;
        contentPane.buildMenus();

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

        if (args.length > 0)
            contentPane.loadTraceFile(args[0]);
    }

    public static void main(String[] args) {
        final String[] _args = args;
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI(_args);
            }
        });
    }
}


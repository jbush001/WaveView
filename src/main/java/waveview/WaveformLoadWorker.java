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

import java.io.File;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class WaveformLoadWorker extends SwingWorker<Void, Void> {
    public interface LoadFinishedHandler {
        // On success, error message will be null.
        // If an error occurs, errorMessage will be set.
        void handleLoadSuccess(WaveformDataModel newModel);
        void handleLoadError(String errorMessage);
    }

    private final File file;
    private final ProgressMonitor progressMonitor;
    private final WaveformDataModel newModel = new WaveformDataModel();
    private String errorMessage;
    private LoadFinishedHandler finishHandler;

    public WaveformLoadWorker(File file, ProgressMonitor progressMonitor, LoadFinishedHandler finishHandler) {
        this.file = file;
        this.progressMonitor = progressMonitor;
        this.finishHandler = finishHandler;
    }

    @Override
    public Void doInBackground() {
        /// @todo Determine the loader type dynamically
        try {
            WaveformLoader.ProgressListener progressListener = new WaveformLoader.ProgressListener() {
                @Override
                public boolean updateProgress(final int percentRead) {
                    // Accessing the component from a different thread, technically
                    // a no no, but probably okay.
                    if (progressMonitor.isCanceled())
                        return false;

                    SwingUtilities.invokeLater(() -> progressMonitor.setProgress(percentRead));

                    return true;
                }
            };

            Profiler profiler = new Profiler();
            profiler.start();
            new VCDLoader().load(file, newModel.startBuilding(), progressListener);
            profiler.finish();
            System.out.println("Loaded in " + profiler.getExecutionTime() + " ms");
            System.out.println("Allocated " + profiler.getMemoryAllocated() + " bytes of memory");
        } catch (Exception exc) {
            errorMessage = exc.getMessage();
        }

        return null;
    }

    // Executed on main thread
    @Override
    protected void done() {
        progressMonitor.close();
        if (errorMessage == null) {
            finishHandler.handleLoadSuccess(newModel);
        } else {
            finishHandler.handleLoadError(errorMessage);
        }
    }
}

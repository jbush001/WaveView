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
import java.io.IOException;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import waveview.wavedata.VcdLoader;
import waveview.wavedata.WaveformDataModel;
import waveview.wavedata.WaveformLoader;

///
/// Loads the waveform in a separate background thread so the UI
/// doesn't freeze. When this completes loading, it will call into
/// a handler. The handler is responsible for synchronization with the
/// main thread.
///
public final class WaveformLoadWorker extends SwingWorker<Void, Void> {
    private final File file;
    private final ProgressMonitor progressMonitor;
    private final WaveformDataModel newModel = new WaveformDataModel();
    private final LoadFinishedHandler finishHandler;
    private String errorMessage;

    public interface LoadFinishedHandler {
        // On success, error message will be null.
        // If an error occurs, errorMessage will be set.
        void handleLoadSuccess(WaveformDataModel newModel);
        void handleLoadError(String errorMessage);
    }

    public WaveformLoadWorker(
        File file, ProgressMonitor progressMonitor, LoadFinishedHandler finishHandler) {
        this.file = file;
        this.progressMonitor = progressMonitor;
        this.finishHandler = finishHandler;
    }

    @Override
    public Void doInBackground() {
        /// @todo Determine the loader type dynamically
        try {
            WaveformLoader.ProgressListener progressListener =
                new WaveformLoader.ProgressListener() {
                    @Override
                    public boolean updateProgress(final int percentRead) {
                        // Accessing the component from a different thread,
                        // technically a no no, but probably okay.
                        if (progressMonitor.isCanceled())
                            return false;

                        SwingUtilities.invokeLater(() -> progressMonitor.setProgress(percentRead));

                        return true;
                    }
                };

            Profiler profiler = new Profiler();
            profiler.start();
            new VcdLoader().load(file, newModel.startBuilding(), progressListener);
            profiler.finish();
            System.out.println("Loaded in " + profiler.getExecutionTime() + " ms");
            System.out.println("Allocated " + profiler.getMemoryAllocated() + " bytes of memory");
        } catch (IOException exc) {
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

//
// Copyright 2018 Jeff Bush
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

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import javax.swing.ProgressMonitor;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import waveview.WaveformDataModel;
import waveview.WaveformLoadWorker;

public class WaveformLoadWorkerTest {
    private static final int FINISH_TIMEOUT = 10000;

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private boolean loadFinished = false;
    private File tempFile;
    private WaveformDataModel newModel;
    private String errorMessage;

    class MockLoadFinishedHandler implements WaveformLoadWorker.LoadFinishedHandler {
        @Override
        public void handleLoadFinished(WaveformDataModel newModel, String errorMessage) {
            synchronized (WaveformLoadWorkerTest.this) {
                WaveformLoadWorkerTest.this.newModel = newModel;
                WaveformLoadWorkerTest.this.errorMessage = errorMessage;
                loadFinished = true;
                WaveformLoadWorkerTest.this.notifyAll();
            }
        }
    }

    @After
    public void cleanUpTest() {
        if (tempFile != null && tempFile.exists()) {
            try {
                tempFile.delete();
            } catch (SecurityException exc) {
                // Nothing to do...
            }
        }
    }

    private File makeVcdFile() throws IOException {
        assert tempFile == null;

        StringBuilder vcdContents = new StringBuilder();
        vcdContents.append("$timescale 1ns $end $scope module mod1 $end $var wire 1 A data $end $upscope $end $enddefinitions $end\n");
        for (int i = 0; i < 10000; i++) {
            vcdContents.append('#');
            vcdContents.append(i * 5);
            vcdContents.append("\n1A\n");
        }

        tempFile = tempFolder.newFile("test.vcd");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(vcdContents.toString().getBytes(StandardCharsets.US_ASCII));
        }

        return tempFile;
    }

    @Test
    public void testLoadSuccess() throws Exception {
        ProgressMonitor monitor = mock(ProgressMonitor.class);
        when(monitor.isCanceled()).thenReturn(false);
        doAnswer(new Answer<Void>() {
            int lastUpdate = -1;

            @Override
            public Void answer(InvocationOnMock invocation) {
                int percentRead = invocation.getArgument(0);
                assertTrue(percentRead >= lastUpdate);
                assertTrue(percentRead >= 0);
                assertTrue(percentRead <= 100);
                lastUpdate = percentRead;
                return null;
            }
        }).when(monitor).setProgress(anyInt());

        WaveformLoadWorker.LoadFinishedHandler finishHandler = new MockLoadFinishedHandler();
        File loadFile = makeVcdFile();
        WaveformLoadWorker worker = new WaveformLoadWorker(loadFile, monitor, finishHandler);
        worker.execute();
        waitUntilFinished();
        verify(monitor, atLeastOnce()).setProgress(anyInt());
        assertNotSame(null, newModel);
        assertSame(null, errorMessage);
    }

    @Test
    public void testLoadError() throws Exception {
        ProgressMonitor monitor = mock(ProgressMonitor.class);
        when(monitor.isCanceled()).thenReturn(false);
        File loadFile = new File("adsfhadkjhfakldshfasdfadsf"); // Shouldn't exist
        WaveformLoadWorker.LoadFinishedHandler finishHandler = new MockLoadFinishedHandler();
        WaveformLoadWorker worker = new WaveformLoadWorker(loadFile, monitor, finishHandler);
        worker.execute();
        waitUntilFinished();
        assertNotSame(null, errorMessage);
    }

    @Test
    public void testLoadCancelled() throws Exception {
        ProgressMonitor monitor = mock(ProgressMonitor.class);
        when(monitor.isCanceled()).thenReturn(true);
        File loadFile = makeVcdFile();
        WaveformLoadWorker.LoadFinishedHandler finishHandler = new MockLoadFinishedHandler();
        WaveformLoadWorker worker = new WaveformLoadWorker(loadFile, monitor, finishHandler);
        worker.execute();
        waitUntilFinished();
        assertNotSame(null, errorMessage);
    }

    void waitUntilFinished() throws TimeoutException {
        long expiration = System.currentTimeMillis() + FINISH_TIMEOUT;

        while (true) {
            synchronized (this) {
                if (loadFinished)
                    break;

                long now = System.currentTimeMillis();
                if (now > expiration) {
                    throw new TimeoutException("Timed out waiting for completion");
                }

                try {
                    wait(expiration - now);
                } catch (InterruptedException exc) {
                    // ignore, we will retry
                }
            }
        }
    }
}

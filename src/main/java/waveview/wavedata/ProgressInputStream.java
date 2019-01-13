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

package waveview.wavedata;

import java.io.IOException;
import java.io.InputStream;

public final class ProgressInputStream extends InputStream {
    private long totalRead;
    private long lastProgressUpdate;
    private final long updateInterval;
    private final InputStream wrapped;
    private final Listener listener;

    public interface Listener { void updateProgress(long totalRead) throws IOException; }

    public ProgressInputStream(InputStream wrapped, Listener listener, long updateInterval) {
        this.wrapped = wrapped;
        this.listener = listener;
        this.updateInterval = updateInterval;
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }

    @Override
    public int read() throws IOException {
        int got = wrapped.read();
        if (got != -1) {
            totalRead++;
            maybeNotifyListener();
        }

        return got;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int got = wrapped.read(b);
        if (got != -1) {
            totalRead += got;
            maybeNotifyListener();
        }

        return got;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int got = wrapped.read(b, off, len);
        if (got != -1) {
            totalRead += got;
            maybeNotifyListener();
        }

        return got;
    }

    public long getTotalRead() {
        return totalRead;
    }

    private void maybeNotifyListener() throws IOException {
        if (totalRead - lastProgressUpdate >= updateInterval) {
            listener.updateProgress(totalRead);
            lastProgressUpdate = totalRead;
        }
    }
}

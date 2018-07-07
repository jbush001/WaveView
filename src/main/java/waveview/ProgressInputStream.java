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

import java.io.IOException;
import java.io.InputStream;

class ProgressInputStream extends InputStream {
    private long totalRead;
    private final InputStream wrapped;

    ProgressInputStream(InputStream wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }

    @Override
    public int read() throws IOException {
        int got = wrapped.read();
        if (got >= 0) {
            totalRead++;
        }

        return got;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int got = wrapped.read(b);
        if (got >= 0) {
            totalRead += got;
        }

        return got;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int got = wrapped.read(b, off, len);
        if (got >= 0) {
            totalRead += got;
        }

        return got;
    }

    long getTotalRead() {
        return totalRead;
    }
}
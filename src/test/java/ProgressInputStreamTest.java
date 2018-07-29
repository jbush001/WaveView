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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import waveview.ProgressInputStream;

public class ProgressInputStreamTest {
    private final InputStream inputStream = mock(InputStream.class);
    private ProgressInputStream progressInputStream;
    private final Random random = new Random();

    @Before
    public void initTest() {
        progressInputStream = new ProgressInputStream(inputStream);
    }

    @Test
    public void testReadByte() throws IOException {
        when(inputStream.read()).thenReturn(17);
        assertEquals(17, progressInputStream.read());
        assertEquals(1, progressInputStream.getTotalRead());
    }

    @Test
    public void testReadArray() throws IOException {
        final int BYTES_REQUESTED = 15;
        final int BYTES_RETURNED = 12;

        byte[] testSource = new byte[BYTES_REQUESTED];
        random.nextBytes(testSource);

        when(inputStream.read(any(byte[].class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();

                // This will return less data than requested.
                System.arraycopy(testSource, 0, (byte[]) args[0], 0, BYTES_RETURNED);
                return BYTES_RETURNED;
            }
        });

        byte[] expected = new byte[testSource.length];
        System.arraycopy(testSource, 0, expected, 0, BYTES_RETURNED);
        byte[] testDest = new byte[BYTES_REQUESTED];
        assertEquals(BYTES_RETURNED, progressInputStream.read(testDest));
        assertArrayEquals(expected, testDest);
        assertEquals(BYTES_RETURNED, progressInputStream.getTotalRead());
    }

    @Test
    public void testReadArrayWithOffset() throws IOException {
        final int BYTES_REQUESTED = 13;
        final int BYTES_RETURNED = 10;
        final int REQUEST_OFFSET = 7;

        byte[] testSource = new byte[64];
        random.nextBytes(testSource);

        when(inputStream.read(any(byte[].class), anyInt(), anyInt())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                assertEquals(REQUEST_OFFSET, args[1]);   // Offset, see below
                assertEquals(BYTES_REQUESTED, args[2]);  // Length, also below

                // This will return less data than requested.
                System.arraycopy(testSource, 0, (byte[]) args[0], REQUEST_OFFSET, BYTES_RETURNED);
                return BYTES_RETURNED;
            }
        });

        byte[] expectedDest = new byte[REQUEST_OFFSET + BYTES_RETURNED];
        System.arraycopy(testSource, 0, expectedDest, REQUEST_OFFSET, BYTES_RETURNED);
        byte[] testDest = new byte[REQUEST_OFFSET + BYTES_RETURNED];
        assertEquals(BYTES_RETURNED, progressInputStream.read(testDest, REQUEST_OFFSET, BYTES_REQUESTED));
        assertArrayEquals(expectedDest, testDest);
        assertEquals(BYTES_RETURNED, progressInputStream.getTotalRead());
    }

    @Test
    public void close() throws IOException {
        progressInputStream.close();
        verify(inputStream).close();
    }

    // XXX return end of stream (negative count) from these methods, ensure the count is correct.
}

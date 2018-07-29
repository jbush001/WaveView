
//
// Copyright 2016 Jeff Bush
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
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.junit.Before;
import org.junit.Test;
import waveview.NetSearchListModelAdapter;
import waveview.WaveformDataModel;

public class NetSearchListModelAdapterTest {
    private final WaveformDataModel model = new WaveformDataModel();
    private ListDataListener listener;
    NetSearchListModelAdapter nslma;

    @Before
    public void setUpTest() {
        model.startBuilding()
            .enterScope("mod1")
            .newNet(0, "fooxxx", 1)
            .newNet(1, "xfooxx", 1)
            .newNet(2, "xxfoox", 1)
            .newNet(3, "xxxfoo", 1)
            .newNet(4, "bbbbb", 1)
            .newNet(5, "bbbb", 1)
            .newNet(6, "bbb", 1)
            .newNet(7, "bb", 1)
            .newNet(8, "yyy", 1)
            .exitScope();

        nslma = new NetSearchListModelAdapter(model);
        listener = mock(ListDataListener.class);
        nslma.addListDataListener(listener);
    }

    @Test
    public void defaultPattern() {
        assertEquals(9, nslma.getSize());
        assertEquals("mod1.fooxxx", nslma.getElementAt(0));
        assertEquals("mod1.xfooxx", nslma.getElementAt(1));
        assertEquals("mod1.xxfoox", nslma.getElementAt(2));
        assertEquals("mod1.xxxfoo", nslma.getElementAt(3));
        assertEquals("mod1.bbbbb", nslma.getElementAt(4));
        assertEquals("mod1.bbbb", nslma.getElementAt(5));
        assertEquals("mod1.bbb", nslma.getElementAt(6));
        assertEquals("mod1.bb", nslma.getElementAt(7));
        assertEquals("mod1.yyy", nslma.getElementAt(8));
    }

    @Test
    public void testPartialMatch1() {
        nslma.setPattern("foo");
        verify(listener).contentsChanged(refEq(new ListDataEvent(nslma,
                ListDataEvent.CONTENTS_CHANGED, 0, 4)));
        verifyNoMoreInteractions(listener);
        assertEquals(4, nslma.getSize());
        assertEquals("mod1.fooxxx", nslma.getElementAt(0));
        assertEquals("mod1.xfooxx", nslma.getElementAt(1));
        assertEquals("mod1.xxfoox", nslma.getElementAt(2));
        assertEquals("mod1.xxxfoo", nslma.getElementAt(3));
    }

    @Test
    public void testPartialMatch2() {
        nslma.setPattern("bbbb");
        verify(listener).contentsChanged(refEq(new ListDataEvent(nslma,
                ListDataEvent.CONTENTS_CHANGED, 0, 2)));
        verifyNoMoreInteractions(listener);
        assertEquals(2, nslma.getSize());
        assertEquals("mod1.bbbbb", nslma.getElementAt(0));
        assertEquals("mod1.bbbb", nslma.getElementAt(1));
    }

    @Test
    public void testNoMatch() {
        nslma.setPattern("z");
        verify(listener).contentsChanged(refEq(new ListDataEvent(nslma,
                ListDataEvent.CONTENTS_CHANGED, 0, 0)));
        verifyNoMoreInteractions(listener);
        assertEquals(0, nslma.getSize());
    }
}

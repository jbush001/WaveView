
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
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import waveview.NetSearchListModelAdapter;
import waveview.WaveformBuilder;
import waveview.WaveformDataModel;

public class NetSearchListModelAdapterTest {
    private final WaveformDataModel model = new WaveformDataModel();
    private ListDataListener listener;
    NetSearchListModelAdapter nslma;

    static class ListDataEventMatcher implements ArgumentMatcher<ListDataEvent> {
        private int type;
        private int index1;
        private Object source;

        ListDataEventMatcher(Object source, int type, int index1) {
            this.source = source;
            this.type = type;
            this.index1 = index1;
        }

        @Override
        public boolean matches(ListDataEvent event) {
            return event.getSource() == source
                && event.getType() == this.type
                && event.getIndex0() == 0
                && event.getIndex1() == this.index1;
        }

        @Override
        public String toString() {
            return "[" + this.type + ", 0, " + this.index1 + "]";
        }
    }

    @Before
    public void setUpTest() {
        WaveformBuilder builder = model.startBuilding();
        builder.enterScope("mod1");
        builder.newNet("fooxxx", -1, 1);
        builder.newNet("xfooxx", -1, 1);
        builder.newNet("xxfoox", -1, 1);
        builder.newNet("xxxfoo", -1, 1);
        builder.newNet("bbbbb", -1, 1);
        builder.newNet("bbbb", -1, 1);
        builder.newNet("bbb", -1, 1);
        builder.newNet("bb", -1, 1);
        builder.newNet("yyy", -1, 1);
        builder.exitScope();

        nslma = new NetSearchListModelAdapter(model);
        listener = spy(ListDataListener.class);
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
        verify(listener).contentsChanged(argThat(new ListDataEventMatcher(nslma,
                 ListDataEvent.CONTENTS_CHANGED, 4)));
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
        verify(listener).contentsChanged(argThat(new ListDataEventMatcher(nslma,
                ListDataEvent.CONTENTS_CHANGED, 2)));
        verifyNoMoreInteractions(listener);
        assertEquals(2, nslma.getSize());
        assertEquals("mod1.bbbbb", nslma.getElementAt(0));
        assertEquals("mod1.bbbb", nslma.getElementAt(1));
    }

    @Test
    public void testNoMatch() {
        nslma.setPattern("z");
        verify(listener).contentsChanged(argThat(new ListDataEventMatcher(nslma,
                ListDataEvent.CONTENTS_CHANGED, 0)));
        verifyNoMoreInteractions(listener);
        assertEquals(0, nslma.getSize());
    }
}

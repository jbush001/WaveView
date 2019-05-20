
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Test;
import waveview.wavedata.BitVector;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.NetTreeNode;
import waveview.wavedata.Transition;
import waveview.wavedata.WaveformDataModel;
import waveview.wavedata.WaveformDataModel.AmbiguousNetException;

public class WaveformDataModelTest {
    private final WaveformDataModel model = new WaveformDataModel();

    @Test
    public void buildWaveformDataModel() {
        model.startBuilding()
            .setTimescale(-9)
            .enterScope("mod1")
            .newNet(0, "net1", 1)
            .newNet(1, "net2", 3)
            .enterScope("mod2")
            .newNet(2, "net3", 2)
            .exitScope()
            .exitScope()
            .appendTransition(0, 10, new BitVector("1", 2))
            .appendTransition(0, 20, new BitVector("0", 2))
            .appendTransition(1, 10, new BitVector("100", 2))
            .appendTransition(1, 15, new BitVector("010", 2))
            .appendTransition(2, 12, new BitVector("11", 2))
            .appendTransition(2, 17, new BitVector("01", 2))
            .loadFinished();

        NetTreeNode root = model.getNetTree();
        assertEquals(3, root.getChildCount());
        NetTreeNode kid0 = root.getChild(0);
        NetTreeNode kid1 = root.getChild(1);
        NetTreeNode kid2 = root.getChild(2);
        NetTreeNode grandkid0 = kid2.getChild(0);

        assertEquals("net1", kid0.toString());
        assertTrue(kid0.isLeaf());
        assertEquals("net2", kid1.toString());
        assertTrue(kid1.isLeaf());
        assertEquals("mod2", kid2.toString());
        assertFalse(kid2.isLeaf());
        assertEquals("net3", grandkid0.toString());
        assertTrue(grandkid0.isLeaf());

        assertEquals(3, model.getTotalNetCount());
        assertEquals(20, model.getMaxTimestamp());

        NetDataModel netData1 = model.findNet("mod1.net1");
        NetDataModel netData2 = model.findNet("mod1.net2");
        NetDataModel netData3 = model.findNet("mod1.mod2.net3");
        assertNotNull(netData1);
        assertSame(netData1, model.getNetDataModel(0));
        assertNotNull(netData2);
        assertSame(netData2, model.getNetDataModel(1));
        assertNotNull(netData3);
        assertSame(netData3, model.getNetDataModel(2));

        // Make sure the nets in the tree match what was given during building
        assertSame(netData1, kid0.getNetDataModel());
        assertSame(netData2, kid1.getNetDataModel());
        assertSame(netData3, grandkid0.getNetDataModel());

        assertEquals("net1", netData1.getShortName());
        assertEquals("net2", netData2.getShortName());
        assertEquals("net3", netData3.getShortName());
        assertEquals("mod1.net1", netData1.getFullName());
        assertEquals("mod1.net2", netData2.getFullName());
        assertEquals("mod1.mod2.net3", netData3.getFullName());
        assertEquals(1, netData1.getWidth());
        assertEquals(3, netData2.getWidth());
        assertEquals(2, netData3.getWidth());

        Iterator<Transition> ati = netData2.findTransition(12);
        Transition t = ati.next();
        assertEquals(10, t.getTimestamp());
        assertEquals(0, t.compare(new BitVector("100", 2)));
    }

    @Test
    public void aliasNet() {
        model.startBuilding()
            .setTimescale(-9)
            .enterScope("mod1")
            .newNet(0, "net1", 1)
            .newNet(0, "net2", 1) // aliases net1
            .exitScope()
            .appendTransition(0, 17, new BitVector("1", 2))
            .loadFinished();

        NetTreeNode root = model.getNetTree();
        assertEquals(2, root.getChildCount());
        NetTreeNode kid0 = root.getChild(0);
        NetTreeNode kid1 = root.getChild(1);

        NetDataModel netData1 = model.getNetDataModel(0);
        NetDataModel netData2 = model.getNetDataModel(1);

        assertSame(netData1, kid0.getNetDataModel());
        assertSame(netData2, kid1.getNetDataModel());

        Iterator<Transition> ati = netData1.findTransition(0);
        assertEquals(17, ati.next().getTimestamp());

        // Same transition should be in this one (they share a TransitionVector)
        ati = netData2.findTransition(0);
        assertEquals(17, ati.next().getTimestamp());
    }

    // Regression test: when a net is aliased, it doesn't increment the net
    // index. Make sure WaveformDataModel doesn't get out of sync.
    @Test
    public void netAfterAlias() {
        model.startBuilding()
            .setTimescale(-9)
            .enterScope("mod1")
            .newNet(0, "net1", 1)
            .newNet(0, "net2", 1) // aliases net1
            .newNet(1, "net3", 1) // not alias
            .exitScope()
            .appendTransition(0, 17, new BitVector("1", 2))
            .appendTransition(1, 21, new BitVector("0", 2))
            .loadFinished();

        NetDataModel netData3 = model.getNetDataModel(2);

        Iterator<Transition> ati = netData3.findTransition(0);
        assertEquals(21, ati.next().getTimestamp());
    }

    @Test
    public void copyFrom() {
        model.startBuilding()
            .setTimescale(-9)
            .enterScope("mod1")
            .newNet(0, "net1", 1)
            .newNet(0, "net2", 1)
            .exitScope()
            .appendTransition(0, 17, new BitVector("1", 2))
            .loadFinished();

        WaveformDataModel model2 = new WaveformDataModel();
        model2.copyFrom(model);

        // Ensure timescale was copied
        assertEquals(-9, model2.getTimescale());

        // Ensure max timestamp was copied
        assertEquals(17, model2.getMaxTimestamp());

        // Ensure net data models were copied
        assertSame(model.getNetDataModel(0), model2.getNetDataModel(0));
        assertSame(model.getNetDataModel(1), model2.getNetDataModel(1));

        // Ensure net name map was copied
        assertSame(model.getNetDataModel(0), model2.findNet("mod1.net1"));
        assertSame(model.getNetDataModel(1), model2.findNet("mod1.net2"));

        // Ensure net tree was copied
        NetTreeNode root = model2.getNetTree();
        assertEquals(2, root.getChildCount());
        NetTreeNode kid0 = root.getChild(0);
        NetTreeNode kid1 = root.getChild(1);
        assertSame(model.getNetDataModel(0), kid0.getNetDataModel());
        assertSame(model.getNetDataModel(1), kid1.getNetDataModel());
    }

    @Test
    public void fuzzyMatch() throws AmbiguousNetException {
        WaveformDataModel waveformDataModel = new WaveformDataModel();
        waveformDataModel.startBuilding()
            .enterScope("aaaa")
            .enterScope("bbbbb")
            .newNet(0, "cc", 1)
            .newNet(1, "dd", 1)
            .newNet(2, "ee", 1)
            .newNet(3, "ff", 1)
            .exitScope()
            .enterScope("ddddd")
            .newNet(2, "ee", 1) // Alias of above net
            .newNet(4, "cc", 1) // Different net with same name
            .exitScope()
            .exitScope()
            .loadFinished();

        NetDataModel aaaaBbbbbCc = waveformDataModel.getNetDataModel(0);
        NetDataModel aaaaBbbbbDd = waveformDataModel.getNetDataModel(1);
        NetDataModel aaaaBbbbbEe = waveformDataModel.getNetDataModel(2);
        NetDataModel dddddCc = waveformDataModel.getNetDataModel(5);
        NetDataModel eeeeeFf = new NetDataModel("ff", "eeeee.ff", null);
        waveformDataModel.addDecodedNet(eeeeeFf);
        NetDataModel eeeeeGg = new NetDataModel("gg", "eeeee.gg", null);
        waveformDataModel.addDecodedNet(eeeeeGg);

        try {
            waveformDataModel.fuzzyFindNet("cc");
            fail("Did not throw exception");
        } catch (AmbiguousNetException exc) {
            // Expected
            assertEquals("Ambiguous net \"cc\"", exc.getMessage());
        }

        assertSame(aaaaBbbbbCc, waveformDataModel.fuzzyFindNet("bbbbb.cc"));
        assertSame(aaaaBbbbbCc, waveformDataModel.fuzzyFindNet("aaaa.bbbbb.cc"));
        assertSame(dddddCc, waveformDataModel.fuzzyFindNet("ddddd.cc"));
        assertSame(aaaaBbbbbDd, waveformDataModel.fuzzyFindNet("dd"));

        // Even though there are two matches for this, it isn't ambiguous, because
        // they are aliases.
        assertSame(aaaaBbbbbEe, waveformDataModel.fuzzyFindNet("ee"));

        // Find decoded net
        assertSame(eeeeeGg, waveformDataModel.fuzzyFindNet("gg"));

        // Ambiguous name in decoded nets collides with trace nets
        try {
            waveformDataModel.fuzzyFindNet("ff");
            fail("Did not throw exception");
        } catch (AmbiguousNetException exc) {
            // Expected
            assertEquals("Ambiguous net \"ff\"", exc.getMessage());
        }

        // Stem mismatch
        try {
            waveformDataModel.fuzzyFindNet("fffff.bbbbb.cc");
            fail("Did not throw exception");
        } catch (NoSuchElementException exc) {
            // Expected
            assertEquals("Unknown net \"fffff.bbbbb.cc\"", exc.getMessage());
        }

        // Should only match at dot boundary
        try {
            waveformDataModel.fuzzyFindNet("d");
            fail("Did not throw exception");
        } catch (NoSuchElementException exc) {
            // Expected
            assertEquals("Unknown net \"d\"", exc.getMessage());
        }

        try {
            waveformDataModel.fuzzyFindNet("xxx.aaaa.bbbbb.cc");
            fail("Did not throw exception");
        } catch (NoSuchElementException exc) {
            assertEquals("Unknown net \"xxx.aaaa.bbbbb.cc\"", exc.getMessage());
        }
    }

    @Test
    public void findNet() {
        WaveformDataModel waveformDataModel = new WaveformDataModel();
        waveformDataModel.startBuilding()
            .enterScope("mod1")
            .newNet(0, "foo", 1)
            .newNet(1, "bar", 1)
            .exitScope()
            .loadFinished();

        waveformDataModel.addDecodedNet(new NetDataModel("baz", "decoded.baz", null));
        waveformDataModel.addDecodedNet(new NetDataModel("floo", "decoded.floo", null));

        assertEquals("mod1.foo", waveformDataModel.findNet("mod1.foo").getFullName());
        assertEquals("decoded.baz", waveformDataModel.findNet("decoded.baz").getFullName());
        assertEquals("decoded.floo", waveformDataModel.findNet("decoded.floo").getFullName());
        assertSame(null, waveformDataModel.findNet("blargh"));
    }

    @Test
    public void generateDecodedName() {
        WaveformDataModel waveformDataModel = new WaveformDataModel();
        assertEquals("SPI_0", waveformDataModel.generateDecodedName("SPI"));
        assertEquals("UART_1", waveformDataModel.generateDecodedName("UART"));
    }
}

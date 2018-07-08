
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
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import waveview.BitVector;
import waveview.NetDataModel;
import waveview.NetTreeModel;
import waveview.WaveformBuilder;
import waveview.WaveformDataModel;
import waveview.Transition;

public class WaveformDataModelTest {
    private final WaveformDataModel model = new WaveformDataModel();
    private WaveformBuilder builder;

    @Before
    public void initTest() {
        builder = model.startBuilding();
    }

    @Test
    public void buildWaveformDataModel() {
        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int net1 = builder.newNet("net1", -1, 1);
        int net2 = builder.newNet("net2", -1, 3);
        builder.enterScope("mod2");
        int net3 = builder.newNet("net3", -1, 2);
        builder.exitScope();
        builder.exitScope();
        builder.appendTransition(net1, 10, new BitVector("1", 2));
        builder.appendTransition(net1, 20, new BitVector("0", 2));
        builder.appendTransition(net2, 10, new BitVector("100", 2));
        builder.appendTransition(net2, 15, new BitVector("010", 2));
        builder.appendTransition(net3, 12, new BitVector("11", 2));
        builder.appendTransition(net3, 17, new BitVector("01", 2));
        builder.loadFinished();

        NetTreeModel netTree = model.getNetTree();
        Object root = netTree.getRoot();
        assertEquals(3, netTree.getChildCount(root));
        Object kid0 = netTree.getChild(root, 0);
        Object kid1 = netTree.getChild(root, 1);
        Object kid2 = netTree.getChild(root, 2);
        Object grandkid0 = netTree.getChild(kid2, 0);

        assertEquals("net1", kid0.toString());
        assertTrue(netTree.isLeaf(kid0));
        assertEquals("net2", kid1.toString());
        assertTrue(netTree.isLeaf(kid1));
        assertEquals("mod2", kid2.toString());
        assertFalse(netTree.isLeaf(kid2));
        assertEquals("net3", grandkid0.toString());
        assertTrue(netTree.isLeaf(grandkid0));

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
        assertSame(netData1, model.getNetFromTreeObject(kid0));
        assertSame(netData2, model.getNetFromTreeObject(kid1));
        assertSame(netData3, model.getNetFromTreeObject(grandkid0));

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
        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int net1 = builder.newNet("net1", -1, 1);
        builder.newNet("net2", net1, 1);    // aliases net1
        builder.exitScope();
        builder.appendTransition(net1, 17, new BitVector("1", 2));
        builder.loadFinished();

        NetTreeModel netTree = model.getNetTree();
        Object root = netTree.getRoot();
        assertEquals(2, netTree.getChildCount(root));
        Object kid0 = netTree.getChild(root, 0);
        Object kid1 = netTree.getChild(root, 1);

        NetDataModel netData1 = model.getNetDataModel(0);
        NetDataModel netData2 = model.getNetDataModel(1);

        assertSame(netData1, model.getNetFromTreeObject(kid0));
        assertSame(netData2, model.getNetFromTreeObject(kid1));

        Iterator<Transition> ati = netData1.findTransition(0);
        assertEquals(17, ati.next().getTimestamp());

        // Same transition should be in this one (they share a TransitionVector)
        ati = netData2.findTransition(0);
        assertEquals(17, ati.next().getTimestamp());
    }

    @Test
    public void copyFrom() {
        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int net1 = builder.newNet("net1", -1, 1);
        builder.newNet("net2", net1, 1);
        builder.exitScope();
        builder.appendTransition(net1, 17, new BitVector("1", 2));
        builder.loadFinished();

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
        NetTreeModel netTree = model2.getNetTree();
        Object root = netTree.getRoot();
        assertEquals(2, netTree.getChildCount(root));
        Object kid0 = netTree.getChild(root, 0);
        Object kid1 = netTree.getChild(root, 1);
        assertSame(model.getNetDataModel(0), model2.getNetFromTreeObject(kid0));
        assertSame(model.getNetDataModel(1), model2.getNetFromTreeObject(kid1));
    }
}

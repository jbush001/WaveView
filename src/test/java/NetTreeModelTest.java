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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import waveview.NetDataModel;
import waveview.NetTreeModel;
import waveview.TransitionVector;

public class NetTreeModelTest {
    @Test
    public void lookup() {
        NetDataModel child1 = new NetDataModel("child1", "scope1.child1", new TransitionVector(1));
        NetDataModel child2 = new NetDataModel("child2", "scope1.child2", new TransitionVector(1));
        NetDataModel child3 = new NetDataModel("child3", "scope1.scope2.child3", new TransitionVector(1));
        NetDataModel child4 = new NetDataModel("child4", "scope1.scope2.child4", new TransitionVector(1));
        NetDataModel child5 = new NetDataModel("child5", "scope1.child5", new TransitionVector(1));

        NetTreeModel model = new NetTreeModel();
        NetTreeModel.Builder builder = model.startBuilding();
        builder.enterScope("scope1");
        builder.addNet(child1);
        builder.addNet(child2);
        builder.enterScope("scope2");
        builder.addNet(child3);
        builder.addNet(child4);
        builder.leaveScope();
        builder.addNet(child5);

        Object root = model.getRoot();
        assertEquals(4, model.getChildCount(root));
        Object kid0 = model.getChild(root, 0);
        Object kid1 = model.getChild(root, 1);
        Object kid2 = model.getChild(root, 2);
        Object kid3 = model.getChild(root, 3);

        Object grandkid0 = model.getChild(kid2, 0);
        Object grandkid1 = model.getChild(kid2, 1);

        assertEquals("child1", kid0.toString());
        assertEquals(0, model.getChildCount(kid0));
        assertTrue(model.isLeaf(kid0));
        assertEquals(0, model.getIndexOfChild(root, kid0));
        assertSame(child1, model.getNetFromTreeObject(kid0));

        assertEquals("child2", kid1.toString());
        assertEquals(0, model.getChildCount(kid1));
        assertTrue(model.isLeaf(kid1));
        assertEquals(1, model.getIndexOfChild(root, kid1));
        assertSame(child2, model.getNetFromTreeObject(kid1));

        assertEquals("scope2", kid2.toString());
        assertEquals(2, model.getChildCount(kid2));
        assertFalse(model.isLeaf(kid2));
        assertEquals(2, model.getIndexOfChild(root, kid2));

        assertEquals("child5", kid3.toString());
        assertEquals(0, model.getChildCount(kid3));
        assertTrue(model.isLeaf(kid3));
        assertEquals(3, model.getIndexOfChild(root, kid3));
        assertSame(child5, model.getNetFromTreeObject(kid3));

        assertEquals("child3", grandkid0.toString());
        assertEquals(0, model.getChildCount(grandkid0));
        assertTrue(model.isLeaf(grandkid0));
        assertEquals(0, model.getIndexOfChild(kid2, grandkid0));
        assertSame(child3, model.getNetFromTreeObject(grandkid0));

        assertEquals("child4", grandkid1.toString());
        assertEquals(0, model.getChildCount(grandkid1));
        assertTrue(model.isLeaf(grandkid1));
        assertEquals(1, model.getIndexOfChild(kid2, grandkid1));
        assertSame(child4, model.getNetFromTreeObject(grandkid1));
    }

    @Test
    public void valueForPathChanged() {
        NetTreeModel model = new NetTreeModel();
        try {
            model.valueForPathChanged(null, null);
            fail("Didn't throw exception");
        } catch (UnsupportedOperationException exc) {
        }
    }

    // If you call $dumpvars more than once in iverilog, it will
    // exit the root node and re-push it. Ensure this is handled properly.
    @Test
    public void doubleRoot() {
        NetDataModel child1 = new NetDataModel("child1", "scope1.child1", new TransitionVector(1));

        NetTreeModel model = new NetTreeModel();
        NetTreeModel.Builder builder = model.startBuilding();
        builder.enterScope("scope1");
        builder.leaveScope();
        builder.enterScope("scope1");
        builder.addNet(child1);
        builder.leaveScope();

        Object root = model.getRoot();
        assertEquals(1, model.getChildCount(root));
        Object kid0 = model.getChild(root, 0);
        assertEquals("child1", kid0.toString());
    }
}

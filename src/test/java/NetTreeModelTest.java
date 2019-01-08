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
import org.junit.Test;
import waveview.wavedata.NetTreeNode;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.TransitionVector;

public class NetTreeModelTest {
    @Test
    public void lookup() {
        NetDataModel child1 = new NetDataModel("child1", "scope1.child1", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel child2 = new NetDataModel("child2", "scope1.child2", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel child3 = new NetDataModel("child3", "scope1.scope2.child3", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel child4 = new NetDataModel("child4", "scope1.scope2.child4", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetDataModel child5 = new NetDataModel("child5", "scope1.child5", TransitionVector.Builder.createBuilder(1).getTransitionVector());

        NetTreeNode.Builder builder = new NetTreeNode.Builder();
        builder.enterScope("scope1");
        builder.addNet(child1);
        builder.addNet(child2);
        builder.enterScope("scope2");
        builder.addNet(child3);
        builder.addNet(child4);
        builder.leaveScope();
        builder.addNet(child5);

        NetTreeNode root = builder.getRoot();
        assertEquals(4, root.getChildCount());
        NetTreeNode kid0 = root.getChild(0);
        NetTreeNode kid1 = root.getChild(1);
        NetTreeNode kid2 = root.getChild(2);
        NetTreeNode kid3 = root.getChild(3);

        NetTreeNode grandkid0 = kid2.getChild(0);
        NetTreeNode grandkid1 = kid2.getChild(1);

        assertEquals("child1", kid0.toString());
        assertEquals(0, kid0.getChildCount());
        assertTrue(kid0.isLeaf());
        assertEquals(0, root.getIndexOfChild(kid0));
        assertSame(child1, kid0.getNetDataModel());

        assertEquals("child2", kid1.toString());
        assertEquals(0, kid1.getChildCount());
        assertTrue(kid1.isLeaf());
        assertEquals(1, root.getIndexOfChild(kid1));
        assertSame(child2, kid1.getNetDataModel());

        assertEquals("scope2", kid2.toString());
        assertEquals(2, kid2.getChildCount());
        assertFalse(kid2.isLeaf());
        assertEquals(2, root.getIndexOfChild(kid2));

        assertEquals("child5", kid3.toString());
        assertEquals(0, kid3.getChildCount());
        assertTrue(kid3.isLeaf());
        assertEquals(3, root.getIndexOfChild(kid3));
        assertSame(child5, kid3.getNetDataModel());

        assertEquals("child3", grandkid0.toString());
        assertEquals(0, grandkid0.getChildCount());
        assertTrue(grandkid0.isLeaf());
        assertEquals(0, kid2.getIndexOfChild(grandkid0));
        assertSame(child3, grandkid0.getNetDataModel());

        assertEquals("child4", grandkid1.toString());
        assertEquals(0, grandkid1.getChildCount());
        assertTrue(grandkid1.isLeaf());
        assertEquals(1, kid2.getIndexOfChild(grandkid1));
        assertSame(child4, grandkid1.getNetDataModel());
    }

    // If you call $dumpvars more than once in iverilog, it will
    // exit the root node and re-push it. Ensure this is handled properly.
    @Test
    public void doubleRoot() {
        NetDataModel child1 = new NetDataModel("child1", "scope1.child1", TransitionVector.Builder.createBuilder(1).getTransitionVector());
        NetTreeNode.Builder builder = new NetTreeNode.Builder();
        builder.enterScope("scope1");
        builder.leaveScope();
        builder.enterScope("scope1");
        builder.addNet(child1);
        builder.leaveScope();

        NetTreeNode root = builder.getRoot();
        assertEquals(1, root.getChildCount());
        NetTreeNode kid0 = root.getChild(0);
        assertEquals("child1", kid0.toString());
    }
}

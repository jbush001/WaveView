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

import static org.junit.Assert.*;
import org.junit.*;

public class NetTreeModelTest
{
    @Test public void test1()
    {
        NetTreeModel model = new NetTreeModel();
        model.enterScope("scope1");
        model.addNet("child1", 17);
        model.addNet("child2", 19);
        model.enterScope("scope2");
        model.addNet("child3", 23);
        model.addNet("child4", 27);
        model.leaveScope();
        model.addNet("child5", 31);

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
        assertEquals(17, model.getNetFromTreeObject(kid0));

        assertEquals("child2", kid1.toString());
        assertEquals(0, model.getChildCount(kid1));
        assertTrue(model.isLeaf(kid1));
        assertEquals(1, model.getIndexOfChild(root, kid1));
        assertEquals(19, model.getNetFromTreeObject(kid1));

        assertEquals("scope2", kid2.toString());
        assertEquals(2, model.getChildCount(kid2));
        assertFalse(model.isLeaf(kid2));
        assertEquals(2, model.getIndexOfChild(root, kid2));

        assertEquals("child5", kid3.toString());
        assertEquals(0, model.getChildCount(kid3));
        assertTrue(model.isLeaf(kid3));
        assertEquals(3, model.getIndexOfChild(root, kid3));
        assertEquals(31, model.getNetFromTreeObject(kid3));

        assertEquals("child3", grandkid0.toString());
        assertEquals(0, model.getChildCount(grandkid0));
        assertTrue(model.isLeaf(grandkid0));
        assertEquals(0, model.getIndexOfChild(kid2, grandkid0));
        assertEquals(23, model.getNetFromTreeObject(grandkid0));

        assertEquals("child4", grandkid1.toString());
        assertEquals(0, model.getChildCount(grandkid1));
        assertTrue(model.isLeaf(grandkid1));
        assertEquals(1, model.getIndexOfChild(kid2, grandkid1));
        assertEquals(27, model.getNetFromTreeObject(grandkid1));
    }
}
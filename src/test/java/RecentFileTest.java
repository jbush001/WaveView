//
// Copyright 2017 Jeff Bush
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

import waveview.*;
import static org.junit.Assert.*;
import org.junit.*;
import java.util.ArrayList;

public class RecentFileTest {

    @Test
    public void testLru() {
        RecentFiles files = new RecentFiles();

        // Add 10 items.
        files.add("a");
        files.add("b");
        files.add("c");
        files.add("d");
        files.add("e");
        files.add("f");
        files.add("g");
        files.add("h");
        files.add("i");
        files.add("j");

        ArrayList<String> list = files.getList();
        assertEquals(10, list.size());
        assertEquals("j", list.get(0));
        assertEquals("i", list.get(1));
        assertEquals("h", list.get(2));
        assertEquals("g", list.get(3));
        assertEquals("f", list.get(4));
        assertEquals("e", list.get(5));
        assertEquals("d", list.get(6));
        assertEquals("c", list.get(7));
        assertEquals("b", list.get(8));
        assertEquals("a", list.get(9));

        // Add an item that doesn't exist yet. This should kick an older
        // item out.
        files.add("k");

        list = files.getList();
        assertEquals(10, list.size());
        assertEquals("k", list.get(0));
        assertEquals("j", list.get(1));
        assertEquals("i", list.get(2));
        assertEquals("h", list.get(3));
        assertEquals("g", list.get(4));
        assertEquals("f", list.get(5));
        assertEquals("e", list.get(6));
        assertEquals("d", list.get(7));
        assertEquals("c", list.get(8));
        assertEquals("b", list.get(9));

        // Try to add existing item. Should move to the most recently
        // used slot

        files.add("g");

        list = files.getList();
        assertEquals(10, list.size());
        assertEquals("g", list.get(0));
        assertEquals("k", list.get(1));
        assertEquals("j", list.get(2));
        assertEquals("i", list.get(3));
        assertEquals("h", list.get(4));
        assertEquals("f", list.get(5));
        assertEquals("e", list.get(6));
        assertEquals("d", list.get(7));
        assertEquals("c", list.get(8));
        assertEquals("b", list.get(9));
    }

    @Test
    public void testPack() {
        RecentFiles files = new RecentFiles();
        files.add("a");
        files.add("bb");
        files.add("ccc");
        files.add("dd/dd");
        files.add("eeeee");
        files.add("ff/ffff");
        files.add("ggggg");
        files.add("hhhh");
        files.add("iii");
        files.add("jj");

        assertEquals("jj;iii;hhhh;ggggg;ff/ffff;eeeee;dd/dd;ccc;bb;a", files.pack());
    }

    @Test
    public void testUnpack() {
        RecentFiles files = new RecentFiles();
        files.unpack("jj;iii;hhhh;ggggg;ff/ffff;eeeee;dd/dd;ccc;bb;a");

        ArrayList<String> list = files.getList();
        assertEquals("jj", list.get(0));
        assertEquals("iii", list.get(1));
        assertEquals("hhhh", list.get(2));
        assertEquals("ggggg", list.get(3));
        assertEquals("ff/ffff", list.get(4));
        assertEquals("eeeee", list.get(5));
        assertEquals("dd/dd", list.get(6));
        assertEquals("ccc", list.get(7));
        assertEquals("bb", list.get(8));
        assertEquals("a", list.get(9));
    }
}

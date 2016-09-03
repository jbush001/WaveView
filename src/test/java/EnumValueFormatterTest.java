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
import java.io.File;
import java.io.IOException;

public class EnumValueFormatterTest {
    File getTestFile(String name) {
        return new File("src/test/resources/enum_mapping/" + name);
    }

    @Test
    public void testFormat() throws IOException {
        EnumValueFormatter vf = new EnumValueFormatter();
        vf.loadFromFile(getTestFile("test1.txt"));
        assertEquals("STATE_INIT", vf.format(new BitVector("1", 10)));
        assertEquals("STATE_LOAD", vf.format(new BitVector("2", 10)));
        assertEquals("STATE_WAIT", vf.format(new BitVector("3", 10)));
        assertEquals("??? (4)", vf.format(new BitVector("4", 10)));
    }
}

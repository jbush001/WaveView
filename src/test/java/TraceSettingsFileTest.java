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
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;

public class TraceSettingsFileTest {
    @Rule
    public TemporaryFolder fTempFolder = new TemporaryFolder();

    @Test
    public void testLoad() throws Exception {
        TraceDataModel dataModel = new TraceDataModel();
        TraceViewModel sourceViewModel = new TraceViewModel();

        TraceBuilder builder = dataModel.startBuilding();
        builder.enterModule("mod1");
        builder.newNet("net1", -1, 1);
        builder.newNet("net2", -1, 1);
        builder.enterModule("mod2");
        builder.newNet("net3", -1, 1);
        builder.newNet("net4", -1, 1);
        builder.enterModule("mod3");
        builder.newNet("net5", -1, 1);
        builder.newNet("net6", -1, 1);
        builder.exitModule();
        builder.newNet("net7", -1, 1);
        builder.exitModule();
        builder.newNet("net8", -1, 1);
        builder.exitModule();

        // Populate sourceViewModel
        sourceViewModel.makeNetVisible(1);
        sourceViewModel.makeNetVisible(2);
        sourceViewModel.saveNetSet("set1");

        sourceViewModel.removeAllNets();
        sourceViewModel.makeNetVisible(6);
        sourceViewModel.makeNetVisible(7);
        sourceViewModel.saveNetSet("set2");

        sourceViewModel.removeAllNets();
        sourceViewModel.makeNetVisible(0);
        sourceViewModel.makeNetVisible(2);
        sourceViewModel.makeNetVisible(4);
        sourceViewModel.makeNetVisible(5);

        sourceViewModel.setValueFormatter(0, new BinaryValueFormatter());
        sourceViewModel.setValueFormatter(1, new DecimalValueFormatter());
        sourceViewModel.setValueFormatter(2, new HexadecimalValueFormatter());
        EnumValueFormatter enumFormatter = new EnumValueFormatter();
        enumFormatter.addMapping(0, "STATE_INIT");
        enumFormatter.addMapping(1, "STATE_LOAD");
        enumFormatter.addMapping(2, "STATE_STORE");
        sourceViewModel.setValueFormatter(3, enumFormatter);

        sourceViewModel.addMarker("marker1", 1234);
        sourceViewModel.addMarker("marker2", 5678);
        assertEquals(1, sourceViewModel.getIdForMarker(0));
        assertEquals(2, sourceViewModel.getIdForMarker(1));

        // Save and reload contents
        TraceViewModel destViewModel = new TraceViewModel();
        File file = fTempFolder.newFile("test.vcd");
        (new TraceSettingsFile(file, dataModel, sourceViewModel)).write();
        (new TraceSettingsFile(file, dataModel, destViewModel)).read();

        // Check destViewModel
        assertEquals(4, destViewModel.getVisibleNetCount());
        assertEquals(0, destViewModel.getVisibleNet(0));
        assertEquals(2, destViewModel.getVisibleNet(1));
        assertEquals(4, destViewModel.getVisibleNet(2));
        assertEquals(5, destViewModel.getVisibleNet(3));

        assertTrue(destViewModel.getValueFormatter(0) instanceof BinaryValueFormatter);
        assertTrue(destViewModel.getValueFormatter(1) instanceof DecimalValueFormatter);
        assertTrue(destViewModel.getValueFormatter(2) instanceof HexadecimalValueFormatter);
        enumFormatter = (EnumValueFormatter) destViewModel.getValueFormatter(3);
        assertTrue(enumFormatter instanceof EnumValueFormatter);
        assertEquals(3, enumFormatter.getMappingCount());
        assertEquals("STATE_INIT", enumFormatter.getNameByIndex(0));
        assertEquals("STATE_LOAD", enumFormatter.getNameByIndex(1));
        assertEquals("STATE_STORE", enumFormatter.getNameByIndex(2));

        assertEquals(2, destViewModel.getNetSetCount());
        assertEquals("set1", destViewModel.getNetSetName(0));
        assertEquals("set2", destViewModel.getNetSetName(1));

        destViewModel.selectNetSet(0);
        assertEquals(2, destViewModel.getVisibleNetCount());
        assertEquals(1, destViewModel.getVisibleNet(0));
        assertEquals(2, destViewModel.getVisibleNet(1));

        destViewModel.selectNetSet(1);
        assertEquals(2, destViewModel.getVisibleNetCount());
        assertEquals(6, destViewModel.getVisibleNet(0));
        assertEquals(7, destViewModel.getVisibleNet(1));

        assertEquals(2, destViewModel.getMarkerCount());
        assertEquals("marker1", destViewModel.getDescriptionForMarker(0));
        assertEquals("marker2", destViewModel.getDescriptionForMarker(1));
        assertEquals(1234, destViewModel.getTimestampForMarker(0));
        assertEquals(5678, destViewModel.getTimestampForMarker(1));
        assertEquals(1, destViewModel.getIdForMarker(0));
        assertEquals(2, destViewModel.getIdForMarker(1));
    }


}
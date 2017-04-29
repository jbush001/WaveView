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

import waveview.*;
import static org.junit.Assert.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.File;

public class TraceSettingsFileTest {
    @Rule
    public TemporaryFolder fTempFolder = new TemporaryFolder();

    @Test
    public void testLoad() throws Exception {
        TraceDataModel dataModel = new TraceDataModel();
        TraceDisplayModel sourceDisplayModel = new TraceDisplayModel();

        TraceBuilder builder = dataModel.startBuilding();
        builder.enterScope("mod1");
        builder.newNet("net1", -1, 1);
        builder.newNet("net2", -1, 1);
        builder.enterScope("mod2");
        builder.newNet("net3", -1, 1);
        builder.newNet("net4", -1, 1);
        builder.enterScope("mod3");
        builder.newNet("net5", -1, 1);
        builder.newNet("net6", -1, 1);
        builder.exitScope();
        builder.newNet("net7", -1, 1);
        builder.exitScope();
        builder.newNet("net8", -1, 1);
        builder.exitScope();
        builder.loadFinished();

        // Populate sourceDisplayModel
        sourceDisplayModel.makeNetVisible(1);
        sourceDisplayModel.makeNetVisible(2);
        sourceDisplayModel.saveNetSet("set1");

        sourceDisplayModel.removeAllNets();
        sourceDisplayModel.makeNetVisible(6);
        sourceDisplayModel.makeNetVisible(7);
        sourceDisplayModel.saveNetSet("set2");

        sourceDisplayModel.removeAllNets();
        sourceDisplayModel.makeNetVisible(0);
        sourceDisplayModel.makeNetVisible(2);
        sourceDisplayModel.makeNetVisible(4);
        sourceDisplayModel.makeNetVisible(5);

        sourceDisplayModel.setValueFormatter(0, new BinaryValueFormatter());
        sourceDisplayModel.setValueFormatter(1, new DecimalValueFormatter());
        sourceDisplayModel.setValueFormatter(2, new HexadecimalValueFormatter());
        EnumValueFormatter enumFormatter = new EnumValueFormatter();
        enumFormatter.loadFromFile(new File("src/test/resources/enum_mapping/test1.txt"));
        sourceDisplayModel.setValueFormatter(3, enumFormatter);

        sourceDisplayModel.addMarker("marker1", 1234);
        sourceDisplayModel.addMarker("marker2", 5678);
        assertEquals(1, sourceDisplayModel.getIdForMarker(0));
        assertEquals(2, sourceDisplayModel.getIdForMarker(1));

        sourceDisplayModel.setHorizontalScale(123.0);

        // Save and reload contents
        TraceDisplayModel destDisplayModel = new TraceDisplayModel();
        File file = fTempFolder.newFile("test1.settings");
        (new TraceSettingsFile(file, dataModel, sourceDisplayModel)).write();
        (new TraceSettingsFile(file, dataModel, destDisplayModel)).read();

        // Check destDisplayModel
        assertEquals(4, destDisplayModel.getVisibleNetCount());
        assertEquals(0, destDisplayModel.getVisibleNet(0));
        assertEquals(2, destDisplayModel.getVisibleNet(1));
        assertEquals(4, destDisplayModel.getVisibleNet(2));
        assertEquals(5, destDisplayModel.getVisibleNet(3));

        assertTrue(destDisplayModel.getValueFormatter(0) instanceof BinaryValueFormatter);
        assertTrue(destDisplayModel.getValueFormatter(1) instanceof DecimalValueFormatter);
        assertTrue(destDisplayModel.getValueFormatter(2) instanceof HexadecimalValueFormatter);
        assertTrue(destDisplayModel.getValueFormatter(3) instanceof EnumValueFormatter);
        enumFormatter = (EnumValueFormatter) destDisplayModel.getValueFormatter(3);
        assertEquals("STATE_INIT", enumFormatter.format(new BitVector("1", 10)));
        assertEquals("STATE_LOAD", enumFormatter.format(new BitVector("2", 10)));
        assertEquals("STATE_WAIT", enumFormatter.format(new BitVector("3", 10)));

        assertEquals(2, destDisplayModel.getNetSetCount());
        assertEquals("set1", destDisplayModel.getNetSetName(0));
        assertEquals("set2", destDisplayModel.getNetSetName(1));

        destDisplayModel.selectNetSet(0);
        assertEquals(2, destDisplayModel.getVisibleNetCount());
        assertEquals(1, destDisplayModel.getVisibleNet(0));
        assertEquals(2, destDisplayModel.getVisibleNet(1));

        destDisplayModel.selectNetSet(1);
        assertEquals(2, destDisplayModel.getVisibleNetCount());
        assertEquals(6, destDisplayModel.getVisibleNet(0));
        assertEquals(7, destDisplayModel.getVisibleNet(1));

        assertEquals(2, destDisplayModel.getMarkerCount());
        assertEquals("marker1", destDisplayModel.getDescriptionForMarker(0));
        assertEquals("marker2", destDisplayModel.getDescriptionForMarker(1));
        assertEquals(1234, destDisplayModel.getTimestampForMarker(0));
        assertEquals(5678, destDisplayModel.getTimestampForMarker(1));
        assertEquals(1, destDisplayModel.getIdForMarker(0));
        assertEquals(2, destDisplayModel.getIdForMarker(1));

        assertEquals(123.0, sourceDisplayModel.getHorizontalScale(), 0.001);
    }

    // When the data model changes, ensure the loader falls back
    // gracefully. Specifically if a visible net is no longer in
    // the data model after it is reloaded
    // @todo Does not test when markers are put in past the end time.
    //  (not currently implemented in loader)
    @Test
    public void testDataModelChanged() throws Exception {
        TraceDataModel sourceDataModel = new TraceDataModel();
        TraceDisplayModel sourceDisplayModel = new TraceDisplayModel();
        TraceBuilder builder1 = sourceDataModel.startBuilding();
        builder1.enterScope("mod1");
        builder1.newNet("net1", -1, 1);
        builder1.newNet("net2", -1, 1);
        builder1.newNet("net3", -1, 1);
        builder1.exitScope();
        builder1.loadFinished();
        sourceDisplayModel.makeNetVisible(0);
        sourceDisplayModel.makeNetVisible(1);
        sourceDisplayModel.makeNetVisible(2);

        TraceDataModel destDataModel = new TraceDataModel();
        TraceDisplayModel destDisplayModel = new TraceDisplayModel();

        TraceBuilder builder2 = destDataModel.startBuilding();
        builder2.enterScope("mod1");
        builder2.newNet("net1", -1, 1);
        builder2.newNet("net4", -1, 1);
        builder2.newNet("net3", -1, 1);
        builder2.exitScope();
        builder2.loadFinished();

        File file = fTempFolder.newFile("test2.settings");
        (new TraceSettingsFile(file, sourceDataModel, sourceDisplayModel)).write();
        (new TraceSettingsFile(file, destDataModel, destDisplayModel)).read();

        assertEquals(2, destDisplayModel.getVisibleNetCount());
        assertEquals(0, destDisplayModel.getVisibleNet(0));
        assertEquals(2, destDisplayModel.getVisibleNet(1));
    }

    // If the formatter class name is unknown, fall back to binary
    @Test
    public void testBadFormatter() throws Exception {
        File file = new File("src/test/resources/trace_settings/bad_formatter.traceconfig");
        TraceDataModel dataModel = new TraceDataModel();
        TraceBuilder builder = dataModel.startBuilding();
        builder.enterScope("foo");
        builder.newNet("bar", -1, 1);
        builder.exitScope();
        builder.loadFinished();

        TraceDisplayModel displayModel = new TraceDisplayModel();
        (new TraceSettingsFile(file, dataModel, displayModel)).read();
        assertTrue(displayModel.getValueFormatter(0) instanceof BinaryValueFormatter);
    }

    // Test generating config file name for subdirectory
    @Test
    public void testConfigFileName1() throws Exception {
        assertEquals("foo/bar/.trace.vcd.traceconfig",
            TraceSettingsFile.settingsFileName(new File(
            "foo/bar/trace.vcd")).toString());
    }

    /// Regression test. In this case, the full path isn't passed.
    /// It was putting 'null' inside the filename.
    @Test
    public void testConfigFileName2() throws Exception {
        assertEquals(".trace.vcd.traceconfig",
            TraceSettingsFile.settingsFileName(new File(
            "trace.vcd")).toString());
    }
}

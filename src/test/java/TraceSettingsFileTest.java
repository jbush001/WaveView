
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import waveview.BinaryValueFormatter;
import waveview.BitVector;
import waveview.DecimalValueFormatter;
import waveview.EnumValueFormatter;
import waveview.HexadecimalValueFormatter;
import waveview.NetDataModel;
import waveview.TraceBuilder;
import waveview.TraceDataModel;
import waveview.TracePresentationModel;
import waveview.TraceSettingsFile;

public class TraceSettingsFileTest {
    @Rule
    public final TemporaryFolder fTempFolder = new TemporaryFolder();

    @Test
    public void load() throws Exception {
        TraceDataModel dataModel = new TraceDataModel();
        TracePresentationModel sourcePresentationModel = new TracePresentationModel();

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

        NetDataModel netDataModels[] = new NetDataModel[8];
        int index = 0;
        for (NetDataModel model : dataModel) {
            netDataModels[index++] = model;
        }

        // Populate sourcePresentationModel
        sourcePresentationModel.addNet(netDataModels[1]);
        sourcePresentationModel.addNet(netDataModels[2]);
        sourcePresentationModel.saveNetSet("set1");

        sourcePresentationModel.removeAllNets();
        sourcePresentationModel.addNet(netDataModels[6]);
        sourcePresentationModel.addNet(netDataModels[7]);
        sourcePresentationModel.saveNetSet("set2");

        sourcePresentationModel.removeAllNets();
        sourcePresentationModel.addNet(netDataModels[0]);
        sourcePresentationModel.addNet(netDataModels[2]);
        sourcePresentationModel.addNet(netDataModels[4]);
        sourcePresentationModel.addNet(netDataModels[5]);

        sourcePresentationModel.setValueFormatter(0, new BinaryValueFormatter());
        sourcePresentationModel.setValueFormatter(1, new DecimalValueFormatter());
        sourcePresentationModel.setValueFormatter(2, new HexadecimalValueFormatter());
        EnumValueFormatter enumFormatter = new EnumValueFormatter(
                new File("src/test/resources/enum_mapping/test1.txt"));
        sourcePresentationModel.setValueFormatter(3, enumFormatter);

        sourcePresentationModel.addMarker("marker1", 1234);
        sourcePresentationModel.addMarker("marker2", 5678);
        assertEquals(1, sourcePresentationModel.getIdForMarker(0));
        assertEquals(2, sourcePresentationModel.getIdForMarker(1));

        sourcePresentationModel.setHorizontalScale(123.0);

        // Save and reload contents
        TracePresentationModel destPresentationModel = new TracePresentationModel();
        File file = fTempFolder.newFile("test1.settings");
        new TraceSettingsFile(file, dataModel, sourcePresentationModel).write();
        new TraceSettingsFile(file, dataModel, destPresentationModel).read();

        // Check destPresentationModel
        assertEquals(4, destPresentationModel.getVisibleNetCount());
        assertSame(netDataModels[0], destPresentationModel.getVisibleNet(0));
        assertSame(netDataModels[2], destPresentationModel.getVisibleNet(1));
        assertSame(netDataModels[4], destPresentationModel.getVisibleNet(2));
        assertSame(netDataModels[5], destPresentationModel.getVisibleNet(3));

        assertTrue(destPresentationModel.getValueFormatter(0) instanceof BinaryValueFormatter);
        assertTrue(destPresentationModel.getValueFormatter(1) instanceof DecimalValueFormatter);
        assertTrue(destPresentationModel.getValueFormatter(2) instanceof HexadecimalValueFormatter);
        assertTrue(destPresentationModel.getValueFormatter(3) instanceof EnumValueFormatter);
        enumFormatter = (EnumValueFormatter) destPresentationModel.getValueFormatter(3);
        assertEquals("STATE_INIT", enumFormatter.format(new BitVector("1", 10)));
        assertEquals("STATE_LOAD", enumFormatter.format(new BitVector("2", 10)));
        assertEquals("STATE_WAIT", enumFormatter.format(new BitVector("3", 10)));

        assertEquals(2, destPresentationModel.getNetSetCount());
        assertEquals("set1", destPresentationModel.getNetSetName(0));
        assertEquals("set2", destPresentationModel.getNetSetName(1));

        destPresentationModel.selectNetSet(0);
        assertEquals(2, destPresentationModel.getVisibleNetCount());
        assertSame(netDataModels[1], destPresentationModel.getVisibleNet(0));
        assertSame(netDataModels[2], destPresentationModel.getVisibleNet(1));

        destPresentationModel.selectNetSet(1);
        assertEquals(2, destPresentationModel.getVisibleNetCount());
        assertSame(netDataModels[6], destPresentationModel.getVisibleNet(0));
        assertSame(netDataModels[7], destPresentationModel.getVisibleNet(1));

        assertEquals(2, destPresentationModel.getMarkerCount());
        assertEquals("marker1", destPresentationModel.getDescriptionForMarker(0));
        assertEquals("marker2", destPresentationModel.getDescriptionForMarker(1));
        assertEquals(1234, destPresentationModel.getTimestampForMarker(0));
        assertEquals(5678, destPresentationModel.getTimestampForMarker(1));
        assertEquals(1, destPresentationModel.getIdForMarker(0));
        assertEquals(2, destPresentationModel.getIdForMarker(1));

        assertEquals(123.0, sourcePresentationModel.getHorizontalScale(), 0.001);
    }

    // When the data model changes, ensure the loader falls back
    // gracefully. Specifically if a visible net is no longer in
    // the data model after it is reloaded
    // @todo Does not test when markers are put in past the end time.
    // (not currently implemented in loader)
    @Test
    public void dataModelChanged() throws Exception {
        TraceDataModel sourceDataModel = new TraceDataModel();
        TracePresentationModel sourcePresentationModel = new TracePresentationModel();
        TraceBuilder builder1 = sourceDataModel.startBuilding();
        builder1.enterScope("mod1");
        builder1.newNet("net1", -1, 1);
        builder1.newNet("net2", -1, 1);
        builder1.newNet("net3", -1, 1);
        builder1.exitScope();
        builder1.loadFinished();

        sourcePresentationModel.addNet(sourceDataModel.getNetDataModel(0));
        sourcePresentationModel.addNet(sourceDataModel.getNetDataModel(1));
        sourcePresentationModel.addNet(sourceDataModel.getNetDataModel(2));

        TraceDataModel destDataModel = new TraceDataModel();
        TracePresentationModel destPresentationModel = new TracePresentationModel();

        TraceBuilder builder2 = destDataModel.startBuilding();
        builder2.enterScope("mod1");
        builder2.newNet("net1", -1, 1);
        builder2.newNet("net4", -1, 1);
        builder2.newNet("net3", -1, 1);
        builder2.exitScope();
        builder2.loadFinished();

        File file = fTempFolder.newFile("test2.settings");
        (new TraceSettingsFile(file, sourceDataModel, sourcePresentationModel)).write();
        (new TraceSettingsFile(file, destDataModel, destPresentationModel)).read();

        assertEquals(2, destPresentationModel.getVisibleNetCount());
        assertSame(destDataModel.getNetDataModel(0), destPresentationModel.getVisibleNet(0));
        assertSame(destDataModel.getNetDataModel(2), destPresentationModel.getVisibleNet(1));
    }

    // If the formatter class name is unknown, fall back to binary
    @Test
    public void badFormatter() throws Exception {
        File file = new File("src/test/resources/trace_settings/bad_formatter.traceconfig");
        TraceDataModel dataModel = new TraceDataModel();
        TraceBuilder builder = dataModel.startBuilding();
        builder.enterScope("foo");
        builder.newNet("bar", -1, 1);
        builder.exitScope();
        builder.loadFinished();

        TracePresentationModel presentationModel = new TracePresentationModel();
        new TraceSettingsFile(file, dataModel, presentationModel).read();
        assertTrue(presentationModel.getValueFormatter(0) instanceof BinaryValueFormatter);
    }

    // Test generating config file name for subdirectory
    @Test
    public void configFileName1() throws Exception {
        assertEquals("foo/bar/.trace.vcd.traceconfig",
                TraceSettingsFile.settingsFileName(new File("foo/bar/trace.vcd")).toString());
    }

    /// Regression test. In this case, the full path isn't passed.
    /// It was putting 'null' inside the filename.
    @Test
    public void configFileName2() throws Exception {
        assertEquals(".trace.vcd.traceconfig", TraceSettingsFile.settingsFileName(new File("trace.vcd")).toString());
    }
}

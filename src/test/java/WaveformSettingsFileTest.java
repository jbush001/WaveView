
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import waveview.BinaryValueFormatter;
import waveview.OctalValueFormatter;
import waveview.DecimalValueFormatter;
import waveview.EnumValueFormatter;
import waveview.HexadecimalValueFormatter;
import waveview.WaveformPresentationModel;
import waveview.WaveformSettingsFile;
import waveview.wavedata.BitVector;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.WaveformDataModel;

public class WaveformSettingsFileTest {
    @Rule public final TemporaryFolder fTempFolder = new TemporaryFolder();

    @Test
    public void saveLoadScale() throws IOException, WaveformSettingsFile.SettingsFileException {
        WaveformDataModel dataModel = new WaveformDataModel();
        WaveformPresentationModel sourcePresentationModel = new WaveformPresentationModel();
        sourcePresentationModel.setHorizontalScale(123.0);

        // Save and reload contents
        WaveformPresentationModel destPresentationModel = new WaveformPresentationModel();
        File file = fTempFolder.newFile("test1.settings");
        new WaveformSettingsFile(file, dataModel, sourcePresentationModel).write();
        new WaveformSettingsFile(file, dataModel, destPresentationModel).read();

        // Check destPresentationModel
        assertEquals(123.0, sourcePresentationModel.getHorizontalScale(), 0.001);
    }

    @Test
    public void saveLoadNetSets() throws IOException, WaveformSettingsFile.SettingsFileException {
        WaveformDataModel dataModel = new WaveformDataModel();
        WaveformPresentationModel sourcePresentationModel = new WaveformPresentationModel();

        dataModel.startBuilding()
            .enterScope("mod1")
            .newNet(0, "net1", 1)
            .newNet(1, "net2", 1)
            .enterScope("mod2")
            .newNet(2, "net3", 1)
            .newNet(3, "net4", 1)
            .enterScope("mod3")
            .newNet(4, "net5", 1)
            .exitScope()
            .exitScope()
            .exitScope()
            .loadFinished();

        NetDataModel[] netDataModels = new NetDataModel[5];
        int index = 0;
        for (NetDataModel model : dataModel) {
            netDataModels[index++] = model;
        }

        // Populate sourcePresentationModel
        sourcePresentationModel.addNet(netDataModels[0]);
        sourcePresentationModel.addNet(netDataModels[1]);
        sourcePresentationModel.saveNetSet("set1");

        sourcePresentationModel.removeAllNets();
        sourcePresentationModel.addNet(netDataModels[2]);
        sourcePresentationModel.addNet(netDataModels[3]);
        sourcePresentationModel.addNet(netDataModels[4]);
        sourcePresentationModel.saveNetSet("set2");

        // Save and reload contents
        WaveformPresentationModel destPresentationModel = new WaveformPresentationModel();
        File file = fTempFolder.newFile("test1.settings");
        new WaveformSettingsFile(file, dataModel, sourcePresentationModel).write();
        new WaveformSettingsFile(file, dataModel, destPresentationModel).read();

        // Check destPresentationModel
        assertEquals(2, destPresentationModel.getNetSetCount());
        assertEquals("set1", destPresentationModel.getNetSetName(0));
        assertEquals("set2", destPresentationModel.getNetSetName(1));

        destPresentationModel.selectNetSet(0);
        assertEquals(2, destPresentationModel.getVisibleNetCount());
        assertSame(netDataModels[0], destPresentationModel.getVisibleNet(0));
        assertSame(netDataModels[1], destPresentationModel.getVisibleNet(1));

        destPresentationModel.selectNetSet(1);
        assertEquals(3, destPresentationModel.getVisibleNetCount());
        assertSame(netDataModels[2], destPresentationModel.getVisibleNet(0));
        assertSame(netDataModels[3], destPresentationModel.getVisibleNet(1));
        assertSame(netDataModels[4], destPresentationModel.getVisibleNet(2));
    }

    @Test
    public void saveLoadValueFormatters() throws IOException {
        WaveformDataModel dataModel = new WaveformDataModel();
        WaveformPresentationModel sourcePresentationModel = new WaveformPresentationModel();

        dataModel.startBuilding()
            .enterScope("mod1")
            .newNet(0, "net1", 1)
            .newNet(1, "net2", 1)
            .newNet(1, "net3", 1)
            .newNet(1, "net4", 1)
            .newNet(1, "net5", 1)
            .exitScope()
            .loadFinished();

        NetDataModel[] netDataModels = new NetDataModel[5];
        int index = 0;
        for (NetDataModel model : dataModel) {
            netDataModels[index++] = model;
        }

        // Populate sourcePresentationModel
        sourcePresentationModel.addNet(netDataModels[0]);
        sourcePresentationModel.addNet(netDataModels[1]);
        sourcePresentationModel.addNet(netDataModels[2]);
        sourcePresentationModel.addNet(netDataModels[3]);
        sourcePresentationModel.addNet(netDataModels[4]);

        sourcePresentationModel.setValueFormatter(0, new BinaryValueFormatter());
        sourcePresentationModel.setValueFormatter(1, new OctalValueFormatter());
        sourcePresentationModel.setValueFormatter(2, new DecimalValueFormatter());
        sourcePresentationModel.setValueFormatter(3, new HexadecimalValueFormatter());
        EnumValueFormatter enumFormatter =
            new EnumValueFormatter(new File("src/test/resources/enum_mapping/test1.txt"));
        sourcePresentationModel.setValueFormatter(4, enumFormatter);

        // Save and reload contents
        WaveformPresentationModel destPresentationModel = new WaveformPresentationModel();
        File file = fTempFolder.newFile("test1.settings");
        new WaveformSettingsFile(file, dataModel, sourcePresentationModel).write();
        new WaveformSettingsFile(file, dataModel, destPresentationModel).read();

        // Check destPresentationModel
        assertEquals(5, destPresentationModel.getVisibleNetCount());
        assertSame(netDataModels[0], destPresentationModel.getVisibleNet(0));
        assertSame(netDataModels[1], destPresentationModel.getVisibleNet(1));
        assertSame(netDataModels[2], destPresentationModel.getVisibleNet(2));
        assertSame(netDataModels[3], destPresentationModel.getVisibleNet(3));
        assertSame(netDataModels[4], destPresentationModel.getVisibleNet(4));

        assertTrue(destPresentationModel.getValueFormatter(0) instanceof BinaryValueFormatter);
        assertTrue(destPresentationModel.getValueFormatter(1) instanceof OctalValueFormatter);
        assertTrue(destPresentationModel.getValueFormatter(2) instanceof DecimalValueFormatter);
        assertTrue(destPresentationModel.getValueFormatter(3) instanceof HexadecimalValueFormatter);
        assertTrue(destPresentationModel.getValueFormatter(4) instanceof EnumValueFormatter);
        enumFormatter = (EnumValueFormatter) destPresentationModel.getValueFormatter(4);
        assertEquals("STATE_INIT", enumFormatter.format(new BitVector("1", 10)));
        assertEquals("STATE_LOAD", enumFormatter.format(new BitVector("2", 10)));
        assertEquals("STATE_WAIT", enumFormatter.format(new BitVector("3", 10)));
    }

    @Test
    public void saveLoadMarkers() throws IOException {
        WaveformDataModel dataModel = new WaveformDataModel();
        WaveformPresentationModel sourcePresentationModel = new WaveformPresentationModel();

        dataModel.startBuilding()
            .enterScope("mod1")
            .newNet(0, "net1", 1)
            .exitScope()
            .loadFinished();

        sourcePresentationModel.addMarker("marker1", 1234);
        sourcePresentationModel.addMarker("marker2", 5678);

        // Save and reload contents
        WaveformPresentationModel destPresentationModel = new WaveformPresentationModel();
        File file = fTempFolder.newFile("test1.settings");
        new WaveformSettingsFile(file, dataModel, sourcePresentationModel).write();
        new WaveformSettingsFile(file, dataModel, destPresentationModel).read();

        // Check destPresentationModel
        assertEquals(2, destPresentationModel.getMarkerCount());
        assertEquals("marker1", destPresentationModel.getDescriptionForMarker(0));
        assertEquals("marker2", destPresentationModel.getDescriptionForMarker(1));
        assertEquals(1234, destPresentationModel.getTimestampForMarker(0));
        assertEquals(5678, destPresentationModel.getTimestampForMarker(1));
        assertEquals(1, destPresentationModel.getIdForMarker(0));
        assertEquals(2, destPresentationModel.getIdForMarker(1));
    }

    @Test
    public void saveLoadDecodedNet() throws IOException {
        System.out.println("saveLoadDecodedNet");
        WaveformDataModel dataModel = new WaveformDataModel();
        WaveformPresentationModel sourcePresentationModel = new WaveformPresentationModel();
        dataModel.startBuilding()
            .enterScope("mod1")
            .newNet(0, "net1", 1)
            .newNet(1, "net2", 1)
            .exitScope()
            .loadFinished();

        String[] sourceInputs = {"mod1.net1", "mod1.net2"};
        String[] sourceParams = {"foo"};
        NetDataModel decoded = new NetDataModel("short", "full.name", "SPI",
            sourceInputs, sourceParams, null);
        sourcePresentationModel.addNet(decoded);

        WaveformPresentationModel destPresentationModel = new WaveformPresentationModel();
        File file = fTempFolder.newFile("test1.settings");
        new WaveformSettingsFile(file, dataModel, sourcePresentationModel).write();
        new WaveformSettingsFile(file, dataModel, destPresentationModel).read();

        // Check destPresentationModel
        assertEquals(1, destPresentationModel.getVisibleNetCount());
        NetDataModel newDecodedModel = destPresentationModel
            .getVisibleNet(0);

        assertEquals("name", newDecodedModel.getShortName());
        assertEquals("full.name", newDecodedModel.getFullName());
        assertArrayEquals(sourceInputs, newDecodedModel.getDecoderInputNets());
        assertArrayEquals(sourceParams, newDecodedModel.getDecoderParams());
        assertSame(newDecodedModel, dataModel.findNet("full.name"));
    }

    // When the data model changes on disk between the time the settings file
    // was saved and when it was reloaded, ensure the loader falls back
    // gracefully. Specifically if a visible net is no longer in the data model
    // after it is reloaded
    // @todo Does not test when markers are put in past the end time.
    // (not currently implemented in loader)
    @Test
    public void dataModelChanged() throws IOException {
        WaveformDataModel sourceDataModel = new WaveformDataModel();
        WaveformPresentationModel sourcePresentationModel = new WaveformPresentationModel();
        sourceDataModel.startBuilding()
            .enterScope("mod1")
            .newNet(0, "net1", 1)
            .newNet(1, "net2", 1)
            .newNet(2, "net3", 1)
            .exitScope()
            .loadFinished();

        sourcePresentationModel.addNet(sourceDataModel.getNetDataModel(0));
        sourcePresentationModel.addNet(sourceDataModel.getNetDataModel(1));
        sourcePresentationModel.addNet(sourceDataModel.getNetDataModel(2));

        WaveformDataModel destDataModel = new WaveformDataModel();
        WaveformPresentationModel destPresentationModel = new WaveformPresentationModel();

        destDataModel.startBuilding()
            .enterScope("mod1")
            .newNet(0, "net1", 1)
            .newNet(1, "net4", 1)
            .newNet(2, "net3", 1)
            .exitScope()
            .loadFinished();

        File file = fTempFolder.newFile("test2.settings");
        new WaveformSettingsFile(file, sourceDataModel, sourcePresentationModel).write();
        new WaveformSettingsFile(file, destDataModel, destPresentationModel).read();

        assertEquals(2, destPresentationModel.getVisibleNetCount());
        assertSame(destDataModel.getNetDataModel(0), destPresentationModel.getVisibleNet(0));
        assertSame(destDataModel.getNetDataModel(2), destPresentationModel.getVisibleNet(1));
    }

    // If the formatter class name is unknown, fall back to binary
    @Test
    public void badFormatter() throws IOException {
        File file = new File("src/test/resources/waveform_settings/bad_formatter.waveconfig");
        WaveformDataModel dataModel = new WaveformDataModel();
        dataModel.startBuilding().enterScope("foo").newNet(0, "bar", 1).exitScope().loadFinished();

        WaveformPresentationModel presentationModel = new WaveformPresentationModel();
        new WaveformSettingsFile(file, dataModel, presentationModel).read();
        assertTrue(presentationModel.getValueFormatter(0) instanceof BinaryValueFormatter);
    }

    // Test generating config file name for subdirectory
    @Test
    public void configFileName1() throws IOException {
        assertEquals("foo/bar/.dumpfile.vcd.waveconfig",
            WaveformSettingsFile.settingsFileName(new File("foo/bar/dumpfile.vcd")).toString());
    }

    /// Regression test. In this case, the full path isn't passed.
    /// It was putting 'null' inside the filename.
    @Test
    public void configFileName2() throws IOException {
        assertEquals(".dumpfile.vcd.waveconfig",
            WaveformSettingsFile.settingsFileName(new File("dumpfile.vcd")).toString());
    }

    @Test
    public void invalidFile() throws IOException {
        try {
            WaveformDataModel dataModel = new WaveformDataModel();
            WaveformPresentationModel presentationModel = new WaveformPresentationModel();
            File file = new File("src/test/resources/waveform_settings/invalid.waveconfig");
            new WaveformSettingsFile(file, dataModel, presentationModel).read();
            fail("didn't throw exception");
        } catch (WaveformSettingsFile.SettingsFileException exc) {
            // Expected
        }
    }
}

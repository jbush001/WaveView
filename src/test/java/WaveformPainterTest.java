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
import java.io.*;
import java.awt.image.*;
import java.awt.*;
import javax.imageio.ImageIO;

//
// This tests the waveform drawing delegates by comparing to reference
// images stored in the resource directory. If a test fails, it will generate
// a file with the 'actual-' prefix that shows what was generated. This will
// break if there any changes to the look of the output (even thought the
// output may be "correct"). In this case, the generated image can be
// manually inspected, and, if it is correct, copied over the old reference
// image.
//
public class WaveformPainterTest {
    private static final String RESOURCE_DIR = "src/test/resources/waveformpainter/";

    private boolean imagesEqual(BufferedImage a, BufferedImage b) {
        int aWidth = a.getWidth();
        int aHeight = a.getHeight();
        if (aWidth != b.getWidth() ||
            aHeight != b.getHeight())
            return false;

        for (int y = 0; y < aHeight; y++) {
            for (int x = 0; x < aWidth; x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y))
                    return false;
            }
        }

        return true;
    }

    private void testRender(File referenceFile, WaveformPainter painter,
        TraceDataModel model, double scale, ValueFormatter formatter)
        throws IOException {

        // Create test image
        final int WIDTH = 512;
        final int HEIGHT = 30;
        BufferedImage testImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics g = testImage.createGraphics();
        painter.paint(g, model, 0, 0, new Rectangle(0, 0, WIDTH, HEIGHT),
            scale, formatter);
        g.dispose();

        BufferedImage referenceImage = null;
        if (referenceFile.exists())
            referenceImage = ImageIO.read(referenceFile);

        if (referenceImage == null || !imagesEqual(referenceImage, testImage)) {
            // Write the actual image for debugging if there is a problem.
            File outputFile = new File(referenceFile.getParent() + "/actual-"
                + referenceFile.getName());
            ImageIO.write(testImage, "png", outputFile);
            if (referenceImage != null)
                fail("image mismatch");
            else
                fail("reference image is missing");
        }
    }

    private TraceDataModel createSingleBitTrace() {
        TraceDataModel model = new TraceDataModel();
        TraceBuilder builder = model.startBuilding();

        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int net1 = builder.newNet("net1", -1, 1);
        builder.exitScope();

        builder.appendTransition(net1, 0, new BitVector("0", 2));
        builder.appendTransition(net1, 25, new BitVector("1", 2));
        builder.appendTransition(net1, 50, new BitVector("0", 2));
        builder.appendTransition(net1, 75, new BitVector("z", 2));
        builder.appendTransition(net1, 100, new BitVector("1", 2));
        builder.appendTransition(net1, 125, new BitVector("z", 2));
        builder.appendTransition(net1, 150, new BitVector("0", 2));
        builder.appendTransition(net1, 175, new BitVector("x", 2));
        builder.appendTransition(net1, 200, new BitVector("1", 2));
        builder.appendTransition(net1, 225, new BitVector("x", 2));
        builder.appendTransition(net1, 250, new BitVector("0", 2));
        builder.loadFinished();

        return model;
    }

    private TraceDataModel createMultiBitTrace() {
        TraceDataModel model = new TraceDataModel();
        TraceBuilder builder = model.startBuilding();

        builder.setTimescale(-9);
        builder.enterScope("mod1");
        int net1 = builder.newNet("net1", -1, 8);
        builder.exitScope();

        builder.appendTransition(net1, 0, new BitVector("00000000", 2));        // 50 wide
        builder.appendTransition(net1, 50, new BitVector("00000001", 2));       // 40 wide
        builder.appendTransition(net1, 90, new BitVector("00000010", 2));       // 30 wide
        builder.appendTransition(net1, 120, new BitVector("00000011", 2));      // 20 wide
        builder.appendTransition(net1, 140, new BitVector("00000100", 2));      // 10 wide
        builder.appendTransition(net1, 145, new BitVector("00000101", 2));      // 5 wide
        builder.appendTransition(net1, 150, new BitVector("zzzzzzzz", 2));
        builder.appendTransition(net1, 180, new BitVector("11111111", 2));
        builder.appendTransition(net1, 210, new BitVector("xxxxxxxx", 2));
        builder.appendTransition(net1, 250, new BitVector("00000000", 2));
        builder.loadFinished();

        return model;
    }

    @Test
    public void testSingleBitPainterS2() throws Exception {
        TraceDataModel model = createSingleBitTrace();
        testRender(new File(RESOURCE_DIR + "single-bit-scale2.png"), new SingleBitPainter(),
            model, 2.0, null);
    }

    @Test
    public void testSingleBitPainterS05() throws Exception {
        TraceDataModel model = createSingleBitTrace();
        testRender(new File(RESOURCE_DIR + "single-bit-scale05.png"), new SingleBitPainter(),
            model, 0.5, null);
    }

    @Test
    public void testMultiBitPainterS2() throws Exception {
        TraceDataModel model = createMultiBitTrace();
        testRender(new File(RESOURCE_DIR + "multi-bit-scale2.png"), new MultiBitPainter(),
            model, 2.0, new BinaryValueFormatter());
    }

    @Test
    public void testMultiBitPainterS05() throws Exception {
        TraceDataModel model = createMultiBitTrace();
        testRender(new File(RESOURCE_DIR + "multi-bit-scale05.png"), new MultiBitPainter(),
            model, 0.5, new BinaryValueFormatter());
    }
}

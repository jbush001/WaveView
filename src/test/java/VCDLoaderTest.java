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
import java.nio.file.Paths;
import java.io.*;
import java.util.Vector;

public class VCDLoaderTest
{
    // Implements TraceBuilder interface, which the VCDLoader will write into,
    // but asserts if events don't match a pre-defined sequence.
    public class ExpectTraceBuilder implements TraceBuilder
    {
        static final int EXPECT_ENTER = 0;
        static final int EXPECT_EXIT = 1;
        static final int EXPECT_NET = 2;
        static final int EXPECT_TRANSITION = 3;
        static final int EXPECT_FINISHED = 4;

        class Event
        {
            Event(int type)
            {
                fType = type;
            }

            int fType;
            String fName;
            int fId;
            int fWidth;
            long fTimestamp;
            BitVector fValues;
        }

        public void enterModule(String name)
        {
            System.out.println("enterModule " + name);

            Event event = fEventList.elementAt(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_ENTER);
        }

        public void exitModule()
        {
            System.out.println("exitModule");

            Event event = fEventList.elementAt(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_EXIT);
        }

        public int newNet(String shortName, int cloneId, int width)
        {
            System.out.println("newNet " + shortName + " " + cloneId + " " + width);

            Event event = fEventList.elementAt(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_NET);
            assertEquals(event.fName, shortName);
            assertEquals(event.fId, cloneId);
            assertEquals(event.fWidth, width);

            fNetWidths.add(new Integer(width));
            return fNetWidths.size() - 1;
        }

        public int getNetWidth(int netId)
        {
            return fNetWidths.elementAt(netId);
        }

        public void appendTransition(int id, long timestamp, BitVector values)
        {
            System.out.println("appendTransition " + id + " " + timestamp + " " + values.toString(2));

            Event event = fEventList.elementAt(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_TRANSITION);
            assertEquals(event.fTimestamp, timestamp);
            assertEquals(0, values.compare(event.fValues));
        }

        public void loadFinished()
        {
            System.out.println("loadFinished");

            Event event = fEventList.elementAt(fCurrentEvent++);
            assertEquals(event.fType, EXPECT_FINISHED);
        }

        void expectEnter(String name)
        {
            Event event = new Event(EXPECT_ENTER);
            event.fName = name;
            fEventList.add(event);
        }

        void expectExit()
        {
            fEventList.add(new Event(EXPECT_EXIT));
        }

        void expectNet(String name, int cloneId, int width)
        {
            Event event = new Event(EXPECT_NET);
            event.fName = name;
            event.fId = cloneId;
            event.fWidth = width;
            fEventList.add(event);
        }

        void expectTransition(int id, long timestamp, String bitString)
        {
            Event event = new Event(EXPECT_TRANSITION);
            event.fId = id;
            event.fTimestamp = timestamp;
            event.fValues = new BitVector(bitString, 2);
            fEventList.add(event);
        }

        void expectFinished()
        {
            fEventList.add(new Event(EXPECT_FINISHED));
        }

        Vector<Integer> fNetWidths = new Vector<Integer>();
        Vector<Event> fEventList = new Vector<Event>();
        int fCurrentEvent = 0;
    }

    // Simulataneously builds VCD file contents and populates the
    // ExpectTraceBuilder with matching events
    class TestBuilder
    {
        void setTimestampMultiplier(int value)
        {
            fTimestampMultiplier = value;
        }

        void addString(String data)
        {
            fVCDContents.append(data);
        }

        void enterModule(String name)
        {
            fVCDContents.append("$scope module ");
            fVCDContents.append(name);
            fVCDContents.append(" $end\n");
            fTraceBuilder.expectEnter(name);
        }

        void exitModule()
        {
            fVCDContents.append("$upscope $end\n");
            fTraceBuilder.expectExit();
        }

        int defineNet(String name, int cloneId, int width)
        {
            int index = fNetIsMultiBit.size();
            fNetIsMultiBit.add(width != 1);
            fVCDContents.append("$var wire ");
            fVCDContents.append(width);
            fVCDContents.append(' ');
            fVCDContents.append(getIdForIndex(index));
            fVCDContents.append(' ');
            fVCDContents.append(name);
            fVCDContents.append(" $end\n");

            fTraceBuilder.expectNet(name, cloneId, width);
            return index;
        }

        void endDefinitions()
        {
            fVCDContents.append("$enddefinitions $end\n");
        }

        void appendTransition(int netId, long timestamp, String bitString)
        {
            if (fLastTimestamp != timestamp)
            {
                fLastTimestamp = timestamp;
                fVCDContents.append('#');
                fVCDContents.append(timestamp);
                fVCDContents.append('\n');
            }

            if (fNetIsMultiBit.elementAt(netId))
            {
                fVCDContents.append('b');
                fVCDContents.append(bitString);
                fVCDContents.append(' ');
            }
            else
                fVCDContents.append(bitString);

            fVCDContents.append(getIdForIndex(netId));
            fVCDContents.append('\n');

            fTraceBuilder.expectTransition(netId, timestamp * fTimestampMultiplier, bitString);
        }

        void finish()
        {
            fTraceBuilder.expectFinished();
            System.out.println("output is\n" + fVCDContents.toString());
        }

        InputStream getVCDInputStream()
        {
            return new ByteArrayInputStream(fVCDContents.toString().getBytes());
        }

        private String getIdForIndex(int index)
        {
            return String.valueOf((char) (index + 33));
        }

        TraceBuilder getTraceBuilder()
        {
            return fTraceBuilder;
        }

        Vector<Boolean> fNetIsMultiBit = new Vector<Boolean>();
        ExpectTraceBuilder fTraceBuilder = new ExpectTraceBuilder();
        StringBuffer fVCDContents = new StringBuffer();
        int fTimestampMultiplier;
        long fLastTimestamp = -1;
    }


    // Timescale is one microsecond
    @Test public void testTimescaleUs()
    {
        TestBuilder builder = new TestBuilder();
        builder.setTimestampMultiplier(1000);
        builder.addString("$timescale\n   1us\n$end\n");
        builder.enterModule("mod1");
        builder.defineNet("clk", -1, 1);
        builder.exitModule();
        builder.endDefinitions();
        builder.appendTransition(0, 0, "1");
        builder.appendTransition(0, 1, "0");
        builder.appendTransition(0, 2, "1");
        builder.appendTransition(0, 3, "0");
        builder.appendTransition(0, 4, "1");
        builder.finish();

        VCDLoader loader = new VCDLoader();
        loader.load(builder.getVCDInputStream(), builder.getTraceBuilder());

    }

    // Timescale is one nanosecond
    @Test public void testTimescaleNs()
    {
        TestBuilder builder = new TestBuilder();
        builder.setTimestampMultiplier(1);
        builder.addString("$timescale\n   1ns\n$end\n");
        builder.enterModule("mod1");
        builder.defineNet("clk", -1, 1);
        builder.exitModule();
        builder.endDefinitions();
        builder.appendTransition(0, 0, "1");
        builder.appendTransition(0, 1, "0");
        builder.appendTransition(0, 2, "1");
        builder.appendTransition(0, 3, "0");
        builder.appendTransition(0, 4, "1");
        builder.finish();

        VCDLoader loader = new VCDLoader();
        loader.load(builder.getVCDInputStream(), builder.getTraceBuilder());
    }

    @Test public void testMultibit()
    {
        TestBuilder builder = new TestBuilder();
        builder.setTimestampMultiplier(1);
        builder.addString("$timescale\n   1ns\n$end\n");
        builder.enterModule("mod1");
        builder.defineNet("value", -1, 16);
        builder.exitModule();
        builder.endDefinitions();
        builder.appendTransition(0, 0, "1010111000101011");
        builder.appendTransition(0, 5, "1101010010100010");
        builder.finish();

        VCDLoader loader = new VCDLoader();
        loader.load(builder.getVCDInputStream(), builder.getTraceBuilder());
    }

}

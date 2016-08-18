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
    public class TestTraceBuilder implements TraceBuilder
    {
        public void enterModule(String name)
        {
            fResultString.append("enter " + name + ";");
        }

        public void exitModule()
        {
            fResultString.append("exit;");
        }

        public int newNet(String shortName, int cloneId, int width)
        {
            fResultString.append("new " + shortName + "," + cloneId + "," + width + ";");
            fNetWidths.add(new Integer(width));
            return fNetWidths.size() - 1;
        }

        public int getNetWidth(int netId)
        {
            return fNetWidths.elementAt(netId);
        }

        public void appendTransition(int id, long timestamp, BitVector values)
        {
            fResultString.append("append " + id + "," + timestamp + "," + values.toString(16) + ";");
        }

        public void loadFinished()
        {
            fResultString.append("finished;");
        }

        StringBuffer fResultString = new StringBuffer();
        int fNextNet = 0;
        Vector<Integer> fNetWidths = new Vector<Integer>();
    }

    @Test public void testLoad1()
	{
        VCDLoader loader = new VCDLoader();
        TestTraceBuilder builder = new TestTraceBuilder();
        try
        {
            InputStream is = new FileInputStream(new File("src/test/resources/test1.vcd"));
            loader.load(is, builder);
            assertEquals("enter mod1;enter mod2;new addr,-1,32;new clk,-1,1;new request,-1,1;"
                + "new reset,-1,1;new data,-1,32;exit;exit;append 4,0,XXXXXXXX;append 3,0,1;"
                + "append 2,0,0;append 1,0,0;append 0,0,00000000;append 4,50,00000000;"
                + "append 1,50,1;append 1,100,0;append 3,150,0;append 4,200,00000002;"
                + "append 0,200,00000001;append 2,200,1;append 1,200,1;append 1,250,0;"
                + "append 2,300,0;append 1,300,1;append 1,350,0;append 4,400,00000003;"
                + "append 0,400,00000002;append 2,400,1;append 1,400,1;append 1,450,0;"
                + "append 4,500,00000000;append 0,500,00000003;append 1,500,1;append 1,550,0;"
                + "append 2,600,0;append 1,600,1;append 1,650,0;append 1,700,1;append 1,750,0;"
                + "finished;",
                builder.fResultString.toString());
        }
        catch (Exception exc)
        {
            fail(exc.toString());
        }
    }
}

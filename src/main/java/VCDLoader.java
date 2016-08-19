//
// Copyright 2011-2012 Jeff Bush
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

//
// Parse a value change dump (VCD) formatted text file and push the contents into a provided trace model
//

import java.io.*;
import java.util.*;

class VCDLoader implements TraceLoader
{
    public VCDLoader()
    {
    }

    public boolean load(InputStream is, TraceBuilder builder)
    {
        fTokenizer = new StreamTokenizer(new BufferedReader(new InputStreamReader(is)));
        fTokenizer.resetSyntax();
        fTokenizer.wordChars(33, 126);
        fTokenizer.whitespaceChars('\r', '\r');
        fTokenizer.whitespaceChars('\n', '\n');
        fTokenizer.whitespaceChars(' ', ' ');
        fTokenizer.whitespaceChars('\t', '\t');
        try
        {
            fInputStream = new BufferedInputStream(is);
            fTraceBuilder = builder;

            while (parseDefinition())
                ;

            while (parseTransition())
                ;

            builder.loadFinished();

            System.out.println("parsed " + fTotalTransitions + " total transitions");
            System.out.println("" + fNetMap.size() + " total nets");
            return true;
        }
        catch (Exception exc)
        {
            System.out.println("caught exception " + exc);
            exc.printStackTrace();
            return false;
        }
    }

    private void parseScope() throws Exception
    {
        nextToken();    // Scope type (ignore)
        nextToken();
        fTraceBuilder.enterModule(getTokenString());
        match("$end");
    }

    private void parseUpscope() throws Exception
    {
        match("$end");
        fTraceBuilder.exitModule();
    }

    private void parseVar() throws Exception
    {
        nextToken();    // type
        nextToken();    // size
        int width = Integer.parseInt(getTokenString());

        nextToken();
        String id = getTokenString();
        nextToken();
        String netName = getTokenString();
        do
        {
            if (!nextToken())
                break;
        }
        while (!getTokenString().equals("$end"));

        Integer net = fNetMap.get(id);
        if (net == null)
        {
            // We've never seen this net before
            // Strip off the width declaration
            int openBracket = netName.indexOf('[');
            if (openBracket != -1)
                netName = netName.substring(0, openBracket);

            fNetMap.put(id, fTraceBuilder.newNet(netName, -1, width));
        }
        else
        {
            // Shares data with existing net.  Add as clone.
            fTraceBuilder.newNet(netName, net.intValue(), width);
        }
    }

    private void parseTimescale() throws Exception
    {
        nextToken();

        String s = getTokenString();
        switch (s.charAt(s.length() - 2))    // we want the prefix for the time unit: ####xs
        {
            case 'n':    // Nano-seconds
                fNanoSecondsPerIncrement = 1;
                s = s.substring(0, s.length() - 2);
                break;
            case 'u':    // Microseconds
                s = s.substring(0, s.length() - 2);
                fNanoSecondsPerIncrement = 1000;
                break;

            case 's':   // Seconds
                fNanoSecondsPerIncrement = 1000000000;
                s = s.substring(0, s.length() - 1);
                break;

            // XXX need to handle other units

            default:
                throw new Exception("Line " + fTokenizer.lineno() + ": unknown timescale value "
                    + getTokenString());
        }

        fNanoSecondsPerIncrement *= Long.parseLong(s);
        match("$end");
    }

    private boolean parseDefinition() throws Exception
    {
        nextToken();
        if (getTokenString().equals("$scope"))
            parseScope();
        else if (getTokenString().equals("$var"))
            parseVar();
        else if (getTokenString().equals("$upscope"))
            parseUpscope();
        else if (getTokenString().equals("$timescale"))
            parseTimescale();
        else if (getTokenString().equals("$enddefinitions"))
        {
            match("$end");
            return false;
        }
        else
        {
            // Ignore this defintion
            do
            {
                if (!nextToken())
                    return false;
            }
            while (!getTokenString().equals("$end"));
        }

        return true;
    }

    private boolean parseTransition() throws Exception
    {
        fTotalTransitions++;

        if (!nextToken())
            return false;

        if (getTokenString().charAt(0) == '#')
        {
            // If the line begins with a #, this is a timestamp.
            fCurrentTime = Long.parseLong(getTokenString().substring(1)) * fNanoSecondsPerIncrement;
        }
        else
        {
            if (getTokenString().equals("$dumpvars") || getTokenString().equals("$end"))
                return true;

            String value;
            String id;

            if (getTokenString().charAt(0) == 'b')
            {
                // Multiple value net.  Value appears first, followed by space, then identifier
                value = getTokenString().substring(1);
                nextToken();
                id = getTokenString();
            }
            else
            {
                // Single value net.  identifier first, then value, no space.
                value = getTokenString().substring(0, 1);
                id = getTokenString().substring(1);
            }

            Integer net = fNetMap.get(id);
            if (net == null)
            {
                System.out.println("unknown net " + id + " value " + value);
                return true;
            }

            int netWidth = fTraceBuilder.getNetWidth(net.intValue());
            BitVector decodedValues = new BitVector(netWidth);
            if (value.equals("z") && netWidth > 1)
            {
                for (int i = 0; i < netWidth; i++)
                    decodedValues.setBit(i, BitVector.VALUE_Z);
            }
            else if (value.equals("x") && netWidth > 1)
            {
                for (int i = 0; i < netWidth; i++)
                    decodedValues.setBit(i, BitVector.VALUE_X);
            }
            else
            {
                // Decode and pad if necessary.
                // XXX should this be done inside BitVector
                int bitIndex = netWidth - 1;
                for (int i = 0; i < value.length(); i++)
                {
                    int bitValue;
                    switch (value.charAt(i))
                    {
                        case 'z':
                            bitValue = BitVector.VALUE_Z;
                            break;

                        case '1':
                            bitValue = BitVector.VALUE_1;
                            break;

                        case '0':
                            bitValue = BitVector.VALUE_0;
                            break;

                        case 'x':
                            bitValue = BitVector.VALUE_X;
                            break;

                        default:
                            System.out.println("Warning: invalid bit value found");
                            bitValue = BitVector.VALUE_X;
                            // XXX perhaps return error
                    }

                    decodedValues.setBit(bitIndex--, bitValue);
                }
            }

            fTraceBuilder.appendTransition(net.intValue(), fCurrentTime, decodedValues);
        }

        return true;
    }

    private boolean isNum(int c)
    {
        return c >= '0' && c <= '9';
    }

    private boolean isSpace(int c)
    {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private boolean isAlphaNum(int c)
    {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }

    private void match(String value) throws Exception
    {
        nextToken();
        if (!getTokenString().equals(value))
        {
            throw new Exception("Line " + fTokenizer.lineno() + ": parse error, expected " + value + " got "
                + getTokenString());
        }
    }

    private boolean nextToken() throws IOException
    {
        return fTokenizer.nextToken() != StreamTokenizer.TT_EOF;
    }

    private String getTokenString()
    {
        return fTokenizer.sval;
    }

    private StreamTokenizer fTokenizer;
    private TraceBuilder fTraceBuilder;
    private InputStream fInputStream;
    private long fCurrentTime;
    private HashMap<String, Integer> fNetMap = new HashMap<String, Integer>();
    private long fNanoSecondsPerIncrement;    /// @todo Switch to be unit agnostic
    private int fTotalTransitions;
};

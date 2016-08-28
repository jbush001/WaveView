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

///
/// Parse a value change dump (VCD) formatted text file and push the contents into a
/// provided trace model
///

import java.io.*;
import java.util.*;

class VCDLoader implements TraceLoader {
    @Override
    public void load(File file, TraceBuilder builder, ProgressListener listener)
        throws LoadException, IOException {
        fProgressListener = listener;
        fProgressStream = new ProgressInputStream(new FileInputStream(file));
        fTokenizer = new StreamTokenizer(new BufferedReader(new InputStreamReader(fProgressStream)));
        fFileLength = file.length();
        fTokenizer.resetSyntax();
        fTokenizer.wordChars(33, 126);
        fTokenizer.whitespaceChars('\r', '\r');
        fTokenizer.whitespaceChars('\n', '\n');
        fTokenizer.whitespaceChars(' ', ' ');
        fTokenizer.whitespaceChars('\t', '\t');

        fInputStream = new BufferedInputStream(new FileInputStream(file));
        fTraceBuilder = builder;

        while (parseDefinition())
            ;

        while (parseTransition())
            ;

        builder.loadFinished();

        System.out.println("parsed " + fTotalTransitions + " total transitions");
        System.out.println("" + fNetMap.size() + " total nets");
    }

    private class Net
    {
        Net(int builderID, int width) {
            fBuilderID = builderID;
            fWidth = width;
        }

        int fBuilderID;  /// ID given to this net by the builder
        int fWidth;
    }

    private void parseScope() throws LoadException, IOException {
        nextToken(true);    // Scope type (ignore)
        nextToken(true);
        fTraceBuilder.enterModule(getTokenString());
        match("$end");
    }

    private void parseUpscope() throws LoadException, IOException {
        match("$end");
        fTraceBuilder.exitModule();
    }

    private void parseVar() throws LoadException, IOException {
        nextToken(true);    // type
        nextToken(true);    // size
        int width = Integer.parseInt(getTokenString());

        nextToken(true);
        String id = getTokenString();
        nextToken(true);
        String netName = getTokenString();

        // If this has a width like [16:0], Ignore it.
        nextToken(true);
        if (getTokenString().charAt(0) != '[')
            fTokenizer.pushBack();

        match("$end");

        Net net = fNetMap.get(id);
        if (net == null) {
            // We've never seen this net before
            // Strip off the width declaration
            int openBracket = netName.indexOf('[');
            if (openBracket != -1)
                netName = netName.substring(0, openBracket);

            net = new Net(fTraceBuilder.newNet(netName, -1, width), width);
            fNetMap.put(id, net);
        } else {
            // Shares data with existing net.  Add as clone.
            fTraceBuilder.newNet(netName, net.fBuilderID, width);
        }
    }

    private void parseTimescale() throws LoadException, IOException {
        nextToken(true);

        String s = getTokenString();
        int unitStart = 0;
        while (unitStart < s.length() && Character.isDigit(s.charAt(unitStart)))
            unitStart++;

        String unit = s.substring(unitStart);

        if (unit.equals("ns"))
            fNanoSecondsPerIncrement = 1;
        else if (unit.equals("us"))
            fNanoSecondsPerIncrement = 1000;
        else if (unit.equals("s"))
            fNanoSecondsPerIncrement = 1000000000;
        else {
            throw new LoadException("line " + fTokenizer.lineno() + ": unknown timescale value "
                                    + getTokenString());
        }

        fNanoSecondsPerIncrement *= Long.parseLong(s.substring(0, unitStart));
        match("$end");
    }

    /// @returns true if there are more definitions, false if it has hit
    /// the end of the definitions section
    private boolean parseDefinition() throws LoadException, IOException {
        nextToken(true);
        if (getTokenString().equals("$scope"))
            parseScope();
        else if (getTokenString().equals("$var"))
            parseVar();
        else if (getTokenString().equals("$upscope"))
            parseUpscope();
        else if (getTokenString().equals("$timescale"))
            parseTimescale();
        else if (getTokenString().equals("$enddefinitions")) {
            match("$end");
            return false;
        } else {
            // Ignore this defintion
            do {
                nextToken(true);
            } while (!getTokenString().equals("$end"));
        }

        return true;
    }

    private boolean parseTransition() throws LoadException, IOException {
        fTotalTransitions++;

        if (!nextToken(false))
            return false;

        if (getTokenString().charAt(0) == '#') {
            // If the line begins with a #, this is a timestamp.
            fCurrentTime = Long.parseLong(getTokenString().substring(1))
                           * fNanoSecondsPerIncrement;
        } else {
            if (getTokenString().equals("$dumpvars") || getTokenString().equals("$end"))
                return true;

            String value;
            String id;

            if (getTokenString().charAt(0) == 'b') {
                // Multiple value net.  Value appears first, followed by space, then identifier
                value = getTokenString().substring(1);
                nextToken(true);
                id = getTokenString();
            } else {
                // Single value net.  identifier first, then value, no space.
                value = getTokenString().substring(0, 1);
                id = getTokenString().substring(1);
            }

            Net net = fNetMap.get(id);
            if (net == null) {
                throw new LoadException("line " + fTokenizer.lineno()
                    + ": Unknown net id " + id);
            }

            BitVector decodedValues = new BitVector(net.fWidth);
            if (value.equals("z") && net.fWidth > 1) {
                for (int i = 0; i < net.fWidth; i++)
                    decodedValues.setBit(i, BitVector.VALUE_Z);
            } else if (value.equals("x") && net.fWidth > 1) {
                for (int i = 0; i < net.fWidth; i++)
                    decodedValues.setBit(i, BitVector.VALUE_X);
            } else {
                // Decode and pad if necessary.
                // XXX should this be done inside BitVector
                int bitIndex = net.fWidth - 1;
                for (int i = 0; i < value.length(); i++) {
                    int bitValue;
                    switch (value.charAt(i)) {
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
                        throw new LoadException("line " + fTokenizer.lineno() + ": Invalid logic value");
                    }

                    decodedValues.setBit(bitIndex--, bitValue);
                }
            }

            fTraceBuilder.appendTransition(net.fBuilderID, fCurrentTime, decodedValues);
        }

        return true;
    }

    private void match(String value) throws LoadException, IOException {
        nextToken(true);
        if (!getTokenString().equals(value)) {
            throw new LoadException("line " + fTokenizer.lineno() + ": parse error, expected " + value + " got "
                                    + getTokenString());
        }
    }

    /// @param require If true and the next token is the end of file, this will throw an exception.
    /// @returns True if token was returned, false if not
    private boolean nextToken(boolean require) throws LoadException, IOException {
        if (fProgressListener != null) {
            // Update periodically
            long totalRead = fProgressStream.getTotalRead();
            if (totalRead - fLastProgressUpdate > 0x10000) {
                if (!fProgressListener.updateProgress((int)(totalRead * 100 / fFileLength)))
                    throw new LoadException("load cancelled");

                fLastProgressUpdate = totalRead;
            }
        }

        if (fTokenizer.nextToken() == StreamTokenizer.TT_EOF) {
            if (require) {
                throw new LoadException("line " + fTokenizer.lineno()
                                        + ": unexpected end of file");
            } else
                return false;
        }

        return true;
    }

    private String getTokenString() {
        return fTokenizer.sval;
    }

    private class ProgressInputStream extends InputStream {
        ProgressInputStream(InputStream wrapped) {
            fWrapped = wrapped;
        }

        @Override
        public int read() throws IOException {
            int got = fWrapped.read();
            if (got >= 0)
                fTotalRead++;

            return got;
        }

        // @note skip and byte array versions of read are not used by the Tokenizer

        long getTotalRead() {
            return fTotalRead;
        }

        private long fTotalRead;
        private InputStream fWrapped;
    }

    private StreamTokenizer fTokenizer;
    private TraceBuilder fTraceBuilder;
    private InputStream fInputStream;
    private long fCurrentTime;
    private HashMap<String, Net> fNetMap = new HashMap<String, Net>();
    private long fNanoSecondsPerIncrement;    /// @todo Switch to be unit agnostic
    private int fTotalTransitions;
    private ProgressListener fProgressListener;
    private ProgressInputStream fProgressStream;
    private long fLastProgressUpdate;
    private long fFileLength;
};

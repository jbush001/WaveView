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

import java.io.*;
import java.util.*;

///
/// Parse a value change dump (VCD) formatted text file and push the contents into a
/// provided trace model
/// All section references are to IEEE 1364-2001.
///
class VCDLoader implements TraceLoader {
    @Override
    public void load(File file, TraceBuilder builder, ProgressListener listener)
        throws LoadException, IOException {
        fProgressListener = listener;
        fProgressStream = new ProgressInputStream(new FileInputStream(file));
        fTokenizer = new StreamTokenizer(new BufferedReader(
            new InputStreamReader(fProgressStream)));
        fFileLength = file.length();
        fUpdateInterval = fFileLength / 100;
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

    private static class Net {
        Net(int builderID, int width) {
            fBuilderID = builderID;
            fWidth = width;
        }

        int fBuilderID;  /// ID given to this net by the builder
        int fWidth;
    }

    /// 18.2.3.4 $scope
    /// var_declaration_scope ::= $scope scope_type scope_identifier $end
    /// scope_type ::= begin | fork | function | module | task
    /// @todo Record type and put into model
    private void parseScope() throws LoadException, IOException {
        nextToken(true);   // Scope type
        nextToken(true);
        String scopeIdentifier = getTokenString();
        fTraceBuilder.enterScope(scopeIdentifier);
        match("$end");
    }

    // 18.2.3.6 $upscope
    private void parseUpscope() throws LoadException, IOException {
        match("$end");
        fTraceBuilder.exitScope();
    }

    /// 18.2.3.8 $var
    /// vcd_declaration_vars ::= $var var_type identifer_code reference $end
    /// var_type ::= event | integer | parameter | real | reg | supply0
    ///              | tri | triand | trior | tri0 | tri1 | wand | wire | wor
    /// size ::= decimal_number
    /// reference ::= identifier | identifier [bit_select_index]
    ///               | identifier[msb_index:lsb_index]
    /// index := decimal_number
    private void parseVar() throws LoadException, IOException {
        nextToken(true);    // type (ignored)
        nextToken(true);    // size
        int width = Integer.parseInt(getTokenString());

        nextToken(true);
        String id = getTokenString();
        nextToken(true);
        String netName = getTokenString();

        // Bit select index (ignore)
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

    /// 18.2.3.5 $timescale
    /// vcd_declaration_timescale ::= $timescale time_number time_unit $end
    /// time_number ::= 1 | 10 | 100
    /// time_unit ::= s | ms | us | ns | ps | fs
    private void parseTimescale() throws LoadException, IOException {
        nextToken(true);

        String s = getTokenString();
        int unitStart = 0;
        while (unitStart < s.length() && Character.isDigit(s.charAt(unitStart)))
            unitStart++;

        String unit = s.substring(unitStart);
        int order = 1;
        if (unit.equals("fs"))
            order = -15;
        else if (unit.equals("ps"))
            order = -12;
        else if (unit.equals("ns"))
            order = -9;
        else if (unit.equals("us"))
            order = -6;
        else if (unit.equals("ms"))
            order = -3;
        else if (unit.equals("s"))
            order = 0;
        else {
            throw new LoadException("line " + fTokenizer.lineno()
                + ": unknown timescale value " + getTokenString());
        }

        int timeNumber = Integer.parseInt(s.substring(0, unitStart));
        if (timeNumber == 100)
            order += 2;
        else if (timeNumber == 10)
            order += 1;
        else if (timeNumber != 1) {
            throw new LoadException("line " + fTokenizer.lineno()
                + ": bad timescale value " + getTokenString());
        }

        match("$end");
        fTraceBuilder.setTimescale(order);
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
            long nextTimestamp = Long.parseLong(getTokenString().substring(1));
            if (nextTimestamp >= fCurrentTime)
                fCurrentTime = nextTimestamp;
            else
                System.out.println("warning: timestamp out of order line " + fTokenizer.lineno());
        } else {
            if (getTokenString().equals("$dumpvars") || getTokenString().equals("$end"))
                return true;

            String value;
            String id;
            char leadingVal = getTokenString().charAt(0);

            // @todo Does not support real types.
            switch (leadingVal) {
                case '0':
                case '1':
                case 'z':
                case 'Z':
                case 'x':
                case 'X':
                    // Single bit value
                    // 18.2.1 scalar_value_change ::= value identifier_code
                    // (no space)
                    value = getTokenString().substring(0, 1);
                    id = getTokenString().substring(1);
                    break;

                case 'b':
                    // Multi bit value
                    // 18.2.1 vector_value_change ::= b binary_number identification_code
                    value = getTokenString().substring(1);
                    nextToken(true);
                    id = getTokenString();
                    break;

                case 'r':
                case 'R':
                    throw new LoadException("line " + fTokenizer.lineno()
                        + ": real values are not supported");

                default:
                    throw new LoadException("line " + fTokenizer.lineno()
                        + ": invalid value type '" + leadingVal + "'");
            }

            Net net = fNetMap.get(id);
            if (net == null) {
                throw new LoadException("line " + fTokenizer.lineno()
                    + ": Unknown net id " + id);
            }

            BitVector decodedValues = new BitVector(net.fWidth);

            // Decode and pad if necessary.
            // 18.2.1 value ::= 0 | 1 | x | X | z | Z
            int valueLength = value.length();
            int bitsToCopy = Math.min(valueLength, net.fWidth);
            int outBit = 0;
            int bitValue = BitVector.VALUE_0;
            while (outBit < bitsToCopy) {
                switch (value.charAt(valueLength - outBit - 1)) {
                case 'z':
                case 'Z':
                    bitValue = BitVector.VALUE_Z;
                    break;

                case 'x':
                case 'X':
                    bitValue = BitVector.VALUE_X;
                    break;

                case '1':
                    bitValue = BitVector.VALUE_1;
                    break;

                case '0':
                    bitValue = BitVector.VALUE_0;
                    break;

                default:
                    throw new LoadException("line " + fTokenizer.lineno()
                        + ": invalid logic value");
                }

                decodedValues.setBit(outBit++, bitValue);
            }

            // Table 83: Rules for left-extending vector values
            // 0 & 1 extend with 0. Z extends with Z, X extends with X.
            int padValue;
            if (bitValue == BitVector.VALUE_Z)
                padValue = BitVector.VALUE_Z;
            else if (bitValue == BitVector.VALUE_X)
                padValue = BitVector.VALUE_X;
            else
                padValue = BitVector.VALUE_0;

            while (outBit < net.fWidth)
                decodedValues.setBit(outBit++, padValue);

            fTraceBuilder.appendTransition(net.fBuilderID, fCurrentTime, decodedValues);
        }

        return true;
    }

    private void match(String value) throws LoadException, IOException {
        nextToken(true);
        if (!getTokenString().equals(value)) {
            throw new LoadException("line " + fTokenizer.lineno()
                + ": parse error, expected " + value + " got "
                + getTokenString());
        }
    }

    /// @param require If true and the next token is the end of file, this will
    ///        throw an exception.
    /// @returns True if token was returned, false if not
    private boolean nextToken(boolean require) throws LoadException, IOException {
        if (fProgressListener != null) {
            // Update periodically
            long totalRead = fProgressStream.getTotalRead();
            if (totalRead - fLastProgressUpdate > fUpdateInterval) {
                if (!fProgressListener.updateProgress((int)(totalRead
                    * 100 / fFileLength)))
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

    private static class ProgressInputStream extends InputStream {
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

        @Override
        public int read(byte[] b) throws IOException {
            int got = fWrapped.read(b);
            if (got >= 0)
                fTotalRead += got;

            return got;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int got = fWrapped.read(b, off, len);
            if (got >= 0)
                fTotalRead += got;

            return got;
        }

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
    private int fTotalTransitions;
    private ProgressListener fProgressListener;
    private ProgressInputStream fProgressStream;
    private long fLastProgressUpdate;
    private long fFileLength;
    private long fUpdateInterval;
};

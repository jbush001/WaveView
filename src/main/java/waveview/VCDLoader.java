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

package waveview;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.HashMap;

///
/// Parse a value change dump (VCD) formatted text file and push the contents into a
/// provided trace model
/// All section references are to IEEE 1364-2001.
///
public class VCDLoader implements TraceLoader {
    private StreamTokenizer tokenizer;
    private TraceBuilder traceBuilder;
    private long currentTime;
    private final HashMap<String, Net> netMap = new HashMap<>();
    private int totalTransitions;
    private ProgressListener progressListener;
    private ProgressInputStream progressStream;
    private long lastProgressUpdate;
    private long fileLength;
    private long updateInterval;

    @Override
    public void load(File file, TraceBuilder traceBuilder, ProgressListener progressListener)
            throws LoadException, IOException {
        this.progressListener = progressListener;
        this.traceBuilder = traceBuilder;

        try (FileInputStream inputStream = new FileInputStream(file))  {
            progressStream = new ProgressInputStream(inputStream);
            tokenizer = new StreamTokenizer(new BufferedReader(new InputStreamReader(progressStream, "UTF8")));
            fileLength = file.length();
            updateInterval = fileLength / 100;
            tokenizer.resetSyntax();
            tokenizer.wordChars(33, 126);
            tokenizer.whitespaceChars('\r', '\r');
            tokenizer.whitespaceChars('\n', '\n');
            tokenizer.whitespaceChars(' ', ' ');
            tokenizer.whitespaceChars('\t', '\t');

            while (nextToken(false)) {
                if (getTokenString().charAt(0) == '$')
                    parseDefinition();
                else
                    parseTransition();
            }

            traceBuilder.loadFinished();
        }

        System.out.println("parsed " + totalTransitions + " total transitions");
        System.out.println(Integer.toString(netMap.size()) + " total nets");
    }

    private static class Net {
        final int builderId; /// ID given to this net by the builder
        final int width;

        Net(int builderId, int width) {
            this.builderId = builderId;
            this.width = width;
        }
    }

    /// 18.2.3.4 $scope
    /// var_declaration_scope ::= $scope scope_type scope_identifier $end
    /// scope_type ::= begin | fork | function | module | task
    /// @todo Record type and put into model
    private void parseScope() throws LoadException, IOException {
        nextToken(true); // Scope type
        nextToken(true);
        String scopeIdentifier = getTokenString();
        traceBuilder.enterScope(scopeIdentifier);
        match("$end");
    }

    // 18.2.3.6 $upscope
    private void parseUpscope() throws LoadException, IOException {
        match("$end");
        traceBuilder.exitScope();
    }

    /// 18.2.3.8 $var
    /// vcd_declaration_vars ::= $var var_type identifer_code reference $end
    /// var_type ::= event | integer | parameter | real | reg | supply0
    /// | tri | triand | trior | tri0 | tri1 | wand | wire | wor
    /// size ::= decimal_number
    /// reference ::= identifier | identifier [bit_select_index]
    /// | identifier[msb_index:lsb_index]
    /// index := decimal_number
    private void parseVar() throws LoadException, IOException {
        nextToken(true); // type (ignored)
        nextToken(true); // size
        int width = Integer.parseInt(getTokenString());

        nextToken(true);
        String id = getTokenString();
        nextToken(true);
        String netName = getTokenString();

        // Bit select index (ignore)
        nextToken(true);
        if (getTokenString().charAt(0) != '[')
            tokenizer.pushBack();

        match("$end");

        Net net = netMap.get(id);
        if (net == null) {
            // We've never seen this net before
            // Strip off the width declaration
            int openBracket = netName.indexOf('[');
            if (openBracket != -1) {
                netName = netName.substring(0, openBracket);
            }

            net = new Net(traceBuilder.newNet(netName, -1, width), width);
            netMap.put(id, net);
        } else {
            // Shares data with existing net. Add as clone.
            traceBuilder.newNet(netName, net.builderId, width);
        }
    }

    /// 18.2.3.5 $timescale
    /// vcd_declaration_timescale ::= $timescale time_number time_unit $end
    /// time_number ::= 1 | 10 | 100
    /// time_unit ::= s | ms | us | ns | ps | fs
    private void parseTimescale() throws LoadException, IOException {
        nextToken(true);
        String s = getTokenString();

        // Check if the unit is part of the same token, e.g. 1ps:
        int unitStart = 0;
        while (unitStart < s.length() && !Character.isAlphabetic(s.charAt(unitStart))) {
            unitStart++;
        }

        String unit;
        if (unitStart < s.length()) {
            // Found unit inside token, split out
            unit = s.substring(unitStart);
        } else {
            // Unit is next token (there's a space between number and unit)
            nextToken(true);
            unit = getTokenString();
        }

        int order = 1;
        switch (unit) {
        case "fs":
            order = -15;
            break;
        case "ps":
            order = -12;
            break;
        case "ns":
            order = -9;
            break;
        case "us":
            order = -6;
            break;
        case "ms":
            order = -3;
            break;
        case "s":
            order = 0;
            break;
        default:
            throw new LoadException("line " + tokenizer.lineno() + ": unknown timescale value " + getTokenString());
        }

        int timeNumber = Integer.parseInt(s.substring(0, unitStart));
        if (timeNumber == 100) {
            order += 2;
        } else if (timeNumber == 10) {
            order += 1;
        } else if (timeNumber != 1) {
            throw new LoadException("line " + tokenizer.lineno() + ": bad timescale value " + getTokenString());
        }

        match("$end");
        traceBuilder.setTimescale(order);
    }

    /// @returns true if there are more definitions, false if it has hit
    /// the end of the definitions section
    private void parseDefinition() throws LoadException, IOException {
        switch (getTokenString()) {
        case "$scope":
            parseScope();
            break;
        case "$var":
            parseVar();
            break;
        case "$upscope":
            parseUpscope();
            break;
        case "$timescale":
            parseTimescale();
            break;
        case "$enddefinitions":
            match("$end");
            break;
        case "$dumpvars":
        case "$end":
            // ignore directive, but not what comes in-between
            break;
        default:
            // Ignore everything inside this definition.
            do {
                nextToken(true);
            } while (!getTokenString().equals("$end"));
            break;
        }
    }

    private void parseTransition() throws LoadException, IOException {
        totalTransitions++;
        char leadingVal = getTokenString().charAt(0);
        if (leadingVal == '#') {
            // If the line begins with a #, this is a timestamp.
            long nextTimestamp = Long.parseLong(getTokenString().substring(1));
            if (nextTimestamp >= currentTime) {
                currentTime = nextTimestamp;
            } else {
                System.out.println("warning: timestamp out of order line " + tokenizer.lineno());
            }
        } else {
            String value;
            String id;

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
                throw new LoadException("line " + tokenizer.lineno() + ": real values are not supported");
            default:
                throw new LoadException("line " + tokenizer.lineno() + ": invalid value type '" + leadingVal + "'");
            }

            Net net = netMap.get(id);
            if (net == null) {
                throw new LoadException("line " + tokenizer.lineno() + ": Unknown net id " + id);
            }

            BitVector decodedValues = new BitVector(net.width);

            // Decode and pad if necessary.
            // 18.2.1 value ::= 0 | 1 | x | X | z | Z
            int valueLength = value.length();
            int bitsToCopy = Math.min(valueLength, net.width);
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
                    throw new LoadException("line " + tokenizer.lineno() + ": invalid logic value");
                }

                decodedValues.setBit(outBit++, bitValue);
            }

            // Table 83: Rules for left-extending vector values
            // 0 & 1 extend with 0. Z extends with Z, X extends with X.
            int padValue;
            if (bitValue == BitVector.VALUE_Z) {
                padValue = BitVector.VALUE_Z;
            } else if (bitValue == BitVector.VALUE_X) {
                padValue = BitVector.VALUE_X;
            } else {
                padValue = BitVector.VALUE_0;
            }

            while (outBit < net.width) {
                decodedValues.setBit(outBit++, padValue);
            }

            traceBuilder.appendTransition(net.builderId, currentTime, decodedValues);
        }
    }

    private void match(String value) throws LoadException, IOException {
        nextToken(true);
        if (!getTokenString().equals(value)) {
            throw new LoadException(
                    "line " + tokenizer.lineno() + ": parse error, expected " + value + " got " + getTokenString());
        }
    }

    /// @param require If true and the next token is the end of file, this will
    /// throw an exception.
    /// @returns True if token was returned, false if not
    private boolean nextToken(boolean require) throws LoadException, IOException {
        if (progressListener != null) {
            // Update periodically
            long totalRead = progressStream.getTotalRead();
            if (totalRead - lastProgressUpdate > updateInterval) {
                if (!progressListener.updateProgress((int) (totalRead * 100 / fileLength))) {
                    throw new LoadException("load cancelled");
                }

                lastProgressUpdate = totalRead;
            }
        }

        if (tokenizer.nextToken() == StreamTokenizer.TT_EOF) {
            if (require) {
                throw new LoadException("line " + tokenizer.lineno() + ": unexpected end of file");
            } else
                return false;
        }

        return true;
    }

    private String getTokenString() {
        return tokenizer.sval;
    }

    private static class ProgressInputStream extends InputStream {
        private long totalRead;
        private final InputStream wrapped;

        ProgressInputStream(InputStream wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void close() throws IOException {
            wrapped.close();
        }

        @Override
        public int read() throws IOException {
            int got = wrapped.read();
            if (got >= 0) {
                totalRead++;
            }

            return got;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int got = wrapped.read(b);
            if (got >= 0) {
                totalRead += got;
            }

            return got;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int got = wrapped.read(b, off, len);
            if (got >= 0) {
                totalRead += got;
            }

            return got;
        }

        long getTotalRead() {
            return totalRead;
        }
    }
}

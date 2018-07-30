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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

///
/// Parse a value change dump (VCD) formatted text file and push the contents into a
/// provided waveform model
/// All section references are to IEEE 1364-2001.
///
public class VCDLoader implements WaveformLoader {
    private static class Var {
        Var(int netIndex, int width) {
            this.netIndex = netIndex;
            this.width = width;
        }

        int netIndex;
        int width;
    }

    private StreamTokenizer tokenizer;
    private WaveformBuilder waveformBuilder;
    private long currentTime;
    private final Map<String, Var> varMap = new HashMap<>();
    private int totalTransitions;
    private ProgressListener progressListener;
    private long fileLength;
    private int nextNetIndex;

    @Override
    public void load(File file, WaveformBuilder waveformBuilder, ProgressListener progressListener)
            throws IOException {
        this.waveformBuilder = waveformBuilder;
        this.progressListener = progressListener;
        fileLength = file.length();

        try (InputStream inputStream = new FileInputStream(file))  {
            long updateInterval = fileLength / 100;
            InputStream progressStream = new ProgressInputStream(inputStream,
                    (totalRead) -> updateProgress(totalRead), updateInterval);
            initTokenizer(progressStream);
            parseFile();
        }

        System.out.println("parsed " + totalTransitions + " total transitions");
        System.out.println(Integer.toString(varMap.size()) + " total nets");
    }

    private void updateProgress(long totalRead) throws IOException {
        if (progressListener != null &&
                !progressListener.updateProgress((int) (totalRead * 100 / fileLength))) {
            throw new LoadFormatException("load cancelled");
        }
    }

    private void initTokenizer(InputStream inputStream) {
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        tokenizer = new StreamTokenizer(new BufferedReader(reader));
        tokenizer.resetSyntax();
        tokenizer.wordChars(33, 126);
        tokenizer.whitespaceChars('\r', '\r');
        tokenizer.whitespaceChars('\n', '\n');
        tokenizer.whitespaceChars(' ', ' ');
        tokenizer.whitespaceChars('\t', '\t');
    }

    private void parseFile() throws IOException {
        while (nextToken(false)) {
            char leading = getTokenString().charAt(0);
            if (leading == '$') {
                parseDefinition();
            } else if (leading == '#') {
                parseTimestamp();
            } else {
                parseTransition();
            }
        }

        waveformBuilder.loadFinished();
    }

    /// @returns true if there are more definitions, false if it has hit
    /// the end of the definitions section
    private void parseDefinition() throws IOException {
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
                ignoreUntilDollarEnd();
                break;
        }
    }

    // Unknown definition, throw away tokens until the end
    private void ignoreUntilDollarEnd() throws IOException {
        do {
            nextToken(true);
        } while (!getTokenString().equals("$end"));
    }

    private void parseTimestamp() {
        // If the line begins with a #, this is a timestamp.
        long nextTimestamp = Long.parseLong(getTokenString().substring(1));
        if (nextTimestamp >= currentTime) {
            currentTime = nextTimestamp;
        } else {
            System.out.println("warning: timestamp out of order line " + tokenizer.lineno());
        }
    }

    /// 18.2.3.4 $scope
    /// var_declaration_scope ::= $scope scope_type scope_identifier $end
    /// scope_type ::= begin | fork | function | module | task
    /// @todo Record type and put into model
    private void parseScope() throws IOException {
        nextToken(true); // Scope type
        nextToken(true);
        String scopeIdentifier = getTokenString();
        waveformBuilder.enterScope(scopeIdentifier);
        match("$end");
    }

    // 18.2.3.6 $upscope
    private void parseUpscope() throws IOException {
        match("$end");
        waveformBuilder.exitScope();
    }

    /// 18.2.3.8 $var
    /// vcd_declaration_vars ::= $var var_type identifer_code reference $end
    /// var_type ::= event | integer | parameter | real | reg | supply0
    /// | tri | triand | trior | tri0 | tri1 | wand | wire | wor
    /// size ::= decimal_number
    /// reference ::= identifier | identifier [bit_select_index]
    /// | identifier[msb_index:lsb_index]
    /// index := decimal_number
    private void parseVar() throws IOException {
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

        Var var = varMap.get(id);
        if (var == null) {
            // We've never seen this var before
            // Strip off the width declaration
            int openBracket = netName.indexOf('[');
            if (openBracket != -1) {
                netName = netName.substring(0, openBracket);
            }

            waveformBuilder.newNet(nextNetIndex, netName, width);
            varMap.put(id, new Var(nextNetIndex, width));
            nextNetIndex++;
        } else {
            if (width != var.width) {
                throw new LoadFormatException("line " + tokenizer.lineno()
                    + ": alias net does not match width of parent (" + width + " != " + var.width + ")");
            }

            // Shares data with existing net. Add as alias.
            waveformBuilder.newNet(var.netIndex, netName, width);
        }
    }

    /// 18.2.3.5 $timescale
    /// vcd_declaration_timescale ::= $timescale time_number time_unit $end
    /// time_number ::= 1 | 10 | 100
    /// time_unit ::= s | ms | us | ns | ps | fs
    private void parseTimescale() throws IOException {
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
                throw new LoadFormatException("line " + tokenizer.lineno() + ": unknown timescale value " + getTokenString());
        }

        int timeNumber = Integer.parseInt(s.substring(0, unitStart));
        if (timeNumber == 100) {
            order += 2;
        } else if (timeNumber == 10) {
            order += 1;
        } else if (timeNumber != 1) {
            throw new LoadFormatException("line " + tokenizer.lineno() + ": bad timescale value " + getTokenString());
        }

        match("$end");
        waveformBuilder.setTimescale(order);
    }

    private void parseTransition() throws IOException {
        totalTransitions++;
        char leadingVal = getTokenString().charAt(0);
        String value;
        String id;

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
                throw new LoadFormatException("line " + tokenizer.lineno() + ": real values are not supported");
            default:
                throw new LoadFormatException("line " + tokenizer.lineno() + ": invalid value type '" + leadingVal + "'");
        }

        Var var = varMap.get(id);
        if (var == null) {
            throw new LoadFormatException("line " + tokenizer.lineno() + ": Unknown var id " + id);
        }

        BitVector decodedValues = decodeBinaryValueString(value, var.width);
        waveformBuilder.appendTransition(var.netIndex, currentTime, decodedValues);
    }

    private BitVector decodeBinaryValueString(String valueString, int width) throws LoadFormatException {
        BitVector value = new BitVector(width);

        // Decode and pad if necessary.
        // 18.2.1 value ::= 0 | 1 | x | X | z | Z
        int valueLength = valueString.length();
        int bitsToCopy = Math.min(valueLength, width);
        BitValue bitValue = BitValue.ZERO;
        int outBit = 0;
        try {
            // Reading from right to left
            while (outBit < bitsToCopy) {
                bitValue = BitValue.fromChar(valueString.charAt(valueLength - outBit - 1));
                value.setBit(outBit++, bitValue);
            }
        } catch (NumberFormatException exc) {
            throw new LoadFormatException("line " + tokenizer.lineno() + ": invalid logic value");
        }

        // Table 83: Rules for left-extending vector values
        // 0 & 1 extend with 0. Z extends with Z, X extends with X.
        BitValue padValue;
        if (bitValue == BitValue.Z) {
            padValue = BitValue.Z;
        } else if (bitValue == BitValue.X) {
            padValue = BitValue.X;
        } else {
            padValue = BitValue.ZERO;
        }

        while (outBit < width) {
            value.setBit(outBit++, padValue);
        }

        return value;
    }

    private void match(String value) throws IOException {
        nextToken(true);
        if (!getTokenString().equals(value)) {
            throw new LoadFormatException(
                    "line " + tokenizer.lineno() + ": parse error, expected " + value + " got " + getTokenString());
        }
    }

    /// @param require If true and the next token is the end of file, this will
    /// throw an exception.
    /// @returns True if token was returned, false if not
    private boolean nextToken(boolean require) throws IOException {
        if (tokenizer.nextToken() == StreamTokenizer.TT_EOF) {
            if (require) {
                throw new LoadFormatException("line " + tokenizer.lineno() + ": unexpected end of file");
            } else
                return false;
        }

        return true;
    }

    private String getTokenString() {
        return tokenizer.sval;
    }
}

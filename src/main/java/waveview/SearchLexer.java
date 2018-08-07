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

final class SearchLexer {
    static final int TOK_IDENTIFIER = 1000;
    static final int TOK_END = 1001;
    static final int TOK_LITERAL = 1002;
    static final int TOK_GREATER = 1003;
    static final int TOK_GREATER_EQUAL = 1004;
    static final int TOK_LESS_THAN = 1005;
    static final int TOK_LESS_EQUAL = 1006;
    static final int TOK_NOT_EQUAL = 1007;

    private enum State {
        SCAN_INIT, SCAN_IDENTIFIER, SCAN_LITERAL_TYPE, SCAN_GEN_NUM, SCAN_GREATER, SCAN_LESS, SCAN_BINARY,
        SCAN_DECIMAL, SCAN_HEXADECIMAL
    }

    private int lexerOffset;
    private final StringBuilder currentTokenValue = new StringBuilder();
    private int pushBackChar = -1;
    private int pushBackToken = -1;
    private int tokenStart;
    private BitVector literalValue;
    private final String searchString;

    SearchLexer(String searchString) {
        this.searchString = searchString;
    }

    int nextToken() throws SearchFormatException {
        if (pushBackToken != -1) {
            int token = pushBackToken;
            pushBackToken = -1;
            return token;
        }

        State state = State.SCAN_INIT;
        currentTokenValue.setLength(0);

        for (;;) {
            int c;

            if (pushBackChar == -1) {
                if (lexerOffset == searchString.length()) {
                    c = -1;
                } else {
                    c = searchString.charAt(lexerOffset++);
                }
            } else {
                c = pushBackChar;
                pushBackChar = -1;
            }

            switch (state) {
                case SCAN_INIT:
                    tokenStart = lexerOffset - 1;
                    if (c == -1) {
                        return TOK_END;
                    } else if (c == '\'') {
                        state = State.SCAN_LITERAL_TYPE;
                    } else if (isAlpha(c)) {
                        pushBackChar = c;
                        state = State.SCAN_IDENTIFIER;
                    } else if (isNum(c)) {
                        pushBackChar = c;
                        state = State.SCAN_DECIMAL;
                    } else if (c == '>') {
                        state = State.SCAN_GREATER;
                    } else if (c == '<') {
                        state = State.SCAN_LESS;
                    } else if (!isSpace(c)) {
                        return c;
                    }

                    break;

                case SCAN_GREATER:
                    if (c == '<') {
                        return TOK_NOT_EQUAL;
                    } else if (c == '=') {
                        return TOK_GREATER_EQUAL;
                    } else {
                        pushBackChar = c;
                        return TOK_GREATER;
                    }

                case SCAN_LESS:
                    if (c == '>') {
                        return TOK_NOT_EQUAL;
                    } else if (c == '=') {
                        return TOK_LESS_EQUAL;
                    } else {
                        pushBackChar = c;
                        return TOK_LESS_THAN;
                    }

                case SCAN_IDENTIFIER:
                    if (isAlphaNum(c) || c == '_' || c == '.') {
                        currentTokenValue.append((char) c);
                    } else if (c == '(') { // Start generate index
                        currentTokenValue.append((char) c);
                        state = State.SCAN_GEN_NUM;
                    } else {
                        pushBackChar = c;
                        return TOK_IDENTIFIER;
                    }

                    break;

                case SCAN_GEN_NUM:
                    currentTokenValue.append((char) c);
                    if (c == ')') {
                        state = State.SCAN_IDENTIFIER;
                    }

                    break;

                case SCAN_LITERAL_TYPE:
                    if (c == 'b') {
                        state = State.SCAN_BINARY;
                    } else if (c == 'h') {
                        state = State.SCAN_HEXADECIMAL;
                    } else if (c == 'd') {
                        state = State.SCAN_DECIMAL;
                    } else {
                        throw new SearchFormatException("unknown type " + (char) c, getTokenStart(), getTokenEnd());
                    }

                    break;

                case SCAN_BINARY:
                    if (c == '0' || c == '1' || c == 'x' || c == 'z' || c == 'X' || c == 'Z') {
                        currentTokenValue.append((char) c);
                    } else {
                        literalValue = new BitVector(getTokenString(), 2);
                        pushBackChar = c;
                        return TOK_LITERAL;
                    }

                    break;

                case SCAN_DECIMAL:
                    if (c >= '0' && c <= '9') {
                        currentTokenValue.append((char) c);
                    } else {
                        literalValue = new BitVector(getTokenString(), 10);
                        pushBackChar = c;
                        return TOK_LITERAL;
                    }

                    break;

                case SCAN_HEXADECIMAL:
                    if (isHexDigit(c) || c == 'x' || c == 'z' || c == 'X' || c == 'Z') {
                        currentTokenValue.append((char) c);
                    } else {
                        literalValue = new BitVector(getTokenString(), 16);
                        pushBackChar = c;
                        return TOK_LITERAL;
                    }

                    break;
            }
        }
    }

    private static boolean isAlpha(int value) {
        return (value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z');
    }

    private static boolean isNum(int value) {
        return value >= '0' && value <= '9';
    }

    private static boolean isHexDigit(int value) {
        return (value >= '0' && value <= '9') || (value >= 'a' && value <= 'f') || (value >= 'A' && value <= 'F');
    }

    private static boolean isAlphaNum(int value) {
        return isAlpha(value) || isNum(value);
    }

    private static boolean isSpace(int value) {
        return value == ' ' || value == '\t' || value == '\n' || value == '\r';
    }

    void pushBackToken(int tok) {
        pushBackToken = tok;
    }

    String getTokenString() {
        return currentTokenValue.toString();
    }

    BitVector getLiteralValue() {
        return literalValue;
    }

    int getTokenStart() {
        return tokenStart;
    }

    int getTokenEnd() {
        if (currentTokenValue.length() == 0) {
            return tokenStart;
        } else {
            return tokenStart + currentTokenValue.length() - 1;
        }
    }
}
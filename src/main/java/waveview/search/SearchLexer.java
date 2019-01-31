//
// Copyright 2011-2019 Jeff Bush
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

package waveview.search;

import waveview.wavedata.BitVector;

final class SearchLexer {
    private enum State {
        SCAN_INIT,
        SCAN_IDENTIFIER,
        SCAN_LITERAL_TYPE,
        SCAN_LITERAL_VALUE,
        SCAN_GEN_NUM,
        SCAN_GREATER,
        SCAN_LESS,
        SCAN_BANG,
        SCAN_EQUAL,
        SCAN_AMPERSAND,
        SCAN_PIPE
    }

    private int lexerOffset;
    private boolean pushedBackToken;
    private Token lastToken;
    private final String searchString;

    SearchLexer(String searchString) {
        this.searchString = searchString;
    }

    Token nextToken() throws SearchFormatException {
        if (pushedBackToken) {
            pushedBackToken = false;
            return lastToken;
        }

        lastToken = scanNextToken();
        return lastToken;
    }

    void pushBackToken() {
        assert !pushedBackToken;

        pushedBackToken = true;
    }

    private Token scanNextToken() throws SearchFormatException {
        StringBuilder currentTokenValue = new StringBuilder();
        int tokenStart = lexerOffset;
        State state = State.SCAN_INIT;
        int literalRadix = 10;

        for (;;) {
            int c = nextChar();
            switch (state) {
                case SCAN_INIT:
                    tokenStart = lexerOffset - 1;
                    if (c == -1) {
                        return new Token(Token.Type.END, lexerOffset - 1, c);
                    } else if (c == '\'') {
                        state = State.SCAN_LITERAL_TYPE;
                    } else if (isAlpha(c)) {
                        pushBackChar();
                        state = State.SCAN_IDENTIFIER;
                    } else if (isNum(c)) {
                        pushBackChar();
                        literalRadix = 10;
                        state = State.SCAN_LITERAL_VALUE;
                    } else {
                        switch (c) {
                            case '!':
                                state = State.SCAN_BANG;
                                break;
                            case '=':
                                state = State.SCAN_EQUAL;
                                break;
                            case '&':
                                state = State.SCAN_AMPERSAND;
                                break;
                            case '|':
                                state = State.SCAN_PIPE;
                                break;
                            case '>':
                                state = State.SCAN_GREATER;
                                break;
                            case '<':
                                state = State.SCAN_LESS;
                                break;
                            case '(':
                                return new Token(Token.Type.LPAREN, lexerOffset - 1, c);
                            case ')':
                                return new Token(Token.Type.RPAREN, lexerOffset - 1, c);
                            case '[':
                                return new Token(Token.Type.LBRACKET, lexerOffset - 1, c);
                            case ']':
                                return new Token(Token.Type.RBRACKET, lexerOffset - 1, c);
                            case ':':
                                return new Token(Token.Type.COLON, lexerOffset - 1, c);
                            default:
                                if (!isSpace(c)) {
                                    throw new SearchFormatException("Unknown character " + (char) c,
                                        lexerOffset - 1, lexerOffset - 1);
                                }
                        }
                    }

                    break;

                case SCAN_GREATER:
                    if (c == '=') {
                        return new Token(Token.Type.GREATER_EQUAL, tokenStart, c);
                    } else {
                        pushBackChar();
                        return new Token(Token.Type.GREATER, tokenStart, c);
                    }

                case SCAN_LESS:
                    if (c == '=') {
                        return new Token(Token.Type.LESS_EQUAL, tokenStart, c);
                    } else {
                        pushBackChar();
                        return new Token(Token.Type.LESS_THAN, tokenStart, c);
                    }

                case SCAN_BANG:
                    if (c == '=') {
                        return new Token(
                            Token.Type.NOT_EQUAL, tokenStart, tokenStart + 1, "!=", null);
                    } else {
                        throw new SearchFormatException(
                            "Unknown character !", tokenStart, tokenStart);
                    }

                case SCAN_EQUAL:
                    if (c == '=') {
                        return new Token(Token.Type.EQUAL, tokenStart, tokenStart + 1, "==", null);
                    } else {
                        throw new SearchFormatException(
                            "Unknown character =", tokenStart, tokenStart);
                    }

                case SCAN_AMPERSAND:
                    if (c == '&') {
                        return new Token(
                            Token.Type.DOUBLE_AMPERSAND, tokenStart, tokenStart + 1, "!=", null);
                    } else {
                        throw new SearchFormatException(
                            "Unknown character &", tokenStart, tokenStart);
                    }

                case SCAN_PIPE:
                    if (c == '|') {
                        return new Token(
                            Token.Type.DOUBLE_PIPE, tokenStart, tokenStart + 1, "!=", null);
                    } else {
                        throw new SearchFormatException(
                            "Unknown character |", tokenStart, tokenStart);
                    }

                case SCAN_IDENTIFIER:
                    if (isAlphaNum(c) || c == '_' || c == '.') {
                        currentTokenValue.append((char) c);
                    } else if (c == '(') { // Start generate index
                        currentTokenValue.append((char) c);
                        state = State.SCAN_GEN_NUM;
                    } else {
                        pushBackChar();
                        return new Token(Token.Type.IDENTIFIER, tokenStart, lexerOffset - 1,
                            currentTokenValue.toString(), null);
                    }

                    break;

                case SCAN_GEN_NUM:
                    currentTokenValue.append((char) c);
                    if (c == ')') {
                        state = State.SCAN_IDENTIFIER;
                    }

                    break;

                case SCAN_LITERAL_TYPE:
                    if (c == 'b' || c == 'B') {
                        literalRadix = 2;
                    } else if (c == 'o' || c == 'O') {
                        literalRadix = 8;
                    } else if (c == 'd' || c == 'D') {
                        literalRadix = 10;
                    } else if (c == 'h' || c == 'H') {
                        literalRadix = 16;
                    } else {
                        throw new SearchFormatException(
                            "Unknown type " + (char) c, tokenStart, lexerOffset - 1);
                    }

                    state = State.SCAN_LITERAL_VALUE;
                    break;

                case SCAN_LITERAL_VALUE:
                    if (isValidDigit(c, literalRadix) || c == 'x' || c == 'z'
                            || c == 'X' || c == 'Z') {
                        currentTokenValue.append((char) c);
                    } else {
                        BitVector literalValue = new BitVector(currentTokenValue.toString(),
                            literalRadix);
                        pushBackChar();
                        return new Token(Token.Type.LITERAL, tokenStart, lexerOffset - 1,
                            currentTokenValue.toString(), literalValue);
                    }

                    break;
            }
        }
    }

    private int nextChar() {
        assert lexerOffset <= searchString.length() + 1;

        if (lexerOffset == searchString.length()) {
            lexerOffset++;
            return -1;
        }

        return searchString.charAt(lexerOffset++);
    }

    private void pushBackChar() {
        lexerOffset--;
    }

    private static boolean isAlpha(int value) {
        return (value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z');
    }

    private static boolean isNum(int value) {
        return value >= '0' && value <= '9';
    }

    private static boolean isValidDigit(int value, int radix) {
        if (value >= '0' && value <= '9') {
            return (value - '0') < radix;
        } else if (value >= 'A' && value <= 'F') {
            return radix == 16;
        } else if (value >= 'a' && value <= 'f') {
            return radix == 16;
        } else {
            return false;
        }
    }

    private static boolean isAlphaNum(int value) {
        return isAlpha(value) || isNum(value);
    }

    private static boolean isSpace(int value) {
        return value == ' ' || value == '\t' || value == '\n' || value == '\r';
    }
}

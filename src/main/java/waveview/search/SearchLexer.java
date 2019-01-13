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
        SCAN_GEN_NUM,
        SCAN_GREATER,
        SCAN_LESS,
        SCAN_BANG,
        SCAN_EQUAL,
        SCAN_AMPERSAND,
        SCAN_PIPE,
        SCAN_BINARY,
        SCAN_DECIMAL,
        SCAN_HEXADECIMAL
    }

    private int lexerOffset;
    private boolean pushedBackToken;
    private Token lastToken;
    private final String searchString;

    SearchLexer(String searchString) { this.searchString = searchString; }

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
                    state = State.SCAN_DECIMAL;
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
                    return new Token(Token.Type.NOT_EQUAL, tokenStart,
                        tokenStart + 1,  "!=", null);
                } else {
                    throw new SearchFormatException("Unknown character !",
                        tokenStart, tokenStart);
                }

            case SCAN_EQUAL:
                if (c == '=') {
                    return new Token(Token.Type.EQUAL, tokenStart, tokenStart
                        + 1, "==", null);
                } else {
                    throw new SearchFormatException("Unknown character =",
                        tokenStart, tokenStart);
                }

            case SCAN_AMPERSAND:
                if (c == '&') {
                    return new Token(Token.Type.DOUBLE_AMPERSAND, tokenStart,
                        tokenStart + 1, "!=", null);
                } else {
                    throw new SearchFormatException("Unknown character &",
                        tokenStart, tokenStart);
                }

            case SCAN_PIPE:
                if (c == '|') {
                    return new Token(Token.Type.DOUBLE_PIPE, tokenStart,
                        tokenStart + 1, "!=", null);
                } else {
                    throw new SearchFormatException("Unknown character |",
                        tokenStart, tokenStart);
                }

            case SCAN_IDENTIFIER:
                if (isAlphaNum(c) || c == '_' || c == '.') {
                    currentTokenValue.append((char)c);
                } else if (c == '(') { // Start generate index
                    currentTokenValue.append((char)c);
                    state = State.SCAN_GEN_NUM;
                } else {
                    pushBackChar();
                    return new Token(Token.Type.IDENTIFIER, tokenStart,
                                     lexerOffset - 1,
                                     currentTokenValue.toString(), null);
                }

                break;

            case SCAN_GEN_NUM:
                currentTokenValue.append((char)c);
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
                    throw new SearchFormatException(
                        "Unknown type " + (char)c, tokenStart, lexerOffset - 1);
                }

                break;

            case SCAN_BINARY:
                if (c == '0' || c == '1' || c == 'x' || c == 'z' || c == 'X' ||
                    c == 'Z') {
                    currentTokenValue.append((char)c);
                } else {
                    BitVector literalValue =
                        new BitVector(currentTokenValue.toString(), 2);
                    pushBackChar();
                    return new Token(
                        Token.Type.LITERAL, tokenStart, lexerOffset - 1,
                        currentTokenValue.toString(), literalValue);
                }

                break;

            case SCAN_DECIMAL:
                if (c >= '0' && c <= '9') {
                    currentTokenValue.append((char)c);
                } else {
                    BitVector literalValue =
                        new BitVector(currentTokenValue.toString(), 10);
                    pushBackChar();
                    return new Token(
                        Token.Type.LITERAL, tokenStart, lexerOffset - 1,
                        currentTokenValue.toString(), literalValue);
                }

                break;

            case SCAN_HEXADECIMAL:
                if (isHexDigit(c) || c == 'x' || c == 'z' || c == 'X' ||
                    c == 'Z') {
                    currentTokenValue.append((char)c);
                } else {
                    BitVector literalValue =
                        new BitVector(currentTokenValue.toString(), 16);
                    pushBackChar();
                    return new Token(
                        Token.Type.LITERAL, tokenStart, lexerOffset - 1,
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

    private void pushBackChar() { lexerOffset--; }

    private static boolean isAlpha(int value) {
        return (value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z');
    }

    private static boolean isNum(int value) {
        return value >= '0' && value <= '9';
    }

    private static boolean isHexDigit(int value) {
        return (value >= '0' && value <= '9') ||
            (value >= 'a' && value <= 'f') || (value >= 'A' && value <= 'F');
    }

    private static boolean isAlphaNum(int value) {
        return isAlpha(value) || isNum(value);
    }

    private static boolean isSpace(int value) {
        return value == ' ' || value == '\t' || value == '\n' || value == '\r';
    }
}

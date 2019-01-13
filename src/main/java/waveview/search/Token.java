//
// Copyright 2019 Jeff Bush
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

final class Token {
    enum Type {
        IDENTIFIER,
        END,
        LITERAL,
        GREATER,
        GREATER_EQUAL,
        LESS_THAN,
        LESS_EQUAL,
        NOT_EQUAL,
        LPAREN,
        RPAREN,
        LBRACKET,
        RBRACKET,
        COLON,
        EQUAL,
        DOUBLE_AMPERSAND,
        DOUBLE_PIPE
    }
    ;

    private final Type type;
    private final int start;
    private final int end;
    private BitVector literalValue;
    private final String stringValue;

    Token(Type type, int start, int end, String stringValue, BitVector literalValue) {
        this.type = type;
        this.start = start;
        this.end = end;
        this.stringValue = stringValue;
        this.literalValue = literalValue;
    }

    Token(Type type, int start, int c) {
        this.type = type;
        this.start = start;
        this.end = start;
        this.stringValue = Character.toString((char) c);
    }

    Type getType() {
        return type;
    }

    int getStart() {
        return start;
    }

    int getEnd() {
        return end;
    }

    BitVector getLiteralValue() {
        return literalValue;
    }

    @Override
    public String toString() {
        return stringValue;
    }
}

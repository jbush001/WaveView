

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

import waveview.BitVector;
import waveview.NetDataModel;
import waveview.WaveformDataModel;

public final class SearchParser {
    private final SearchLexer lexer;
    private final WaveformDataModel waveformDataModel;
    private final BooleanExpressionNode searchExpression;

    public SearchParser(WaveformDataModel waveformDataModel, String searchString) throws SearchFormatException {
        this.waveformDataModel = waveformDataModel;
        lexer = new SearchLexer(searchString);
        searchExpression = parseExpression();
        matchToken(SearchLexer.TOK_END);
    }

    BooleanExpressionNode getExpression() {
        return searchExpression;
    }

    /// Read the next token and throw an exception if the type does
    /// not match the parameter.
    private void matchToken(int tokenType) throws SearchFormatException {
        int got = lexer.nextToken();
        if (got != tokenType) {
            if (got == SearchLexer.TOK_END) {
                throw new SearchFormatException("unexpected end of string", lexer.getTokenStart(), lexer.getTokenEnd());
            } else {
                throw new SearchFormatException("unexpected value", lexer.getTokenStart(), lexer.getTokenEnd());
            }
        }
    }

    /// Read the next token and check if is an identifier that matches
    /// the passed type. If not, push back the token and return false.
    /// @note this is case insensitive
    private boolean tryToMatchToken(String value) throws SearchFormatException {
        int lookahead = lexer.nextToken();
        if (lookahead != SearchLexer.TOK_IDENTIFIER || !lexer.getTokenString().equalsIgnoreCase(value)) {
            lexer.pushBackToken(lookahead);
            return false;
        }

        return true;
    }

    private BooleanExpressionNode parseExpression() throws SearchFormatException {
        return parseOr();
    }

    private BooleanExpressionNode parseOr() throws SearchFormatException {
        BooleanExpressionNode left = parseAnd();
        while (tryToMatchToken("or")) {
            left = new OrExpressionNode(left, parseAnd());
        }

        return left;
    }

    private BooleanExpressionNode parseAnd() throws SearchFormatException {
        BooleanExpressionNode left = parseCondition();
        while (tryToMatchToken("and")) {
            left = new AndExpressionNode(left, parseCondition());
        }

        return left;
    }

    private BooleanExpressionNode parseCondition() throws SearchFormatException {
        int lookahead = lexer.nextToken();
        if (lookahead == '(') {
            BooleanExpressionNode node = parseExpression();
            matchToken(')');
            return node;
        }

        lexer.pushBackToken(lookahead);
        ValueNode left = parseValue();
        lookahead = lexer.nextToken();
        switch (lookahead) {
            case SearchLexer.TOK_GREATER:
                return new GreaterThanExpressionNode(left, parseValue());
            case SearchLexer.TOK_GREATER_EQUAL:
                return new GreaterEqualExpressionNode(left, parseValue());
            case SearchLexer.TOK_LESS_THAN:
                return new LessThanExpressionNode(left, parseValue());
            case SearchLexer.TOK_LESS_EQUAL:
                return new LessEqualExpressionNode(left, parseValue());
            case SearchLexer.TOK_NOT_EQUAL:
                return new NotEqualExpressionNode(left, parseValue());
            case '=':
                return new EqualExpressionNode(left, parseValue());
            default:
                // If there's not an operator, treat as != 0
                lexer.pushBackToken(lookahead);
                return new NotEqualExpressionNode(left, new ConstValueNode(BitVector.ZERO));
        }
    }

    private ValueNode parseValue() throws SearchFormatException {
        int lookahead = lexer.nextToken();
        if (lookahead == SearchLexer.TOK_IDENTIFIER) {
            NetDataModel netDataModel = waveformDataModel.findNet(lexer.getTokenString());
            if (netDataModel == null) {
                throw new SearchFormatException("unknown net \"" + lexer.getTokenString() + "\"", lexer.getTokenStart(),
                        lexer.getTokenEnd());
            }

            return new NetValueNode(netDataModel);
        } else {
            lexer.pushBackToken(lookahead);
            matchToken(SearchLexer.TOK_LITERAL);
            return new ConstValueNode(lexer.getLiteralValue());
        }
    }
}

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
        matchToken(Token.Type.END);
    }

    BooleanExpressionNode getExpression() {
        return searchExpression;
    }

    /// Read the next token and throw an exception if the type does
    /// not match the parameter.
    private void matchToken(Token.Type tokenType) throws SearchFormatException {
        Token token = lexer.nextToken();
        if (token.getType() != tokenType) {
            if (token.getType() == Token.Type.END) {
                throw new SearchFormatException("unexpected end of string", token.getStart(), token.getEnd());
            } else {
                throw new SearchFormatException("unexpected value", token.getStart(), token.getEnd());
            }
        }
    }

    /// Read the next token and check if is an identifier that matches
    /// the passed type. If not, push back the token and return false.
    /// @note this is case insensitive
    private boolean tryToMatchToken(String value) throws SearchFormatException {
        Token lookahead = lexer.nextToken();
        if (lookahead.getType() != Token.Type.IDENTIFIER
                || !lookahead.toString().equalsIgnoreCase(value)) {
            lexer.pushBackToken();
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
        Token lookahead = lexer.nextToken();
        if (lookahead.getType() == Token.Type.LPAREN) {
            BooleanExpressionNode node = parseExpression();
            matchToken(Token.Type.RPAREN);
            return node;
        }

        lexer.pushBackToken();
        ValueNode left = parseValue();
        lookahead = lexer.nextToken();
        switch (lookahead.getType()) {
            case GREATER:
                return new GreaterThanExpressionNode(left, parseValue());
            case GREATER_EQUAL:
                return new GreaterEqualExpressionNode(left, parseValue());
            case LESS_THAN:
                return new LessThanExpressionNode(left, parseValue());
            case LESS_EQUAL:
                return new LessEqualExpressionNode(left, parseValue());
            case NOT_EQUAL:
                return new NotEqualExpressionNode(left, parseValue());
            case EQUAL:
                return new EqualExpressionNode(left, parseValue());
            default:
                // If there's not an operator, treat as != 0
                lexer.pushBackToken();
                return new NotEqualExpressionNode(left, new ConstValueNode(BitVector.ZERO));
        }
    }

    private ValueNode parseValue() throws SearchFormatException {
        Token lookahead = lexer.nextToken();
        if (lookahead.getType() == Token.Type.IDENTIFIER) {
            NetDataModel netDataModel = waveformDataModel.findNet(lookahead.toString());
            if (netDataModel == null) {
                throw new SearchFormatException("unknown net \"" + lookahead.toString() + "\"",
                        lookahead.getStart(), lookahead.getEnd());
            }

            return new NetValueNode(netDataModel);
        } else {
            lexer.pushBackToken();
            matchToken(Token.Type.LITERAL);
            return new ConstValueNode(lookahead.getLiteralValue());
        }
    }
}

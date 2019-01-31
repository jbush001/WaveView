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

import java.util.NoSuchElementException;

import waveview.wavedata.BitVector;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.WaveformDataModel;
import waveview.wavedata.WaveformDataModel.AmbiguousNetException;

final class SearchParser {
    private final SearchLexer lexer;
    private final WaveformDataModel waveformDataModel;
    private final BooleanExpressionNode searchExpression;

    SearchParser(WaveformDataModel waveformDataModel, String searchString)
        throws SearchFormatException {
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
    private Token matchToken(Token.Type tokenType) throws SearchFormatException {
        Token token = lexer.nextToken();
        if (token.getType() != tokenType) {
            if (token.getType() == Token.Type.END) {
                throw new SearchFormatException(
                    "Unexpected end of string", token.getStart(), token.getEnd());
            } else {
                throw new SearchFormatException(
                    "Unexpected token", token.getStart(), token.getEnd());
            }
        }

        return token;
    }

    /// Read the next token and check if is an identifier that matches
    /// the passed type. If not, push back the token and return false.
    /// @note this is case insensitive
    private boolean tryToMatchToken(Token.Type tokenType) throws SearchFormatException {
        Token lookahead = lexer.nextToken();
        if (lookahead.getType() != tokenType) {
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
        while (tryToMatchToken(Token.Type.DOUBLE_PIPE)) {
            left = new OrExpressionNode(left, parseAnd());
        }

        return left;
    }

    private BooleanExpressionNode parseAnd() throws SearchFormatException {
        BooleanExpressionNode left = parseCondition();
        while (tryToMatchToken(Token.Type.DOUBLE_AMPERSAND)) {
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
            String name = lookahead.toString();
            NetDataModel netDataModel;
            try {
                netDataModel = waveformDataModel.fuzzyFindNet(name);
            } catch (AmbiguousNetException exc) {
                throw new SearchFormatException(
                    "Ambiguous net \"" + name + "\"", lookahead.getStart(),
                    lookahead.getEnd());
            } catch (NoSuchElementException exc) {
                throw new SearchFormatException("Unknown net \"" + name + "\"",
                    lookahead.getStart(), lookahead.getEnd());
            }

            lookahead = lexer.nextToken();
            if (lookahead.getType() == Token.Type.LBRACKET) {
                Token highIndexTok = matchToken(Token.Type.LITERAL);
                int highIndex = highIndexTok.getLiteralValue().intValue();
                int lowIndex;

                int width = netDataModel.getTransitionVector().getWidth();
                lookahead = lexer.nextToken();
                if (lookahead.getType() == Token.Type.COLON) {
                    Token lowIndexTok = matchToken(Token.Type.LITERAL);
                    lowIndex = lowIndexTok.getLiteralValue().intValue();
                    if (highIndex >= width || highIndex < lowIndex) {
                        throw new SearchFormatException("Invalid bit slice range",
                            highIndexTok.getStart(), lowIndexTok.getEnd());
                    }
                } else {
                    lexer.pushBackToken();
                    lowIndex = highIndex;
                    if (highIndex >= width) {
                        throw new SearchFormatException("Invalid bit slice index",
                            highIndexTok.getStart(), highIndexTok.getEnd());
                    }
                }

                matchToken(Token.Type.RBRACKET);

                return new NetValueNode(netDataModel, lowIndex, highIndex);
            } else {
                lexer.pushBackToken();
                return new NetValueNode(netDataModel);
            }
        } else {
            lexer.pushBackToken();
            matchToken(Token.Type.LITERAL);
            return new ConstValueNode(lookahead.getLiteralValue());
        }
    }
}

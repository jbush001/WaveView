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
import waveview.wavedata.NetDataModel;
import waveview.wavedata.WaveformDataModel;

public final class SearchParser {
    private final SearchLexer lexer;
    private final WaveformDataModel waveformDataModel;
    private final BooleanExpressionNode searchExpression;

    public SearchParser(WaveformDataModel waveformDataModel,
                        String searchString) throws SearchFormatException {
        this.waveformDataModel = waveformDataModel;
        lexer = new SearchLexer(searchString);
        searchExpression = parseExpression();
        matchToken(Token.Type.END);
    }

    BooleanExpressionNode getExpression() { return searchExpression; }

    /// Read the next token and throw an exception if the type does
    /// not match the parameter.
    private Token matchToken(Token.Type tokenType) throws SearchFormatException {
        Token token = lexer.nextToken();
        if (token.getType() != tokenType) {
            if (token.getType() == Token.Type.END) {
                throw new SearchFormatException("Unexpected end of string",
                                                token.getStart(),
                                                token.getEnd());
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
    private boolean tryToMatchToken(String value) throws SearchFormatException {
        Token lookahead = lexer.nextToken();
        if (lookahead.getType() != Token.Type.IDENTIFIER ||
            !lookahead.toString().equalsIgnoreCase(value)) {
            lexer.pushBackToken();
            return false;
        }

        return true;
    }

    private BooleanExpressionNode parseExpression()
        throws SearchFormatException {
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

    private BooleanExpressionNode parseCondition()
        throws SearchFormatException {
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
            return new NotEqualExpressionNode(
                left, new ConstValueNode(BitVector.ZERO));
        }
    }

    private ValueNode parseValue() throws SearchFormatException {
        Token lookahead = lexer.nextToken();
        if (lookahead.getType() == Token.Type.IDENTIFIER) {
            NetDataModel netDataModel = tryToFindNet(lookahead);
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
                                                         highIndexTok.getStart(),
                                                         lowIndexTok.getEnd());
                    }
                } else {
                    lexer.pushBackToken();
                    lowIndex = highIndex;
                    if (highIndex >= width) {
                        throw new SearchFormatException("Invalid bit slice index",
                                                         highIndexTok.getStart(),
                                                         highIndexTok.getEnd());
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

    // This does a fuzzy match to find the net. If there is ambiguity (two nets
    // with the same name that aren't aliases), it will throw a SearchFormatException.
    private NetDataModel tryToFindNet(Token token) throws SearchFormatException {
        NetDataModel match = null;
        String name = token.toString();
        for (NetDataModel netDataModel : waveformDataModel) {
            if (isPartialNetNameMatch(netDataModel.getFullName(), name)) {
                if (match == null) {
                    match = netDataModel;
                } else if (match.getTransitionVector() != netDataModel.getTransitionVector()) {
                    throw new SearchFormatException("Ambiguous net \""
                        + name + "\"", token.getStart(), token.getEnd());
                }
            }
        }

        if (match == null) {
            throw new SearchFormatException("Unknown net \"" + name + "\"",
                token.getStart(), token.getEnd());
        }

        return match;
    }

    // Determine if one net name is a subset of another.
    // This works backward, comparing each dot delimited segment.
    // @param haystack A fully qualified dot name of a signal. For example,
    //    mod1.mod2.dat
    // @param needle A name that may be a subset (including a complete match)
    private static boolean isPartialNetNameMatch(String haystack, String needle) {
        int haystackSegmentEnd = haystack.length();
        int needleSegmentEnd = needle.length();
        while (needleSegmentEnd > 0) {
            if (haystackSegmentEnd <= 0) {
                // There are still elements in needle, but not in haystack.
                // Since haystack is a full path, this can't be a match.
                // For example:
                //  haystack:  bar.baz
                //  needle:  foo.bar.baz
                return false;
            }

            // These may be -1, which will cause code below to check from the
            // beginning of the string.
            int haystackSegmentBegin = haystack.lastIndexOf('.', haystackSegmentEnd - 1);
            int needleSegmentBegin = needle.lastIndexOf('.', needleSegmentEnd - 1);
            if (!haystack.substring(haystackSegmentBegin + 1, haystackSegmentEnd).equals(
                needle.substring(needleSegmentBegin + 1, needleSegmentEnd))) {
                // Subpaths don't match
                return false;
            }

            needleSegmentEnd = needleSegmentBegin;
            haystackSegmentEnd = haystackSegmentBegin;
        }

        return true;
    }
}

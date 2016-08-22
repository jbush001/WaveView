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

import java.lang.Math;
import java.util.*;

///
/// The Query class allows searching for logic conditions using complex boolean expressions
/// For example: (ena = 1 & (addr = 'h1000 or addr = 'h2000))
/// It builds an expression tree to represent the search criteria. It is optimized
/// for fast searching, skipping events that cannot meet the criteria.
///
/// @todo Support >=, <=, ~, etc
/// @todo Support proper logical operators &&, ||
/// @todo Support partial multi-net matches
///
public class Query {
    public Query(TraceDataModel traceModel, String queryString) throws ParseException {
        fTraceDataModel = traceModel;
        fQueryString = queryString;
        fStringOffset = 0;
        fExpression = parseExpression();
    }

    ///
    /// Scan forward to find the next timestamp that matches this query's expression
    /// If the startTimestamp is already a match, it will not be returned. This will
    ///  instead scan to the next transition
    /// @bug If startTimestamp is before the first event, and the first event matches,
    ///      this will not return it.
    /// @bug When searching with OR, may snap to the middle of a region that matches
    ///      (need to verify)
    /// @returns
    ///   -1 If there are no matches in the forward direction
    ///      timestamp of the next forward match otherwise
    ///
    public long getNextMatch(long startTimestamp) {
        long timestamp = startTimestamp;
        while (true) {
            if (fExpression.evaluate(timestamp, fQueryHint) && timestamp != startTimestamp)
                return timestamp;

            if (fQueryHint.forwardTimestamp < 0)
                return -1;

            timestamp = fQueryHint.forwardTimestamp;
        }
    }

    ///
    /// Scan backward to find the previous timestamp that matches this query's expression
    /// If the startTimestamp is already a match, it will not be returned. This will
    ///  instead scan to the previous transition
    /// @bug This will snap to the beginning of the region that matches, not the end.
    /// @returns
    ///   -1 If there are no matches in the backward direction
    ///      timestamp of the next backward match otherwise
    ///
    public long getPreviousMatch(long startTimestamp) {
        long timestamp = startTimestamp;
        while (true) {
            if (fExpression.evaluate(timestamp, fQueryHint) && timestamp != startTimestamp)
                return timestamp;

            if (fQueryHint.backwardTimestamp < 0)
                return -1;

            timestamp = fQueryHint.backwardTimestamp;
        }
    }

    class ParseException extends Exception {
        public ParseException(String what) {
            super(what);
            fErrorMessage = what;
            fStartOffset = fTokenStart;
            if (fCurrentTokenValue.length() > 0)
                fEndOffset = fTokenStart + fCurrentTokenValue.length() - 1;
            else
                fEndOffset = fTokenStart;
        }

        @Override
        public String toString() {
            return fErrorMessage;
        }

        public int getStartOffset() {
            return fStartOffset;
        }

        public int getEndOffset() {
            return fEndOffset;
        }

        private String fErrorMessage;
        private int fStartOffset;
        private int fEndOffset;
    }

    private static final int TOK_IDENTIFIER = 1000;
    private static final int TOK_END = 1001;
    private static final int TOK_LITERAL = 1002;

    private static final int STATE_INIT = 0;
    private static final int STATE_SCAN_IDENTIFIER = 1;
    private static final int STATE_SCAN_LITERAL = 2;
    private static final int STATE_SCAN_LITERAL_TYPE = 3;

    private static final int LITERAL_TYPE_DECIMAL = 0;
    private static final int LITERAL_TYPE_HEX = 1;
    private static final int LITERAL_TYPE_BINARY = 2;

    String fQueryString;
    int fStringOffset;
    StringBuffer fCurrentTokenValue = new StringBuffer();
    int fCurrentTokenType;
    int fPushBackChar = -1;
    int fPushBackToken = -1;
    int fTokenStart;
    BitVector fBitVector;

    void pushBackToken(int tok) {
        fPushBackToken = tok;
    }

    boolean isAlpha(int value) {
        return (value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z');
    }

    boolean isNum(int value) {
        return value >= '0' && value <= '9';
    }

    boolean isHexDigit(int value) {
        return (value >= '0' && value <= '9')
               || (value >= 'a' && value <='f')
               || (value >= 'A' && value <= 'F');
    }

    boolean isAlphaNum(int value) {
        return isAlpha(value) || isNum(value);
    }

    boolean isSpace(int value) {
        return value == ' ' || value == '\r' || value == '\n' || value == '\t';
    }

    int parseToken() throws ParseException {
        if (fPushBackToken != -1) {
            int token = fPushBackToken;
            fPushBackToken = -1;
            return token;
        }

        int state = STATE_INIT;
        fCurrentTokenValue.setLength(0);

        for (;;) {
            int c;

            if (fPushBackChar != -1) {
                c = fPushBackChar;
                fPushBackChar = -1;
            } else {
                if (fStringOffset == fQueryString.length())
                    c = -1;
                else
                    c = fQueryString.charAt(fStringOffset++);
            }

            switch (state) {
            case STATE_INIT:
                fTokenStart = fStringOffset - 1;
                if (c == -1)
                    return TOK_END;
                else if ("(<>)=|&".indexOf(c) != -1)
                    return c;
                else if (c == '\'')
                    state = STATE_SCAN_LITERAL_TYPE;
                else if (isAlpha(c)) {
                    fPushBackChar = c;
                    state = STATE_SCAN_IDENTIFIER;
                } else if (isNum(c)) {
                    fPushBackChar = c;
                    fCurrentTokenType = LITERAL_TYPE_DECIMAL;
                    state = STATE_SCAN_LITERAL;
                }

                break;

            case STATE_SCAN_IDENTIFIER:
                if (isAlphaNum(c) || c == '_' || c == '.')
                    fCurrentTokenValue.append((char) c);
                else {
                    fPushBackChar = c;
                    return TOK_IDENTIFIER;
                }

                break;

            case STATE_SCAN_LITERAL_TYPE:
                if (c == 'b')
                    fCurrentTokenType = LITERAL_TYPE_BINARY;
                else if (c == 'h')
                    fCurrentTokenType = LITERAL_TYPE_HEX;
                else if (c == 'd')
                    fCurrentTokenType = LITERAL_TYPE_DECIMAL;
                else
                    throw new ParseException("unknown type " + (char) c);

                state = STATE_SCAN_LITERAL;
                break;

            case STATE_SCAN_LITERAL:
                if (isHexDigit(c)
                        || ((fCurrentTokenType == LITERAL_TYPE_BINARY || fCurrentTokenType == LITERAL_TYPE_HEX)
                            && (c == 'x' || c == 'z' || c == 'X' || c == 'Z'))) {
                    fCurrentTokenValue.append((char) c);
                } else {
                    switch (fCurrentTokenType) {
                    case LITERAL_TYPE_BINARY:
                        fBitVector = new BitVector(fCurrentTokenValue.toString(), 2);
                        break;

                    case LITERAL_TYPE_HEX:
                        fBitVector = new BitVector(fCurrentTokenValue.toString(), 16);
                        break;

                    case LITERAL_TYPE_DECIMAL:
                        fBitVector = new BitVector(fCurrentTokenValue.toString(), 10);
                        break;

                    default:
                        assert false;   // Should not get here
                    }

                    fPushBackChar = c;
                    return TOK_LITERAL;
                }

                break;
            }
        }
    }

    void match(int tokenType) throws ParseException {
        int got = parseToken();
        if (got != tokenType) {
            if (got == TOK_END)
                throw new ParseException("unexpected end of string");
            else
                throw new ParseException("unexpected value");
        }
    }

    ExpressionNode parseExpression() throws ParseException {
        ExpressionNode node = parseOr();
        match(TOK_END);
        return node;
    }

    ExpressionNode parseOr() throws ParseException {
        ExpressionNode left = parseAnd();

        int lookahead = parseToken();
        if (lookahead != '|') {
            pushBackToken(lookahead);
            return left;
        }

        ExpressionNode right = parseExpression();
        return new OrExpressionNode(left, right);
    }

    ExpressionNode parseAnd() throws ParseException {
        ExpressionNode left = parseCondition();

        int lookahead = parseToken();
        if (lookahead != '&') {
            pushBackToken(lookahead);
            return left;
        }

        ExpressionNode right = parseExpression();
        return new AndExpressionNode(left, right);
    }

    ExpressionNode parseCondition() throws ParseException {
        // if this is an '(', call parseExpression
        int lookahead = parseToken();
        if (lookahead == '(') {
            ExpressionNode node = parseExpression();
            match(')');
            return node;
        }

        if (lookahead != TOK_IDENTIFIER)
            throw new ParseException("unexpected token, expected identifier");

        int netId = fTraceDataModel.findNet(fCurrentTokenValue.toString());
        if (netId < 0)
            throw new ParseException("unknown net \"" + fCurrentTokenValue.toString() + "\"");

        // check conditional.
        lookahead = parseToken();
        switch (lookahead) {
        case '>':
            match(TOK_LITERAL);
            return new GreaterThanExpressionNode(netId, fBitVector);
        case '<':
            match(TOK_LITERAL);
            return new LessThanExpressionNode(netId, fBitVector);
        case '=':
            match(TOK_LITERAL);
            return new EqualExpressionNode(netId, fBitVector);
        default:
            throw new ParseException("Unknown conditional operator");
        }
    }

    private class QueryHint {
        long forwardTimestamp;
        long backwardTimestamp;
    }

    private abstract class ExpressionNode {
        /// Determine if this subexpression is true at the timestamp provided.
        /// @param timestamp Timestamp and which to evaluate.  If timestamp is right on a transition,
        ///  the value *after* the transition will be used
        /// @param outHint Sets the next timestamp that is worth investigating. Skips transitions
        ///  for nets that aren't
        /// @return
        ///   - true if the value at the timestamp makes this expression true
        ///   - false if the value at the timestamp makes this expression true
        abstract public boolean evaluate(long timestamp, QueryHint outHint);
    }

    private abstract class BooleanExpressionNode extends ExpressionNode {
        BooleanExpressionNode(ExpressionNode left, ExpressionNode right) {
            fLeftChild = left;
            fRightChild = right;
        }

        @Override
        public boolean evaluate(long timestamp, QueryHint outHint) {
            boolean leftResult = fLeftChild.evaluate(timestamp, fLeftHint);
            boolean rightResult = fRightChild.evaluate(timestamp, fRightHint);

            // Forward timestamp
            if (fLeftHint.forwardTimestamp >= 0 && fRightHint.forwardTimestamp >= 0) {
                // Here we skip events that cannot possibly make the expression evaluate to true.
                if (leftResult && rightResult)
                    outHint.forwardTimestamp = Math.min(fRightHint.forwardTimestamp, fLeftHint.forwardTimestamp);
                else if (leftResult)
                    outHint.forwardTimestamp = fRightHint.forwardTimestamp;
                else if (rightResult)
                    outHint.forwardTimestamp = fLeftHint.forwardTimestamp;
                else
                    outHint.forwardTimestamp = nextForwardEvent(fRightHint.forwardTimestamp, fLeftHint.forwardTimestamp);
            } else if (fLeftHint.forwardTimestamp >= 0)
                outHint.forwardTimestamp = fLeftHint.forwardTimestamp;
            else if (fRightHint.forwardTimestamp >= 0)
                outHint.forwardTimestamp = fRightHint.forwardTimestamp;
            else
                outHint.forwardTimestamp = -1;

            // Backward timestamp
            if (fLeftHint.backwardTimestamp >= 0 && fRightHint.backwardTimestamp >= 0) {
                if (leftResult && rightResult)
                    outHint.backwardTimestamp = Math.max(fRightHint.backwardTimestamp, fLeftHint.backwardTimestamp);
                else if (leftResult)
                    outHint.backwardTimestamp = fRightHint.backwardTimestamp;
                else if (rightResult)
                    outHint.backwardTimestamp = fLeftHint.backwardTimestamp;
                else
                    outHint.backwardTimestamp = nextBackwardEvent(fRightHint.backwardTimestamp, fLeftHint.backwardTimestamp);
            } else if (fLeftHint.backwardTimestamp >= 0)
                outHint.backwardTimestamp = fLeftHint.backwardTimestamp;
            else if (fRightHint.backwardTimestamp >= 0)
                outHint.backwardTimestamp = fRightHint.backwardTimestamp;
            else
                outHint.backwardTimestamp = -1;

            return compareResults(leftResult, rightResult);
        }

        abstract protected boolean compareResults(boolean value1, boolean value2);

        /// If both conditions are false, pick which event we should evaluate next.
        abstract protected long nextForwardEvent(long timestamp1, long timestamp2);
        abstract protected long nextBackwardEvent(long timestamp1, long timestamp2);

        private ExpressionNode fLeftChild;
        private ExpressionNode fRightChild;

        // These are preallocated for efficiency and aren't used outside the evaluate() call.
        private QueryHint fLeftHint = new QueryHint();
        private QueryHint fRightHint = new QueryHint();
    }

    private class OrExpressionNode extends BooleanExpressionNode {
        public OrExpressionNode(ExpressionNode left, ExpressionNode right) {
            super(left, right);
        }

        @Override
        protected boolean compareResults(boolean value1, boolean value2) {
            return value1 || value2;
        }

        // If both are false, either one changing may cause the expression to be true.
        // Therefore, pick the soonest one
        @Override
        protected long nextForwardEvent(long nextEvent1, long nextEvent2) {
            return Math.min(nextEvent1, nextEvent2);
        }

        @Override
        protected long nextBackwardEvent(long nextEvent1, long nextEvent2) {
            return Math.max(nextEvent1, nextEvent2);
        }
    }

    private class AndExpressionNode extends BooleanExpressionNode {
        public AndExpressionNode(ExpressionNode left, ExpressionNode right) {
            super(left, right);
        }

        @Override
        protected boolean compareResults(boolean value1, boolean value2) {
            return value1 && value2;
        }

        // If both are false, both must change for the expression to be true.
        // Therefore, pick the later one.
        @Override
        protected long nextForwardEvent(long nextEvent1, long nextEvent2) {
            return Math.max(nextEvent1, nextEvent2);
        }

        @Override
        protected long nextBackwardEvent(long nextEvent1, long nextEvent2) {
            return Math.min(nextEvent1, nextEvent2);
        }
    }

    private abstract class ComparisonExpressionNode extends ExpressionNode {
        protected ComparisonExpressionNode(int netId, BitVector expected) {
            fNetId = netId;
            fExpected = expected;
        }

        @Override
        public boolean evaluate(long timestamp, QueryHint outHint) {
            TransitionVector.Iterator i = fTraceDataModel.findTransition(fNetId, timestamp);
            Transition t = i.next();
            outHint.forwardTimestamp = i.getNextTimestamp();
            outHint.backwardTimestamp = i.getPrevTimestamp();
            return doCompare(t, fExpected);
        }

        abstract protected boolean doCompare(BitVector value1, BitVector value2);

        private int fNetId;
        private BitVector fExpected;
    }

    private class EqualExpressionNode extends ComparisonExpressionNode {
        public EqualExpressionNode(int netId, BitVector match) {
            super(netId, match);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) == 0;
        }
    }

    private class NotEqualExpressionNode extends ComparisonExpressionNode {
        public NotEqualExpressionNode(int netId, BitVector match) {
            super(netId, match);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) != 0;
        }
    }

    private class GreaterThanExpressionNode extends ComparisonExpressionNode {
        public GreaterThanExpressionNode(int netId, BitVector match) {
            super(netId, match);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) > 0;
        }
    }

    private class LessThanExpressionNode extends ComparisonExpressionNode {
        public LessThanExpressionNode(int netId, BitVector match) {
            super(netId, match);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) < 0;
        }
    }

    private TraceDataModel fTraceDataModel;
    private QueryHint fQueryHint = new QueryHint();
    private ExpressionNode fExpression;
}

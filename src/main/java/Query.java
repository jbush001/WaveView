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
class Query {
    Query(TraceDataModel traceModel, String queryString) throws ParseException {
        fTraceDataModel = traceModel;
        fQueryString = queryString;
        fLexerOffset = 0;
        fExpression = parseExpression();
        match(TOK_END);
    }

    /// Mainly useful for unit testing
    /// @returns true if this query matches at the passed timestamp
    boolean matches(long timestamp) {
        return fExpression.evaluate(fTraceDataModel, timestamp, fQueryHint);
    }

    ///
    /// Scan forward to find the next timestamp that matches this query's expression
    /// If the startTimestamp is already a match, it will not be returned. This will
    ///  instead scan to the next transition
    /// @bug If startTimestamp is before the first event, and the first event matches,
    ///      this will not return it.
    /// @returns
    ///   -1 If there are no matches in the forward direction
    ///      timestamp of the next forward match otherwise
    ///
    long getNextMatch(long startTimestamp) {
        long currentTime = startTimestamp;
        boolean currentValue = fExpression.evaluate(fTraceDataModel, currentTime, fQueryHint);

        // If the start timestamp is already at a region that is true, scan first
        // to find a place where the expression is false. We'll then scan again
        // to where it is true.
        while (currentValue) {
            if (fQueryHint.forwardTimestamp == -1)
                return -1;  // End of trace

            currentTime = fQueryHint.forwardTimestamp;
            currentValue = fExpression.evaluate(fTraceDataModel, currentTime, fQueryHint);
        }

        while (!currentValue) {
            if (fQueryHint.forwardTimestamp == -1)
                return -1;  // End of trace

            currentTime = fQueryHint.forwardTimestamp;
            currentValue = fExpression.evaluate(fTraceDataModel, currentTime, fQueryHint);
        }

        return currentTime;
    }

    ///
    /// Scan backward to find the end of the region that
    /// matches this query's expression If the startTimestamp is already
    /// in a match, it will jump to end of the previous region that matches.
    /// @returns
    ///   -1 If there are no matches in the backward direction
    ///      timestamp of the next backward match otherwise
    ///
    long getPreviousMatch(long startTimestamp) {
        long currentTime = startTimestamp;
        boolean currentValue = fExpression.evaluate(fTraceDataModel, currentTime, fQueryHint);
        while (currentValue) {
            if (fQueryHint.backwardTimestamp == -1)
                return -1;  // End of trace

            currentTime = fQueryHint.backwardTimestamp;
            currentValue = fExpression.evaluate(fTraceDataModel, currentTime, fQueryHint);
        }

        while (!currentValue) {
            if (fQueryHint.backwardTimestamp == -1)
                return -1;  // End of trace

            currentTime = fQueryHint.backwardTimestamp;
            currentValue = fExpression.evaluate(fTraceDataModel, currentTime, fQueryHint);
        }

        return currentTime;
    }

    class ParseException extends Exception {
        ParseException(String what) {
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

        int getStartOffset() {
            return fStartOffset;
        }

        int getEndOffset() {
            return fEndOffset;
        }

        private String fErrorMessage;
        private int fStartOffset;
        private int fEndOffset;
    }

    public String toString() {
        return fExpression.toString();
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


    private static boolean isAlpha(int value) {
        return (value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z');
    }

    private static boolean isNum(int value) {
        return value >= '0' && value <= '9';
    }

    private static boolean isHexDigit(int value) {
        return (value >= '0' && value <= '9')
               || (value >= 'a' && value <='f')
               || (value >= 'A' && value <= 'F');
    }

    private static boolean isAlphaNum(int value) {
        return isAlpha(value) || isNum(value);
    }

    private static boolean isSpace(int value) {
        return value == ' ' || value == '\t' || value == '\n' || value == '\r';
    }

    private int nextToken() throws ParseException {
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
                if (fLexerOffset == fQueryString.length())
                    c = -1;
                else
                    c = fQueryString.charAt(fLexerOffset++);
            }

            switch (state) {
            case STATE_INIT:
                fTokenStart = fLexerOffset - 1;
                if (c == -1)
                    return TOK_END;
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
                else if (!isSpace(c))
                    return c;

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
                    }

                    fPushBackChar = c;
                    return TOK_LITERAL;
                }

                break;
            }
        }
    }

    private void pushBackToken(int tok) {
        fPushBackToken = tok;
    }

    private void match(int tokenType) throws ParseException {
        int got = nextToken();
        if (got != tokenType) {
            if (got == TOK_END)
                throw new ParseException("unexpected end of string");
            else
                throw new ParseException("unexpected value");
        }
    }

    private ExpressionNode parseExpression() throws ParseException {
        return parseOr();
    }

    private ExpressionNode parseOr() throws ParseException {
        ExpressionNode left = parseAnd();

        while (true) {
            int lookahead = nextToken();
            if (lookahead != '|') {
                pushBackToken(lookahead);
                return left;
            }

            ExpressionNode right = parseAnd();
            left = new OrExpressionNode(left, right);
        }
    }

    private ExpressionNode parseAnd() throws ParseException {
        ExpressionNode left = parseCondition();

        while (true) {
            int lookahead = nextToken();
            if (lookahead != '&') {
                pushBackToken(lookahead);
                return left;
            }

            ExpressionNode right = parseCondition();
            left = new AndExpressionNode(left, right);
        }
    }

    private ExpressionNode parseCondition() throws ParseException {
        int lookahead = nextToken();
        if (lookahead == '(') {
            ExpressionNode node = parseExpression();
            match(')');
            return node;
        }

        if (lookahead != TOK_IDENTIFIER)
            throw new ParseException("unexpected value");

        int netId = fTraceDataModel.findNet(fCurrentTokenValue.toString());
        if (netId < 0)
            throw new ParseException("unknown net \"" + fCurrentTokenValue.toString() + "\"");

        lookahead = nextToken();
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
            // If there's not an operator, treat as != 0
            pushBackToken(lookahead);
            return new NotEqualExpressionNode(netId, ZERO_VEC);
        }
    }

    private static class QueryHint {
        long forwardTimestamp;
        long backwardTimestamp;
    }

    private static abstract class ExpressionNode {
        /// Determine if this subexpression is true at the timestamp provided.
        /// @param timestamp Timestamp and which to evaluate.  If timestamp
        ///  is on a transition, the value after the transition will be used
        /// @param outHint Contains the next timestamp where the value of the
        ///   expression may change. It is guaranteed that no transition will occur
        ///   sooner than this value.
        /// @return
        ///   - true if the value at the timestamp makes this expression true
        ///   - false if the value at the timestamp makes this expression true
        abstract boolean evaluate(TraceDataModel model, long timestamp, QueryHint outHint);
    }

    private static abstract class BooleanExpressionNode extends ExpressionNode {
        BooleanExpressionNode(ExpressionNode left, ExpressionNode right) {
            fLeftChild = left;
            fRightChild = right;
        }

        @Override
        boolean evaluate(TraceDataModel model, long timestamp, QueryHint outHint) {
            boolean leftResult = fLeftChild.evaluate(model, timestamp, fLeftHint);
            boolean rightResult = fRightChild.evaluate(model, timestamp, fRightHint);

            // Compute the hints, which are the soonest time this expression
            // *could* change value. We will call the subclassed methods to determine
            // the actual hint type.
            long nextLeft = fLeftHint.forwardTimestamp >= 0
                ? fLeftHint.forwardTimestamp : Long.MAX_VALUE;
            long nextRight = fRightHint.forwardTimestamp >= 0
                ? fRightHint.forwardTimestamp : Long.MAX_VALUE;
            outHint.forwardTimestamp = nextHint(leftResult, rightResult, nextLeft, nextRight,
                false);
            if (outHint.forwardTimestamp == Long.MAX_VALUE)
                outHint.forwardTimestamp = -1;

            long prevLeft = fLeftHint.backwardTimestamp >= 0
                ? fLeftHint.backwardTimestamp : -Long.MAX_VALUE;
            long prevRight = fRightHint.backwardTimestamp >= 0
                ? fRightHint.backwardTimestamp : -Long.MAX_VALUE;
            outHint.backwardTimestamp = nextHint(leftResult, rightResult, prevLeft, prevRight,
                true);
            if (outHint.backwardTimestamp == -Long.MAX_VALUE)
                outHint.backwardTimestamp = -1;

            // Return the result at this time
            return compareResults(leftResult, rightResult);
        }

        abstract protected boolean compareResults(boolean value1, boolean value2);
        abstract protected long nextHint(boolean leftResult, boolean rightResult,
            long nextLeftTimestamp, long nextRightTimestamp, boolean searchBackward);

        protected ExpressionNode fLeftChild;
        protected ExpressionNode fRightChild;

        // These are preallocated for efficiency and aren't used outside
        // the evaluate() call.
        private QueryHint fLeftHint = new QueryHint();
        private QueryHint fRightHint = new QueryHint();
    }

    private static class OrExpressionNode extends BooleanExpressionNode {
        OrExpressionNode(ExpressionNode left, ExpressionNode right) {
            super(left, right);
        }

        @Override
        protected boolean compareResults(boolean value1, boolean value2) {
            return value1 || value2;
        }

        @Override
        protected long nextHint(boolean leftResult, boolean rightResult,
            long nextLeftTimestamp, long nextRightTimestamp, boolean searchBackward) {

            if (leftResult && rightResult) {
                // Both expressions are true. The only way for this to become
                // false is if when both change to false.
                if (searchBackward)
                    return Math.min(nextLeftTimestamp, nextRightTimestamp);
                else
                    return Math.max(nextLeftTimestamp, nextRightTimestamp);
            }
            else if (leftResult) {
                // Currently true. It can only become false when left result
                // becomes false.
                return nextLeftTimestamp;
            } else if (rightResult)  {
                // Currently true. It can only become false when right result
                // becomes false.
                return nextRightTimestamp;
            } else {
                // Both expressions are false. May become true if either subexpression
                // changes.
                if (searchBackward)
                    return Math.max(nextLeftTimestamp, nextRightTimestamp);
                else
                    return Math.min(nextLeftTimestamp, nextRightTimestamp);
            }
        }

        @Override
        public String toString() {
            return "(or " + fLeftChild + " " + fRightChild + ")";
        }
    }

    private static class AndExpressionNode extends BooleanExpressionNode {
        AndExpressionNode(ExpressionNode left, ExpressionNode right) {
            super(left, right);
        }

        @Override
        protected boolean compareResults(boolean value1, boolean value2) {
            return value1 && value2;
        }

        @Override
        protected long nextHint(boolean leftResult, boolean rightResult,
            long nextLeftTimestamp, long nextRightTimestamp,
            boolean searchBackward) {

            if (leftResult && rightResult) {
                // Both expressions are true. Either expression changing
                // could make it false.
                if (searchBackward)
                    return Math.max(nextLeftTimestamp, nextRightTimestamp);
                else
                    return Math.min(nextLeftTimestamp, nextRightTimestamp);
            }
            else if (leftResult) {
                // Currently false. It can only become true when right result
                // becomes true.
                return nextRightTimestamp;
            } else if (rightResult)  {
                // Currently false. It can only become true when left result
                // becomes true.
                return nextLeftTimestamp;
            } else {
                // Both expressions are false. Both must change before this
                // may become true.
                if (searchBackward)
                    return Math.min(nextLeftTimestamp, nextRightTimestamp);
                else
                    return Math.max(nextLeftTimestamp, nextRightTimestamp);
            }
        }

        @Override
        public String toString() {
            return "(and " + fLeftChild + " " + fRightChild + ")";
        }
    }

    private static abstract class ComparisonExpressionNode extends ExpressionNode {
        protected ComparisonExpressionNode(int netId, BitVector expected) {
            fNetId = netId;
            fExpected = expected;
        }

        @Override
        boolean evaluate(TraceDataModel model, long timestamp, QueryHint outHint) {
            Iterator<Transition> i = model.findTransition(fNetId, timestamp);
            Transition t = i.next();
            boolean result = doCompare(t, fExpected);
            if (timestamp >= t.getTimestamp())
                outHint.backwardTimestamp = t.getTimestamp() - 1;
            else
                outHint.backwardTimestamp = -1;

            if (i.hasNext()) {
                t = i.next();
                outHint.forwardTimestamp = t.getTimestamp();
            }
            else
                outHint.forwardTimestamp = -1;

            return result;
        }

        abstract protected boolean doCompare(BitVector value1, BitVector value2);

        protected int fNetId;
        protected BitVector fExpected;
    }

    private static class EqualExpressionNode extends ComparisonExpressionNode {
        EqualExpressionNode(int netId, BitVector match) {
            super(netId, match);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) == 0;
        }

        @Override
        public String toString() {
            return "(equal net" + fNetId + " " + fExpected + ")";
        }
    }

    private static class NotEqualExpressionNode extends ComparisonExpressionNode {
        NotEqualExpressionNode(int netId, BitVector match) {
            super(netId, match);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) != 0;
        }

        @Override
        public String toString() {
            return "(notequal net" + fNetId + " " + fExpected + ")";
        }
    }

    private static class GreaterThanExpressionNode extends ComparisonExpressionNode {
        GreaterThanExpressionNode(int netId, BitVector match) {
            super(netId, match);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) > 0;
        }

        @Override
        public String toString() {
            return "(greater net" + fNetId + " " + fExpected + ")";
        }
    }

    private static class LessThanExpressionNode extends ComparisonExpressionNode {
        LessThanExpressionNode(int netId, BitVector match) {
            super(netId, match);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) < 0;
        }

        @Override
        public String toString() {
            return "(lessthan net" + fNetId + " " + fExpected + ")";
        }
    }

    private String fQueryString;
    private int fLexerOffset;
    private StringBuffer fCurrentTokenValue = new StringBuffer();
    private int fCurrentTokenType;
    private int fPushBackChar = -1;
    private int fPushBackToken = -1;
    private int fTokenStart;
    private BitVector fBitVector;
    private TraceDataModel fTraceDataModel;
    private QueryHint fQueryHint = new QueryHint();
    private ExpressionNode fExpression;
    private static final BitVector ZERO_VEC = new BitVector("0", 2);
}


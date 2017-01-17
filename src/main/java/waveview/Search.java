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

import java.lang.Math;
import java.util.*;

///
/// The Search class allows searching for logic conditions using complex boolean expressions
/// For example: (ena = 1 and (addr = 'h1000 or addr = 'h2000))
/// It builds an expression tree to represent the search criteria. It is optimized
/// for fast searching, skipping events that cannot meet the criteria.
///
/// @todo Support partial multi-net matches
///
public class Search {
    public Search(TraceDataModel traceModel, String searchString) throws ParseException {
        fTraceDataModel = traceModel;
        fLexer = new Lexer(searchString);
        fSearchExpression = parseExpression();
        match(Lexer.TOK_END);
    }

    /// Mainly useful for unit testing
    /// @returns true if this search string matches at the passed timestamp
    public boolean matches(long timestamp) {
        SearchHint hint = new SearchHint();
        return fSearchExpression.evaluate(fTraceDataModel, timestamp, hint);
    }

    ///
    /// Scan forward to find the next timestamp that matches this search's expression
    /// If the startTimestamp is already a match, it will not be returned. This will
    ///  instead scan to the next transition
    /// @bug If startTimestamp is before the first event, and the first event matches,
    ///      this will not return it.
    /// @returns
    ///   -1 If there are no matches in the forward direction
    ///      timestamp of the next forward match otherwise
    ///
    public long getNextMatch(long startTimestamp) {
        SearchHint hint = new SearchHint();
        long currentTime = startTimestamp;
        boolean currentValue = fSearchExpression.evaluate(fTraceDataModel, currentTime, hint);

        // If the start timestamp is already at a region that is true, scan
        // first to find a place where the expression is false.
        while (currentValue) {
            if (hint.forwardTimestamp == Long.MAX_VALUE)
                return -1;  // End of trace

            currentTime = hint.forwardTimestamp;
            currentValue = fSearchExpression.evaluate(fTraceDataModel, currentTime, hint);
        }

        // Scan to find where the expression is true
        while (!currentValue) {
            if (hint.forwardTimestamp == Long.MAX_VALUE)
                return -1;  // End of trace

            currentTime = hint.forwardTimestamp;
            currentValue = fSearchExpression.evaluate(fTraceDataModel, currentTime, hint);
        }

        return currentTime;
    }

    ///
    /// Scan backward to find the end of the region that
    /// matches this search's expression If the startTimestamp is already
    /// in a match, it will jump to end of the previous region that matches.
    /// @returns
    ///   -1 If there are no matches in the backward direction
    ///      timestamp of the next backward match otherwise
    ///
    public long getPreviousMatch(long startTimestamp) {
        SearchHint hint = new SearchHint();
        long currentTime = startTimestamp;
        boolean currentValue = fSearchExpression.evaluate(fTraceDataModel, currentTime, hint);
        while (currentValue) {
            if (hint.backwardTimestamp == Long.MIN_VALUE)
                return -1;  // End of trace

            currentTime = hint.backwardTimestamp;
            currentValue = fSearchExpression.evaluate(fTraceDataModel, currentTime, hint);
        }

        while (!currentValue) {
            if (hint.backwardTimestamp == Long.MIN_VALUE)
                return -1;  // End of trace

            currentTime = hint.backwardTimestamp;
            currentValue = fSearchExpression.evaluate(fTraceDataModel, currentTime, hint);
        }

        return currentTime;
    }

    static public class ParseException extends Exception {
        ParseException(String what, int start, int end) {
            super(what);
            fStartOffset = start;
            fEndOffset = end;
        }

        public int getStartOffset() {
            return fStartOffset;
        }

        public int getEndOffset() {
            return fEndOffset;
        }

        private int fStartOffset;
        private int fEndOffset;
    }

    @Override
    public String toString() {
        return fSearchExpression.toString();
    }

    static private class Lexer {
        static final int TOK_IDENTIFIER = 1000;
        static final int TOK_END = 1001;
        static final int TOK_LITERAL = 1002;
        static final int TOK_GREATER = 1003;
        static final int TOK_GREATER_EQUAL = 1004;
        static final int TOK_LESS_THAN = 1005;
        static final int TOK_LESS_EQUAL = 1006;
        static final int TOK_NOT_EQUAL = 1007;

        private enum State {
            SCAN_INIT,
            SCAN_IDENTIFIER,
            SCAN_LITERAL_TYPE,
            SCAN_GEN_NUM,
            SCAN_GREATER,
            SCAN_LESS,
            SCAN_BINARY,
            SCAN_DECIMAL,
            SCAN_HEXADECIMAL
        }

        Lexer(String searchString) {
            fSearchString = searchString;
        }

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

        int nextToken() throws ParseException {
            if (fPushBackToken != -1) {
                int token = fPushBackToken;
                fPushBackToken = -1;
                return token;
            }

            State state = State.SCAN_INIT;
            fCurrentTokenValue.setLength(0);

            for (;;) {
                int c;

                if (fPushBackChar != -1) {
                    c = fPushBackChar;
                    fPushBackChar = -1;
                } else {
                    if (fLexerOffset == fSearchString.length())
                        c = -1;
                    else
                        c = fSearchString.charAt(fLexerOffset++);
                }

                switch (state) {
                case SCAN_INIT:
                    fTokenStart = fLexerOffset - 1;
                    if (c == -1)
                        return TOK_END;
                    else if (c == '\'')
                        state = State.SCAN_LITERAL_TYPE;
                    else if (isAlpha(c)) {
                        fPushBackChar = c;
                        state = State.SCAN_IDENTIFIER;
                    } else if (isNum(c)) {
                        fPushBackChar = c;
                        state = State.SCAN_DECIMAL;
                    } else if (c == '>')
                        state = State.SCAN_GREATER;
                    else if (c == '<')
                        state = State.SCAN_LESS;
                    else if (!isSpace(c))
                        return c;

                    break;

                case SCAN_GREATER:
                    if (c == '<')
                        return TOK_NOT_EQUAL;
                    else if (c == '=')
                        return TOK_GREATER_EQUAL;
                    else {
                        fPushBackChar = c;
                        return TOK_GREATER;
                    }

                case SCAN_LESS:
                    if (c == '>')
                        return TOK_NOT_EQUAL;
                    else if (c == '=')
                        return TOK_LESS_EQUAL;
                    else {
                        fPushBackChar = c;
                        return TOK_LESS_THAN;
                    }

                case SCAN_IDENTIFIER:
                    if (isAlphaNum(c) || c == '_' || c == '.')
                        fCurrentTokenValue.append((char) c);
                    else if (c == '(') {    // Start generate index
                        fCurrentTokenValue.append((char) c);
                        state = State.SCAN_GEN_NUM;
                    }
                    else {
                        fPushBackChar = c;
                        return TOK_IDENTIFIER;
                    }

                    break;

                case SCAN_GEN_NUM:
                    fCurrentTokenValue.append((char) c);
                    if (c == ')')
                        state = State.SCAN_IDENTIFIER;

                    break;


                case SCAN_LITERAL_TYPE:
                    if (c == 'b')
                        state = State.SCAN_BINARY;
                    else if (c == 'h')
                        state = State.SCAN_HEXADECIMAL;
                    else if (c == 'd')
                        state = State.SCAN_DECIMAL;
                    else
                        throw new ParseException("unknown type " + (char) c, getTokenStart(),
                            getTokenEnd());

                    break;

                case SCAN_BINARY:
                    if (c == '0' || c == '1' || c == 'x' || c == 'z' || c == 'X' || c == 'Z')
                        fCurrentTokenValue.append((char) c);
                    else {
                        fLiteralValue = new BitVector(getTokenString(), 2);
                        fPushBackChar = c;
                        return TOK_LITERAL;
                    }

                    break;

                case SCAN_DECIMAL:
                    if (c >= '0' && c <= '9')
                        fCurrentTokenValue.append((char) c);
                    else {
                        fLiteralValue = new BitVector(getTokenString(), 10);
                        fPushBackChar = c;
                        return TOK_LITERAL;
                    }

                    break;

                case SCAN_HEXADECIMAL:
                    if (isHexDigit(c) || c == 'x' || c == 'z' || c == 'X' || c == 'Z')
                        fCurrentTokenValue.append((char) c);
                    else {
                        fLiteralValue = new BitVector(getTokenString(), 16);
                        fPushBackChar = c;
                        return TOK_LITERAL;
                    }

                    break;
                }
            }
        }

        void pushBackToken(int tok) {
            fPushBackToken = tok;
        }

        String getTokenString() {
            return fCurrentTokenValue.toString();
        }

        BitVector getLiteralValue() {
            return fLiteralValue;
        }

        int getTokenStart() {
            return fTokenStart;
        }

        int getTokenEnd() {
            if (fCurrentTokenValue.length() == 0)
                return fTokenStart;
            else
                return fTokenStart + fCurrentTokenValue.length() - 1;
        }

        private int fLexerOffset;
        private StringBuffer fCurrentTokenValue = new StringBuffer();
        private int fPushBackChar = -1;
        private int fPushBackToken = -1;
        private int fTokenStart;
        private BitVector fLiteralValue;
        private String fSearchString;
    }

    /// Read the next token and throw an exception if the type does
    /// not match the parameter.
    private void match(int tokenType) throws ParseException {
        int got = fLexer.nextToken();
        if (got != tokenType) {
            if (got == Lexer.TOK_END)
                throw new ParseException("unexpected end of string",
                    fLexer.getTokenStart(), fLexer.getTokenEnd());
            else
                throw new ParseException("unexpected value",
                    fLexer.getTokenStart(), fLexer.getTokenEnd());
        }
    }

    /// Read the next token and check if is an identifier that matches
    /// the passed type. If not, push back the token and return false.
    /// @note this is case insensitive
    private boolean tryToMatch(String value) throws ParseException {
        int lookahead = fLexer.nextToken();
        if (lookahead != Lexer.TOK_IDENTIFIER
            || !fLexer.getTokenString().equalsIgnoreCase(value)) {
            fLexer.pushBackToken(lookahead);
            return false;
        }

        return true;
    }

    private ExpressionNode parseExpression() throws ParseException {
        return parseOr();
    }

    private ExpressionNode parseOr() throws ParseException {
        ExpressionNode left = parseAnd();
        while (tryToMatch("or"))
            left = new OrExpressionNode(left, parseAnd());

        return left;
    }

    private ExpressionNode parseAnd() throws ParseException {
        ExpressionNode left = parseCondition();
        while (tryToMatch("and"))
            left = new AndExpressionNode(left, parseCondition());

        return left;
    }

    private ExpressionNode parseCondition() throws ParseException {
        int lookahead = fLexer.nextToken();
        if (lookahead == '(') {
            ExpressionNode node = parseExpression();
            match(')');
            return node;
        }

        fLexer.pushBackToken(lookahead);
        ValueNode left = parseValue();
        lookahead = fLexer.nextToken();
        switch (lookahead) {
        case Lexer.TOK_GREATER:
            return new GreaterThanExpressionNode(left, parseValue());
        case Lexer.TOK_GREATER_EQUAL:
            return new GreaterEqualExpressionNode(left, parseValue());
        case Lexer.TOK_LESS_THAN:
            return new LessThanExpressionNode(left, parseValue());
        case Lexer.TOK_LESS_EQUAL:
            return new LessEqualExpressionNode(left, parseValue());
        case Lexer.TOK_NOT_EQUAL:
            return new NotEqualExpressionNode(left, parseValue());
        case '=':
            return new EqualExpressionNode(left, parseValue());
        default:
            // If there's not an operator, treat as != 0
            fLexer.pushBackToken(lookahead);
            return new NotEqualExpressionNode(left, new ConstValueNode(ZERO_VEC));
        }
    }

    private ValueNode parseValue() throws ParseException {
        int lookahead = fLexer.nextToken();
        if (lookahead == Lexer.TOK_IDENTIFIER) {
            int netId = fTraceDataModel.findNet(fLexer.getTokenString());
            if (netId < 0)
                throw new ParseException("unknown net \"" + fLexer.getTokenString() + "\"",
                    fLexer.getTokenStart(), fLexer.getTokenEnd());

            return new NetValueNode(netId, fTraceDataModel.getNetWidth(netId));
        } else {
            fLexer.pushBackToken(lookahead);
            match(Lexer.TOK_LITERAL);
            return new ConstValueNode(fLexer.getLiteralValue());
        }
    }

    private static class SearchHint {
        long forwardTimestamp;
        long backwardTimestamp;
    }

    private static abstract class ExpressionNode {
        /// Determine if this subexpression is true at the passed timestamp.
        /// @param timestamp Timestamp and which to evaluate.  If timestamp
        ///  is at a transition, the value after the transition will be used
        /// @param outHint Contains the next timestamp where the value of the
        ///   expression may change. It is guaranteed that no transition will occur
        ///   sooner than this value.
        /// @return
        ///   - true if the value at the timestamp makes this expression true
        ///   - false if the value at the timestamp makes this expression true
        abstract boolean evaluate(TraceDataModel model, long timestamp, SearchHint outHint);
    }

    private static abstract class BooleanExpressionNode extends ExpressionNode {
        BooleanExpressionNode(ExpressionNode left, ExpressionNode right) {
            fLeftChild = left;
            fRightChild = right;
        }

        @Override
        boolean evaluate(TraceDataModel model, long timestamp, SearchHint outHint) {
            boolean leftResult = fLeftChild.evaluate(model, timestamp, fLeftHint);
            boolean rightResult = fRightChild.evaluate(model, timestamp, fRightHint);

            // Compute the hints, which are the soonest time this expression
            // *could* change value. Call the subclassed methods.
            outHint.forwardTimestamp = nextHint(leftResult, rightResult,
                fLeftHint.forwardTimestamp, fRightHint.forwardTimestamp, false);
            outHint.backwardTimestamp = nextHint(leftResult, rightResult,
                fLeftHint.backwardTimestamp, fRightHint.backwardTimestamp, true);

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
        private SearchHint fLeftHint = new SearchHint();
        private SearchHint fRightHint = new SearchHint();
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
                // false is if both change to false.
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

    private static abstract class ValueNode {
        abstract BitVector evaluate(TraceDataModel model, long timestamp,
            SearchHint outHint);
    }

    private static class NetValueNode extends ValueNode {
        NetValueNode(int netId, int width) {
            fNetId = netId;
            fValue = new BitVector(width);
        }

        @Override
        BitVector evaluate(TraceDataModel model, long timestamp, SearchHint outHint) {
            Iterator<Transition> i = model.findTransition(fNetId, timestamp);
            Transition t = i.next();
            fValue.assign(t);
            if (timestamp >= t.getTimestamp())
                outHint.backwardTimestamp = t.getTimestamp() - 1;
            else
                outHint.backwardTimestamp = Long.MIN_VALUE;

            if (i.hasNext()) {
                t = i.next();
                outHint.forwardTimestamp = t.getTimestamp();
            } else
                outHint.forwardTimestamp = Long.MAX_VALUE;

            return fValue;
        }

        @Override
        public String toString() {
            return "net" + fNetId;
        }

        int fNetId;

        // Preallocated for efficiency. This is returned by evaluate.
        BitVector fValue = new BitVector();
    }

    private static class ConstValueNode extends ValueNode {
        ConstValueNode(BitVector constValue) {
            fValue = new BitVector(constValue);
        }

        @Override
        BitVector evaluate(TraceDataModel model, long timestamp, SearchHint outHint) {
            outHint.backwardTimestamp = Long.MIN_VALUE;
            outHint.forwardTimestamp = Long.MAX_VALUE;
            return fValue;
        }

        @Override
        public String toString() {
            return fValue.toString();
        }

        BitVector fValue;
    }

    private static abstract class ComparisonExpressionNode extends ExpressionNode {
        protected ComparisonExpressionNode(ValueNode left, ValueNode right) {
            fLeftNode = left;
            fRightNode = right;
        }

        @Override
        boolean evaluate(TraceDataModel model, long timestamp, SearchHint outHint) {
            BitVector leftValue = fLeftNode.evaluate(model, timestamp, fLeftHint);
            BitVector rightValue = fRightNode.evaluate(model, timestamp, fRightHint);
            boolean result = doCompare(leftValue, rightValue);
            outHint.backwardTimestamp = Math.max(fLeftHint.backwardTimestamp,
                fRightHint.backwardTimestamp);
            outHint.forwardTimestamp = Math.min(fLeftHint.forwardTimestamp,
                fRightHint.forwardTimestamp);
            return result;
        }

        abstract protected boolean doCompare(BitVector value1, BitVector value2);

        protected ValueNode fLeftNode;
        protected ValueNode fRightNode;

        // These are preallocated for efficiency and aren't used outside
        // the evaluate() call.
        private SearchHint fLeftHint = new SearchHint();
        private SearchHint fRightHint = new SearchHint();
    }

    private static class EqualExpressionNode extends ComparisonExpressionNode {
        EqualExpressionNode(ValueNode left, ValueNode right) {
            super(left, right);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) == 0;
        }

        @Override
        public String toString() {
            return "(eq " + fLeftNode + " " + fRightNode + ")";
        }
    }

    private static class NotEqualExpressionNode extends ComparisonExpressionNode {
        NotEqualExpressionNode(ValueNode left, ValueNode right) {
            super(left, right);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) != 0;
        }

        @Override
        public String toString() {
            return "(ne " + fLeftNode + " " + fRightNode + ")";
        }
    }

    private static class GreaterThanExpressionNode extends ComparisonExpressionNode {
        GreaterThanExpressionNode(ValueNode left, ValueNode right) {
            super(left, right);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) > 0;
        }

        @Override
        public String toString() {
            return "(gt " + fLeftNode + " " + fRightNode + ")";
        }
    }

    private static class GreaterEqualExpressionNode extends ComparisonExpressionNode {
        GreaterEqualExpressionNode(ValueNode left, ValueNode right) {
            super(left, right);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) >= 0;
        }

        @Override
        public String toString() {
            return "(ge " + fLeftNode + " " + fRightNode + ")";
        }
    }

    private static class LessThanExpressionNode extends ComparisonExpressionNode {
        LessThanExpressionNode(ValueNode left, ValueNode right) {
            super(left, right);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) < 0;
        }

        @Override
        public String toString() {
            return "(lt " + fLeftNode + " " + fRightNode + ")";
        }
    }

    private static class LessEqualExpressionNode extends ComparisonExpressionNode {
        LessEqualExpressionNode(ValueNode left, ValueNode right) {
            super(left, right);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) <= 0;
        }

        @Override
        public String toString() {
            return "(le " + fLeftNode + " " + fRightNode + ")";
        }
    }

    private Lexer fLexer;
    private TraceDataModel fTraceDataModel;
    private ExpressionNode fSearchExpression;
    private static final BitVector ZERO_VEC = new BitVector("0", 2);
}


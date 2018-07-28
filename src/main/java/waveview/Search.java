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

import java.util.Iterator;

///
/// The Search class allows searching for logic conditions using complex boolean expressions
/// For example: (ena = 1 and (addr = 'h1000 or addr = 'h2000))
/// It builds an expression tree to represent the search criteria. It is optimized
/// for fast searching, skipping events that cannot meet the criteria.
///
/// @todo Support slice multi-net matches
///
public class Search {
    private static final BitVector ZERO_VEC = new BitVector("0", 2);
    private final Lexer lexer;
    private final WaveformDataModel waveformDataModel;
    private ExpressionNode searchExpression;

    /// Generate a search given a set of nets that matches at the given timestamp.
    public static String generateSearch(NetDataModel[] nets, long timestamp) {
        StringBuilder searchExpr = new StringBuilder();
        boolean first = true;
        for (NetDataModel netDataModel : nets) {
            if (first) {
                first = false;
            } else {
                searchExpr.append(" and ");
            }

            searchExpr.append(netDataModel.getFullName());

            Transition t = netDataModel.findTransition(timestamp).next();
            searchExpr.append(" = 'h");
            searchExpr.append(t.toString(16));
        }

        return searchExpr.toString();
    }

    public Search(WaveformDataModel waveformDataModel, String searchString) throws ParseException {
        this.waveformDataModel = waveformDataModel;
        lexer = new Lexer(searchString);
        searchExpression = parseExpression();
        match(Lexer.TOK_END);
    }

    /// Mainly useful for unit testing
    /// @returns true if this search string matches at the passed timestamp
    public boolean matches(long timestamp) {
        SearchHint hint = new SearchHint();
        return searchExpression.evaluate(waveformDataModel, timestamp, hint);
    }

    ///
    /// Scan forward to find the next timestamp that matches this search's
    /// expression
    /// If the startTimestamp is already a match, it will not be returned. This will
    /// instead scan to the next transition
    /// @bug If startTimestamp is before the first event, and the first event
    /// matches,
    /// this will not return it.
    /// @returns
    /// -1 If there are no matches in the forward direction
    /// timestamp of the next forward match otherwise
    ///
    public long getNextMatch(long startTimestamp) {
        SearchHint hint = new SearchHint();
        long currentTime = startTimestamp;
        boolean currentValue = searchExpression.evaluate(waveformDataModel, currentTime, hint);

        // If the start timestamp is already at a region that is true, scan
        // first to find a place where the expression is false.
        while (currentValue) {
            if (hint.forwardTimestamp == Long.MAX_VALUE) {
                return -1; // End of waveform
            }

            currentTime = hint.forwardTimestamp;
            currentValue = searchExpression.evaluate(waveformDataModel, currentTime, hint);
        }

        // Scan to find where the expression is true
        while (!currentValue) {
            if (hint.forwardTimestamp == Long.MAX_VALUE) {
                return -1; // End of waveform
            }

            currentTime = hint.forwardTimestamp;
            currentValue = searchExpression.evaluate(waveformDataModel, currentTime, hint);
        }

        return currentTime;
    }

    ///
    /// Scan backward to find the end of the region that
    /// matches this search's expression If the startTimestamp is already
    /// in a match, it will jump to end of the previous region that matches.
    /// @returns
    /// -1 If there are no matches in the backward direction
    /// timestamp of the next backward match otherwise
    ///
    public long getPreviousMatch(long startTimestamp) {
        SearchHint hint = new SearchHint();
        long currentTime = startTimestamp;
        boolean currentValue = searchExpression.evaluate(waveformDataModel, currentTime, hint);
        while (currentValue) {
            if (hint.backwardTimestamp == Long.MIN_VALUE) {
                return -1; // End of waveform
            }

            currentTime = hint.backwardTimestamp;
            currentValue = searchExpression.evaluate(waveformDataModel, currentTime, hint);
        }

        while (!currentValue) {
            if (hint.backwardTimestamp == Long.MIN_VALUE) {
                return -1; // End of waveform
            }

            currentTime = hint.backwardTimestamp;
            currentValue = searchExpression.evaluate(waveformDataModel, currentTime, hint);
        }

        return currentTime;
    }

    public static class ParseException extends Exception {
        private final int startOffset;
        private final int endOffset;

        ParseException(String what, int startOffset, int endOffset) {
            super(what);
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }

        public int getStartOffset() {
            return startOffset;
        }

        public int getEndOffset() {
            return endOffset;
        }
    }

    @Override
    public String toString() {
        return searchExpression.toString();
    }

    private static class Lexer {
        static final int TOK_IDENTIFIER = 1000;
        static final int TOK_END = 1001;
        static final int TOK_LITERAL = 1002;
        static final int TOK_GREATER = 1003;
        static final int TOK_GREATER_EQUAL = 1004;
        static final int TOK_LESS_THAN = 1005;
        static final int TOK_LESS_EQUAL = 1006;
        static final int TOK_NOT_EQUAL = 1007;

        private enum State {
            SCAN_INIT, SCAN_IDENTIFIER, SCAN_LITERAL_TYPE, SCAN_GEN_NUM, SCAN_GREATER, SCAN_LESS, SCAN_BINARY,
            SCAN_DECIMAL, SCAN_HEXADECIMAL
        }

        private int lexerOffset;
        private final StringBuilder currentTokenValue = new StringBuilder();
        private int pushBackChar = -1;
        private int pushBackToken = -1;
        private int tokenStart;
        private BitVector literalValue;
        private final String searchString;

        Lexer(String searchString) {
            this.searchString = searchString;
        }

        private static boolean isAlpha(int value) {
            return (value >= 'a' && value <= 'z') || (value >= 'A' && value <= 'Z');
        }

        private static boolean isNum(int value) {
            return value >= '0' && value <= '9';
        }

        private static boolean isHexDigit(int value) {
            return (value >= '0' && value <= '9') || (value >= 'a' && value <= 'f') || (value >= 'A' && value <= 'F');
        }

        private static boolean isAlphaNum(int value) {
            return isAlpha(value) || isNum(value);
        }

        private static boolean isSpace(int value) {
            return value == ' ' || value == '\t' || value == '\n' || value == '\r';
        }

        int nextToken() throws ParseException {
            if (pushBackToken != -1) {
                int token = pushBackToken;
                pushBackToken = -1;
                return token;
            }

            State state = State.SCAN_INIT;
            currentTokenValue.setLength(0);

            for (;;) {
                int c;

                if (pushBackChar == -1) {
                    if (lexerOffset == searchString.length()) {
                        c = -1;
                    } else {
                        c = searchString.charAt(lexerOffset++);
                    }
                } else {
                    c = pushBackChar;
                    pushBackChar = -1;
                }

                switch (state) {
                case SCAN_INIT:
                    tokenStart = lexerOffset - 1;
                    if (c == -1) {
                        return TOK_END;
                    } else if (c == '\'') {
                        state = State.SCAN_LITERAL_TYPE;
                    } else if (isAlpha(c)) {
                        pushBackChar = c;
                        state = State.SCAN_IDENTIFIER;
                    } else if (isNum(c)) {
                        pushBackChar = c;
                        state = State.SCAN_DECIMAL;
                    } else if (c == '>') {
                        state = State.SCAN_GREATER;
                    } else if (c == '<') {
                        state = State.SCAN_LESS;
                    } else if (!isSpace(c)) {
                        return c;
                    }

                    break;

                case SCAN_GREATER:
                    if (c == '<') {
                        return TOK_NOT_EQUAL;
                    } else if (c == '=') {
                        return TOK_GREATER_EQUAL;
                    } else {
                        pushBackChar = c;
                        return TOK_GREATER;
                    }

                case SCAN_LESS:
                    if (c == '>') {
                        return TOK_NOT_EQUAL;
                    } else if (c == '=') {
                        return TOK_LESS_EQUAL;
                    } else {
                        pushBackChar = c;
                        return TOK_LESS_THAN;
                    }

                case SCAN_IDENTIFIER:
                    if (isAlphaNum(c) || c == '_' || c == '.') {
                        currentTokenValue.append((char) c);
                    } else if (c == '(') { // Start generate index
                        currentTokenValue.append((char) c);
                        state = State.SCAN_GEN_NUM;
                    } else {
                        pushBackChar = c;
                        return TOK_IDENTIFIER;
                    }

                    break;

                case SCAN_GEN_NUM:
                    currentTokenValue.append((char) c);
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
                        throw new ParseException("unknown type " + (char) c, getTokenStart(), getTokenEnd());
                    }

                    break;

                case SCAN_BINARY:
                    if (c == '0' || c == '1' || c == 'x' || c == 'z' || c == 'X' || c == 'Z') {
                        currentTokenValue.append((char) c);
                    } else {
                        literalValue = new BitVector(getTokenString(), 2);
                        pushBackChar = c;
                        return TOK_LITERAL;
                    }

                    break;

                case SCAN_DECIMAL:
                    if (c >= '0' && c <= '9') {
                        currentTokenValue.append((char) c);
                    } else {
                        literalValue = new BitVector(getTokenString(), 10);
                        pushBackChar = c;
                        return TOK_LITERAL;
                    }

                    break;

                case SCAN_HEXADECIMAL:
                    if (isHexDigit(c) || c == 'x' || c == 'z' || c == 'X' || c == 'Z') {
                        currentTokenValue.append((char) c);
                    } else {
                        literalValue = new BitVector(getTokenString(), 16);
                        pushBackChar = c;
                        return TOK_LITERAL;
                    }

                    break;
                }
            }
        }

        void pushBackToken(int tok) {
            pushBackToken = tok;
        }

        String getTokenString() {
            return currentTokenValue.toString();
        }

        BitVector getLiteralValue() {
            return literalValue;
        }

        int getTokenStart() {
            return tokenStart;
        }

        int getTokenEnd() {
            if (currentTokenValue.length() == 0) {
                return tokenStart;
            } else {
                return tokenStart + currentTokenValue.length() - 1;
            }
        }
    }

    /// Read the next token and throw an exception if the type does
    /// not match the parameter.
    private void match(int tokenType) throws ParseException {
        int got = lexer.nextToken();
        if (got != tokenType) {
            if (got == Lexer.TOK_END) {
                throw new ParseException("unexpected end of string", lexer.getTokenStart(), lexer.getTokenEnd());
            } else {
                throw new ParseException("unexpected value", lexer.getTokenStart(), lexer.getTokenEnd());
            }
        }
    }

    /// Read the next token and check if is an identifier that matches
    /// the passed type. If not, push back the token and return false.
    /// @note this is case insensitive
    private boolean tryToMatch(String value) throws ParseException {
        int lookahead = lexer.nextToken();
        if (lookahead != Lexer.TOK_IDENTIFIER || !lexer.getTokenString().equalsIgnoreCase(value)) {
            lexer.pushBackToken(lookahead);
            return false;
        }

        return true;
    }

    private ExpressionNode parseExpression() throws ParseException {
        return parseOr();
    }

    private ExpressionNode parseOr() throws ParseException {
        ExpressionNode left = parseAnd();
        while (tryToMatch("or")) {
            left = new OrExpressionNode(left, parseAnd());
        }

        return left;
    }

    private ExpressionNode parseAnd() throws ParseException {
        ExpressionNode left = parseCondition();
        while (tryToMatch("and")) {
            left = new AndExpressionNode(left, parseCondition());
        }

        return left;
    }

    private ExpressionNode parseCondition() throws ParseException {
        int lookahead = lexer.nextToken();
        if (lookahead == '(') {
            ExpressionNode node = parseExpression();
            match(')');
            return node;
        }

        lexer.pushBackToken(lookahead);
        ValueNode left = parseValue();
        lookahead = lexer.nextToken();
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
            lexer.pushBackToken(lookahead);
            return new NotEqualExpressionNode(left, new ConstValueNode(ZERO_VEC));
        }
    }

    private ValueNode parseValue() throws ParseException {
        int lookahead = lexer.nextToken();
        if (lookahead == Lexer.TOK_IDENTIFIER) {
            NetDataModel netDataModel = waveformDataModel.findNet(lexer.getTokenString());
            if (netDataModel == null) {
                throw new ParseException("unknown net \"" + lexer.getTokenString() + "\"", lexer.getTokenStart(),
                        lexer.getTokenEnd());
            }

            return new NetValueNode(netDataModel);
        } else {
            lexer.pushBackToken(lookahead);
            match(Lexer.TOK_LITERAL);
            return new ConstValueNode(lexer.getLiteralValue());
        }
    }

    private static class SearchHint {
        long forwardTimestamp;
        long backwardTimestamp;
    }

    private abstract static class ExpressionNode {
        /// Determine if this subexpression is true at the passed timestamp.
        /// @param timestamp Timestamp and which to evaluate. If timestamp
        /// is at a transition, the value after the transition will be used
        /// @param outHint Contains the next timestamp where the value of the
        /// expression may change. It is guaranteed that no transition will occur
        /// sooner than this value.
        /// @return
        /// - true if the value at the timestamp makes this expression true
        /// - false if the value at the timestamp makes this expression true
        abstract boolean evaluate(WaveformDataModel model, long timestamp, SearchHint outHint);
    }

    private abstract static class BooleanExpressionNode extends ExpressionNode {
        protected final ExpressionNode leftChild;
        protected final ExpressionNode rightChild;

        // These are preallocated for efficiency and aren't used outside
        // the evaluate() call.
        private final SearchHint leftHint = new SearchHint();
        private final SearchHint rightHint = new SearchHint();

        BooleanExpressionNode(ExpressionNode leftChild, ExpressionNode rightChild) {
            this.leftChild = leftChild;
            this.rightChild = rightChild;
        }

        @Override
        boolean evaluate(WaveformDataModel model, long timestamp, SearchHint outHint) {
            boolean leftResult = leftChild.evaluate(model, timestamp, leftHint);
            boolean rightResult = rightChild.evaluate(model, timestamp, rightHint);

            // Compute the hints, which are the soonest time this expression
            // *could* change value. Call the subclassed methods.
            outHint.forwardTimestamp = nextHint(leftResult, rightResult, leftHint.forwardTimestamp,
                    rightHint.forwardTimestamp, false);
            outHint.backwardTimestamp = nextHint(leftResult, rightResult, leftHint.backwardTimestamp,
                    rightHint.backwardTimestamp, true);

            // Return the result at this time
            return compareResults(leftResult, rightResult);
        }

        protected abstract boolean compareResults(boolean value1, boolean value2);

        protected abstract long nextHint(boolean leftResult, boolean rightResult, long nextLeftTimestamp,
                long nextRightTimestamp, boolean searchBackward);
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
        protected long nextHint(boolean leftResult, boolean rightResult, long nextLeftTimestamp,
                long nextRightTimestamp, boolean searchBackward) {

            if (leftResult && rightResult) {
                // Both expressions are true. The only way for this to become
                // false is if both change to false.
                if (searchBackward) {
                    return Math.min(nextLeftTimestamp, nextRightTimestamp);
                } else {
                    return Math.max(nextLeftTimestamp, nextRightTimestamp);
                }
            } else if (leftResult) {
                // Currently true. It can only become false when left result
                // becomes false.
                return nextLeftTimestamp;
            } else if (rightResult) {
                // Currently true. It can only become false when right result
                // becomes false.
                return nextRightTimestamp;
            } else {
                // Both expressions are false. May become true if either subexpression
                // changes.
                if (searchBackward) {
                    return Math.max(nextLeftTimestamp, nextRightTimestamp);
                } else {
                    return Math.min(nextLeftTimestamp, nextRightTimestamp);
                }
            }
        }

        @Override
        public String toString() {
            return "(or " + leftChild + " " + rightChild + ")";
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
        protected long nextHint(boolean leftResult, boolean rightResult, long nextLeftTimestamp,
                long nextRightTimestamp, boolean searchBackward) {

            if (leftResult && rightResult) {
                // Both expressions are true. Either expression changing
                // could make it false.
                if (searchBackward) {
                    return Math.max(nextLeftTimestamp, nextRightTimestamp);
                } else {
                    return Math.min(nextLeftTimestamp, nextRightTimestamp);
                }
            } else if (leftResult) {
                // Currently false. It can only become true when right result
                // becomes true.
                return nextRightTimestamp;
            } else if (rightResult) {
                // Currently false. It can only become true when left result
                // becomes true.
                return nextLeftTimestamp;
            } else {
                // Both expressions are false. Both must change before this
                // may become true.
                if (searchBackward) {
                    return Math.min(nextLeftTimestamp, nextRightTimestamp);
                } else {
                    return Math.max(nextLeftTimestamp, nextRightTimestamp);
                }
            }
        }

        @Override
        public String toString() {
            return "(and " + leftChild + " " + rightChild + ")";
        }
    }

    private abstract static class ValueNode {
        abstract BitVector evaluate(WaveformDataModel model, long timestamp, SearchHint outHint);
    }

    private static class NetValueNode extends ValueNode {
        private final NetDataModel netDataModel;

        // Preallocated for efficiency. This is returned by evaluate.
        private BitVector value;

        NetValueNode(NetDataModel netDataModel) {
            this.netDataModel = netDataModel;
            value = new BitVector(netDataModel.getWidth());
        }

        @Override
        BitVector evaluate(WaveformDataModel model, long timestamp, SearchHint outHint) {
            Iterator<Transition> i = netDataModel.findTransition(timestamp);
            Transition t = i.next();
            value.assign(t);
            if (timestamp >= t.getTimestamp()) {
                outHint.backwardTimestamp = t.getTimestamp() - 1;
            } else {
                outHint.backwardTimestamp = Long.MIN_VALUE;
            }

            if (i.hasNext()) {
                t = i.next();
                outHint.forwardTimestamp = t.getTimestamp();
            } else {
                outHint.forwardTimestamp = Long.MAX_VALUE;
            }

            return value;
        }

        @Override
        public String toString() {
            return netDataModel.getFullName();
        }
    }

    private static class ConstValueNode extends ValueNode {
        BitVector value;

        ConstValueNode(BitVector constValue) {
            value = new BitVector(constValue);
        }

        @Override
        BitVector evaluate(WaveformDataModel model, long timestamp, SearchHint outHint) {
            outHint.backwardTimestamp = Long.MIN_VALUE;
            outHint.forwardTimestamp = Long.MAX_VALUE;
            return value;
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    private abstract static class ComparisonExpressionNode extends ExpressionNode {
        protected final ValueNode leftNode;
        protected final ValueNode rightNode;

        // These are preallocated for efficiency and aren't used outside
        // the evaluate() call.
        private final SearchHint leftHint = new SearchHint();
        private final SearchHint rightHint = new SearchHint();

        protected ComparisonExpressionNode(ValueNode leftNode, ValueNode rightNode) {
            this.leftNode = leftNode;
            this.rightNode = rightNode;
        }

        @Override
        boolean evaluate(WaveformDataModel model, long timestamp, SearchHint outHint) {
            BitVector leftValue = leftNode.evaluate(model, timestamp, leftHint);
            BitVector rightValue = rightNode.evaluate(model, timestamp, rightHint);
            boolean result = doCompare(leftValue, rightValue);
            outHint.backwardTimestamp = Math.max(leftHint.backwardTimestamp, rightHint.backwardTimestamp);
            outHint.forwardTimestamp = Math.min(leftHint.forwardTimestamp, rightHint.forwardTimestamp);
            return result;
        }

        protected abstract boolean doCompare(BitVector value1, BitVector value2);
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
            return "(eq " + leftNode + " " + rightNode + ")";
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
            return "(ne " + leftNode + " " + rightNode + ")";
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
            return "(gt " + leftNode + " " + rightNode + ")";
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
            return "(ge " + leftNode + " " + rightNode + ")";
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
            return "(lt " + leftNode + " " + rightNode + ")";
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
            return "(le " + leftNode + " " + rightNode + ")";
        }
    }
}

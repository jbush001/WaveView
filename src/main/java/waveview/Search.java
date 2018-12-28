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
/// XXX The term 'match' is overloaded here: it refers in some places to the act of searching,
/// and others in parsing.
///
public final class Search {
    private static final BitVector ZERO_VEC = new BitVector("0", 2);
    private final SearchLexer lexer;
    private final WaveformDataModel waveformDataModel;
    private final BooleanExpressionNode searchExpression;

    /// Generate a search given a set of nets that matches at the given timestamp.
    public static String generateFromValuesAt(NetDataModel[] nets, long timestamp) {
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

    public Search(WaveformDataModel waveformDataModel, String searchString) throws SearchFormatException {
        this.waveformDataModel = waveformDataModel;
        lexer = new SearchLexer(searchString);
        searchExpression = parseExpression();
        match(SearchLexer.TOK_END);
    }

    /// Mainly useful for unit testing
    /// @returns true if this search string matches at the passed timestamp
    public boolean matches(long timestamp) {
        return searchExpression.evaluate(waveformDataModel, timestamp);
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
        long currentTime = startTimestamp;
        boolean currentValue = searchExpression.evaluate(waveformDataModel, currentTime);

        // If the start timestamp is already at a region that is true, scan
        // first to find a place where the expression is false.
        while (currentValue) {
            if (searchExpression.forwardHint == Long.MAX_VALUE) {
                return -1; // End of waveform
            }

            currentTime = searchExpression.forwardHint;
            currentValue = searchExpression.evaluate(waveformDataModel, currentTime);
        }

        // Scan to find where the expression is true
        while (!currentValue) {
            if (searchExpression.forwardHint == Long.MAX_VALUE) {
                return -1; // End of waveform
            }

            currentTime = searchExpression.forwardHint;
            currentValue = searchExpression.evaluate(waveformDataModel, currentTime);
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
        long currentTime = startTimestamp;
        boolean currentValue = searchExpression.evaluate(waveformDataModel, currentTime);
        while (currentValue) {
            if (searchExpression.backwardHint == Long.MIN_VALUE) {
                return -1; // End of waveform
            }

            currentTime = searchExpression.backwardHint;
            currentValue = searchExpression.evaluate(waveformDataModel, currentTime);
        }

        while (!currentValue) {
            if (searchExpression.backwardHint == Long.MIN_VALUE) {
                return -1; // End of waveform
            }

            currentTime = searchExpression.backwardHint;
            currentValue = searchExpression.evaluate(waveformDataModel, currentTime);
        }

        return currentTime;
    }

    @Override
    public String toString() {
        return searchExpression.toString();
    }

    /// Read the next token and throw an exception if the type does
    /// not match the parameter.
    private void match(int tokenType) throws SearchFormatException {
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
    private boolean tryToMatch(String value) throws SearchFormatException {
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
        while (tryToMatch("or")) {
            left = new OrExpressionNode(left, parseAnd());
        }

        return left;
    }

    private BooleanExpressionNode parseAnd() throws SearchFormatException {
        BooleanExpressionNode left = parseCondition();
        while (tryToMatch("and")) {
            left = new AndExpressionNode(left, parseCondition());
        }

        return left;
    }

    private BooleanExpressionNode parseCondition() throws SearchFormatException {
        int lookahead = lexer.nextToken();
        if (lookahead == '(') {
            BooleanExpressionNode node = parseExpression();
            match(')');
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
                return new NotEqualExpressionNode(left, new ConstValueNode(ZERO_VEC));
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
            match(SearchLexer.TOK_LITERAL);
            return new ConstValueNode(lexer.getLiteralValue());
        }
    }

    private static class ExpressionNode {
        // These are set as a side effect of evaluating the node at a specific time.
        // They contain the next timestamp where the value of the expression may change.
        // It is guaranteed that no transition will occur sooner than this value.
        long forwardHint;
        long backwardHint;

        protected ExpressionNode() {}
    }

    private abstract static class BooleanExpressionNode extends ExpressionNode {
        /// Determine if this subexpression is true at the passed timestamp.
        /// @param timestamp Timestamp and which to evaluate. If timestamp
        /// is at a transition, the value after the transition will be used
        /// @return
        /// - true if the value at the timestamp makes this expression true
        /// - false if the value at the timestamp makes this expression true
        abstract boolean evaluate(WaveformDataModel model, long timestamp);
    }

    private abstract static class LogicalExpressionNode extends BooleanExpressionNode {
        protected final BooleanExpressionNode leftChild;
        protected final BooleanExpressionNode rightChild;

        LogicalExpressionNode(BooleanExpressionNode leftChild, BooleanExpressionNode rightChild) {
            this.leftChild = leftChild;
            this.rightChild = rightChild;
        }

        @Override
        boolean evaluate(WaveformDataModel model, long timestamp) {
            boolean leftResult = leftChild.evaluate(model, timestamp);
            boolean rightResult = rightChild.evaluate(model, timestamp);

            forwardHint = nextForwardHint(leftResult, rightResult, leftChild.forwardHint,
                    rightChild.forwardHint);
            backwardHint = nextBackwardHint(leftResult, rightResult, leftChild.backwardHint,
                    rightChild.backwardHint);

            return compareResults(leftResult, rightResult);
        }

        protected abstract boolean compareResults(boolean value1, boolean value2);

        protected abstract long nextForwardHint(boolean leftResult, boolean rightResult,
                long nextLeftTimestamp, long nextRightTimestamp);
        protected abstract long nextBackwardHint(boolean leftResult, boolean rightResult,
                long nextLeftTimestamp, long nextRightTimestamp);
    }

    private static final class OrExpressionNode extends LogicalExpressionNode {
        OrExpressionNode(BooleanExpressionNode left, BooleanExpressionNode right) {
            super(left, right);
        }

        @Override
        protected boolean compareResults(boolean value1, boolean value2) {
            return value1 || value2;
        }

        @Override
        protected long nextForwardHint(boolean leftResult, boolean rightResult, long nextLeftTimestamp,
                long nextRightTimestamp) {
            if (leftResult && rightResult) {
                // Both expressions are true. The only way for this to become
                // false is if both change to false.
                return Math.max(nextLeftTimestamp, nextRightTimestamp);
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
                return Math.min(nextLeftTimestamp, nextRightTimestamp);
            }
        }

        // Mirror of above
        @Override
        protected long nextBackwardHint(boolean leftResult, boolean rightResult, long nextLeftTimestamp,
                long nextRightTimestamp) {
            if (leftResult && rightResult) {
                return Math.min(nextLeftTimestamp, nextRightTimestamp);
            } else if (leftResult) {
                return nextLeftTimestamp;
            } else if (rightResult) {
                return nextRightTimestamp;
            } else {
                return Math.max(nextLeftTimestamp, nextRightTimestamp);
            }
        }

        @Override
        public String toString() {
            return "(or " + leftChild + " " + rightChild + ")";
        }
    }

    private static final class AndExpressionNode extends LogicalExpressionNode {
        AndExpressionNode(BooleanExpressionNode left, BooleanExpressionNode right) {
            super(left, right);
        }

        @Override
        protected boolean compareResults(boolean value1, boolean value2) {
            return value1 && value2;
        }

        @Override
        protected long nextForwardHint(boolean leftResult, boolean rightResult, long nextLeftTimestamp,
                long nextRightTimestamp) {
            if (leftResult && rightResult) {
                // Both expressions are true. Either expression changing
                // could make it false.
                return Math.min(nextLeftTimestamp, nextRightTimestamp);
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
                return Math.max(nextLeftTimestamp, nextRightTimestamp);
            }
        }

        // Mirror of above
        @Override
        protected long nextBackwardHint(boolean leftResult, boolean rightResult, long nextLeftTimestamp,
                long nextRightTimestamp) {
            if (leftResult && rightResult) {
                return Math.max(nextLeftTimestamp, nextRightTimestamp);
            } else if (leftResult) {
                return nextRightTimestamp;
            } else if (rightResult) {
                return nextLeftTimestamp;
            } else {
                return Math.min(nextLeftTimestamp, nextRightTimestamp);
            }
        }

        @Override
        public String toString() {
            return "(and " + leftChild + " " + rightChild + ")";
        }
    }

    private abstract static class ValueNode extends ExpressionNode {
        abstract BitVector evaluate(WaveformDataModel model, long timestamp);
    }

    private static final class NetValueNode extends ValueNode {
        private final NetDataModel netDataModel;

        NetValueNode(NetDataModel netDataModel) {
            this.netDataModel = netDataModel;
        }

        @Override
        BitVector evaluate(WaveformDataModel model, long timestamp) {
            Iterator<Transition> i = netDataModel.findTransition(timestamp);
            Transition t = i.next();
            BitVector value = new BitVector(t);
            if (timestamp >= t.getTimestamp()) {
                backwardHint = t.getTimestamp() - 1;
            } else {
                backwardHint = Long.MIN_VALUE;
            }

            if (i.hasNext()) {
                t = i.next();
                forwardHint = t.getTimestamp();
            } else {
                forwardHint = Long.MAX_VALUE;
            }

            return value;
        }

        @Override
        public String toString() {
            return netDataModel.getFullName();
        }
    }

    private static final class ConstValueNode extends ValueNode {
        private final BitVector value;

        ConstValueNode(BitVector constValue) {
            value = new BitVector(constValue);
        }

        @Override
        BitVector evaluate(WaveformDataModel model, long timestamp) {
            backwardHint = Long.MIN_VALUE;
            forwardHint = Long.MAX_VALUE;
            return value;
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    private abstract static class ComparisonExpressionNode extends BooleanExpressionNode {
        protected final ValueNode leftChild;
        protected final ValueNode rightChild;

        protected ComparisonExpressionNode(ValueNode leftNode, ValueNode rightNode) {
            this.leftChild = leftNode;
            this.rightChild = rightNode;
        }

        @Override
        boolean evaluate(WaveformDataModel model, long timestamp) {
            BitVector leftValue = leftChild.evaluate(model, timestamp);
            BitVector rightValue = rightChild.evaluate(model, timestamp);
            boolean result = doCompare(leftValue, rightValue);
            backwardHint = Math.max(leftChild.backwardHint, rightChild.backwardHint);
            forwardHint = Math.min(leftChild.forwardHint, rightChild.forwardHint);
            return result;
        }

        protected abstract boolean doCompare(BitVector value1, BitVector value2);
    }

    private static final class EqualExpressionNode extends ComparisonExpressionNode {
        EqualExpressionNode(ValueNode left, ValueNode right) {
            super(left, right);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) == 0;
        }

        @Override
        public String toString() {
            return "(eq " + leftChild + " " + rightChild + ")";
        }
    }

    private static final class NotEqualExpressionNode extends ComparisonExpressionNode {
        NotEqualExpressionNode(ValueNode left, ValueNode right) {
            super(left, right);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) != 0;
        }

        @Override
        public String toString() {
            return "(ne " + leftChild + " " + rightChild + ")";
        }
    }

    private static final class GreaterThanExpressionNode extends ComparisonExpressionNode {
        GreaterThanExpressionNode(ValueNode left, ValueNode right) {
            super(left, right);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) > 0;
        }

        @Override
        public String toString() {
            return "(gt " + leftChild + " " + rightChild + ")";
        }
    }

    private static final class GreaterEqualExpressionNode extends ComparisonExpressionNode {
        GreaterEqualExpressionNode(ValueNode left, ValueNode right) {
            super(left, right);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) >= 0;
        }

        @Override
        public String toString() {
            return "(ge " + leftChild + " " + rightChild + ")";
        }
    }

    private static final class LessThanExpressionNode extends ComparisonExpressionNode {
        LessThanExpressionNode(ValueNode left, ValueNode right) {
            super(left, right);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) < 0;
        }

        @Override
        public String toString() {
            return "(lt " + leftChild + " " + rightChild + ")";
        }
    }

    private static final class LessEqualExpressionNode extends ComparisonExpressionNode {
        LessEqualExpressionNode(ValueNode left, ValueNode right) {
            super(left, right);
        }

        @Override
        protected boolean doCompare(BitVector value1, BitVector value2) {
            return value1.compare(value2) <= 0;
        }

        @Override
        public String toString() {
            return "(le " + leftChild + " " + rightChild + ")";
        }
    }
}

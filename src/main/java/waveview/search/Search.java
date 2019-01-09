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

import waveview.wavedata.NetDataModel;
import waveview.wavedata.Transition;
import waveview.wavedata.WaveformDataModel;

///
/// The Search class allows searching for logic conditions using complex boolean
/// expressions For example: (ena = 1 and (addr = 'h1000 or addr = 'h2000)) It
/// builds an expression tree to represent the search criteria. It is optimized
/// for fast searching, skipping events that cannot meet the criteria.
///
/// @todo Support slice multi-net matches
///
public final class Search {
    private final WaveformDataModel waveformDataModel;
    private final BooleanExpressionNode searchExpression;

    /// Generate a search given a set of nets that matches at the given
    /// timestamp.
    public static String generateFromValuesAt(NetDataModel[] nets,
                                              long timestamp) {
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

    public Search(WaveformDataModel waveformDataModel, String searchString)
        throws SearchFormatException {
        this.waveformDataModel = waveformDataModel;
        SearchParser parser = new SearchParser(waveformDataModel, searchString);
        searchExpression = parser.getExpression();
    }

    /// Mainly useful for unit testing
    /// @returns true if this search string matches at the passed timestamp
    public boolean matches(long timestamp) {
        return searchExpression.evaluate(timestamp);
    }

    ///
    /// Scan forward to find the next timestamp that matches this search's
    /// expression
    /// If the startTimestamp is already a match, it will not be returned. This
    /// will instead scan to the next transition
    /// @bug If startTimestamp is before the first event, and the first event
    /// matches,
    /// this will not return it.
    /// @returns
    /// -1 If there are no matches in the forward direction
    /// timestamp of the next forward match otherwise
    ///
    public long getNextMatch(long startTimestamp) {
        long currentTime = startTimestamp;
        boolean currentValue =
            searchExpression.evaluate(currentTime);

        // If the start timestamp is already at a region that is true, scan
        // first to find a place where the expression is false.
        while (currentValue) {
            if (searchExpression.forwardHint == Long.MAX_VALUE) {
                return -1; // End of waveform
            }

            currentTime = searchExpression.forwardHint;
            currentValue =
                searchExpression.evaluate(currentTime);
        }

        // Scan to find where the expression is true
        while (!currentValue) {
            if (searchExpression.forwardHint == Long.MAX_VALUE) {
                return -1; // End of waveform
            }

            currentTime = searchExpression.forwardHint;
            currentValue =
                searchExpression.evaluate(currentTime);
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
        boolean currentValue =
            searchExpression.evaluate(currentTime);
        while (currentValue) {
            if (searchExpression.backwardHint == Long.MIN_VALUE) {
                return -1; // End of waveform
            }

            currentTime = searchExpression.backwardHint;
            currentValue =
                searchExpression.evaluate(currentTime);
        }

        while (!currentValue) {
            if (searchExpression.backwardHint == Long.MIN_VALUE) {
                return -1; // End of waveform
            }

            currentTime = searchExpression.backwardHint;
            currentValue =
                searchExpression.evaluate(currentTime);
        }

        return currentTime;
    }

    @Override
    public String toString() {
        return searchExpression.toString();
    }
}

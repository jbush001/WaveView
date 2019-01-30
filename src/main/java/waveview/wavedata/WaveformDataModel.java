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

package waveview.wavedata;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

///
/// Contains information about nets and transitions. View state is contained
/// in WaveformPresentationModel.
///

public final class WaveformDataModel implements Iterable<NetDataModel> {
    private long maxTimestamp;
    private Map<String, NetDataModel> fullNameToNetMap = new HashMap<>();
    private List<NetDataModel> nets = new ArrayList<>();
    private List<NetDataModel> decodedNets = new ArrayList<>();
    private NetTreeNode netTree;
    private int timescale;
    private int decodeIndex;

    public static class AmbiguousNetException extends Exception {
        public AmbiguousNetException(String what) {
            super(what);
        }
    }


    public NetTreeNode getNetTree() {
        return netTree;
    }

    /// A bit of a kludge. Used when loading a new model.
    public void copyFrom(WaveformDataModel from) {
        maxTimestamp = from.maxTimestamp;
        fullNameToNetMap = from.fullNameToNetMap;
        nets = from.nets;
        netTree = from.netTree;
        timescale = from.timescale;
        decodedNets = from.decodedNets;
    }

    public WaveformBuilder startBuilding() {
        nets.clear();
        fullNameToNetMap.clear();
        netTree = null;

        return new ConcreteWaveformBuilder();
    }

    public NetDataModel getNetDataModel(int netId) {
        return nets.get(netId);
    }

    public long getMaxTimestamp() {
        return maxTimestamp;
    }

    public int getTimescale() {
        return timescale;
    }

    public int getTotalNetCount() {
        return nets.size();
    }

    /// Look up net by full path
    public NetDataModel findNet(String name) {
        NetDataModel model = fullNameToNetMap.get(name);
        if (model != null) {
            return model;
        }

        for (NetDataModel decoded : decodedNets) {
            if (decoded.getFullName().equals(name)) {
                return decoded;
            }
        }

        return null;
    }

    // This does a fuzzy match to find the net. If there is ambiguity (two nets
    // with the same name that aren't aliases), it will throw a SearchFormatException.
    public NetDataModel fuzzyFindNet(String name)
            throws NoSuchElementException, AmbiguousNetException {
        NetDataModel match = null;
        for (NetDataModel netDataModel : nets) {
            if (isPartialNetNameMatch(netDataModel.getFullName(), name)) {
                if (match == null) {
                    match = netDataModel;
                } else if (match.getTransitionVector() != netDataModel.getTransitionVector()) {
                    throw new AmbiguousNetException("Ambiguous net \"" + name + "\"");
                }
            }
        }

        for (NetDataModel netDataModel : decodedNets) {
            if (isPartialNetNameMatch(netDataModel.getFullName(), name)) {
                if (match == null) {
                    match = netDataModel;
                } else if (match.getTransitionVector() != netDataModel.getTransitionVector()) {
                    throw new AmbiguousNetException("Ambiguous net \"" + name + "\"");
                }
            }
        }

        if (match == null) {
            throw new NoSuchElementException("Unknown net \"" + name + "\"");
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

            if (!haystack.substring(haystackSegmentBegin + 1, haystackSegmentEnd)
                     .equals(needle.substring(needleSegmentBegin + 1, needleSegmentEnd))) {
                // Subpaths don't match
                return false;
            }

            needleSegmentEnd = needleSegmentBegin;
            haystackSegmentEnd = haystackSegmentBegin;
        }

        return true;
    }

    @Override
    public Iterator<NetDataModel> iterator() {
        return nets.iterator();
    }

    public String generateDecodedName(String decoderName) {
        return decoderName + '_' + decodeIndex++;
    }

    public void addDecodedNet(NetDataModel model) {
        decodedNets.add(model);
    }

    private class ConcreteWaveformBuilder implements WaveformBuilder {
        private final Deque<String> scopeStack = new ArrayDeque<>();
        private final NetTreeNode.Builder treeBuilder = new NetTreeNode.Builder();

        // This mirrors nets in WaveformDataModel and must be kept in sync
        // with it.
        private final List<TransitionVector.Builder> transitionBuilders = new ArrayList<>();

        @Override
        public WaveformBuilder setTimescale(int timescale) {
            WaveformDataModel.this.timescale = timescale;
            return this;
        }

        @Override
        public WaveformBuilder enterScope(String name) {
            treeBuilder.enterScope(name);
            scopeStack.addLast(name);
            return this;
        }

        @Override
        public WaveformBuilder exitScope() {
            treeBuilder.leaveScope();
            scopeStack.removeLast();
            return this;
        }

        @Override
        public WaveformBuilder loadFinished() {
            maxTimestamp = 0;
            for (NetDataModel model : nets) {
                maxTimestamp = Math.max(maxTimestamp, model.getMaxTimestamp());
            }

            netTree = treeBuilder.getRoot();

            return this;
        }

        @Override
        public WaveformBuilder appendTransition(int id, long timestamp, BitVector values) {
            transitionBuilders.get(id).appendTransition(timestamp, values);
            return this;
        }

        @Override
        public WaveformBuilder newNet(int netId, String shortName, int width) {
            // Build full path
            StringBuilder fullName = new StringBuilder();
            for (String scope : scopeStack) {
                if (fullName.length() != 0) {
                    fullName.append('.');
                }

                fullName.append(scope);
            }

            fullName.append('.').append(shortName);

            NetDataModel net;
            TransitionVector.Builder builder;

            if (netId < transitionBuilders.size()) {
                // alias of existing net
                builder = transitionBuilders.get(netId);
            } else {
                // new net
                assert netId == transitionBuilders.size();
                builder = TransitionVector.Builder.createBuilder(width);
                transitionBuilders.add(builder);
            }

            net = new NetDataModel(shortName, fullName.toString(), builder.getTransitionVector());
            nets.add(net);
            treeBuilder.addNet(net);
            fullNameToNetMap.put(fullName.toString(), net);
            return this;
        }
    }
}

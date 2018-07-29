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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

///
/// Contains information about nets and transitions. View state is contained
/// in WaveformPresentationModel.
///

public class WaveformDataModel implements Iterable<NetDataModel> {
    private long maxTimestamp;
    private Map<String, NetDataModel> fullNameToNetMap = new HashMap<>();
    private List<NetDataModel> allNets = new ArrayList<>();
    private NetTreeModel netTree = new NetTreeModel();
    private int timescale;

    public NetTreeModel getNetTree() {
        return netTree;
    }

    /// A bit of a kludge. Used when loading a new model.
    public void copyFrom(WaveformDataModel from) {
        maxTimestamp = from.maxTimestamp;
        fullNameToNetMap = from.fullNameToNetMap;
        allNets = from.allNets;
        netTree = from.netTree;
        timescale = from.timescale;
    }

    public WaveformBuilder startBuilding() {
        allNets.clear();
        fullNameToNetMap.clear();
        netTree.clear();

        return new ConcreteWaveformBuilder();
    }

    public NetDataModel getNetDataModel(int netId) {
        return allNets.get(netId);
    }

    public long getMaxTimestamp() {
        return maxTimestamp;
    }

    public int getTimescale() {
        return timescale;
    }

    public NetDataModel getNetFromTreeObject(Object o) {
        return netTree.getNetFromTreeObject(o);
    }

    public int getTotalNetCount() {
        return allNets.size();
    }

    /// Look up net by full path
    public NetDataModel findNet(String name) {
        return fullNameToNetMap.get(name);
    }

    @Override
    public Iterator<NetDataModel> iterator() {
        return allNets.iterator();
    }

    private class ConcreteWaveformBuilder implements WaveformBuilder {
        private final Deque<String> scopeStack = new ArrayDeque<>();
        private NetTreeModel.Builder treeBuilder = netTree.startBuilding();

        // This mirrors allNets in WaveformDataModel and must be kept in sync with it.
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
            for (NetDataModel model : allNets) {
                maxTimestamp = Math.max(maxTimestamp, model.getMaxTimestamp());
            }

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

            fullName.append('.');
            fullName.append(shortName);

            NetDataModel net;
            TransitionVector.Builder builder;

            if (netId < transitionBuilders.size()) {
                // alias of existing net
                builder = transitionBuilders.get(netId);
            } else {
                // new net
                assert netId == transitionBuilders.size();
                builder = new TransitionVector.Builder(width);
                transitionBuilders.add(builder);
            }

            net = new NetDataModel(shortName, fullName.toString(), builder.getTransitionVector());
            allNets.add(net);
            treeBuilder.addNet(net);
            fullNameToNetMap.put(fullName.toString(), net);
            return this;
        }
    }
}

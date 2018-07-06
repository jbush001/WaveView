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

///
/// Contains information about nets and transitions. View state is contained
/// in TraceDisplayModel.
///

public class TraceDataModel implements Iterable<NetDataModel> {
    private long maxTimestamp;
    private HashMap<String, NetDataModel> fullNameToNetMap = new HashMap<>();
    private ArrayList<NetDataModel> allNets = new ArrayList<>();
    private NetTreeModel netTree = new NetTreeModel();
    private int timescale;

    public NetTreeModel getNetTree() {
        return netTree;
    }

    /// A bit of a kludge. Used when loading a new model.
    public void copyFrom(TraceDataModel from) {
        maxTimestamp = from.maxTimestamp;
        fullNameToNetMap = from.fullNameToNetMap;
        allNets = from.allNets;
        netTree = from.netTree;
        timescale = from.timescale;
    }

    public TraceBuilder startBuilding() {
        allNets.clear();
        fullNameToNetMap.clear();
        netTree.clear();

        return new ConcreteTraceBuilder();
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

    private class ConcreteTraceBuilder implements TraceBuilder {
        private final Deque<String> scopeStack = new ArrayDeque<>();
        private NetTreeModel.Builder treeBuilder = netTree.startBuilding();

        // This mirrors allNets in TraceDataModel and must be kept in sync with it.
        private final ArrayList<TransitionVector> transitionVectors = new ArrayList<>();

        @Override
        public void setTimescale(int timescale) {
            TraceDataModel.this.timescale = timescale;
        }

        @Override
        public void enterScope(String name) {
            treeBuilder.enterScope(name);
            scopeStack.addLast(name);
        }

        @Override
        public void exitScope() {
            treeBuilder.leaveScope();
            scopeStack.removeLast();
        }

        @Override
        public void loadFinished() {
            maxTimestamp = 0;
            for (NetDataModel model : allNets) {
                maxTimestamp = Math.max(maxTimestamp, model.getMaxTimestamp());
            }
        }

        @Override
        public void appendTransition(int id, long timestamp, BitVector values) {
            transitionVectors.get(id).appendTransition(timestamp, values);
        }

        @Override
        public int newNet(String shortName, int cloneId, int width) {
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
            TransitionVector transitionVector;
            if (cloneId == -1) {
                transitionVector = new TransitionVector(width);
                net = new NetDataModel(shortName, fullName.toString(), transitionVector);
            } else {
                transitionVector = transitionVectors.get(cloneId);
                net = new NetDataModel(shortName, fullName.toString(), allNets.get(cloneId));
            }

            allNets.add(net);
            transitionVectors.add(transitionVector);
            assert allNets.size() == transitionVectors.size();

            int thisNetIndex = allNets.size() - 1;
            treeBuilder.addNet(net);
            fullNameToNetMap.put(fullName.toString(), net);
            return thisNetIndex;
        }
    }
}

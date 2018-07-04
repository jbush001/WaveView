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

public class TraceDataModel {
    private long maxTimestamp;
    private HashMap<String, Integer> fullNameToNetMap = new HashMap<>();
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

    public Iterator<Transition> findTransition(int netId, long timestamp) {
        return allNets.get(netId).findTransition(timestamp);
    }

    public long getMaxTimestamp() {
        return maxTimestamp;
    }

    public int getTimescale() {
        return timescale;
    }

    public int getNetFromTreeObject(Object o) {
        return netTree.getNetFromTreeObject(o);
    }

    public int getTotalNetCount() {
        return allNets.size();
    }

    /// Look up net by full path
    public int findNet(String name) {
        Integer i = fullNameToNetMap.get(name);
        if (i == null)
            return -1;

        return i.intValue();
    }

    public int getNetWidth(int index) {
        return allNets.get(index).getWidth();
    }

    public String getShortNetName(int index) {
        return allNets.get(index).getShortName();
    }

    public String getFullNetName(int index) {
        return allNets.get(index).getFullName();
    }

    private static class NetDataModel {
        private final String shortName;
        private final String fullName;
        private final TransitionVector transitionVector;

        NetDataModel(String shortName, String fullName, int width) {
            this.shortName = shortName;
            this.fullName = fullName;
            transitionVector = new TransitionVector(width);
        }

        // This NetDataModel shares its transition data with another one.
        NetDataModel(String shortName, String fullName, NetDataModel cloneFrom) {
            this.shortName = shortName;
            this.fullName = fullName;
            this.transitionVector = cloneFrom.transitionVector;
        }

        String getFullName() {
            return fullName;
        }

        String getShortName() {
            return shortName;
        }

        Iterator<Transition> findTransition(long timestamp) {
            return transitionVector.findTransition(timestamp);
        }

        long getMaxTimestamp() {
            return transitionVector.getMaxTimestamp();
        }

        int getWidth() {
            return transitionVector.getWidth();
        }
    }

    private class ConcreteTraceBuilder implements TraceBuilder {
        private final Deque<String> scopeStack = new ArrayDeque<>();

        @Override
        public void setTimescale(int timescale) {
            TraceDataModel.this.timescale = timescale;
        }

        @Override
        public void enterScope(String name) {
            netTree.enterScope(name);
            scopeStack.addLast(name);
        }

        @Override
        public void exitScope() {
            netTree.leaveScope();
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
            NetDataModel model = allNets.get(id);
            model.transitionVector.appendTransition(timestamp, values);
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
            if (cloneId == -1)
                net = new NetDataModel(shortName, fullName.toString(), width);
            else
                net = new NetDataModel(shortName, fullName.toString(), allNets.get(cloneId));

            allNets.add(net);
            int thisNetIndex = allNets.size() - 1;
            netTree.addNet(shortName, thisNetIndex);
            fullNameToNetMap.put(fullName.toString(), thisNetIndex);
            return thisNetIndex;
        }
    }
}

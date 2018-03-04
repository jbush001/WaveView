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

import java.util.*;

///
/// Contains information about nets and transitions. View state is contained
/// in TraceDisplayModel.
///

public class TraceDataModel {
    public NetTreeModel getNetTree() {
        return fNetTree;
    }

    /// A bit of a kludge. Used when loading a new model.
    public void copyFrom(TraceDataModel from) {
        fMaxTimestamp = from.fMaxTimestamp;
        fFullNameToNetMap = from.fFullNameToNetMap;
        fAllNets = from.fAllNets;
        fNetTree = from.fNetTree;
        fTimescale = from.fTimescale;
    }

    public TraceBuilder startBuilding() {
        fAllNets.clear();
        fFullNameToNetMap.clear();
        fNetTree.clear();

        return new ConcreteTraceBuilder();
    }

    public Iterator<Transition> findTransition(int netId, long timestamp) {
        return fAllNets.get(netId).findTransition(timestamp);
    }

    public long getMaxTimestamp() {
        return fMaxTimestamp;
    }

    public int getTimescale() {
        return fTimescale;
    }

    public int getNetFromTreeObject(Object o) {
        return fNetTree.getNetFromTreeObject(o);
    }

    public int getTotalNetCount() {
        return fAllNets.size();
    }

    /// Look up net by full path
    public int findNet(String name) {
        Integer i = fFullNameToNetMap.get(name);
        if (i == null)
            return -1;

        return i.intValue();
    }

    public int getNetWidth(int index) {
        return fAllNets.get(index).getWidth();
    }

    public String getShortNetName(int index) {
        return fAllNets.get(index).getShortName();
    }

    public String getFullNetName(int index) {
        return fAllNets.get(index).getFullName();
    }

    private static class NetDataModel {
        NetDataModel(String shortName, String fullName, int width) {
            fShortName = shortName;
            fFullName = fullName;
            fTransitionVector = new TransitionVector(width);
        }

        // This NetDataModel shares its transition data with another one.
        NetDataModel(String shortName, String fullName, NetDataModel cloneFrom) {
            fShortName = shortName;
            fFullName = fullName;
            fTransitionVector = cloneFrom.fTransitionVector;
        }

        String getFullName() {
            return fFullName;
        }

        String getShortName() {
            return fShortName;
        }

        Iterator<Transition> findTransition(long timestamp) {
            return fTransitionVector.findTransition(timestamp);
        }

        long getMaxTimestamp() {
            return fTransitionVector.getMaxTimestamp();
        }

        int getWidth() {
            return fTransitionVector.getWidth();
        }

        private TransitionVector fTransitionVector;
        private String fShortName;
        private String fFullName;
    }

    private class ConcreteTraceBuilder implements TraceBuilder {
        @Override
        public void setTimescale(int order) {
            fTimescale = order;
        }

        @Override
        public void enterScope(String name) {
            fNetTree.enterScope(name);
            fScopeStack.addLast(name);
        }

        @Override
        public void exitScope() {
            fNetTree.leaveScope();
            fScopeStack.removeLast();
        }

        @Override
        public void loadFinished() {
            fMaxTimestamp = 0;
            for (NetDataModel model : fAllNets)
                fMaxTimestamp = Math.max(fMaxTimestamp, model.getMaxTimestamp());
        }

        @Override
        public void appendTransition(int id, long timestamp, BitVector values) {
            NetDataModel model = fAllNets.get(id);
            model.fTransitionVector.appendTransition(timestamp, values);
        }

        @Override
        public int newNet(String shortName, int cloneId, int width) {
            // Build full path
            StringBuilder fullName = new StringBuilder();
            for (String scope : fScopeStack) {
                if (fullName.length() != 0)
                    fullName.append('.');

                fullName.append(scope);
            }

            fullName.append('.');
            fullName.append(shortName);

            NetDataModel net;
            if (cloneId == -1)
                net = new NetDataModel(shortName, fullName.toString(), width);
            else
                net = new NetDataModel(shortName, fullName.toString(), fAllNets.get(cloneId));

            fAllNets.add(net);
            int thisNetIndex = fAllNets.size() - 1;
            fNetTree.addNet(shortName, thisNetIndex);
            fFullNameToNetMap.put(fullName.toString(), thisNetIndex);
            return thisNetIndex;
        }

        private Deque<String> fScopeStack = new ArrayDeque<String>();
    }

    private long fMaxTimestamp;
    private HashMap<String, Integer> fFullNameToNetMap = new HashMap<String, Integer>();
    private ArrayList<NetDataModel> fAllNets = new ArrayList<NetDataModel>();
    private NetTreeModel fNetTree = new NetTreeModel();
    private int fTimescale;
}

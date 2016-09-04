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

import java.util.*;

///
/// Contains information about nets and transitions. View state is contained
/// in TraceDisplayModel.
///

class TraceDataModel {
    NetTreeModel getNetTree() {
        return fNetTree;
    }

    /// A bit of a kludge. Used when loading a new model.
    void copyFrom(TraceDataModel from) {
        fMaxTimestamp = from.fMaxTimestamp;
        fFullNameToNetMap = from.fFullNameToNetMap;
        fAllNets = from.fAllNets;
        fNetTree = from.fNetTree;
    }

    TraceBuilder startBuilding() {
        fAllNets.clear();
        fFullNameToNetMap.clear();
        fNetTree.clear();

        return new ConcreteTraceBuilder();
    }

    Iterator<Transition> findTransition(int netId, long timestamp) {
        return fAllNets.get(netId).findTransition(timestamp);
    }

    long getMaxTimestamp() {
        return fMaxTimestamp;
    }

    int getNetFromTreeObject(Object o) {
        return fNetTree.getNetFromTreeObject(o);
    }

    int getTotalNetCount() {
        return fAllNets.size();
    }

    /// look up by fully qualified name
    int findNet(String name) {
        Integer i = fFullNameToNetMap.get(name);
        if (i == null)
            return -1;

        return i.intValue();
    }

    int getNetWidth(int index) {
        return fAllNets.get(index).getWidth();
    }

    String getShortNetName(int index) {
        return fAllNets.get(index).getShortName();
    }

    String getFullNetName(int index) {
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
            if (order < -9)
                System.out.println("unsupported timescale");    // @fixme

            fNanoSecondsPerUnit = (int) Math.pow(10, order + 9);
        }

        @Override
        public void enterScope(String name) {
            fNetTree.enterScope(name);
            fScopeStack.push(name);
        }

        @Override
        public void exitScope() {
            fNetTree.leaveScope();
            fScopeStack.pop();
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
            model.fTransitionVector.appendTransition(timestamp * fNanoSecondsPerUnit,
                values);
        }

        @Override
        public int newNet(String shortName, int cloneId, int width) {
            // Build full path
            StringBuffer fullName = new StringBuffer();
            for (String scope : fScopeStack) {
                if (fullName.length() != 0)
                    fullName.append('.');

                fullName.append(scope);
            }

            fullName.append('.');
            fullName.append(shortName);

            NetDataModel net;
            if (cloneId != -1)
                net = new NetDataModel(shortName, fullName.toString(), fAllNets.get(cloneId));
            else
                net = new NetDataModel(shortName, fullName.toString(), width);

            fAllNets.add(net);
            int thisNetIndex = fAllNets.size() - 1;
            fNetTree.addNet(shortName, thisNetIndex);
            fFullNameToNetMap.put(fullName.toString(), thisNetIndex);
            return thisNetIndex;
        }

        private Stack<String> fScopeStack = new Stack<String>();
        private int fNanoSecondsPerUnit;
    }

    private long fMaxTimestamp;
    private HashMap<String, Integer> fFullNameToNetMap = new HashMap<String, Integer>();
    private ArrayList<NetDataModel> fAllNets = new ArrayList<NetDataModel>();
    private NetTreeModel fNetTree = new NetTreeModel();
}

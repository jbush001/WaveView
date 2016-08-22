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
/// in TraceViewModel.
///

class TraceDataModel {
    public NetTreeModel getNetTree() {
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

    TransitionVector.Iterator findTransition(int netId, long timestamp) {
        return fAllNets.elementAt(netId).findTransition(timestamp);
    }

    public long getMaxTimestamp() {
        return fMaxTimestamp;
    }

    /// @bug Is this needed if we already have the NetTreeModel exposed?
    public int getNetFromTreeObject(Object o) {
        return fNetTree.getNetFromTreeObject(o);
    }

    public int getTotalNetCount() {
        return fAllNets.size();
    }

    /// look up by fully qualified name
    public int findNet(String name) {
        Integer i = fFullNameToNetMap.get(name);
        if (i == null)
            return -1;

        return i.intValue();
    }

    public int getNetWidth(int index) {
        return fAllNets.elementAt(index).getWidth();
    }

    public String getShortNetName(int index) {
        return fAllNets.elementAt(index).getShortName();
    }

    public String getFullNetName(int index) {
        return fAllNets.elementAt(index).getFullName();
    }

    private class NetDataModel {
        NetDataModel(String shortName, int width) {
            fShortName = shortName;
            fTransitionVector = new TransitionVector(width);
        }

        // This NetDataModel shares its transition data with another one.
        /// @todo clean this up by separating into another class
        NetDataModel(String shortName, NetDataModel cloneFrom) {
            fShortName = shortName;
            fTransitionVector = cloneFrom.fTransitionVector;
        }

        void setFullName(String name) {
            fFullName = name;
        }

        String getFullName() {
            return fFullName;
        }

        String getShortName() {
            return fShortName;
        }

        TransitionVector.Iterator findTransition(long timestamp) {
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
        public void enterModule(String name) {
            fNetTree.enterScope(name);
            fScopeStack.push(name);
        }

        @Override
        public void exitModule() {
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
            NetDataModel model = fAllNets.elementAt(id);
            model.fTransitionVector.appendTransition(timestamp, values);
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
                net = new NetDataModel(shortName, fAllNets.elementAt(cloneId));
            else
                net = new NetDataModel(shortName, width);

            net.setFullName(fullName.toString());
            fAllNets.addElement(net);
            int thisNetIndex = fAllNets.size() - 1;
            fNetTree.addNet(shortName, thisNetIndex);
            fFullNameToNetMap.put(fullName.toString(), thisNetIndex);
            return thisNetIndex;
        }

        private Stack<String> fScopeStack = new Stack<String>();
    }

    private long fMaxTimestamp;
    private HashMap<String, Integer> fFullNameToNetMap = new HashMap<String, Integer>();
    private Vector<NetDataModel> fAllNets = new Vector<NetDataModel>();
    private NetTreeModel fNetTree = new NetTreeModel();
}

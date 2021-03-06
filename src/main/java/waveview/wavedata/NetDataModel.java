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

import java.util.Iterator;

///
/// Transitions and information about a single net.
///
public final class NetDataModel {
    private final String shortName;
    private final String fullName;
    private TransitionVector transitionVector;
    private final String decoderName;
    private final String[] decoderInputNets;
    private String[] decoderParameters;

    public NetDataModel(String shortName, String fullName,
            TransitionVector transitionVector) {
        this.shortName = shortName;
        this.fullName = fullName;
        this.transitionVector = transitionVector;
        decoderName = "";
        decoderInputNets = new String[0];
    }

    public NetDataModel(String shortName, String fullName,
            String decoderName, String[] decoderInputNets,
            String[] decoderParameters,
            TransitionVector transitionVector) {
        this.shortName = shortName;
        this.fullName = fullName;
        this.transitionVector = transitionVector;
        this.decoderName = decoderName;
        this.decoderParameters = decoderParameters.clone();
        this.decoderInputNets = decoderInputNets.clone();
    }

    public String getFullName() {
        return fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public String getDecoderName() {
        return decoderName;
    }

    public String[] getDecoderInputNets() {
        return decoderInputNets.clone();
    }

    public String[] getDecoderParams() {
        return decoderParameters.clone();
    }

    public TransitionVector getTransitionVector() {
        return transitionVector;
    }

    public void setTransitionVector(TransitionVector transitionVector) {
        this.transitionVector = transitionVector;
    }

    public Iterator<Transition> findTransition(long timestamp) {
        return transitionVector.findTransition(timestamp);
    }

    public long getMaxTimestamp() {
        return transitionVector.getMaxTimestamp();
    }

    public int getWidth() {
        return transitionVector.getWidth();
    }
}

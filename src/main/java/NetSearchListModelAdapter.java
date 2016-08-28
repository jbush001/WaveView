//
// Copyright 2011-2016 Jeff Bush
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

import java.util.ArrayList;
import javax.swing.ListModel;
import javax.swing.text.Document;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;

///
/// This is the data model for the list of all nets. The user may type in
/// patterns that will refine the list to a smaller set of matches. It registers
/// itself as a listener on the text field where the user types a pattern.
///

class NetSearchListModelAdapter implements ListModel<String>, DocumentListener {
    public NetSearchListModelAdapter(TraceDataModel model) {
        fTraceDataModel = model;
        setPattern("");
    }

    /// @note this isn't a wildcard pattern, it's just a substring match.
    ///   Investigate java.util.regex.Pattern for more complex matches
    /// @param pattern Only items that contain this pattern in some part of
    ///    them will be displayed.
    public void setPattern(String pattern) {
        if (pattern.equals("")) {
            fMatches.clear();
            for (int index = 0; index < fTraceDataModel.getTotalNetCount(); index++)
                fMatches.add(fTraceDataModel.getFullNetName(index));
        } else {
            fMatches.clear();
            for (int index = 0; index < fTraceDataModel.getTotalNetCount(); index++) {
                String name = fTraceDataModel.getFullNetName(index);
                if (name.indexOf(pattern) != -1)
                    fMatches.add(name);
            }
        }

        if (fListener != null)
            fListener.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, fMatches.size()));
    }

    private void filter(Document doc) {
        try {
            setPattern(doc.getText(0, doc.getEndPosition().getOffset()).trim());
        } catch (Exception exc) {
            System.out.println("caught exception " + exc);
        }
    }

    /// This is called when the user changes the search field. Update contents.
    @Override
    public void insertUpdate(DocumentEvent event) {
        filter(event.getDocument());
    }

    /// This is called when the user changes the search field. Update contents.
    @Override
    public void removeUpdate(DocumentEvent event) {
        filter(event.getDocument());
    }

    @Override
    public void changedUpdate(DocumentEvent ev) {
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        assert fListener == null;

        fListener = l;
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        assert fListener != null;

        fListener = null;
    }

    @Override
    public String getElementAt(int index) {
        return fMatches.get(index);
    }

    @Override
    public int getSize() {
        return fMatches.size();
    }

    private ListDataListener fListener;
    private ArrayList<String> fMatches = new ArrayList<String>();
    private TraceDataModel fTraceDataModel;
}

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

package waveview;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.swing.ListModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.WaveformDataModel;

///
/// This is the data model for the list of all nets. The user may type in
/// patterns that will refine the list to a smaller set of matches. It registers
/// itself as a listener on the text field where the user types a pattern.
///
public final class NetSearchListModelAdapter implements ListModel<String>, DocumentListener {
    private final List<ListDataListener> listeners = new ArrayList<>();
    private final WaveformDataModel waveformDataModel;
    private List<String> matches = new ArrayList<>();

    public NetSearchListModelAdapter(WaveformDataModel waveformDataModel) {
        this.waveformDataModel = waveformDataModel;
        setPattern("");
    }

    /// @note this isn't a wildcard pattern, it's just a substring match.
    /// Investigate java.util.regex.Pattern for more complex matches
    /// @param pattern Only items that contain this pattern in some part of
    /// them will be displayed.
    public void setPattern(String pattern) {
        if (pattern.isEmpty()) {
            matches.clear();
            for (NetDataModel netDataModel : waveformDataModel) {
                matches.add(netDataModel.getFullName());
            }
        } else {
            matches = StreamSupport.stream(waveformDataModel.spliterator(), false)
                          .map(model -> model.getFullName())
                          .filter(name -> name.contains(pattern))
                          .collect(Collectors.toList());
        }

        ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED,
            0, matches.size());
        for (ListDataListener listener : listeners) {
            listener.contentsChanged(event);
        }
    }

    private void filter(Document doc) {
        try {
            setPattern(doc.getText(0, doc.getEndPosition().getOffset()).trim());
        } catch (BadLocationException exc) {
            // This shouldn't happen unless there is a logic bug.
            System.out.println("filter: bad location exception " + exc);
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
    public void changedUpdate(DocumentEvent ev) {}

    @Override
    public void addListDataListener(ListDataListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListDataListener(ListDataListener listener) {
        listeners.remove(listener);
    }

    @Override
    public String getElementAt(int index) {
        return matches.get(index);
    }

    @Override
    public int getSize() {
        return matches.size();
    }
}

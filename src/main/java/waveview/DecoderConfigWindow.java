//
// Copyright 2019 Jeff Bush
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

import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import waveview.wavedata.Decoder;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.TransitionVector;
import waveview.wavedata.WaveformDataModel;

final class DecoderConfigWindow extends JDialog {
    private final NetDataModel[] inputModels;
    private JTextField[] textFields;
    private JComboBox<String>[] comboBoxes;
    private Decoder decoder;
    private WaveformPresentationModel presentationModel;
    private WaveformDataModel dataModel;
    private int insertionIndex;

    DecoderConfigWindow(JFrame parent, Decoder decoder,
            WaveformPresentationModel presentationModel,
            WaveformDataModel dataModel,
            int[] selectedIndices) {
        super(parent, "Decoder Config", true);

        NetDataModel[] inputModels = new NetDataModel[selectedIndices.length];
        insertionIndex = 0;
        for (int i = 0; i < selectedIndices.length; i++) {
            inputModels[i] = presentationModel.getVisibleNet(
                selectedIndices[i]);
            if (selectedIndices[i] > insertionIndex) {
                insertionIndex = selectedIndices[i] + 1;
            }
        }

        Container contentPane = new Container();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        this.decoder = decoder;
        this.inputModels = inputModels;
        this.presentationModel = presentationModel;
        this.dataModel = dataModel;

        String[] parameterNames = decoder.getParamNames();
        textFields = new JTextField[parameterNames.length];
        for (int i = 0; i < parameterNames.length; i++) {
            Container container = new Container();
            container.setLayout(new FlowLayout());
            JLabel label = new JLabel(parameterNames[i]);
            container.add(label);
            textFields[i] = new JTextField(20);
            container.add(textFields[i]);
            contentPane.add(container);
        }

        String[] netNames = new String[inputModels.length];
        for (int i = 0; i < inputModels.length; i++) {
            netNames[i] = inputModels[i].getFullName();
        }

        String[] signalNames = decoder.getInputNames();
        comboBoxes = new JComboBox[signalNames.length];
        for (int i = 0; i < signalNames.length; i++) {
            Container container = new Container();
            container.setLayout(new FlowLayout());
            JLabel label = new JLabel(signalNames[i]);
            container.add(label);
            comboBoxes[i] = new JComboBox<>(netNames);
            container.add(comboBoxes[i]);
            contentPane.add(container);
        }

        Container okCancelContainer = new Container();
        okCancelContainer.setLayout(new FlowLayout());
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> ok());
        okCancelContainer.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> cancel());
        okCancelContainer.add(cancelButton);
        contentPane.add(okCancelContainer);

        setContentPane(contentPane);
        pack();
    }

    void ok() {
        try {
            String[] inputNames = new String[comboBoxes.length];
            for (int i = 0; i < comboBoxes.length; i++) {
                inputNames[i] = inputModels[comboBoxes[i].getSelectedIndex()].getFullName();
                decoder.setInput(i, inputModels[comboBoxes[i].getSelectedIndex()]);
            }

            String[] paramVals = new String[textFields.length];
            for (int i = 0; i < textFields.length; i++) {
                paramVals[i] = textFields[i].getText();
                decoder.setParam(i, paramVals[i]);
            }

            TransitionVector transitionVector = decoder.decode();
            String shortName = dataModel.generateDecodedName(decoder.getName());
            String fullName = "decoded." + shortName;
            NetDataModel model = new NetDataModel(shortName, fullName,
                decoder.getName(), inputNames, paramVals, transitionVector);
            dataModel.addDecodedNet(model);
            presentationModel.addNet(insertionIndex, model);
            dispose();
        } catch (IllegalArgumentException exc) {
            JOptionPane.showMessageDialog(this, exc.getMessage());
        }
    }

    void cancel() {
        dispose();
    }
}

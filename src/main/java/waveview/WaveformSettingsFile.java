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

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import waveview.wavedata.NetDataModel;
import waveview.wavedata.WaveformDataModel;

///
/// Load/Save WaveformPresentationModel state associated with a waveform file.
/// During the course of debugging, the user will often regenerate the trace
/// by rerunning the simulation after this file is initially generated.
/// This may invalidate some of the information in the settings file, for
/// example, if nets are added or removed or the simulation runs for a different
/// number of cycles. In this case, this should silently ignore those values.
/// @bug If markers are past the end offset, this should probably drop them.
///
public final class WaveformSettingsFile {
    private final File settingsFile;
    private final WaveformDataModel waveformDataModel;
    private final WaveformPresentationModel waveformPresentationModel;

    public static class SettingsFileException extends IOException {
        SettingsFileException(String description) {
            super(description);
        }
    }

    /// @param file Name of a waveform file
    /// @returns configuration file for this (a .dotfile in the same
    /// directory)
    public static File settingsFileName(File waveformFile) throws IOException {
        String parent = waveformFile.getParent();
        String path;
        if (parent == null) {
            path = "." + waveformFile.getName() + ".waveconfig";
        } else {
            path = waveformFile.getParent() + "/." + waveformFile.getName() + ".waveconfig";
        }

        return new File(path);
    }

    public WaveformSettingsFile(File settingsFile, WaveformDataModel waveformDataModel,
        WaveformPresentationModel waveformPresentationModel) {
        this.settingsFile = settingsFile;
        this.waveformDataModel = waveformDataModel;
        this.waveformPresentationModel = waveformPresentationModel;
    }

    public void write() throws IOException {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.newDocument();

            Element configuration = document.createElement("configuration");
            document.appendChild(configuration);

            Element scale = document.createElement("scale");
            configuration.appendChild(scale);
            scale.appendChild(document.createTextNode(
                Double.toString(waveformPresentationModel.getHorizontalScale())));

            Element netSetsElement = document.createElement("netsets");
            configuration.appendChild(netSetsElement);

            // Set 0 is the currently visible set of nets
            Element netSetElement = makeVisibleNetList(document);
            netSetElement.setAttribute("name", "_default");
            netSetsElement.appendChild(netSetElement);

            // Write out all of our saved net sets
            for (int i = 0; i < waveformPresentationModel.getNetSetCount(); i++) {
                waveformPresentationModel.selectNetSet(i);
                netSetElement = makeVisibleNetList(document);
                netSetElement.setAttribute("name", waveformPresentationModel.getNetSetName(i));
                netSetsElement.appendChild(netSetElement);
            }

            Element markers = document.createElement("markers");
            configuration.appendChild(markers);

            for (int i = 0; i < waveformPresentationModel.getMarkerCount(); i++) {
                Element markerElement = document.createElement("marker");
                markers.appendChild(markerElement);

                Element id = document.createElement("id");
                markerElement.appendChild(id);
                id.appendChild(document.createTextNode(Integer.toString(i)));

                Element timestamp = document.createElement("timestamp");
                markerElement.appendChild(timestamp);
                timestamp.appendChild(document.createTextNode(
                    Long.toString(waveformPresentationModel.getTimestampForMarker(i))));

                Element description = document.createElement("description");
                markerElement.appendChild(description);
                description.appendChild(
                    document.createTextNode(waveformPresentationModel.getDescriptionForMarker(i)));
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(settingsFile);
            transformer.transform(source, result);
        } catch (ParserConfigurationException | TransformerException exc) {
            throw new SettingsFileException(exc.getMessage());
        }
    }

    private Element makeVisibleNetList(Document document) {
        Element netSetElement = document.createElement("netset");

        for (int i = 0; i < waveformPresentationModel.getVisibleNetCount(); i++) {
            NetDataModel netDataModel = waveformPresentationModel.getVisibleNet(i);

            Element sigElement = document.createElement("net");
            netSetElement.appendChild(sigElement);

            Element name = document.createElement("name");
            sigElement.appendChild(name);

            Text nameText = document.createTextNode(netDataModel.getFullName());
            name.appendChild(nameText);

            String decoderName = netDataModel.getDecoderName();
            if (!decoderName.equals("")) {
                Element decoderNameElem = document.createElement("decoder");
                sigElement.appendChild(decoderNameElem);
                Text classNameText = document.createTextNode(decoderName);
                decoderNameElem.appendChild(classNameText);
                Element inputs = document.createElement("inputs");
                for (String net : netDataModel.getDecoderInputNets()) {
                    Element input = document.createElement("input");
                    input.setAttribute("name", net);
                    inputs.appendChild(input);
                }

                sigElement.appendChild(inputs);

                Element params = document.createElement("params");
                for (String paramVal : netDataModel.getDecoderParams()) {
                    Element netElement = document.createElement("param");
                    netElement.setAttribute("value", paramVal);
                    params.appendChild(netElement);
                }

                sigElement.appendChild(params);
            }

            Element format = document.createElement("format");
            sigElement.appendChild(format);

            ValueFormatter formatter = waveformPresentationModel.getValueFormatter(i);
            Class<? extends ValueFormatter> c = formatter.getClass();
            format.appendChild(document.createTextNode(c.getName()));

            if (formatter instanceof EnumValueFormatter) {
                EnumValueFormatter ivf = (EnumValueFormatter) formatter;
                try {
                    Text path = document.createTextNode(ivf.getFile().getCanonicalPath());
                    Element pathElement = document.createElement("path");
                    pathElement.appendChild(path);
                    format.appendChild(pathElement);
                } catch (IOException exc) {
                    System.out.println("Failed to save formatter " + exc);
                }
            }
        }

        return netSetElement;
    }

    private String getSubTag(Element parent, String tagName) {
        return ((Text) parent.getElementsByTagName(tagName).item(0).getFirstChild()).getData();
    }

    private void readNetSet(Element element) {
        waveformPresentationModel.removeAllNets();

        NodeList netElements = element.getElementsByTagName("net");
        for (int i = 0; i < netElements.getLength(); i++) {
            // Get the name
            Element netElem = (Element) netElements.item(i);
            String fullName = getSubTag(netElem, "name");

            ValueFormatter formatter = null;
            Element formatTag = (Element) netElem.getElementsByTagName("format").item(0);
            String formatStr = ((Text) formatTag.getFirstChild()).getData();

            //
            // My original idea was to allow creating custom formatters by
            // creating dynamically loadable classes. Not sure if this is still
            // a good idea.
            //
            try {
                Class<?> c = Class.forName(formatStr);
                if (formatStr.equals(EnumValueFormatter.class.getName())) {
                    String pathStr =((Text) formatTag.getElementsByTagName("path")
                        .item(0).getFirstChild()).getData();
                    formatter = new EnumValueFormatter(new File(pathStr));
                } else {
                    formatter = (ValueFormatter) c.getConstructor().newInstance();
                }
            } catch (LinkageError | ReflectiveOperationException | IOException exc) {
                System.out.println(
                    "unable to instantiate value formatter " + formatStr + ": " + exc);
                formatter = new BinaryValueFormatter();
            }

            NetDataModel netDataModel;
            NodeList decoder = netElem.getElementsByTagName("decoder");
            if (decoder.getLength() == 0) {
                // Normal waveform
                netDataModel = waveformDataModel.findNet(fullName);
            } else {
                // Decoded waveform.
                String decoderName = ((Text) decoder.item(0).getFirstChild())
                    .getData();
                Element inputSignalTag = (Element) netElem
                    .getElementsByTagName("inputs").item(0);
                NodeList inputList = inputSignalTag.getElementsByTagName("input");
                String[] inputs = new String[inputList.getLength()];
                for (int j = 0; j < inputList.getLength(); j++) {
                    inputs[j] = ((Element) inputList.item(j)).getAttribute("name");
                }

                Element paramsTag = (Element) netElem
                    .getElementsByTagName("params").item(0);
                NodeList paramList = paramsTag.getElementsByTagName("param");
                String[] params = new String[paramList.getLength()];
                for (int j = 0; j < paramList.getLength(); j++) {
                    params[j] = ((Element) paramList.item(j)).getAttribute("value");
                }

                // Shorten name
                int dot = fullName.lastIndexOf('.');
                String shortName;
                if (dot == -1) {
                    shortName = fullName;
                } else {
                    shortName = fullName.substring(dot + 1);
                }

                netDataModel = new NetDataModel(shortName, fullName, decoderName, inputs,
                    params, null);
                waveformDataModel.addDecodedNet(netDataModel);
            }

            if (netDataModel == null) {
                System.out.println("unknown net " + fullName);
            } else {
                waveformPresentationModel.addNet(netDataModel);
                waveformPresentationModel.setValueFormatter(
                    waveformPresentationModel.getVisibleNetCount() - 1, formatter);
            }
        }
    }

    public void read() throws IOException {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(settingsFile);

            waveformPresentationModel.setHorizontalScale(
                Double.parseDouble(getSubTag(document.getDocumentElement(), "scale")));

            NodeList netSets = document.getElementsByTagName("netset");
            for (int i = 1; i < netSets.getLength(); i++) {
                Element netSet = (Element) netSets.item(i);
                readNetSet(netSet);
                waveformPresentationModel.saveNetSet(netSet.getAttribute("name"));
            }

            // Index zero represents the current state (which isn't saved as a
            // named net set)
            if (netSets.getLength() > 0) {
                readNetSet((Element) netSets.item(0));
            }

            NodeList markers = document.getElementsByTagName("marker");
            for (int j = 0; j < markers.getLength(); j++) {
                Element markerElem = (Element) markers.item(j);

                /// @bug we ignore the ID and assume these are in order
                /// This will renumber markers if there are gaps. Is that okay?
                // Integer.parseInt(getSubTag(markerElem, "id"));
                waveformPresentationModel.addMarker(getSubTag(markerElem, "description"),
                    Long.parseLong(getSubTag(markerElem, "timestamp")));
            }
        } catch (ParserConfigurationException | SAXException exc) {
            throw new SettingsFileException(exc.getMessage());
        }
    }
}

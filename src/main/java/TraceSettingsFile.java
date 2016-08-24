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

import javax.xml.parsers.*;
import org.w3c.dom.*;
import javax.xml.transform.*;
import java.util.*;
import java.io.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

///
/// Load/Save view settings specific to a loaded trace
///

class TraceSettingsFile {
    public TraceSettingsFile(File file, TraceDataModel dataModel,
                             TraceViewModel viewModel) {  /// @bug make order of view and data model consistent between methods
        fFile = file;
        fDataModel = dataModel;
        fViewModel = viewModel;
    }

    public void write() throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.newDocument();

        Element configuration = document.createElement("configuration");
        document.appendChild(configuration);

        Element scale = document.createElement("scale");
        configuration.appendChild(scale);
        scale.appendChild(document.createTextNode(Double.toString(fViewModel.getHorizontalScale())));

        Element netSetsElement = document.createElement("netsets");
        configuration.appendChild(netSetsElement);

        // Set 0 is the currently visible set of nets
        Element netSetElement = makeVisibleNetList(document);
        netSetElement.setAttribute("name", "_default");
        netSetsElement.appendChild(netSetElement);

        // Write out all of our saved net sets
        for (int i = 0; i < fViewModel.getNetSetCount(); i++) {
            fViewModel.selectNetSet(i);
            netSetElement = makeVisibleNetList(document);
            netSetElement.setAttribute("name", fViewModel.getNetSetName(i));
            netSetsElement.appendChild(netSetElement);
        }

        Element markers = document.createElement("markers");
        configuration.appendChild(markers);

        for (int i = 0; i < fViewModel.getMarkerCount(); i++) {
            Element markerElement = document.createElement("marker");
            markers.appendChild(markerElement);

            Element id = document.createElement("id");
            markerElement.appendChild(id);
            id.appendChild(document.createTextNode(Integer.toString(i)));

            Element timestamp = document.createElement("timestamp");
            markerElement.appendChild(timestamp);
            timestamp.appendChild(document.createTextNode(Long.toString(fViewModel.getTimestampForMarker(i))));

            Element description = document.createElement("description");
            markerElement.appendChild(description);
            description.appendChild(document.createTextNode(fViewModel.getDescriptionForMarker(i)));
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(fFile);
        transformer.transform(source, result);
    }

    public Element makeVisibleNetList(Document document) {
        Element netSetElement = document.createElement("netset");

        for (int i = 0; i < fViewModel.getVisibleNetCount(); i++) {
            int netId = fViewModel.getVisibleNet(i);

            Element sigElement = document.createElement("net");
            netSetElement.appendChild(sigElement);

            Element name = document.createElement("name");
            sigElement.appendChild(name);

            Text nameText = document.createTextNode(fDataModel.getFullNetName(netId));
            name.appendChild(nameText);

            Element format = document.createElement("format");
            sigElement.appendChild(format);

            ValueFormatter formatter = fViewModel.getValueFormatter(i);
            if (formatter instanceof EnumValueFormatter) {
                EnumValueFormatter ivf = (EnumValueFormatter) formatter;
                for (int j = 0; j < ivf.getMappingCount(); j++) {
                    Text idNode = document.createTextNode("" + ivf.getValueByIndex(j));
                    Text valueNode = document.createTextNode(ivf.getNameByIndex(j));

                    Element mappingElement = document.createElement("mapping");
                    format.appendChild(mappingElement);

                    Element idElement = document.createElement("id");
                    mappingElement.appendChild(idElement);
                    idElement.appendChild(idNode);

                    Element textElement = document.createElement("text");
                    mappingElement.appendChild(textElement);
                    textElement.appendChild(valueNode);
                }
            } else {
                Class c = formatter.getClass();
                format.appendChild(document.createTextNode(c.getName()));
            }
        }

        return netSetElement;
    }

    private String getSubTag(Element parent, String tagName) {
        Text elem = (Text) parent.getElementsByTagName(tagName).item(0).getFirstChild();
        if (elem == null)
            return "";

        return elem.getData();
    }

    public void readNetSet(Element element) throws ClassNotFoundException {
        fViewModel.removeAllNets();

        NodeList netElements = element.getElementsByTagName("net");
        for (int i = 0; i < netElements.getLength(); i++) {
            // Get the name
            Element netElem = (Element) netElements.item(i);
            String name = getSubTag(netElem, "name");

            ValueFormatter formatter = null;
            NodeList mappings = netElem.getElementsByTagName("mapping");
            if (mappings.getLength() > 0) {
                formatter = new EnumValueFormatter();
                for (int j = 0; j < mappings.getLength(); j++) {
                    Element mappingElem = (Element) mappings.item(j);
                    String id = getSubTag(mappingElem, "id");
                    String text = getSubTag(mappingElem, "text");
                    ((EnumValueFormatter)formatter).addMapping(Integer.parseInt(id), text);
                }
            } else {
                String format = getSubTag(netElem, "format");
                Class c = Class.forName(format);
                if (c != null) {
                    try {
                        formatter = (ValueFormatter) c.newInstance();
                    } catch (Exception exc) {
                        System.out.println(exc);
                        formatter = new BinaryValueFormatter();
                    }
                } else {
                    System.out.println("unable to find class" + format);
                    formatter = new BinaryValueFormatter();
                }
            }

            int netId = fDataModel.findNet(name);
            if (netId < 0)
                System.out.println("unknown net " + name);
            else {
                fViewModel.makeNetVisible(netId);
                fViewModel.setValueFormatter(fViewModel.getVisibleNetCount() - 1, formatter);
            }
        }
    }

    public void read() throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(fFile);

        fViewModel.setHorizontalScale(Double.parseDouble(getSubTag(document.getDocumentElement(), "scale")));

        // read NetSet
        NodeList netSets = document.getElementsByTagName("netset");

        // Read saved net sets
        for (int i = 1; i < netSets.getLength(); i++) {
            Element netSet = (Element) netSets.item(i);
            readNetSet(netSet);
            fViewModel.saveNetSet(netSet.getAttribute("name"));
        }

        // Default net set
        if (netSets.getLength() > 0)
            readNetSet((Element) netSets.item(0));

        NodeList markers = document.getElementsByTagName("marker");
        for (int j = 0; j < markers.getLength(); j++) {
            Element markerElem = (Element) markers.item(j);

            // XXX note, we ignore the ID and assume these are already in order
            // Integer.parseInt(getSubTag(markerElem, "id"));
            fViewModel.addMarker(getSubTag(markerElem, "description"),
                                 Long.parseLong(getSubTag(markerElem, "timestamp")));
        }
    }

    private File fFile;
    private TraceDataModel fDataModel;
    private TraceViewModel fViewModel;
}

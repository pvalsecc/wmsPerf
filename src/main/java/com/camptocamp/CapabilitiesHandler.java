package com.camptocamp;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Very crude GetCapabilities parser.
 */
public class CapabilitiesHandler extends DefaultHandler {
    private final Map<String, Layer> layers = new HashMap<String, Layer>();
    private final List<Layer> layerStack = new ArrayList<Layer>();
    private final List<String> elementStack = new ArrayList<String>();
    private final String wmsBase;

    public CapabilitiesHandler(String wmsBase) {
        this.wmsBase = wmsBase;
    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals("Layer")) {
            layerStack.add(0, new Layer(layerStack.isEmpty(), wmsBase));
        } else if (qName.equals("BoundingBox")) {
            layerStack.get(0).setBBox(attributes);
        }
        elementStack.add(0, qName);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals("Layer")) {
            layers.put(layerStack.get(0).getName(), layerStack.get(0));
            layerStack.remove(0);
        }
        assert(elementStack.get(0).equals(qName));
        elementStack.remove(0);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (elementStack.isEmpty() || layerStack.isEmpty()) {
            return;
        }
        final String curElement = elementStack.get(0);
        final Layer curLayer = layerStack.get(0);
        if (curElement.equals("Name") && elementStack.get(1).equals("Layer")) {
            curLayer.setName(new String(ch, start, length));
        } else if (curElement.equals("CRS")) {
            curLayer.setCrs(new String(ch, start, length));
        }
    }

    public Map<String, Layer> getLayers() {
        return layers;
    }
}

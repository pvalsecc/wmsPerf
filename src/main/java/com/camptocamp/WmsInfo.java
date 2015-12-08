package com.camptocamp;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WmsInfo {
    private final Map<String, Layer> layers;
    private final String wmsBase;

    public WmsInfo(String wmsBase, boolean verbose) throws Exception {
        this.wmsBase = wmsBase;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            URIBuilder builder = new URIBuilder(wmsBase);
            builder.setParameter("SERVICE", "WMS");
            builder.setParameter("VERSION", "1.3.0");
            builder.setParameter("REQUEST", "GetCapabilities");
            final URI uri = builder.build();
            if (verbose) {
                System.out.println(String.format("Getting capabilities: %s", uri));
            }
            HttpGet get = new HttpGet(uri);
            CloseableHttpResponse response = httpClient.execute(get);
            try {
                checkStatus(response.getStatusLine(), uri);
                layers = parseCapabilities(response.getEntity().getContent());
            } finally {
                response.close();
            }
        } finally {
            httpClient.close();
        }
    }

    private Map<String, Layer> parseCapabilities(InputStream content) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(false);
        SAXParser parse = spf.newSAXParser();
        XMLReader reader = parse.getXMLReader();
        final CapabilitiesHandler handler = new CapabilitiesHandler(wmsBase);
        reader.setContentHandler(handler);
        reader.parse(new InputSource(content));
        return handler.getLayers();
    }

    public static void checkStatus(StatusLine status, URI uri) {
        if (status.getStatusCode() != 200) {
            throw new RuntimeException(String.format("Error while reading %s: %s", uri, status));
        }
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("WmsInfo(\n");
        for (Layer layer: layers.values()) {
            out.append("    ");
            out.append(layer.toString());
            out.append("\n");
        }
        out.append(")");
        return out.toString();
    }

    public Layer getLayer(String layer) {
        return layers.get(layer);
    }

    public List<String> getLayers() {
        List<String> ret = new ArrayList<String>(layers.size());
        for (Layer layer: layers.values()) {
            if (!layer.isRoot()) {
                ret.add(layer.getName());
            }
        }
        Collections.sort(ret);
        return ret;
    }
}

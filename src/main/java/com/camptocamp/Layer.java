package com.camptocamp;

import org.apache.http.client.utils.URIBuilder;
import org.xml.sax.Attributes;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Layer {
    private final boolean root;
    private final String wmsBase;
    private String name;
    private double minX;
    private double minY;
    private double maxX;
    private double maxY;
    private String crs;

    public Layer(boolean root, String wmsBase) {
        this.root = root;
        this.wmsBase = wmsBase;
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setBBox(Attributes attrs) {
        minX = Double.parseDouble(attrs.getValue("minx"));
        minY = Double.parseDouble(attrs.getValue("miny"));
        maxX = Double.parseDouble(attrs.getValue("maxx"));
        maxY = Double.parseDouble(attrs.getValue("maxy"));
    }

    @Override
    public String toString() {
        return String.format("%sLayer(%s, %s, %f, %f, %f, %f)", root ? "Root" : "", name, crs, minX, minY, maxX, maxY);
    }

    public URI getRandomMapUrl(Double zoomLevel) throws URISyntaxException {
        final double minXz;
        final double minYz;
        final double maxXz;
        final double maxYz;
        if(zoomLevel == 1.0) {
            minXz = minX;
            minYz = minY;
            maxXz = maxX;
            maxYz = maxY;
        } else {

            final double widthZ = (maxX - minX) / zoomLevel;
            final double heightZ = (maxY - minY) / zoomLevel;
            final Random random = ThreadLocalRandom.current();
            minXz = randomRange(random, minX, maxX - widthZ);
            minYz = randomRange(random, minY, maxY - heightZ);
            maxXz = minXz + widthZ;
            maxYz = minYz + heightZ;
        }

        URIBuilder builder = new URIBuilder(wmsBase);
        builder.setParameter("SERVICE", "WMS");
        builder.setParameter("VERSION", "1.3.0");
        builder.setParameter("REQUEST", "GetMap");
        builder.setParameter("BBOX", String.format("%f,%f,%f,%f", minXz, minYz, maxXz, maxYz));
        builder.setParameter("CRS", crs);
        builder.setParameter("WIDTH", "1000");
        builder.setParameter("HEIGHT", "1000");
        builder.setParameter("LAYERS", name);
        builder.setParameter("FORMAT", "image/jpeg");
        return builder.build();
    }

    private double randomRange(Random random, double min, double max) {
        return min + (max - min) * random.nextDouble();
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    public boolean isRoot() {
        return root;
    }
}

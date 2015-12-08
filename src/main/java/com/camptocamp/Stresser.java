package com.camptocamp;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class Stresser extends Thread {
    private final double zoomLevel;
    private final int nbIterations;
    private final Layer layer;
    private final boolean verbose;
    private final List<Long> times;
    private final List<Long> sizes;

    public Stresser(double zoomLevel, int nbIterations, Layer layer, boolean verbose) {
        this.zoomLevel = zoomLevel;
        this.nbIterations = nbIterations;
        this.layer = layer;
        this.verbose = verbose;
        this.times = new ArrayList<Long>(nbIterations);
        this.sizes = new ArrayList<Long>(nbIterations);
        start();
    }

    @Override
    public void run() {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            for (int i = 0; i < nbIterations; ++i) {
                try {
                    final URI url = layer.getRandomMapUrl(zoomLevel);
                    final long startTime = System.nanoTime();
                    final long length = getUrl(httpClient, url);
                    final long time = System.nanoTime() - startTime;
                    sizes.add(length);
                    times.add(time);
                    if (verbose) {
                        System.out.println(String.format("%s: %dbytes %dms", url, length, time / 1000000));
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private long getUrl(CloseableHttpClient httpClient, URI url) throws IOException {
        HttpGet get = new HttpGet(url);
        CloseableHttpResponse response = httpClient.execute(get);
        try {
            WmsInfo.checkStatus(response.getStatusLine(), url);
            InputStream content = response.getEntity().getContent();
            byte[] buffer = new byte[1024];
            long length = 0;
            while (true) {
                int curLength = content.read(buffer);
                if (curLength < 0) {
                    break;
                }
                length += curLength;
            }
            return length;
        } finally {
            response.close();
        }
    }

    public void getStats(List<Long> times, List<Long> sizes) {
        times.addAll(this.times);
        sizes.addAll(this.sizes);
    }
}

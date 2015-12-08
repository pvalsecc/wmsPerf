package com.camptocamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        String wmsBase = null;
        List<Double> zoomLevels = new ArrayList<Double>();
        int nbIterations = 20;
        int nbThreads = 1;
        List<String> layers = new ArrayList<String>();
        boolean verbose = false;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-w")) {
                wmsBase = args[++i];
            } else if (args[i].equals("-z")) {
                zoomLevels.add(Double.valueOf(args[++i]));
            } else if (args[i].equals("-n")) {
                nbIterations = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-t")) {
                nbThreads = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-l")) {
                layers.add(args[++i]);
            } else if (args[i].equals("-d")) {
                verbose = true;
            } else if (args[i].equals("-h")) {
                help();
                System.exit(0);
            } else {
                throw new RuntimeException("Unknown argument: " + args[i]);
            }
        }

        if (wmsBase == null) {
            help();
            throw new RuntimeException("Missing -w (WMS base URL)");
        }

        if (zoomLevels.isEmpty()) {
            help();
            throw new RuntimeException("Missing -z (zoom levels)");
        }

        run(wmsBase, zoomLevels, nbIterations, nbThreads, layers, verbose);
    }

    private static void help() {
        System.out.println("Usage:");
        System.out.println("  java -jar wmsLoad-all-1.0.jar -w wmsBaseUrl [{-l layerName}] {-z zoomLevel} [-n nbIteration] [-t nbThreads] [-d]");
    }

    private static void run(String wmsBase, List<Double> zoomLevels, int nbIterations, int nbThreads, List<String> layers, boolean verbose) throws Exception {
        WmsInfo wmsInfo = new WmsInfo(wmsBase, verbose);
        if (layers.isEmpty()) {
            layers = wmsInfo.getLayers();
        }
        if (verbose) {
            System.out.println(wmsInfo.toString());
        }
        final Map<String, Map<Double, Stats>> summary = new HashMap<String, Map<Double, Stats>>();
        for (String layer : layers) {
            final Map<Double, Stats> zoomSummary = new HashMap<Double, Stats>();
            summary.put(layer, zoomSummary);
            final List<Long> layerTimes = new ArrayList<Long>(nbIterations * nbThreads);
            final List<Long> layerSizes = new ArrayList<Long>(nbIterations * nbThreads);
            for (Double zoomLevel : zoomLevels) {
                List<Stresser> threads = new ArrayList<Stresser>(nbThreads);
                for (int i = 0; i < nbThreads; ++i) {
                    threads.add(new Stresser(zoomLevel, nbIterations, wmsInfo.getLayer(layer), verbose));
                }
                for (Stresser thread : threads) {
                    thread.join();
                }
                final List<Long> times = new ArrayList<Long>(nbIterations * nbThreads);
                final List<Long> sizes = new ArrayList<Long>(nbIterations * nbThreads);
                for (Stresser thread : threads) {
                    thread.getStats(times, sizes);
                    thread.getStats(layerTimes, layerSizes);
                }
                Stats timeStats = printStats(String.format("%s/%2fx", layer, zoomLevel), times, sizes, verbose);
                zoomSummary.put(zoomLevel, timeStats);
            }
            printStats(layer, layerTimes, layerSizes, verbose);
        }

        System.out.println("\n\n");
        printSummary(summary, nbThreads, nbIterations, zoomLevels, layers);
    }

    private static void printSummary(Map<String, Map<Double, Stats>> summary, int nbThreads, int nbIterations, List<Double> zoomLevels, List<String> layers) {
        System.out.println(String.format("nbThreads=%d nbIterations=%d", nbThreads, nbIterations));
        System.out.print(String.format("%20s  ", ""));
        for (Double zoom : zoomLevels) {
            System.out.print(String.format("%6.2f       ", zoom));
        }
        System.out.println();

        for (String layer : layers) {
            System.out.print(String.format("%20s  ", layer));
            final Map<Double, Stats> layerStats = summary.get(layer);
            for (Double level : zoomLevels) {
                Stats stat = layerStats.get(level);
                System.out.print(String.format("%6d±%4d  ", stat.getAverage(), stat.getStdDev()));
            }
            System.out.println();
        }
    }

    private static Stats printStats(String description, List<Long> times, List<Long> sizes, boolean verbose) {
        Stats timeStats = new Stats("ms", times, 1000000);
        if (verbose) {
            Stats sizeStats = new Stats("KB", sizes, 1024);
            System.out.println(String.format("%s: %s    %s", description, timeStats, sizeStats));
        }
        return timeStats;
    }

}

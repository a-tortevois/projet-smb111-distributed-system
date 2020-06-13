package fr.tortevois._bootloader;

import fr.tortevois.zigbee.ZigBeeException;

import java.util.TreeMap;

import static fr.tortevois.zigbee.ZigBee.*;

public class ZigBeeTestBootloader {

    private static boolean DEBUG = true;
    private static TreeMap<Integer, int[]> tree;

    public static void main(String[] args) {
        if (DEBUG) {
            execOneTest(4, 2, 3);
        } else {
            execUnitTest();
        }
    }

    public static void execOneTest(int devices, int gateways, int depth) {
        buildTheTree(devices, gateways, depth);
        if (DEBUG) drawTheTree(0, devices, gateways, depth);
        browseTheTree(0, devices, gateways, depth);
        if (tree.size() == 0) {
            System.out.print("  => Successful !");
        } else {
            System.out.print("  => Failed !");
        }
        System.out.println();
    }

    public static void execUnitTest() {
        for (int depth = 1; depth < 9; depth++) {
            for (int gateways = 1; gateways < 8; gateways++) {
                for (int devices = 1; devices < 11; devices++) {
                    execOneTest(devices, gateways, depth);
                }
            }
        }
    }

    public static void buildTheTree(int devices, int gateways, int depth) {
        tree = new TreeMap<>();
        int addressCount = getAddressCount(devices, gateways, depth);

        System.out.print("buildTheTree with Params - Devices: " + devices + " / Gateways: " + gateways + " / Depth: " + depth + " / addressCount: " + addressCount);

        try {
            for (int i = 1; i < addressCount; i++) {
                tree.put(i, getNodeInformation(i, devices, gateways, depth));
            }
        } catch (Exception e) {

        }
    }

    public static void browseTheTree(int search, int devices, int gateways, int depth) {
        int[] interval;
        try {
            interval = getNextTreeInterval(search, devices, gateways, depth);
        } catch (ZigBeeException e) {
            System.err.println("catch ZigBeeException");
            return;
        }

        for (int i = interval[INTERVAL_UPPER_LIMIT]; i < (interval[INTERVAL_UPPER_LIMIT] + devices); i++) {
            checkNode(i, search, interval, TYPE_DEVICE);
        }

        for (int i = interval[INTERVAL_LOW_LIMIT]; i < interval[INTERVAL_UPPER_LIMIT]; i += interval[INTERVAL_STEP]) {
            checkNode(i, search, interval, TYPE_GATEWAY);
            if (interval[INTERVAL_DEPTH] < (depth - 1)) {
                browseTheTree(i, devices, gateways, depth);
            }
        }
    }

    public static void checkNode(int nodeID, int parentNodeID, int[] interval, int type) {
        int[] nodeInformation = tree.get(nodeID);
        if (nodeInformation[NODE_INFO_PARENT] == parentNodeID && nodeInformation[NODE_INFO_DEPTH] == (interval[INTERVAL_DEPTH] + 1) && nodeInformation[NODE_INFO_TYPE] == type) {
            tree.remove(nodeID);
        } else {
            System.err.println("Error for node: " + nodeID);
        }
    }

    public static void drawTheTree(int search, int devices, int gateways, int depth) {
        int[] interval;
        try {
            interval = getNextTreeInterval(search, devices, gateways, depth);
        } catch (ZigBeeException e) {
            System.err.println("catch ZigBeeException");
            return;
        }

        if (search == 0) {
            System.out.println();
            System.out.println("C0");
        }

        for (int i = interval[INTERVAL_UPPER_LIMIT]; i < (interval[INTERVAL_UPPER_LIMIT] + devices); i++) {
            draw(i, interval[INTERVAL_DEPTH], TYPE_DEVICE);
        }

        for (int i = interval[INTERVAL_LOW_LIMIT]; i < interval[INTERVAL_UPPER_LIMIT]; i += interval[INTERVAL_STEP]) {
            draw(i, interval[INTERVAL_DEPTH], TYPE_GATEWAY);
            if (interval[INTERVAL_DEPTH] < (depth - 1)) {
                drawTheTree(i, devices, gateways, depth);
            }
        }
    }

    public static void draw(int node, int depth, int type) {
        StringBuilder str = new StringBuilder();
        str.append("    ".repeat(Math.max(0, depth)));
        str.append(" +- ");

        if (type == TYPE_GATEWAY) {
            str.append("G");
        } else if (type == TYPE_DEVICE) {
            str.append("D");
        }

        System.out.println(str + "" + node);
    }
}

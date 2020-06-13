package fr.tortevois.zigbee;

public final class ZigBee {

    // Status of node
    public final static boolean NODE_CONNECTED = true;
    public final static boolean NODE_NOT_CONNECTED = false;

    // Index for NETWORK_PARAMETERS arrays
    public final static int NETWORK_DEVICES = 0;
    public final static int NETWORK_GATEWAYS = 1;
    public final static int NETWORK_DEPTH = 2;
    public final static int NETWORK_PARAMETERS_COUNT = 3;

    // Index for INTERVAL arrays
    public final static int INTERVAL_DEPTH = 0;
    public final static int INTERVAL_LOW_LIMIT = 1;
    public final static int INTERVAL_UPPER_LIMIT = 2;
    public final static int INTERVAL_STEP = 3;
    public final static int INTERVAL_COUNT = 4;

    // Index for NODE_INFO arrays
    public final static int NODE_INFO_PARENT = 0;
    public final static int NODE_INFO_DEPTH = 1;
    public final static int NODE_INFO_TYPE = 2;
    public final static int NODE_INFO_COUNT = 3;

    // Type of devices
    public final static int TYPE_ROOT = 1;
    public final static int TYPE_GATEWAY = 2;
    public final static int TYPE_DEVICE = 3;

    /**
     * Compute the address count with the tree parameters
     *
     * @param devicesByNode  : max device count by gateway node.
     * @param gatewaysByNode : max gateway count by gateway node.
     * @param treeDepth      : depth of the tree.
     * @return the address count.
     */
    public static int getAddressCount(int devicesByNode, int gatewaysByNode, int treeDepth) {
        if (gatewaysByNode == 1) {
            return treeDepth * (gatewaysByNode + devicesByNode) + 1;
        } else {
            return (int) ((Math.pow(gatewaysByNode, treeDepth) - 1) * (gatewaysByNode + devicesByNode)) / (gatewaysByNode - 1) + 1;
        }
    }

    /**
     * Return the next tree's interval for a node.
     *
     * @param search         : sought node, the one for which we want information.
     * @param devicesByNode  : max device count by gateway node.
     * @param gatewaysByNode : max gateway count by gateway node.
     * @param treeDepth      : depth of the tree.
     * @return An integer array. It contains: the depth of the node sought, the limit of the next range the addresses, and the next step between two gateway's addresses.
     * @throws ZigBeeException if search is out of the address limit.
     */
    public static int[] getNextTreeInterval(int search, int devicesByNode, int gatewaysByNode, int treeDepth) throws ZigBeeException {
        int addressCount = getAddressCount(devicesByNode, gatewaysByNode, treeDepth);

        if (search >= addressCount) {
            throw new ZigBeeException("Unable to find this address");
        }

        // Initialize variables
        int depth = 0; // Be careful, it's 0! Here depth is the node's depth
        int lowLimit = 1; // First gateway address in this part of the tree
        int uppLimit = addressCount - devicesByNode; // First device address in this part of the tree
        int step = (addressCount - 1 - devicesByNode) / gatewaysByNode;

        if (search != 0) {
            // Initialize loop variables
            int node = 1; // Start to 1, with node 1

            // Search the node in the tree
            // We have found the node, if:
            // - search == node : this is a gateway
            // - depthExplored == depth : we are at the end of the tree, this is a gateway
            // - search >= uppLimit : this is a device
            while (node <= search && depth != treeDepth && search < uppLimit) {
                // If the search is in address range, update range parameters with those of the next depth; else go to next address range (move to next the step)
                if (search < (node + step)) {
                    depth++;
                    lowLimit = node + 1;
                    uppLimit = node + step - devicesByNode;
                    step = (step - 1 - devicesByNode) / gatewaysByNode;
                    node++;
                } else {
                    node += step;
                }
            }
            /** OLD version
             for (int node = lowLimit; node < uppLimit; ) {
             if (node > search || depth == treeDepth || search >= uppLimit) {
             break;
             }
             if (search < (node + step)) {
             depth++;
             lowLimit = node + 1;
             uppLimit = node + step - devicesByNode;
             step = (step - 1 - devicesByNode) / gatewaysByNode;
             node++;
             } else {
             node += step;
             }
             }
             */
        }

        int[] interval = new int[INTERVAL_COUNT];
        interval[INTERVAL_DEPTH] = depth; // Here is the depth of the sought node
        interval[INTERVAL_LOW_LIMIT] = lowLimit;
        interval[INTERVAL_UPPER_LIMIT] = uppLimit;
        interval[INTERVAL_STEP] = step;
        return interval;
    }

    /**
     * Return node information : parent node, depth in the tree, type node
     *
     * @param search         : sought node, the one for which we want information.
     * @param devicesByNode  : max device count by gateway node.
     * @param gatewaysByNode : max gateway count by gateway node.
     * @param treeDepth      : depth of the tree.
     * @return An integer array. If the research is successful, the field NODE_INFO_TYPE should be different than 0.
     * @throws ZigBeeException if search is out of the address limit.
     */
    public static int[] getNodeInformation(int search, int devicesByNode, int gatewaysByNode, int treeDepth) throws ZigBeeException {
        int addressCount = getAddressCount(devicesByNode, gatewaysByNode, treeDepth);

        if (search >= addressCount) {
            throw new ZigBeeException("Unable to find this address");
        }

        // Initialize variables
        int parentNodeID = 0;
        int depth = 0;
        int type = 0;

        if (search == 0) {
            // Build the array to return for the root node
            type = TYPE_ROOT;
        } else {
            // Initialize Loop variables
            int lowLimit = 1; // First gateway address in this part of the tree
            int uppLimit = addressCount - devicesByNode; // First device address in this part of the tree
            int step = (addressCount - 1 - devicesByNode) / gatewaysByNode;
            depth = 1; // Start to 1, with node 1
            int node = 1; // Start to 1, with node 1

            // Search the node in the tree
            // We have found the node, if:
            // - search == node : this is a gateway
            // - depthExplored == depth : we are at the end of the tree, this is a gateway
            // - search >= uppLimit : this is a device
            while (node < search && depth != treeDepth && search < uppLimit) {
                // If the search is in address range, update range parameters with those of the next depth; else go to next address range (move to next the step)
                if (search < (node + step)) {
                    parentNodeID = node;
                    depth++;
                    lowLimit = node + 1;
                    uppLimit = node + step - devicesByNode;
                    step = (step - 1 - devicesByNode) / gatewaysByNode;
                    node++;
                } else {
                    node += step;
                }
            }
            /** OLD version
             for (int node = lowLimit; node < uppLimit; ) {
             if (search == node || depth == treeDepth || search >= uppLimit) {
             // We have found the node, go out the loop
             break;
             }
             // If the search is in address range, update range parameters with those of the next depth; else go to next address range (move to next the step)
             if (search < (node + step)) {
             parentNodeID = node;
             depth++;
             lowLimit = node + 1;
             uppLimit = node + step - devicesByNode;
             step = (step - 1 - devicesByNode) / gatewaysByNode;
             node++;
             } else {
             node += step;
             }
             }
             */

            // This is a gateway
            if (isBetween(search, lowLimit, uppLimit)) {
                type = TYPE_GATEWAY;
            }

            // This is a device
            if (isBetween(search, uppLimit, (uppLimit + devicesByNode))) {
                type = TYPE_DEVICE;
            }
        }

        // Initialize the array to return
        int[] nodeInformation = new int[NODE_INFO_COUNT];
        nodeInformation[NODE_INFO_PARENT] = parentNodeID;
        nodeInformation[NODE_INFO_DEPTH] = depth;
        nodeInformation[NODE_INFO_TYPE] = type;
        return nodeInformation;
    }

    /**
     * Check is the value is between an including minimum and an exclude maximum
     *
     * @param value : value to test
     * @param min   : minimum value
     * @param max   : maximum value
     * @return boolean
     */
    private static boolean isBetween(int value, int min, int max) {
        return (value >= min && value < max);
    }
}

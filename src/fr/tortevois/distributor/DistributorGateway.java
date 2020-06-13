package fr.tortevois.distributor;

import fr.tortevois.exception.NoNodeIDAvailable;
import fr.tortevois.gateway.IGateway;
import fr.tortevois.zigbee.ZigBeeException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static fr.tortevois.gateway.IGateway.*;
import static fr.tortevois.utils.Utils.*;
import static fr.tortevois.zigbee.ZigBee.*;

public class DistributorGateway extends Distributor {

    private final static boolean DEBUG = true;

    private IGateway gateway;
    private int[] networkParameters;
    private int[] childrenNodesInformation;

    private Map<Integer, Boolean> connectedGateways;
    private Map<Integer, Boolean> connectedDevices;
    private Map<Integer, InetAddress> clientsAddresses;

    /**
     * Standard DistributorGateway's constructor
     *
     * @param gateway : The gateway
     * @param address : The gateway's address
     * @param port    : The gateway's port
     * @param type    : The distributor type (to get the local node ID)
     */
    public DistributorGateway(IGateway gateway, InetAddress address, int port, int type) {
        super(address, port, type);
        initializeGateway(gateway);
    }

    /**
     * DistributorGateway's constructor for RMI Gateway
     *
     * @param gateway : The gateway
     * @param nodeID  : RMI gateway node ID
     */

    public DistributorGateway(IGateway gateway, int nodeID) {
        super(nodeID);
        initializeGateway(gateway);
    }

    /**
     * The Gateway initializer
     *
     * @param gateway
     */
    private void initializeGateway(IGateway gateway) {
        this.gateway = gateway;
        childrenNodesInformation = new int[INTERVAL_COUNT];
        connectedDevices = new TreeMap<>();
        connectedGateways = new TreeMap<>();
        clientsAddresses = new TreeMap<>();
    }

    /**
     * Set the network parameters
     *
     * @param networkParameters : The network parameters array
     */
    public void setNetworkParameters(int[] networkParameters) {
        this.networkParameters = networkParameters;
    }

    /**
     * Get the network parameters array
     *
     * @return The network parameters array
     */
    public int[] getNetworkParameters() {
        return networkParameters;
    }

    /**
     * Set the available address (nodes ID)
     */
    public void setAvailableNodesID() {
        int[] interval = null;
        try {
            interval = getNextTreeInterval(getNodeID(), networkParameters[NETWORK_DEVICES], networkParameters[NETWORK_GATEWAYS], networkParameters[NETWORK_DEPTH]);
        } catch (ZigBeeException e) {
            System.err.println("Unable to get the next tree interval");
            System.exit(-1);
        }
        childrenNodesInformation[INTERVAL_DEPTH] = interval[INTERVAL_DEPTH];

        if (interval[INTERVAL_DEPTH] < networkParameters[NETWORK_DEPTH]) {
            for (int i = interval[INTERVAL_UPPER_LIMIT]; i < (interval[INTERVAL_UPPER_LIMIT] + networkParameters[NETWORK_DEVICES]); i++) {
                connectedDevices.put(i, NODE_NOT_CONNECTED);
            }

            for (int i = interval[INTERVAL_LOW_LIMIT]; i < interval[INTERVAL_UPPER_LIMIT]; i += interval[INTERVAL_STEP]) {
                connectedGateways.put(i, NODE_NOT_CONNECTED);
            }

            childrenNodesInformation[INTERVAL_LOW_LIMIT] = interval[INTERVAL_LOW_LIMIT];
            childrenNodesInformation[INTERVAL_UPPER_LIMIT] = (interval[INTERVAL_UPPER_LIMIT] + networkParameters[NETWORK_DEVICES]);
            childrenNodesInformation[INTERVAL_STEP] = interval[INTERVAL_STEP];
        } else {
            childrenNodesInformation[INTERVAL_LOW_LIMIT] = getNodeID();
            childrenNodesInformation[INTERVAL_UPPER_LIMIT] = getNodeID();
            childrenNodesInformation[INTERVAL_STEP] = 1;
        }
    }

    /**
     * Get an available address (nodes ID) according to the device type
     *
     * @return An available address (nodes ID)
     */
    public int getAvailableNodeID(int type) throws NoNodeIDAvailable {
        Map<Integer, Boolean> map = null;

        if (type == TYPE_GATEWAY) {
            map = connectedGateways;
        } else if (type == TYPE_DEVICE) {
            map = connectedDevices;
        }

        if (map != null) {
            for (Map.Entry<Integer, Boolean> entry : map.entrySet()) {
                if (entry.getValue() == NODE_NOT_CONNECTED) {
                    map.put(entry.getKey(), NODE_CONNECTED);
                    printTrace(DEBUG, OUT, "Give nodeID " + entry.getKey());
                    return entry.getKey();
                }
            }
        }
        throw new NoNodeIDAvailable();
    }

    /**
     * Override the distributor's callback to process a message received on the listening socket
     *
     * @param msg : the received message
     */
    @Override
    public void messageProcessing(String msg) {
        printTrace(DEBUG, OUT, "DistributorGateway.messageProcessing: " + msg);

        try {
            JSONObject json = (JSONObject) new JSONParser().parse(msg);
            String query = (String) json.get("query");

            switch (query) {
                case QUERY_GET_NODE_ID:
                    execQueryNodeID(json);
                    break;

                case QUERY_NETWORK_PARAMETERS:
                    execQueryNetworkParameters(json);
                    break;

                case QUERY_GET_MONEY:
                case QUERY_GET_STOCK:
                    treatMessage(json);
                    break;

                case REPLY_NETWORK_PARAMETERS:
                    execReplyNetworkParameters(json);
                    break;

                case REPLY_NEW_NODE_CONNEXION:
                case REPLY_GET_STOCK:
                case REPLY_GET_MONEY:
                case ADD_TO_LOGS_HISTORY:
                    gateway.sendMessageToGateway(msg);
                    break;

                default:
                    printTrace(DEBUG, ERR, "DistributorGateway.messageProcessing? : " + msg + " no implemented");
                    // TODO execReplyBadRequest();
                    break;
            }
        } catch (ParseException e) {
            System.err.println("Gateway.readMessageFromSocket ParseException: " + e.getMessage());
        }
    }

    /**
     * Internal pre-treatment of the message
     *
     * @param json : JSON message to broadcast
     */
    private void treatMessage(JSONObject json) {
        String query = (String) json.get("query");
        List<Integer> nodesID = getListNodesID(json);
        broadcastMessage(json, query, nodesID);
    }

    /**
     * Broadcast the message
     *
     * @param json    : JSON message to broadcast
     * @param query   : query
     * @param nodesID : address list of nodes
     */
    public void broadcastMessage(JSONObject json, String query, List<Integer> nodesID) {
        // Local execution
        if (nodesID.contains(getNodeID())) {
            String reply = null;
            switch (query) {
                case QUERY_GET_MONEY:
                    reply = execQueryGetMoney();
                    break;
                case QUERY_GET_STOCK:
                    reply = execQueryGetStock();
                    break;
            }
            gateway.sendMessageToGateway(reply);
        }

        // Send to the children nodesID
        if (hasChildrenQuery(nodesID)) {

            // Send to the children devices
            for (Map.Entry<Integer, Boolean> entry : connectedDevices.entrySet()) {
                int deviceNodeID = entry.getKey();
                if (nodesID.contains(deviceNodeID)) {
                    if (entry.getValue() == NODE_CONNECTED) {
                        sendMessageToSocketNodeID(deviceNodeID, json.toString());
                    } else {
                        execReplyNotConnected(deviceNodeID);
                    }
                }
            }

            // Send to the children gateways
            for (Map.Entry<Integer, Boolean> entry : connectedGateways.entrySet()) {
                int gatewayNodeID = entry.getKey();
                if (hasChildrenQuery(gatewayNodeID, nodesID)) {
                    // If the gateway is connected
                    if (entry.getValue() == NODE_CONNECTED) {
                        sendMessageToSocketNodeID(gatewayNodeID, json.toString());
                    } else {
                        execReplyNotConnected(gatewayNodeID);
                    }
                }
            }
        }
    }

    /**
     * Check if gateway's children are expected (from Interval: Devices + Gateway nodes)
     *
     * @param nodesID : The recipient address list (nodes ID)
     * @return The boolean status
     */
    private boolean hasChildrenQuery(List<Integer> nodesID) {
        for (int nodeID : nodesID) {
            if (nodeID >= childrenNodesInformation[INTERVAL_LOW_LIMIT] && nodeID < childrenNodesInformation[INTERVAL_UPPER_LIMIT]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if gateway's children are expected (from GatewayNodeID: sub-network interval)
     *
     * @param gatewayNodeID : The gateway address
     * @param nodesID       : The recipient address list (nodes ID)
     * @return The boolean status
     */
    private boolean hasChildrenQuery(int gatewayNodeID, List<Integer> nodesID) {
        for (int nodeID : nodesID) {
            if (nodeID >= gatewayNodeID && nodeID < gatewayNodeID + childrenNodesInformation[INTERVAL_STEP]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Send a JSON message to a node ID
     *
     * @param nodeID : The recipient node ID
     * @param json   : The stringify JSON message
     */
    public void sendMessageToSocketNodeID(int nodeID, String json) {
        if (clientsAddresses.containsKey(nodeID)) {
            InetAddress address = clientsAddresses.get(nodeID);
            getSendingSocket().send(json, address, INITIAL_SOCKET_PORT + nodeID);
        } else {
            System.err.println("Adresses not registered for nodeID " + nodeID);
        }
    }

    /**
     * Execute the QUERY_GET_NODE_ID
     *
     * @param json : The incoming JSON frame
     */
    private void execQueryNodeID(JSONObject json) {
        InetAddress address;
        try {
            address = InetAddress.getByName((String) json.get("reply_address"));
        } catch (UnknownHostException e) {
            System.err.println("Gateway.execQueryNodeID UnknownHostException: " + e.getMessage());
            return;
        }

        int port = jsonGetToInteger("reply_port", json);
        int device_type = jsonGetToInteger("device_type", json);

        int nodeID = -1;
        try {
            // Get an Available NodeID
            nodeID = getAvailableNodeID(device_type);
        } catch (NoNodeIDAvailable e) {
            System.err.println("Gateway.execQueryNodeID NoNodeIDAvailable");
        }
        // Build the reply
        json.clear();
        json.put("query", REPLY_GET_NODE_ID);
        json.put("node_id", nodeID);

        // Reply to the specific address and port number
        getSendingSocket().send(json.toString(), address, port);

        if (nodeID != -1) {
            clientsAddresses.put(nodeID, address);
        }
    }

    /**
     * Execute the QUERY_NETWORK_PARAMETERS
     *
     * @param json : The incoming JSON frame
     */
    private void execQueryNetworkParameters(JSONObject json) {
        int nodeID = jsonGetToInteger("node_id", json);
        // Build the reply
        json.clear();
        json.put("query", REPLY_NETWORK_PARAMETERS);
        json.put("devices", networkParameters[NETWORK_DEVICES]);
        json.put("gateways", networkParameters[NETWORK_GATEWAYS]);
        json.put("depth", networkParameters[NETWORK_DEPTH]);
        // Send it
        sendMessageToSocketNodeID(nodeID, json.toString());
    }

    /**
     * Build the REPLY_GET_STOCK for NOT_CONNECTED distributor
     *
     * @param nodeID : The node ID of the NOT_CONNECTED distributor
     */
    public void execReplyNotConnected(int nodeID) {
        JSONObject json = new JSONObject();
        json.put("query", REPLY_GET_STOCK);
        json.put("status", REPLY_STATUS_NOT_CONNECTED);
        json.put("node_id", nodeID);
        gateway.sendMessageToGateway(json.toString());
    }

    /**
     * Execute the REPLY_NETWORK_PARAMETERS and set it
     *
     * @param json : The incoming JSON frame
     */
    private void execReplyNetworkParameters(JSONObject json) {
        int[] networkParameters = new int[NETWORK_PARAMETERS_COUNT];
        networkParameters[NETWORK_DEVICES] = jsonGetToInteger("devices", json);
        networkParameters[NETWORK_GATEWAYS] = jsonGetToInteger("gateways", json);
        networkParameters[NETWORK_DEPTH] = jsonGetToInteger("depth", json);
        setNetworkParameters(networkParameters);
    }
}

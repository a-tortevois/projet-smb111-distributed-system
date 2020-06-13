package fr.tortevois.server;

import fr.tortevois.admin.IAdminInterface;
import fr.tortevois.exception.NoNodeIDAvailable;
import fr.tortevois.gateway.IRmiGateway;
import fr.tortevois.utils.CSV;
import fr.tortevois.zigbee.ZigBee;
import fr.tortevois.zigbee.ZigBeeException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.tortevois.admin.IAdminInterface.ADMIN_INTERFACE_RMI_NAME;
import static fr.tortevois.distributor.Distributor.DEFAULT_FILENAME;
import static fr.tortevois.distributor.Product.PRODUCT_ID;
import static fr.tortevois.distributor.Product.PRODUCT_NAME;
import static fr.tortevois.gateway.IGateway.*;
import static fr.tortevois.utils.CSV.CSV_EXTENSION;
import static fr.tortevois.utils.CSV.CSV_SEPARATOR;
import static fr.tortevois.utils.Utils.*;
import static fr.tortevois.zigbee.ZigBee.*;

public class DistributorManager extends UnicastRemoteObject implements IDistributorManager {

    private final static boolean DEBUG = true;

    private IAdminInterface admin = null;
    private boolean isInit = false;
    private int addressCount;
    private int[] networkParameters;
    private int[] childrenNodesInformation;
    private Map<Integer, Boolean> connectedGateways; // Children in the direct upper ring
    private Map<Integer, Boolean> connectedDistributors; // All the distributors connected
    String expectedReply;
    AtomicBoolean isQueryInProgress;
    AtomicInteger expectedRepliesCount;
    List<String> buffer;

    private Map<Integer, String> productsDatabase;

    private List<String> logsHistory;

    // -- Singleton -------------------------------------------------------------------------------------------------------------

    private static IDistributorManager instance = null;

    static {
        try {
            instance = new DistributorManager();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private DistributorManager() throws RemoteException {
    }

    public static IDistributorManager getInstance() {
        return instance;
    }

    // -- RMI Interface implementation ------------------------------------------------------------------------------------------

    /**
     * Initialize the DistributorManager
     *
     * @param devices  : The device count by gateways nodes
     * @param gateways : The gateway count by gateways nodes
     * @param depth    : The network depth
     * @throws RemoteException
     */
    public void initDistributorManager(int devices, int gateways, int depth) throws RemoteException {
        if (!isInit) {
            networkParameters = new int[NETWORK_PARAMETERS_COUNT];
            networkParameters[NETWORK_DEVICES] = devices;
            networkParameters[NETWORK_GATEWAYS] = gateways;
            networkParameters[NETWORK_DEPTH] = depth;
            childrenNodesInformation = new int[INTERVAL_COUNT];
            addressCount = ZigBee.getAddressCount(devices, gateways, depth);
            connectedGateways = new TreeMap<>();
            connectedDistributors = new TreeMap<>();

            // Initialize query
            expectedReply = "";
            isQueryInProgress = new AtomicBoolean(false);
            expectedRepliesCount = new AtomicInteger(0);
            buffer = new ArrayList<>();

            loadProductsDatabase();

            logsHistory = new ArrayList<>();

            setAvailableNodesID();

            isInit = true;
        } else {
            // Already initialized ...
        }
    }

    /**
     * Get the Admin Interface from RMI
     *
     * @throws RemoteException
     */
    public void getAdminFromRMI() throws RemoteException {
        try {
            admin = (IAdminInterface) Naming.lookup("rmi://localhost/" + ADMIN_INTERFACE_RMI_NAME);
        } catch (Exception e) {
            System.err.println("Unable to get the distributor manager: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Get the Address count computed from network parameters
     *
     * @throws RemoteException
     */
    public int getAddressCount() throws RemoteException {
        return addressCount;
    }

    /**
     * Get the list of connected distributors
     *
     * @return the list of connected distributors
     * @throws RemoteException
     */
    public List<Integer> getConnectedDistributors() throws RemoteException {
        return new ArrayList<>(connectedDistributors.keySet());
    }

    /**
     * Get an available address
     *
     * @return An available address
     * @throws RemoteException
     * @throws NoNodeIDAvailable
     */
    public int getAvailableNodeID() throws RemoteException, NoNodeIDAvailable {
        for (Map.Entry<Integer, Boolean> entry : connectedGateways.entrySet()) {
            if (entry.getValue() == NODE_NOT_CONNECTED) {
                connectedGateways.put(entry.getKey(), NODE_CONNECTED);
                connectedDistributors.put(entry.getKey(), NODE_CONNECTED);
                printTrace(DEBUG, OUT, "Give nodeID " + entry.getKey());
                return entry.getKey();
            }
        }
        throw new NoNodeIDAvailable();
    }

    /**
     * Get the network parameters array
     *
     * @return The network parameters array
     * @throws RemoteException
     */
    public int[] getNetworkParameters() throws RemoteException {
        return networkParameters;
    }

    /**
     * Send a message to an RMI node
     *
     * @param msg     : The string message to send
     * @param nodesID : The address list of node
     * @throws RemoteException
     */
    public void sendMessageToRMI(String msg, List<Integer> nodesID) throws RemoteException {
        printTrace(DEBUG, OUT, "DistributorManager.sendMessageToRMI: " + msg + " | nodesID: " + nodesID);
        // Block util the end of the previous query
        while (isQueryInProgress.get()) {
        }

        switch (msg) {
            case QUERY_GET_MONEY:
                setQuery(REPLY_GET_MONEY, true, nodesID.size());
                broadcastMessage(msg, nodesID);
                break;

            case QUERY_GET_STOCK:
                setQuery(REPLY_GET_STOCK, true, nodesID.size());
                broadcastMessage(msg, nodesID);
                break;

            default:
                // TODO complete here with internal query message
                break;
        }
    }

    /**
     * Read a message from a RMI node
     *
     * @param msg : The JSON string message to read
     * @throws RemoteException
     */
    public void readMessageFromRMI(String msg) throws RemoteException {
        printTrace(DEBUG, OUT, "DistributorManager.readMessageFromRMI: " + msg);

        try {
            JSONObject json = (JSONObject) new JSONParser().parse(msg);
            String query = (String) json.get("query");

            switch (query) {
                case REPLY_NEW_NODE_CONNEXION: {
                    int nodeID = jsonGetToInteger("node_id", json);
                    connectedDistributors.put(nodeID, NODE_CONNECTED);
                    break;
                }

                case ADD_TO_LOGS_HISTORY: {
                    int nodeID = jsonGetToInteger("node_id", json);
                    String log = (String) json.get("log");
                    logsHistory.add(log);
                    break;
                }

                default:
                    if (isQueryInProgress.get() && expectedReply.equals(query)) {
                        buffer.add(msg);
                        expectedRepliesCount.decrementAndGet();
                    }
                    break;
            }
        } catch (ParseException e) {
            System.err.println("DistributorManager.readMessageFromRMI ParseException: " + e.getMessage());
        }
    }

    /**
     * Wait for the end of the query before to continue the execution
     *
     * @throws RemoteException
     */
    public void waitForEndQuery() throws RemoteException {
        int timeout = 0;
        while (isQueryInProgress.get()) {
            if (timeout >= QUERY_TIMEOUT) {
                System.err.println("Request timeout");
            }

            if (expectedRepliesCount.get() == 0 || timeout >= QUERY_TIMEOUT) {
                switch (expectedReply) {
                    case REPLY_GET_MONEY:
                        printToAdmin(execReplyGetMoney());
                        break;

                    case REPLY_GET_STOCK:
                        printToAdmin(execReplyGetStock());
                        break;

                    default:
                        for (String msg : buffer) {
                            printToAdmin(msg);
                        }
                        break;
                }

                buffer.clear();
                setQuery("", false, 0);
                break;
            }

            try {
                Thread.sleep(THREAD_SLEEP_INTERVAL);
                timeout += THREAD_SLEEP_INTERVAL;
            } catch (InterruptedException e) {
                System.err.println("InterruptedException: " + e.getMessage());
            }
        }
    }

    /**
     * Make an address to available
     *
     * @param nodeID : The address to free
     * @return The boolean status
     * @throws RemoteException
     */
    public boolean freeNodeID(int nodeID) throws RemoteException {
        if (connectedDistributors.containsKey(nodeID)) {
            connectedDistributors.put(nodeID, NODE_NOT_CONNECTED);
            if (connectedGateways.containsKey(nodeID)) {
                connectedGateways.put(nodeID, NODE_NOT_CONNECTED);
                printTrace(DEBUG, OUT, "Release Gateway nodeID " + nodeID);
            }
            printTrace(DEBUG, OUT, "Release nodeID " + nodeID);
            return true;
        }

        return false;
    }

    /**
     * Print the logs history to the Administrative Interface
     *
     * @throws RemoteException
     */
    public void displayLogsHistory() throws RemoteException {
        if (logsHistory.size() > 0) {
            for (String line : logsHistory) {
                printToAdmin(line);
            }
        } else {
            printToAdmin("No logs history available to display");
        }
    }

    // -- Internal Methods ------------------------------------------------------------------------------------------------------

    /**
     * Display to the Administrative Interface
     *
     * @param msg : The string message to display
     */
    private void printToAdmin(String msg) {
        if (admin != null) {
            try {
                admin.printOnAdminInterface(msg);
            } catch (RemoteException e) {
                System.err.println("DistributorManager.printToAdmin RemoteException: " + e.getMessage());
            }
        } else {
            System.err.println("Admin is not defined !");
            System.exit(-1);
        }
    }

    /**
     * Load the products database
     */
    private void loadProductsDatabase() {
        List<String> lines = CSV.readFile(DEFAULT_FILENAME + CSV_EXTENSION);
        productsDatabase = new TreeMap<>();
        for (String line : lines) {
            String[] data = line.split(CSV_SEPARATOR);
            int id = Integer.parseInt(data[PRODUCT_ID]);
            productsDatabase.put(id, data[PRODUCT_NAME]);
        }
    }

    /**
     * Set the available address (nodes ID)
     */
    private void setAvailableNodesID() {
        int[] interval = null;
        try {
            interval = getNextTreeInterval(0, networkParameters[NETWORK_DEVICES], networkParameters[NETWORK_GATEWAYS], networkParameters[NETWORK_DEPTH]);
        } catch (ZigBeeException e) {
            System.err.println("Unable to get the next tree interval");
            System.exit(-1);
        }

        for (int i = interval[INTERVAL_LOW_LIMIT]; i < interval[INTERVAL_UPPER_LIMIT]; i += interval[INTERVAL_STEP]) {
            connectedGateways.put(i, NODE_NOT_CONNECTED);
        }

        childrenNodesInformation[INTERVAL_DEPTH] = interval[INTERVAL_DEPTH];
        childrenNodesInformation[INTERVAL_LOW_LIMIT] = interval[INTERVAL_LOW_LIMIT];
        childrenNodesInformation[INTERVAL_UPPER_LIMIT] = (interval[INTERVAL_UPPER_LIMIT] + networkParameters[NETWORK_DEVICES]);
        childrenNodesInformation[INTERVAL_STEP] = interval[INTERVAL_STEP];
    }

    /**
     * Set the in progress query
     *
     * @param query : The in progress query
     * @param flag  : The query's flag
     * @param count : The expected query reply count
     */
    private void setQuery(String query, boolean flag, int count) {
        expectedReply = query;
        isQueryInProgress.set(flag);
        expectedRepliesCount.set(count);
    }

    /**
     * Send a message to an address list
     *
     * @param msg     : The message to send
     * @param nodesID : The address list (nodes ID)
     */
    private void broadcastMessage(String msg, List<Integer> nodesID) {
        if (nodesID.size() >= 1) {
            // Send to the children gateways
            for (Map.Entry<Integer, Boolean> entry : connectedGateways.entrySet()) {
                int gatewayNodeID = entry.getKey();
                if (hasChildrenQuery(gatewayNodeID, nodesID)) {
                    // If the gateway is connected
                    if (entry.getValue() == NODE_CONNECTED) {
                        try {
                            String name = "gateway_" + gatewayNodeID;
                            IRmiGateway gateway = (IRmiGateway) Naming.lookup("rmi://localhost/" + name);
                            gateway.readMessageFromRMI(msg, nodesID);
                        } catch (Exception e) {
                            System.err.println("Unable to get the gateway");
                            e.printStackTrace();
                        }
                    } else {
                        System.err.println("Unable to connect to the gateway " + gatewayNodeID);
                    }
                }
            }
        }
    }

    /**
     * Check if gateway's children are expected
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
     * Tools to print the quantity columns
     *
     * @param str : The quantity to print
     * @param len : The label length
     * @return The formatted string
     */
    private String printQuantity(String str, int len) {
        return String.format("%" + (len - 2) + "s  ", str);
    }

    /**
     * Execute the query's REPLY_GET_MONEY
     *
     * @return The formatted string
     */
    private String execReplyGetMoney() {
        Map<Integer, Double> distributorsMoney = new TreeMap<>();
        for (String msg : buffer) {
            try {
                JSONObject json = (JSONObject) new JSONParser().parse(msg);
                if (jsonGetToInteger("status", json) == REPLY_STATUS_CONNECTED) {
                    int idDistributor = jsonGetToInteger("distributor_id", json);
                    double money = (double) json.get("distributor_money");
                    distributorsMoney.put(idDistributor, money);
                }
            } catch (ParseException e) {
                System.err.println("DistributorManager.waitForEndQuery ParseException: " + e.getMessage());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\nResult of querying to get the Distributors stocks:\n");
        if (distributorsMoney.size() > 0) {
            // Build the Array Header
            sb.append(String.format("%-18s", "")).append("|").append(" Money").append("\n");

            // Build the Array Body
            for (Map.Entry<Integer, Double> distributor : distributorsMoney.entrySet()) {
                int idDistributor = distributor.getKey();
                double money = distributor.getValue();
                sb.append(String.format("%-18s", " Distributor #" + idDistributor)).append("|").append(String.format("  %3.2f%s", money, CURRENCY)).append("\n");
            }
        } else {
            sb.append("Unable to get the distributors money\n");
        }
        return sb.toString();
    }

    /**
     * Execute the query's REPLY_GET_STOCK
     *
     * @return The formatted string
     */
    private String execReplyGetStock() {
        Map<Integer, Map<Integer, Integer>> distributorsStock = new TreeMap<>();
        for (String msg : buffer) {
            try {
                Map<Integer, Integer> stock = new TreeMap<>();
                JSONObject json = (JSONObject) new JSONParser().parse(msg);
                if (jsonGetToInteger("status", json) == REPLY_STATUS_CONNECTED) {
                    int idDistributor = jsonGetToInteger("distributor_id", json);
                    JSONObject distributor_stock = (JSONObject) json.get("distributor_stock");
                    JSONArray idArray = (JSONArray) distributor_stock.get("id");
                    JSONArray quantityArray = (JSONArray) distributor_stock.get("quantity");
                    if (idArray != null && quantityArray != null) {
                        if (idArray.size() == quantityArray.size()) {
                            int len = idArray.size();
                            for (int i = 0; i < len; i++) {
                                int id = (int) ((long) idArray.get(i));
                                int quantity = (int) ((long) quantityArray.get(i));
                                stock.put(id, quantity);
                            }
                        } else {
                            // Size Error
                        }
                    } else {
                        // Null Error
                    }
                    distributorsStock.put(idDistributor, stock);
                }
            } catch (ParseException e) {
                System.err.println("DistributorManager.waitForEndQuery ParseException: " + e.getMessage());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\nResult of querying to get the Distributors stocks:\n");
        if (distributorsStock.size() > 0) {
            // Build the Array Header
            sb.append(String.format("%-18s", "")).append("|");
            for (Map.Entry<Integer, String> product : productsDatabase.entrySet()) {
                sb.append(" ").append(product.getValue()).append(" ").append("|");
            }
            sb.append("\n");

            // Build the Array Body
            for (Map.Entry<Integer, Map<Integer, Integer>> distributor : distributorsStock.entrySet()) {
                int idDistributor = distributor.getKey();
                Map<Integer, Integer> stock = distributor.getValue();
                sb.append(String.format("%-18s", " Distributor #" + idDistributor)).append("|");
                for (Map.Entry<Integer, String> product : productsDatabase.entrySet()) {
                    int idProduct = product.getKey();
                    int len = (product.getValue().length() + 2);
                    if (stock.containsKey(idProduct)) {
                        sb.append(printQuantity(stock.get(idProduct).toString(), len)).append("|");
                    } else {
                        sb.append(printQuantity("-", len)).append("|");
                    }
                }
                sb.append("\n");
            }
        } else {
            sb.append("Unable to get the distributors stocks\n");
        }
        return sb.toString();
    }
}

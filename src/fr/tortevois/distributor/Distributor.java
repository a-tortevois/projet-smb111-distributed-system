package fr.tortevois.distributor;

import fr.tortevois.exception.ProductNotAvailable;
import fr.tortevois.exception.ProductNotFound;
import fr.tortevois.exception.ProductStockAlert;
import fr.tortevois.exception.TooMuchMoneyAlert;
import fr.tortevois.socket.ListeningSocket;
import fr.tortevois.socket.SendingSocket;
import fr.tortevois.utils.CSV;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static fr.tortevois.distributor.Product.*;
import static fr.tortevois.gateway.IGateway.*;
import static fr.tortevois.utils.CSV.CSV_EXTENSION;
import static fr.tortevois.utils.CSV.CSV_SEPARATOR;
import static fr.tortevois.utils.Utils.*;

public class Distributor {

    private final static boolean DEBUG = true;
    public final static String FILENAME_PREFIX = "distributor_";
    public final static String DEFAULT_FILENAME = "default_products_list";
    public final static int INITIAL_SOCKET_PORT = 6000;
    private final static int DEFAULT_LISTEN_PORT = 8080;
    private final UUID uID = UUID.randomUUID();

    private int nodeID = -1;
    private InetAddress gatewayAddress;
    private int gatewayPort = -1;
    private ListeningSocket listeningSocket;
    private SendingSocket sendingSocket;
    private Thread thread;

    private TreeMap<Integer, Product> productsDatabase;
    private double money;

    /**
     * Standard Distributor's constructor
     *
     * @param address : The gateway's address
     * @param port    : The gateway's port
     * @param type    : The distributor type (to get the local node ID)
     */
    public Distributor(InetAddress address, int port, int type) {
        this.gatewayAddress = address;
        this.gatewayPort = port;
        sendingSocket = new SendingSocket();
        try {
            InetAddress clientAddress = InetAddress.getByName(null);

            // Build the query message
            JSONObject json = new JSONObject();
            json.put("query", QUERY_GET_NODE_ID);
            json.put("reply_address", clientAddress.getHostName());
            json.put("reply_port", DEFAULT_LISTEN_PORT);
            json.put("device_type", type);

            // Create the socket
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket(DEFAULT_LISTEN_PORT);
            } catch (SocketException e) {
                System.err.println("Unable to create the socket : " + e.getMessage());
                System.exit(-1);
            }

            // Create the buffer for the message
            byte[] buffer = new byte[BUFFER_MAX_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // Send the query
            sendingSocket.send(json.toString(), gatewayAddress, gatewayPort);
            json.clear();

            // Read message from the Gateway
            socket.receive(packet); // receive is blocking
            String msg = new String(packet.getData(), 0, packet.getLength());
            // Close immediately the socket which should be available for the next connexion !
            socket.close();
            printTrace(DEBUG, OUT, "Distributor.Read: " + msg);

            // Parse the received message
            json = (JSONObject) new JSONParser().parse(msg);
            String reply = (String) json.get("query");
            if (reply.equals(REPLY_GET_NODE_ID)) {
                nodeID = jsonGetToInteger("node_id", json); // Integer.parseInt((String) json.get("node_id"));
            }
        } catch (UnknownHostException e) {
            System.err.println("Unable to get InetAddress");
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException e) {
            System.err.println("IOException: " + e);
        } catch (ParseException e) {
            System.err.println("ParseException: " + e.getMessage());
        }

        if (nodeID < 1) {
            System.err.println("Unable to get a nodeID");
            System.exit(-1);
        }

        printTrace(DEBUG, OUT, "Set nodeID " + nodeID);

        // Broadcast the connexion to the server
        JSONObject json = new JSONObject();
        json.put("query", REPLY_NEW_NODE_CONNEXION);
        json.put("node_id", nodeID);
        sendMessageToGateway(json.toString());

        // Launch the listening UDP socket Thread
        startTread();
    }

    /**
     * Distributor's constructor for RMI Gateway
     *
     * @param nodeID : RMI gateway node ID
     */
    public Distributor(int nodeID) {
        printTrace(DEBUG, OUT, "Set nodeID " + nodeID);
        this.nodeID = nodeID;
        sendingSocket = new SendingSocket();
        // Launch the listening UDP socket Thread
        startTread();
    }

    /**
     * Start the listening socket into a thread
     */
    private void startTread() {
        listeningSocket = new ListeningSocket(this, (INITIAL_SOCKET_PORT + nodeID));
        thread = new Thread(listeningSocket);
        thread.start();
    }

    /**
     * Stop the sockets and the thread
     */
    public void stop() {
        listeningSocket.close();
        thread.interrupt();
        sendingSocket.close();
    }

    /**
     * Get the node ID
     *
     * @return The node ID
     */
    public int getNodeID() {
        return nodeID;
    }

    /**
     * Get the sending socket
     *
     * @return The sending socket
     */
    public SendingSocket getSendingSocket() {
        return sendingSocket;
    }

    /**
     * The distributor's callback to process a message received on the listening socket
     *
     * @param msg : the received message
     */
    public void messageProcessing(String msg) {
        printTrace(DEBUG, OUT, "Distributor.messageProcessing: " + msg);
        String query;

        try {
            JSONObject json = (JSONObject) new JSONParser().parse(msg);
            query = (String) json.get("query");
            List<Integer> nodesID = getListNodesID(json);

            // Local execution
            String reply = null;
            if (nodesID.contains(getNodeID())) {
                switch (query) {
                    case QUERY_GET_MONEY:
                        reply = execQueryGetMoney();
                        break;

                    case QUERY_GET_STOCK:
                        reply = execQueryGetStock();
                        break;

                    default:
                        printTrace(DEBUG, ERR, "Distributor.messageProcessing? : " + msg + " no implemented");
                        reply = execReplyBadRequest();
                        break;
                }
            }

            if (reply == null) {
                reply = execReplyBadRequest();
            }

            sendMessageToGateway(reply);

        } catch (ParseException e) {
            System.err.println("Gateway.readMessageFromSocket ParseException: " + e.getMessage());
        }
    }

    /**
     * Transform a JSON address array to address list
     *
     * @param json : The JSON Object from where to extract the address array
     * @return The address list
     */
    public List<Integer> getListNodesID(JSONObject json) {
        List<Integer> nodesID = new ArrayList<>();
        JSONArray querying_nodes_id = (JSONArray) json.get("querying_nodes_id");
        for (Object o : querying_nodes_id) {
            nodesID.add((int) ((long) o));
        }
        return nodesID;
    }

    /**
     * Send a message to the parent gateway
     *
     * @param msg : The message to send
     */
    public void sendMessageToGateway(String msg) {
        if (gatewayPort == -1) {
            System.err.println("Fatal Error ! Unable to send a message to the gateway. Port is undefined in this context.");
            System.exit(-1);
        }

        sendingSocket.send(msg, gatewayAddress, gatewayPort);
    }

    // --- Common Distributor Query execution -----------------------------------------------------------------------------------

    /**
     * Build the reply for QUERY_GET_MONEY
     *
     * @return The JSON string
     */
    public String execQueryGetMoney() {
        JSONObject json = new JSONObject();
        json.put("query", REPLY_GET_MONEY);
        json.put("status", REPLY_STATUS_CONNECTED);
        json.put("distributor_id", nodeID);
        json.put("distributor_money", money);
        return json.toString();
    }

    /**
     * Build the reply for QUERY_GET_STOCK
     *
     * @return The JSON string
     */
    public String execQueryGetStock() {
        JSONObject json = new JSONObject();
        json.put("query", REPLY_GET_STOCK);
        json.put("status", REPLY_STATUS_CONNECTED);
        json.put("distributor_id", nodeID);
        json.put("distributor_stock", getStockToJson());
        return json.toString();
    }

    /**
     * Build the REPLY_STATUS_BAD_REQUEST
     *
     * @return The JSON string
     */
    public String execReplyBadRequest() {
        JSONObject json = new JSONObject();
        json.put("query", REPLY_GET_STOCK);
        json.put("status", REPLY_STATUS_BAD_REQUEST);
        json.put("distributor_id", nodeID);
        return json.toString();
    }

    // --- Common Distributor functions -----------------------------------------------------------------------------------------

    /**
     * If not exists locally, ask for load the products database
     */
    public void loadProducts() {
        String fileName;
        fileName = FILENAME_PREFIX + getNodeID() + CSV_EXTENSION;
        if (isFileExist(fileName)) {
            setProductsDatabase(fileName);
        } else {
            fileName = DEFAULT_FILENAME + CSV_EXTENSION;
            if (isFileExist(fileName)) {
                setProductsDatabase(fileName);
            } else {
                try {
                    while (productsDatabase == null) {
                        System.out.println("Which file would you upload? ");
                        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                        fileName = br.readLine();
                        if (isFileExist(fileName)) {
                            setProductsDatabase(fileName);
                        } else {
                            System.err.println("File not found !");
                        }
                    }
                } catch (IOException e) {
                    System.err.println("IOException: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Load the products database
     */
    private void setProductsDatabase(String fileName) {
        printTrace(DEBUG, OUT, "Load product database `" + fileName + "`");
        List<String> lines = CSV.readFile(fileName);
        productsDatabase = new TreeMap<>();
        for (String line : lines) {
            String[] data = line.split(CSV_SEPARATOR);
            int id = Integer.parseInt(data[PRODUCT_ID]);
            float price = Float.parseFloat(data[PRODUCT_PRICE]);
            int quantity = Integer.parseInt(data[PRODUCT_QUANTITY]);
            productsDatabase.put(id, new Product(id, data[PRODUCT_NAME], price, quantity));
        }
    }

    /**
     * Choices Menu
     *
     * @throws ProductStockAlert
     * @throws TooMuchMoneyAlert
     */
    public void getChoicesMenu() throws ProductStockAlert, TooMuchMoneyAlert {
        try {
            clearScreen();
            getItemMenuHeader();
            for (Map.Entry<Integer, Product> entry : productsDatabase.entrySet()) {
                entry.getValue().getItemMenu();
            }
            getItemMenuFooter();
            System.out.println("Choose: ");
            String choice;
            do {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                choice = br.readLine();
            } while (choice.isEmpty());
            int productID = Integer.parseInt(choice);
            retrieveOneProduct(productID);
        } catch (
                NumberFormatException e) {
            System.err.println("Wrong entry ! Expected a number ...");
        } catch (
                ProductNotAvailable e) {
            System.err.println("Product isn't available !");
        } catch (
                ProductNotFound e) {
            System.err.println("Product not found !");
        } catch (
                IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }

    }

    /**
     * Get the Item menu header
     */
    private void getItemMenuHeader() {
        System.out.println(String.format("%3s |   %-48s |   %5s   |  %s", "#id", "Product name", "Price", "Q. Available"));
        System.out.println(("----+----------------------------------------------------+-----------+----------------"));
    }

    /**
     * Get the Item footer
     */
    private void getItemMenuFooter() {
        System.out.println(("----+----------------------------------------------------+-----------+----------------"));
    }

    /**
     * Retrieve one product
     *
     * @param productId : The product ID to retrieve
     * @throws ProductNotAvailable
     * @throws ProductNotFound
     * @throws ProductStockAlert
     * @throws TooMuchMoneyAlert
     */
    private void retrieveOneProduct(int productId) throws ProductNotAvailable, ProductNotFound, ProductStockAlert, TooMuchMoneyAlert {
        if (productsDatabase.containsKey(productId)) {
            Product product = productsDatabase.get(productId);
            product.retrieveOne();
            money += product.getPrice();
            if (product.getQuantity() < ALERT_STOCK_MIN) {
                throw new ProductStockAlert(buildLog("The stock is low for " + product.getName()));
            }
            if (money > ALERT_MONEY_MAX) {
                throw new TooMuchMoneyAlert(buildLog("Too much money in the Distributor"));
            }
        } else {
            throw new ProductNotFound();
        }
    }

    /**
     * Get the products stock to JSON
     *
     * @return The JSON object built
     */
    private JSONObject getStockToJson() {
        JSONObject json = new JSONObject();
        JSONArray idArray = new JSONArray();
        JSONArray quantityArray = new JSONArray();
        for (Map.Entry<Integer, Product> productEntry : productsDatabase.entrySet()) {
            Product product = productEntry.getValue();
            idArray.add(product.getId());
            quantityArray.add(product.getQuantity());
        }
        json.put("id", idArray);
        json.put("quantity", quantityArray);
        return json;
    }

    /**
     * Build the log history frame
     *
     * @param msg : The message to log
     * @return The JSON string
     */
    private String buildLog(String msg) {
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        JSONObject json = new JSONObject();
        json.put("query", ADD_TO_LOGS_HISTORY);
        json.put("status", REPLY_STATUS_CONNECTED);
        json.put("node_id", nodeID);
        json.put("log", date + " Distributor #" + nodeID + " : " + msg);
        return json.toString();
    }
    // --------------------------------------------------------------------------------------------------------------------------
}

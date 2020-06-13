package fr.tortevois.admin;

import fr.tortevois.server.IDistributorManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import static fr.tortevois.gateway.IGateway.QUERY_GET_MONEY;
import static fr.tortevois.gateway.IGateway.QUERY_GET_STOCK;
import static fr.tortevois.server.IDistributorManager.DISTRIBUTOR_MANAGER_RMI_NAME;
import static fr.tortevois.utils.Utils.*;

public class AdminInterface extends UnicastRemoteObject implements IAdminInterface {

    private final static boolean DEBUG = false;
    private Map<String, String> commandCallback;
    private Map<String, String> commandHelper;
    private IDistributorManager manager;
    private int addressCount;

    // -- Singleton -------------------------------------------------------------------------------------------------------------

    private static AdminInterface instance = null;

    static {
        try {
            instance = new AdminInterface();
        } catch (RemoteException e) {
            System.err.println("Unable to create the Admin instance");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private AdminInterface() throws RemoteException {
        // Build the internal Map
        buildCommandCallback();
        buildCommandHelper();

        // Get the distributor manager
        try {
            manager = (IDistributorManager) Naming.lookup("rmi://localhost/" + DISTRIBUTOR_MANAGER_RMI_NAME);
            addressCount = manager.getAddressCount();
        } catch (Exception e) {
            System.err.println("Unable to get the distributor manager");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static AdminInterface getInstance() {
        return instance;
    }

    // -- RMI Interface implementation ------------------------------------------------------------------------------------------

    /**
     * Bind the Admin with the DistributorManager (RMI)
     *
     * @throws RemoteException
     */
    public void bindToDistributorManager() throws RemoteException {
        manager.getAdminFromRMI();
    }

    /**
     * Print the message receive from DistributorManager to the Administrative Interface
     *
     * @param msg : the message to print
     * @throws RemoteException
     */
    public void printOnAdminInterface(String msg) throws RemoteException {
        System.out.println(msg);
    }

    /**
     * Wait for the end of the query before to continue the execution
     *
     * @throws RemoteException
     */
    public void waitForEndQuery() throws RemoteException {
        manager.waitForEndQuery();
    }

    // -- Internal Methods ------------------------------------------------------------------------------------------------------

    /**
     * Print the choices menu
     */
    public void getChoicesMenu() {
        System.out.println("Enter a command: ");
    }

    /**
     * Invoke a method by introspection following the command-line query
     *
     * @param query The command-line query
     */
    public void execQuery(String query) {
        String command = parseCommand(query);
        String[] args = parseArgs(query);

        if (commandCallback.containsKey(command)) {
            String callback = commandCallback.get(command);
            try {
                Method method = this.getClass().getMethod(callback, args.getClass());
                method.invoke(this, (Object) args);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                System.err.println("Unable to exec command " + command + " with args " + Arrays.toString(args));
                e.printStackTrace();
            }
        } else {
            System.err.println("Command not found !");
        }
    }

    /**
     * Print the Command Helper
     *
     * @param args Useless, just to simplify the invoke by introspection
     */
    public void printHelp(String[] args) {
        printTitle("Available command-line list");
        for (Map.Entry<String, String> entry : commandHelper.entrySet()) {
            System.out.println(String.format("- %-30s %s", entry.getKey(), entry.getValue()));
        }
    }

    /**
     * Request stock from all the addresses devices passed in arguments
     *
     * @param args An array of addresses passed in CLI
     */
    public void getStock(String[] args) {
        printTrace(DEBUG, OUT, "Invoke getStock with args: " + Arrays.toString(args));

        if (args.length < 1) return;

        List<Integer> nodesID = getNodesIDFromArgs(args);
        if (nodesID.size() >= 1) {
            try {
                manager.sendMessageToRMI(QUERY_GET_STOCK, nodesID);
            } catch (RemoteException e) {
                System.err.println("Unable to request stock");
                e.printStackTrace();
            }
        }
    }

    /**
     * Free all the addresses passed in arguments
     *
     * @param args An array of addresses passed in CLI
     */
    public void freeNodesID(String[] args) {
        printTrace(DEBUG, OUT, "Invoke freeNodesID with args: " + Arrays.toString(args));

        if (args.length < 1) return;

        List<Integer> nodesID = getNodesIDFromArgs(args);
        if (nodesID.size() >= 1) {
            for (Integer address : nodesID) {
                try {
                    if (manager.freeNodeID(address)) {
                        System.out.println("NodeID " + address + " is released");
                    } else {
                        System.err.println("Unknown NodeID to free: " + address);
                    }
                } catch (RemoteException e) {
                    System.err.println("Unable to free the NodeID " + address);
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Display the logs history
     *
     * @param args Useless, just to simplify the invoke by introspection
     */
    public void displayLogsHistory(String[] args) {
        printTrace(DEBUG, OUT, "Invoke displayLogsHistory with args: " + Arrays.toString(args));

        try {
            manager.displayLogsHistory();
        } catch (RemoteException e) {
            System.err.println("Unable to display the log history");
            e.printStackTrace();
        }
    }

    /**
     * Request money from all the addresses devices passed in arguments
     *
     * @param args An array of addresses passed in CLI
     */
    public void getMoney(String[] args) {
        printTrace(DEBUG, OUT, "Invoke getMoney with args: " + Arrays.toString(args));

        if (args.length < 1) return;

        List<Integer> nodesID = getNodesIDFromArgs(args);
        if (nodesID.size() >= 1) {
            try {
                manager.sendMessageToRMI(QUERY_GET_MONEY, nodesID);
            } catch (RemoteException e) {
                System.err.println("Unable to request stock");
                e.printStackTrace();
            }
        }
    }

    /**
     * Convert an array of nodesID arguments to an nodesID list
     *
     * @param args An array of nodesID passed in CLI
     * @return List of addresses
     */
    private List<Integer> getNodesIDFromArgs(String[] args) {
        List<Integer> nodesID = null;
        if (args.length == 1 && args[0].equals("all")) {
            try {
                nodesID = manager.getConnectedDistributors();
                if (nodesID.size() < 1) {
                    System.err.println("No node connected!");
                }
            } catch (RemoteException e) {
                System.err.println("Unable to get the nodesID");
                e.printStackTrace();
            }
        } else {
            nodesID = new ArrayList<>();
            for (String arg : args) {
                if (isNaN(arg)) {
                    System.err.println(arg + " is not a number");
                } else {
                    int nodeID = Integer.parseInt(arg);
                    if (nodeID > 0 && nodeID < addressCount) {
                        nodesID.add(Integer.parseInt(arg));
                    } else {
                        System.err.println("Node " + nodeID + " doesn't exist!");
                    }
                }
            }
        }
        return nodesID;
    }

    // --------------------------------------------------------------------------------------------------------------------------

    /**
     * Internal builder for the Callbacks Methods
     */
    private void buildCommandCallback() {
        if (commandCallback == null) {
            commandCallback = new TreeMap<>();
            commandCallback.put("free", "freeNodesID");
            commandCallback.put("get_money", "getMoney");
            commandCallback.put("get_stock", "getStock");
            commandCallback.put("help", "printHelp");
            commandCallback.put("logs", "displayLogsHistory");
        }
    }

    /**
     * Internal builder for the Command Helper
     */
    private void buildCommandHelper() {
        if (commandHelper == null) {
            commandHelper = new TreeMap<>();
            commandHelper.put("free all | nodesID", "Free all the RMI gateways' nodesID passed in arguments");
            commandHelper.put("get_money all | nodesID", "Get the money for all the distributors' nodesID passed in arguments");
            commandHelper.put("get_stock all | nodesID", "Get the stock for all the distributors' nodesID passed in arguments");
            commandHelper.put("help", "Print the command helper");
            commandHelper.put("logs", "Display the logs history");
        }
    }
}

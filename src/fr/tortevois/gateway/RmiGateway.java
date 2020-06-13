package fr.tortevois.gateway;

import fr.tortevois.distributor.Distributor;
import fr.tortevois.distributor.DistributorGateway;
import fr.tortevois.exception.NoNodeIDAvailable;
import fr.tortevois.server.IDistributorManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import static fr.tortevois.utils.Utils.OUT;
import static fr.tortevois.utils.Utils.printTrace;

public class RmiGateway extends UnicastRemoteObject implements IGateway, IRmiGateway {

    private final static boolean DEBUG = true;
    private final IDistributorManager manager;
    private DistributorGateway distributorGateway = null;

    /**
     * The RMI Gateway's constructor
     *
     * @param manager : The DistributorManager
     * @throws RemoteException
     */
    public RmiGateway(IDistributorManager manager) throws RemoteException {
        this.manager = manager;

        // Get the nodeID
        try {
            distributorGateway = new DistributorGateway(this, manager.getAvailableNodeID());
        } catch (NoNodeIDAvailable e) {
            System.err.println("No nodeID available");
            System.exit(-1);
        } catch (RemoteException e) {
            System.err.println("Unable to get an nodeID");
            e.printStackTrace();
            System.exit(-1);
        }

        // Set Network parameters
        setNetworkParameters();

        // Set Available NodesID
        distributorGateway.setAvailableNodesID();
    }

    // -- RMI Interface implementation ------------------------------------------------------------------------------------------

    /**
     * Read a message sent from the RMI (DistributorManager)
     *
     * @param query   : The message sent
     * @param nodesID : The address list of node
     * @throws RemoteException
     */
    public void readMessageFromRMI(String query, List<Integer> nodesID) throws RemoteException {
        printTrace(DEBUG, OUT, "RmiGateway.readMessageFromRMI: " + query + " | nodesID: " + nodesID);

        // Build message to Broadcast
        JSONObject json = new JSONObject();
        json.put("query", query);
        JSONArray queryingNodesID = new JSONArray();
        queryingNodesID.addAll(nodesID);
        json.put("querying_nodes_id", queryingNodesID);
        distributorGateway.broadcastMessage(json, query, nodesID);
    }

    // -- Gateway Interface implementation --------------------------------------------------------------------------------------

    /**
     * Get the internal Distributor built
     *
     * @return The internal Distributor built
     */
    public Distributor getDistributor() {
        return distributorGateway;
    }

    /**
     * Set the network parameters
     */
    public void setNetworkParameters() {
        try {
            distributorGateway.setNetworkParameters(manager.getNetworkParameters());
        } catch (RemoteException e) {
            System.err.println("Unable to get NetworkParameters");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Send a message to the parent gateway
     *
     * @param msg : The message to send
     */
    public void sendMessageToGateway(String msg) {
        try {
            manager.readMessageFromRMI(msg);
        } catch (RemoteException e) {
            System.err.println("RmiGateway.sendMessageToGateway RemoteException: " + e.getMessage());
        }
    }

    // -- Internal Methods ------------------------------------------------------------------------------------------------------

}
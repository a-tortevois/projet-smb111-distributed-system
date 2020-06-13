package fr.tortevois.server;

import fr.tortevois.exception.NoNodeIDAvailable;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IDistributorManager extends Remote {

    // RMI parameters
    int RMI_PORT = 1099;
    String DISTRIBUTOR_MANAGER_RMI_NAME = "manager";

    int THREAD_SLEEP_INTERVAL = 250;
    int QUERY_TIMEOUT = 15 * 1000;

    // Available methods on RMI

    /**
     * Initialize the DistributorManager
     *
     * @param devices  : The device count by gateways nodes
     * @param gateways : The gateway count by gateways nodes
     * @param depth    : The network depth
     * @throws RemoteException
     */
    void initDistributorManager(int devices, int gateways, int depth) throws RemoteException;

    /**
     * Get the Admin Interface from RMI
     *
     * @throws RemoteException
     */
    void getAdminFromRMI() throws RemoteException;

    /**
     * Get the Address count computed from network parameters
     *
     * @throws RemoteException
     */
    int getAddressCount() throws RemoteException;

    /**
     * Get the list of connected distributors
     *
     * @return The list of connected distributors
     * @throws RemoteException
     */
    List<Integer> getConnectedDistributors() throws RemoteException;

    /**
     * Get an available address
     *
     * @return An available address
     * @throws RemoteException
     * @throws NoNodeIDAvailable
     */
    int getAvailableNodeID() throws RemoteException, NoNodeIDAvailable;

    /**
     * Get the network parameters array
     *
     * @return The network parameters array
     * @throws RemoteException
     */
    int[] getNetworkParameters() throws RemoteException;

    /**
     * Send a message to an RMI node
     *
     * @param msg     : The string message to send
     * @param nodesID : The address list of node
     * @throws RemoteException
     */
    void sendMessageToRMI(String msg, List<Integer> nodesID) throws RemoteException;

    /**
     * Read a message from a RMI node
     *
     * @param msg : The JSON string message to read
     * @throws RemoteException
     */
    void readMessageFromRMI(String msg) throws RemoteException;

    /**
     * Wait for the end of the query before to continue the execution
     *
     * @throws RemoteException
     */
    void waitForEndQuery() throws RemoteException;

    /**
     * Make an address to available
     *
     * @param nodeID : The address to free
     * @return The boolean status
     * @throws RemoteException
     */
    boolean freeNodeID(int nodeID) throws RemoteException;

    /**
     * Print the logs history to the Administrative Interface
     *
     * @throws RemoteException
     */
    void displayLogsHistory() throws RemoteException;
}
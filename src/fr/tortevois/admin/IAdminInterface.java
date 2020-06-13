package fr.tortevois.admin;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAdminInterface extends Remote {
    // RMI parameters
    String ADMIN_INTERFACE_RMI_NAME = "admin";

    // Available methods on RMI

    /**
     * Print the message receive from DistributorManager to the Administrative Interface
     *
     * @param msg : the message to print
     * @throws RemoteException
     */
    void printOnAdminInterface(String msg) throws RemoteException;

    /**
     * Wait for the end of the query before to continue the execution
     *
     * @throws RemoteException
     */
    void waitForEndQuery() throws RemoteException;
}

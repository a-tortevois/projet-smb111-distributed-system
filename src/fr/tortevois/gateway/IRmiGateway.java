package fr.tortevois.gateway;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IRmiGateway extends Remote {

    // Available methods on RMI

    /**
     * Read a query sent from the RMI (DistributorManager)
     *
     * @param query   : The query sent
     * @param nodesID : The address list of node
     * @throws RemoteException
     */
    void readMessageFromRMI(String query, List<Integer> nodesID) throws RemoteException;
}
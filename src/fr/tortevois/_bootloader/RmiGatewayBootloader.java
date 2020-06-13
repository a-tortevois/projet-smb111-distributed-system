package fr.tortevois._bootloader;

import fr.tortevois.distributor.Distributor;
import fr.tortevois.gateway.RmiGateway;
import fr.tortevois.server.IDistributorManager;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;

import static fr.tortevois.utils.Utils.loading;

public class RmiGatewayBootloader {

    public static void main(String[] args) {

        // Get the distributor manager from RMI
        IDistributorManager manager = null;
        try {
            manager = (IDistributorManager) Naming.lookup("rmi://localhost/" + IDistributorManager.DISTRIBUTOR_MANAGER_RMI_NAME);
        } catch (Exception e) {
            System.err.println("Unable to get the DistributorManager");
            e.printStackTrace();
            System.exit(-1);
        }

        // Create a new gateway
        RmiGateway gateway = null;
        try {
            gateway = new RmiGateway(manager);
        } catch (RemoteException e) {
            System.err.println("Unable to create the gateway");
            e.printStackTrace();
            System.exit(-1);
        }

        // Get the distributor's nodeID and bind the gateway to RMI
        Distributor distributor = gateway.getDistributor();
        String name = "gateway_" + distributor.getNodeID();
        try {
            Naming.rebind(name, gateway);
        } catch (RemoteException e) {
            System.err.println("Unable to bind the gateway");
            e.printStackTrace();
            System.exit(-1);
        } catch (MalformedURLException e) {
            System.err.println("Gateway URL error");
            e.printStackTrace();
            System.exit(-1);
        }

        // Load the csv file of products database
        distributor.loadProducts();

        // A funny loading progress bar
        loading();

        // Main run loop
        // Print the choices menu
        while (true) {
            try {
                distributor.getChoicesMenu();
            } catch (Exception e) {
                // Catch ProductStockAlert or TooMuchMoneyAlert and send it to the RMI
                gateway.sendMessageToGateway(e.getMessage());
            }
        }
    }
}

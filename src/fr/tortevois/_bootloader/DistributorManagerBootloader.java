package fr.tortevois._bootloader;

import fr.tortevois.server.DistributorManager;
import fr.tortevois.server.IDistributorManager;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

import static fr.tortevois.server.IDistributorManager.DISTRIBUTOR_MANAGER_RMI_NAME;
import static fr.tortevois.server.IDistributorManager.RMI_PORT;
import static fr.tortevois.utils.Utils.*;

public class DistributorManagerBootloader {

    private final static boolean DEBUG = true;

    public static void main(String[] args) {

        // Check the arguments
        if (args.length != 3) {
            System.err.println("Too few arguments!");
            usage();
        }

        if (isNaN(args[0])) {
            System.err.println("`devices_count` is not a number");
            usage();
        }
        int devices = Integer.parseInt(args[0]);
        if (devices < 0) {
            System.err.println("`devices_count` should be upper than 0 !");
            usage();
        }

        if (isNaN(args[1])) {
            System.err.println("`gateways_count` is not a number");
            usage();
        }
        int gateways = Integer.parseInt(args[1]);
        if (gateways < 0) {
            System.err.println("`gateways_count` should be upper than 0 !");
            usage();
        }

        if (isNaN(args[2])) {
            System.err.println("`tree_depth` is not a number");
            usage();
        }
        int depth = Integer.parseInt(args[2]);
        if (depth < 0) {
            System.err.println("`tree_depth` should be upper than 0 !");
            usage();
        }

        // Start the RMI
        try {
            LocateRegistry.createRegistry(RMI_PORT);
        } catch (RemoteException e) {
            System.err.println("Unable to get the registry: " + e.getMessage());
            System.exit(-1);
        }

        System.out.println("##################################################");
        System.out.println("#  Start the DistributorManager");
        System.out.println("#  The RMI Server is now available on port " + RMI_PORT);
        System.out.println("##################################################");
        System.out.println();

        // Get the DistributorManager Singleton and initialize it
        IDistributorManager distributorManager = null;
        try {
            distributorManager = DistributorManager.getInstance();
            distributorManager.initDistributorManager(devices, gateways, depth);
        } catch (RemoteException e) {
            System.err.println("Unable to create the DistributorManager: " + e.getMessage());
            System.exit(-1);
        }

        // Bind DistributorManager with RMI
        try {
            Naming.rebind(DISTRIBUTOR_MANAGER_RMI_NAME, distributorManager);
            printTrace(DEBUG, OUT, "DistributorManager rmi://localhost/" + DISTRIBUTOR_MANAGER_RMI_NAME + " ready");
        } catch (RemoteException e) {
            System.err.println("Unable to bind the DistributorManager");
            e.printStackTrace();
            System.exit(-1);
        } catch (MalformedURLException e) {
            System.err.println("DistributorManager URL error");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void usage() {
        System.err.println("Usage: java StartServer devices_count gateways_count tree_depth");
        System.exit(-1);
    }
}

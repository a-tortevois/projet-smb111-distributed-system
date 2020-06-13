package fr.tortevois._bootloader;

import fr.tortevois.admin.AdminInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;

import static fr.tortevois.admin.IAdminInterface.ADMIN_INTERFACE_RMI_NAME;
import static fr.tortevois.utils.Utils.OUT;
import static fr.tortevois.utils.Utils.printTrace;

public class AdminInterfaceBootloader {

    private final static boolean DEBUG = true;

    public static void main(String[] args) {
        System.out.println("##################################################");
        System.out.println("#  Start Administrative interface");
        System.out.println("##################################################");
        System.out.println();

        // Get the Admin Singleton
        AdminInterface adminInterface = AdminInterface.getInstance();

        // Bind Admin with RMI
        try {
            Naming.rebind(ADMIN_INTERFACE_RMI_NAME, adminInterface);
            printTrace(DEBUG, OUT, "AdminInterface rmi://localhost/" + ADMIN_INTERFACE_RMI_NAME + " ready");
        } catch (RemoteException e) {
            System.err.println("Unable to bind the AdminInterface");
            e.printStackTrace();
            System.exit(-1);
        } catch (MalformedURLException e) {
            System.err.println("AdminInterface URL error");
            e.printStackTrace();
            System.exit(-1);
        }

        // Bind Admin with the DistributorManager
        try {
            adminInterface.bindToDistributorManager();
            printTrace(DEBUG, OUT, "AdminInterface is now bound to the DistributorManager");
        } catch (RemoteException e) {
            System.err.println("Unable to bind AdminInterface with the DistributorManager");
            e.printStackTrace();
            System.exit(-1);
        }

        System.out.println("Welcome to the command line administrative interface.");
        System.out.println("Enter \"help\" to get the available command-list.");

        // Loop Menu
        while (true) {
            try {
                // Get the query
                String query;
                do {
                    adminInterface.getChoicesMenu();
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    query = br.readLine();
                } while (query.isEmpty());
                // Execute the query
                adminInterface.execQuery(query);
                adminInterface.waitForEndQuery();
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }
        }
    }
}
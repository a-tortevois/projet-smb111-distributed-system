package fr.tortevois._bootloader;

import fr.tortevois.distributor.Distributor;
import fr.tortevois.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static fr.tortevois.utils.Utils.*;
import static fr.tortevois.zigbee.ZigBee.TYPE_DEVICE;

public class DistributorBootloader {

    private final static boolean DEBUG = true;

    public static void main(String[] args) {
        InetAddress address = null;
        int port = 0;

        if (args.length < 2) {
            try {
                address = InetAddress.getByName(null);
            } catch (UnknownHostException e) {
                System.err.println("Unable to get the InetAddress.");
                e.printStackTrace();
                System.exit(-1);
            }
            String input = null;
            try {

                do {
                    System.out.println("With which port would you want to connect?");
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    input = br.readLine();
                } while (input.isEmpty());
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
                System.exit(-1);
            }
            if (Utils.isNaN(input)) {
                System.err.println("`port` is not a number");
                usage();
            }
            port = Integer.parseInt(input);
        } else if (args.length == 2) {
            try {
                address = InetAddress.getByName(args[0]);
            } catch (UnknownHostException e) {
                System.err.println("Unable to get the InetAddress.");
                e.printStackTrace();
                System.exit(-1);
            }

            if (Utils.isNaN(args[1])) {
                System.err.println("`port` is not a number");
                usage();
            }
            port = Integer.parseInt(args[1]);
        } else {
            System.err.println("Unable to start the Gateway: bad arguments");
            System.exit(-1);
        }

        if (address != null && port > 1024) {
            printTrace(DEBUG, OUT, "Start the Distributor " + address + ":" + port);
            // Create a new gateway and get the distributor
            Distributor distributor = new Distributor(address, port, TYPE_DEVICE);

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
                    distributor.sendMessageToGateway(e.getMessage());
                }
            }
        } else {
            System.err.println("Unable to start the Distributor " + address + ":" + port);
            System.exit(-1);
        }
    }

    private static void usage() {
        System.err.println("Usage: java Distributor_boot IPv4_address port");
        System.exit(-1);
    }

}

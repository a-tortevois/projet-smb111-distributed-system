package fr.tortevois.socket;

import fr.tortevois.distributor.Distributor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import static fr.tortevois.gateway.IGateway.BUFFER_MAX_SIZE;
import static fr.tortevois.utils.Utils.OUT;
import static fr.tortevois.utils.Utils.printTrace;

public class ListeningSocket implements Runnable {

    private final static boolean DEBUG = false;
    private final Distributor distributor;
    private DatagramSocket socket = null;
    private boolean isRunning = true;

    /**
     * ListeningSocket's constructor : open a DatagramSocket
     * @param distributor : Distributor socket owner
     * @param port : Listening port to open
     */
    public ListeningSocket(Distributor distributor, int port) {
        this.distributor = distributor;
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("Unable to create the socket");
            e.printStackTrace();
            System.exit(-1);
        }
        printTrace(DEBUG, OUT, "UDP Socket Server is now available on port: " + port);
    }

    /**
     * Blocking reading loop
     */
    public void run() {
        do {
            // Create the buffer for the message
            byte[] buffer = new byte[BUFFER_MAX_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // Read message from client
            try {
                socket.receive(packet); // receive is blocking
                // InetAddress address = packet.getAddress();
                // int port = packet.getPort();
                String msg = new String(packet.getData(), 0, packet.getLength());
                // printTrace(DEBUG, LEVEL_OUT, "ServerSocket.Read [from: " + address + ":" + port + "] msg:" + msg);
                printTrace(DEBUG, OUT, "ServerSocket.Read:" + msg);
                // Call the distributor to process the received message
                distributor.messageProcessing(msg);
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }
        } while (isRunning);
    }

    /**
     * Close the socket
     */
    public void close() {
        isRunning = false;
        socket.close();
    }
}

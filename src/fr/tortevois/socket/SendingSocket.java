package fr.tortevois.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import static fr.tortevois.utils.Utils.OUT;
import static fr.tortevois.utils.Utils.printTrace;

public class SendingSocket {

    private final static boolean DEBUG = false;
    private DatagramSocket socket;

    /**
     * SendingSocket's constructor : open a DatagramSocket
     */
    public SendingSocket() {
        // Create the socket
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            System.err.println("Unable to create the socket: " + e.getMessage());
            System.exit(-1);
        }
    }

    /**
     * Send a message at the specified address, port through the Socket
     *
     * @param msg : The string message to send
     * @param address : Destination IP address
     * @param port : Destination port
     */
    public void send(String msg, InetAddress address, int port) {
        // Create the buffered message
        byte[] buffer = msg.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);

        // Send the buffered message
        try {
            socket.send(packet);
            printTrace(DEBUG, OUT, "ClientSocket.send: " + msg);
        } catch (IOException e) {
            System.err.println("Unable to send the message: " + e.getMessage());
        }
    }

    /**
     * Close the socket
     */
    public void close() {
        socket.close();
    }
}

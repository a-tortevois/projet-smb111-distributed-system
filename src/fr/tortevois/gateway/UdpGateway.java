package fr.tortevois.gateway;

import fr.tortevois.distributor.Distributor;
import fr.tortevois.distributor.DistributorGateway;
import org.json.simple.JSONObject;

import java.net.InetAddress;

import static fr.tortevois.zigbee.ZigBee.TYPE_GATEWAY;

public class UdpGateway implements IGateway {

    private final static boolean DEBUG = true;
    private final DistributorGateway distributorGateway;

    /**
     * The UDP Gateway's constructor
     *
     * @param gatewayAddress : The parent gateway address
     * @param gatewayPort    : The parent gateway port
     */
    public UdpGateway(InetAddress gatewayAddress, int gatewayPort) {
        // Create the Gateway
        distributorGateway = new DistributorGateway(this, gatewayAddress, gatewayPort, TYPE_GATEWAY);

        // Set Network Parameters
        setNetworkParameters();

        // Set Available Address
        distributorGateway.setAvailableNodesID();

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

        // Build the query message
        JSONObject json = new JSONObject();
        json.put("query", QUERY_NETWORK_PARAMETERS);
        json.put("node_id", distributorGateway.getNodeID());
        // Send the query
        sendMessageToGateway(json.toString());

        // Wait the reply
        System.out.println("Waiting network parameters");
        while (distributorGateway.getNetworkParameters() == null) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                // e.printStackTrace();
            }
        }
    }

    /**
     * Send a message to the parent gateway
     *
     * @param msg : The message to send
     */
    public void sendMessageToGateway(String msg) {
        distributorGateway.sendMessageToGateway(msg);
    }

    // --------------------------------------------------------------------------------------------------------------------------
}

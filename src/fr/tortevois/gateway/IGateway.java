package fr.tortevois.gateway;

import fr.tortevois.distributor.Distributor;

public interface IGateway {

    //
    int ALERT_STOCK_MIN = 2;
    int ALERT_MONEY_MAX = 50;

    // Public constants
    int BUFFER_MAX_SIZE = 1024 * 8; // 8ko

    int REPLY_STATUS_CONNECTED = 200;
    int REPLY_STATUS_BAD_REQUEST = 400;
    int REPLY_STATUS_NOT_CONNECTED = 404;

    String CURRENCY = "€";
    String ADD_TO_LOGS_HISTORY = "add_to_logs_history";

    // Public query
    String QUERY_GET_NODE_ID = "query_get_node_id";
    String QUERY_NETWORK_PARAMETERS = "query_network_parameters";
    String QUERY_GET_STOCK = "query_get_stock";
    String QUERY_GET_MONEY = "query_get_money";

    // Public reply
    String REPLY_GET_NODE_ID = "reply_get_node_id";
    String REPLY_NEW_NODE_CONNEXION = "new_node_connexion";
    String REPLY_NETWORK_PARAMETERS = "reply_network_parameters";
    String REPLY_GET_STOCK = "reply_get_stock";
    String REPLY_GET_MONEY = "reply_get_money";

    /**
     * Get the internal Distributor built
     *
     * @return The internal Distributor built
     */
    Distributor getDistributor();

    /**
     * Set the network parameters
     */
    void setNetworkParameters();

    /**
     * Send a message to the parent gateway
     *
     * @param msg : The message to send
     */
    void sendMessageToGateway(String msg);
}

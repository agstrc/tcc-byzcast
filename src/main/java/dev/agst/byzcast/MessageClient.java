package dev.agst.byzcast;

import java.util.Random;
import java.util.logging.Logger;

import bftsmart.tom.ServiceProxy;

public class MessageClient {
    private ServiceProxy serviceProxy;
    private Logger logger = Logger.getLogger(MessageClient.class.getName());

    MessageClient() {
        var random = new Random();
        this.serviceProxy = new ServiceProxy(random.nextInt(Integer.MAX_VALUE));
    }

    public void send(Message message) {
        byte[] response = this.serviceProxy.invokeOrdered(message.toBytes());
        logger.info("Response: " + new String(response));
    }
}

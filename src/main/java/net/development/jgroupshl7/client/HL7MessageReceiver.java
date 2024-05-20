package net.development.jgroupshl7.client;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HL7MessageReceiver implements Receiver {
    private static final Logger logger = LoggerFactory.getLogger(HL7MessageReceiver.class);
    private Address localAddress;

    // No-argument constructor
    public HL7MessageReceiver() {
    }

    public void setLocalAddress(Address localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    public void receive(Message msg) {
        // Log message sender and local address
        logger.info("Local address is set to: {}", localAddress);

        // Ignore the message if it was sent by the local node
        if (msg.getSrc().equals(localAddress)) {
            logger.info("***Ignoring message sent by the local node.");
            return;
        }

        try {
            String hl7Message = msg.getObject();
            // Get the sender address
            String senderAddress = msg.getSrc().toString();
            logger.info("=**=>Received Forwarded HL7 message from JGroups member::[{}]\n{}", senderAddress, hl7Message);
            // Handle the received HL7 message
        } catch (Exception e) {
            logger.error("Error receiving HL7 message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void viewAccepted(View view) {
        logger.info("*** Received view: {}", view);
    }
}

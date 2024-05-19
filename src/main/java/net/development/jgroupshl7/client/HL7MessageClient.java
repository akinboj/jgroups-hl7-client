package net.development.jgroupshl7.client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.jgroups.JChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HL7MessageClient {
    private static final Logger logger = LoggerFactory.getLogger(HL7MessageClient.class);
    private static final String JGROUPS_CLUSTER_NAME = "HL7Cluster";
    private static final String JGROUPS_CONFIG_FILE = "tcp.xml";
    private static final String REMOTE_HL7_SERVER_HOST = "host.docker.internal";
    private static final int REMOTE_HL7_SERVER_PORT = 2100;
    private static final int LOCAL_HL7_SERVER_PORT = 3200;

    private JChannel channel;
    private ServerSocket localServerSocket;

    public void start() throws Exception {
        HL7MessageReceiver hl7receiver = new HL7MessageReceiver();
        channel = new JChannel(JGROUPS_CONFIG_FILE);
        channel.setReceiver(hl7receiver);
        channel.connect(JGROUPS_CLUSTER_NAME);

        hl7receiver.receiveMessages();

        // Start the local HL7 server
        startLocalHL7Server();

        channel.close();
        localServerSocket.close();
    }

    private void startLocalHL7Server() {
        try {
            localServerSocket = new ServerSocket(LOCAL_HL7_SERVER_PORT);
            logger.info("Local HL7 server started on port {}", LOCAL_HL7_SERVER_PORT);

            // Wait for incoming HL7 messages and send ACK, then forward to remote server
            while (true) {
                try (Socket clientSocket = localServerSocket.accept()) {
                    String hl7Message = MLLPAdapter.receiveHL7Message(clientSocket);
                    logger.info("=**=>Incoming HL7 message: {}", hl7Message);
                    MLLPAdapter.sendACK(clientSocket, hl7Message);
                    sendHL7MessageToRemoteServer(hl7Message);
                } catch (IOException e) {
                    logger.error("Error handling HL7 message: {}", e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            logger.error("Error starting local HL7 server: {}", e.getMessage(), e);
        }
    }

    private void sendHL7MessageToRemoteServer(String hl7Message) {
        try (Socket serverSocket = new Socket(REMOTE_HL7_SERVER_HOST, REMOTE_HL7_SERVER_PORT)) {
            String mllpMessage = MLLPAdapter.generateMLLPMessage(hl7Message);
            MLLPAdapter.sendHL7Message(serverSocket, mllpMessage);
            MLLPAdapter.receiveACK(serverSocket);
        } catch (IOException e) {
            logger.error("Error sending HL7 message to remote server: {}", e.getMessage(), e);
        }
    }
}

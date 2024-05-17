package net.development.jgroupshl7.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import org.jgroups.JChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HL7MessageClient {
    private static final Logger logger = LoggerFactory.getLogger(HL7MessageClient.class);
    private static final String JGROUPS_CLUSTER_NAME = "HL7Cluster";
    private static final String JGROUPS_CONFIG_FILE = "tcp.xml";
    private static final String REMOTE_HL7_SERVER_HOST = "192.168.0.17";
    private static final int REMOTE_HL7_SERVER_PORT = 2100;

    private JChannel channel;

    public void start() throws Exception {
        HL7MessageReceiver hl7receiver = new HL7MessageReceiver();
        channel = new JChannel(JGROUPS_CONFIG_FILE);
        channel.setReceiver(hl7receiver);
        channel.connect(JGROUPS_CLUSTER_NAME);

        hl7receiver.receiveMessages();

        // Prompt the user to enter an HL7 message
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("=**=>Send HL7 message (or 'exit' to quit): ");
        String hl7Message = reader.readLine();

        while (!hl7Message.equalsIgnoreCase("exit")) {
            sendHL7MessageToServer(hl7Message);
            System.out.print("=**=>Send HL7 message (or 'exit' to quit): ");
            hl7Message = reader.readLine();
        }

        channel.close();
    }

    private void sendHL7MessageToServer(String hl7Message) {
        try (Socket serverSocket = new Socket(REMOTE_HL7_SERVER_HOST, REMOTE_HL7_SERVER_PORT)) {
            MLLPAdapter.sendHL7Message(serverSocket, hl7Message);
            MLLPAdapter.receiveACK(serverSocket);
        } catch (IOException e) {
            logger.error("Error sending HL7 message to server: {}", e.getMessage(), e);
        }
    }
}

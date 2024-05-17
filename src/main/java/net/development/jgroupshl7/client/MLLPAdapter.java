package net.development.jgroupshl7.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MLLPAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MLLPAdapter.class);

    public static void sendHL7Message(Socket serverSocket, String hl7Message) {
        try {
            int timeout = 5000; // 5 seconds
            serverSocket.setSoTimeout(timeout);

            // Construct the MLLP-wrapped HL7 message
            String mllpMessage = generateMLLPMessage(hl7Message);

            // Write the MLLP-wrapped HL7 message to the server socket
            OutputStream out = serverSocket.getOutputStream();
            out.write(mllpMessage.getBytes());
            out.flush();
            logger.info("=**=>Outgoing HL7 message to server: {}", mllpMessage);
        } catch (SocketTimeoutException e) {
            logger.error("Timeout occurred while sending HL7 message: {}", e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Error sending HL7 message: {}", e.getMessage(), e);
        }
    }

    public static void receiveACK(Socket serverSocket) {
        try {
            int timeout = 5000; // 5 seconds
            serverSocket.setSoTimeout(timeout);

            // Read the MLLP-wrapped ACK message from the server socket
            BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
            StringBuilder ackMessageBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                ackMessageBuilder.append(line).append("\n");
                if (line.equals("" + (char) 28)) {
                    break;
                }
            }
            String ackMessage = ackMessageBuilder.toString();
            logger.info("=**=>Received ACK message from server: {}", ackMessage);
        } catch (SocketTimeoutException e) {
            logger.error("Timeout occurred while receiving ACK message: {}", e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Error receiving ACK message: {}", e.getMessage(), e);
        }
    }

    private static String generateMLLPMessage(String hl7Message) {
        StringBuilder mllpMessage = new StringBuilder();
        mllpMessage.append((char) 0x0B); // Start of block
        mllpMessage.append(hl7Message);
        mllpMessage.append((char) 0x1C); // End of block
        mllpMessage.append((char) 0x0D); // Carriage return
        return mllpMessage.toString();
    }

}

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
            logger.info("=**=>Outgoing HL7 message: {}", mllpMessage);
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
            logger.info("=**=>Acknowledgement message received:: {}", ackMessage);
        } catch (SocketTimeoutException e) {
            logger.error("Timeout occurred while receiving ACK message: {}", e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Error receiving ACK message: {}", e.getMessage(), e);
        }
    }

    public static void sendACK(Socket clientSocket, String hl7Message) {
        try {
            String ackMessage = generateACKMessage(hl7Message);
            OutputStream out = clientSocket.getOutputStream();
            out.write(ackMessage.getBytes());
            out.flush();
            logger.info("=**=>Sending acknowledgement message:: {}", ackMessage);
        } catch (IOException e) {
            logger.error("Error sending ACK message: {}", e.getMessage(), e);
        }
    }

    public static String receiveHL7Message(Socket clientSocket) {
        try {
            int timeout = 5000; // 5 seconds
            clientSocket.setSoTimeout(timeout);

            // Read the MLLP-wrapped HL7 message from the client socket
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            StringBuilder hl7MessageBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                hl7MessageBuilder.append(line).append("\n");
                if (line.equals("" + (char) 28)) {
                    break;
                }
            }
            return hl7MessageBuilder.toString();
        } catch (SocketTimeoutException e) {
            logger.error("Timeout occurred while receiving HL7 message: {}", e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Error receiving HL7 message: {}", e.getMessage(), e);
        }
        return null;
    }

    public static String generateMLLPMessage(String hl7Message) {
        StringBuilder mllpMessage = new StringBuilder();
        mllpMessage.append((char) 0x0B); // Start of block
        mllpMessage.append(hl7Message).append("\r");
        mllpMessage.append((char) 0x1C); // End of block
        mllpMessage.append((char) 0x0D); // Carriage return
        return mllpMessage.toString();
    }

    private static String generateACKMessage(String hl7Message) {
    	DateFormatter hl7DateTime = new DateFormatter();
        String mshSegment = hl7Message.substring(0, hl7Message.indexOf("\n"));
        String[] mshFields = mshSegment.split("\\|");

        StringBuilder ackMessage = new StringBuilder();
        ackMessage.append((char) 0x0B); // Start of block
        ackMessage.append("MSH|^~\\&|").append(mshFields[3]).append("|").append(mshFields[4])
                   .append("|").append(mshFields[5]).append("||").append(hl7DateTime.hl7AckTimeFormat())
                   .append("||ACK^").append(mshFields[9]).append("|").append(mshFields[10])
                   .append("|P|2.3|\r")
                   .append("MSA|AA|").append(mshFields[10]).append("\r");
        ackMessage.append((char) 0x1C); // End of block
        ackMessage.append((char) 0x0D); // Carriage return
        return ackMessage.toString();
    }
}

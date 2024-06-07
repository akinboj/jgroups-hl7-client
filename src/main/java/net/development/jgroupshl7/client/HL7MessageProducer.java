package net.development.jgroupshl7.client;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.component.mllp.MllpComponent;
import org.apache.camel.component.mllp.MllpConstants;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HL7MessageProducer {
    private static final Logger logger = LoggerFactory.getLogger(HL7MessageProducer.class);
    private static final String JGROUPS_CLUSTER_NAME = "HL7Cluster";
    private static final String JGROUPS_CONFIG_FILE = "kube.xml";
    private static final String REMOTE_HL7_SERVER_HOST = "host.docker.internal";
    private static final int REMOTE_HL7_SERVER_PORT = 2100;

    private JChannel channel;
    private CamelContext camelContext;

    public void start() throws Exception {
        HL7MessageReceiver hl7Receiver = new HL7MessageReceiver() {
            @Override
            public void receive(Message msg) {
                super.receive(msg);

                // Handle received HL7 message
                try {
                    Object obj = msg.getObject();
                    if (obj instanceof String) {
                        String hl7Message = (String) obj;
                        sendHL7Message(hl7Message);
                    } else {
                        logger.error("Received an unsupported message type: {}", obj.getClass().getName());
                    }
                } catch (Exception e) {
                    logger.error("Error receiving HL7 message: {}", e.getMessage(), e);
                }
            }
        };

        // Initialize JGroups channel
        channel = new JChannel(JGROUPS_CONFIG_FILE);
        channel.setReceiver(hl7Receiver);
        channel.connect(JGROUPS_CLUSTER_NAME);

        // Set the local address in the receiver after connecting the channel
        hl7Receiver.setLocalAddress(channel.getAddress());

        // Initialize Camel context and routes
        camelContext = initCamelContext();

        // Start Camel context
        startCamelContext();

        // Add shutdown hook to close resources properly
        addShutdownHook();
    }

    private CamelContext initCamelContext() throws Exception {
        CamelContext context = new DefaultCamelContext();

        // Configure MLLP component
        MllpComponent mllpComponent = new MllpComponent();
        context.addComponent("mllp", mllpComponent);

        // Add routes
        context.addRoutes(createRouteBuilder());

        return context;
    }

    private void startCamelContext() throws Exception {
        camelContext.start();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                camelContext.stop();
                channel.close();
            } catch (Exception e) {
                logger.error("Error during shutdown: {}", e.getMessage(), e);
            }
        }));
    }

    private RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Route to send HL7 messages to remote HL7 server
                from("direct:sendToRemoteHL7Server")
                    .process(exchange -> {
                        String hl7Message = exchange.getIn().getBody(String.class);
                        logger.info("<=**=Outgoing HL7 message:\n{}", hl7Message);
                    })
                    .to("mllp://" + REMOTE_HL7_SERVER_HOST + ":" + REMOTE_HL7_SERVER_PORT)
                    .process(exchange -> {
                    	String ackMessage = exchange.getIn().getHeader(MllpConstants.MLLP_ACKNOWLEDGEMENT_STRING, String.class);
                        logger.info("=**=>Received ACK message:\n{}", ackMessage);
                    });
            }
        };
    }

    private void sendHL7Message(String hl7Message) {
        try {
            // Send HL7 message to the remote HL7 server via Camel route
            camelContext.createProducerTemplate().sendBody("direct:sendToRemoteHL7Server", hl7Message);
        } catch (Exception e) {
            logger.error("Error sending HL7 message to remote server: {}", e.getMessage(), e);
        }
    }
}

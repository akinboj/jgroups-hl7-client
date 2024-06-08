package net.development.jgroupshl7.client;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mllp.MllpComponent;
import org.apache.camel.component.mllp.MllpConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.jgroups.JChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HL7MessageProducer {
    private static final Logger logger = LoggerFactory.getLogger(HL7MessageProducer.class);
    private static final String REMOTE_HL7_SERVER_HOST = "host.docker.internal";
    private static final int REMOTE_HL7_SERVER_PORT = 2100;
    private static final String RABBITMQ_QUEUE_NAME = "hl7-messages";
    private static final String JGROUPS_CLUSTER_NAME = "HL7Cluster";
    private static final String JGROUPS_CONFIG_FILE = "kube.xml";
    private static final String RABBITMQ_HOST = System.getenv("RABBITMQ_HOST");
    private static final String RABBITMQ_PORT = System.getenv("RABBITMQ_PORT");
    private static final String RABBITMQ_USERNAME = System.getenv("RABBITMQ_USERNAME");
    private static final String RABBITMQ_PASSWORD = System.getenv("RABBITMQ_PASSWORD");
    private static final String RABBITMQ_URI = "rabbitmq://" + RABBITMQ_HOST + ":" + RABBITMQ_PORT + "/" + RABBITMQ_QUEUE_NAME
            + "?username=" + RABBITMQ_USERNAME
            + "&password=" + RABBITMQ_PASSWORD
            + "&queue=" + RABBITMQ_QUEUE_NAME
            + "&autoDelete=false"
            + "&durable=true"
            + "&declare=true";

    private JChannel channel;
    private CamelContext camelContext;

    public void start() throws Exception {
        HL7MessageReceiver hl7Receiver = new HL7MessageReceiver();

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
                // Route to consume HL7 messages from RabbitMQ and send to remote HL7 server
                from(RABBITMQ_URI)
                    .log("=*=*>Received HL7 message from message queue: ${body}")
                    .to("mllp://" + REMOTE_HL7_SERVER_HOST + ":" + REMOTE_HL7_SERVER_PORT)
                    .process(exchange -> {
                        String ackMessage = exchange.getIn().getHeader(MllpConstants.MLLP_ACKNOWLEDGEMENT_STRING, String.class);
                        logger.info("=**=>Received ACK message:\n{}", ackMessage);
                    });
            }
        };
    }

}

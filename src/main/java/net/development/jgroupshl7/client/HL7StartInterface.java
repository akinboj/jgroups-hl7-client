package net.development.jgroupshl7.client;

public class HL7StartInterface {
	
	public static void main(String[] args) throws Exception {
    	System.setProperty("jgroups.bind_addr", "192.168.0.17");
    	System.setProperty("jgroups.tcpping.initial_hosts", "192.168.0.17[7800],192.168.0.17[7801]");
        
    	HL7MessageClient cluster = new HL7MessageClient();
    	cluster.start();
    }

}

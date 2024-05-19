package net.development.jgroupshl7.client;

public class HL7StartInterface {
	
	public static void main(String[] args) throws Exception {
		String podIP = System.getenv("MY_POD_IP");
		
    	System.setProperty("jgroups.bind_addr", podIP);
    	System.setProperty("jgroups.tcpping.initial_hosts", podIP + "[7800]," + podIP + "[7801]");
        
    	HL7MessageClient cluster = new HL7MessageClient();
    	cluster.start();
    }

}
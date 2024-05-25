package net.development.jgroupshl7.client;

public class HL7StartInterface {
	
	public static void main(String[] args) throws Exception {
		// String label = System.getenv("KUBERNETES_SERVICE_NAME");
		
    	// System.setProperty("KUBERNETES_LABELS", "app="+label);
        
    	HL7MessageClient cluster = new HL7MessageClient();
    	cluster.start();
    }

}
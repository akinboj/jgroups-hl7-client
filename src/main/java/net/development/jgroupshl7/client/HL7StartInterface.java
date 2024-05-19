package net.development.jgroupshl7.client;

public class HL7StartInterface {
	
	public static void main(String[] args) throws Exception {
		String podIP = System.getenv("MY_POD_IP");
		String namespace = System.getenv("KUBERNETES_NAMESPACE");
		String label = System.getenv("KUBERNETES_SERVICE_NAME");
		
    	System.setProperty("jgroups.bind_addr", podIP);
    	System.setProperty("jgroups.tcpping.initial_hosts", podIP + "[7800]," + podIP + "[7801]");
    	System.setProperty("KUBE_NAMESPACE", namespace);
    	System.setProperty("KUBE_LABEL", "app="+label);
        
    	HL7MessageClient cluster = new HL7MessageClient();
    	cluster.start();
    }

}
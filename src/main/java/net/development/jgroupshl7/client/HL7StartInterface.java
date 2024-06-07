package net.development.jgroupshl7.client;

public class HL7StartInterface {
	
	public static void main(String[] args) throws Exception {
		        
		HL7MessageProducer producer = new HL7MessageProducer();
		producer.start();
    }

}
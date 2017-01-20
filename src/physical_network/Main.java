/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2016)
 *  
 *  YOU SHOULD NOT NEED TO MODIFY THIS CLASS TO MAKE YOUR SYSTEM WORK.
 *  (Since you will only submit on Moodle two files: DataFrame.java and NetworkCard.java)
 *  
 *  Although you SHOULD modify this class to see if your ARQ protocol is working when
 *  noise is added to the wire (see below for how to do this).
 *  Also you may wish to modify it to test other features of the system such as its
 *  ability to transmit sentinel and escape characters as data values in the payload.
 *  
 */
package physical_network;

/**
 * 
 * This is a test which joins two network cards together with a wire pair.
 * It then sends a data frame across the network from Network Card 1 to
 * Network Card 2.
 * 
 * An oscilloscope is also connected to the wire to allow the voltage levels
 * to be monitored over time.
 * 
 * A source for thermal noise can also be connected to the wire which simulates
 * noise on the network to see how robust the transmission process is to noise.
 * 
 * @author kevin-b
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {

//        Integer.toHexString(10);
//        int i = (char)0xA;
//        System.out.printf(String.valueOf(i));


    	// Shared twisted pair wire.
        TwistedWirePair wire = new MyTwistedWirePair();

        // Set network card 1 running connected to the shared wire.
        NetworkCard networkCard1 = new NetworkCard(1, wire);
        networkCard1.init();

        // Set network card 2 running with a simple data frame listener registered.
        NetworkCard networkCard2 = new NetworkCard(2, wire);
        networkCard2.init();

        // Currently noise level is set to 0.0 volts on wire (the 0.0 value).
        // Try increasing it to 3.5 volts to see if the transmission is reliable.
        ThermalNoise thermalNoise = new ThermalNoise("Thermal Noise", 0.0, wire);
        thermalNoise.start();

        // Set oscilloscope monitoring the wire voltage.
        Oscilloscope oscilloscope = new Oscilloscope("Oscilloscope", wire);
        oscilloscope.start();

        // Send a data frame across the link from network card 1 to network card 2.
        DataFrame myMessage = new DataFrame("Hello World", 2);
        System.out.println("\n *** SENDING DATA FRAME: " + myMessage + "\n");
        networkCard1.send(myMessage);

        myMessage = new DataFrame("Earth calling Mars", 2);
        System.out.println("\n *** SENDING DATA FRAME: " + myMessage + "\n");
        networkCard1.send(myMessage);

        myMessage = new DataFrame("Hello Mars", 2);
        System.out.println("\n *** SENDING DATA FRAME: " + myMessage + "\n");
        networkCard1.send(myMessage);

        // Continuously read data frames received by network card 2.
        while (true) {

        	DataFrame receivedData = networkCard2.receive();
        	System.out.println("\n *** RECEIVED: " + receivedData + "\n");

        }
        
    }
}

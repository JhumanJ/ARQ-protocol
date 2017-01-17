/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2016)
 *  
 *  YOU SHOULD NOT NEED TO MODIFY THIS CLASS.
 *  (You will only submit on Moodle two files: DataFrame.java and NetworkCard.java)
 */

package physical_network;

import java.util.HashMap;

/**
 * 
 * Concrete implementation of the Twisted Wire Pair.
 *
 * This implementation will simply ADD TOGETHER all current voltages set
 * by different devices attached to the wire.
 * 
 * Thus you may have "Network Card A" device setting voltages to transfer bits
 * across the wire and at the same time a "Thermal Noise" device which
 * is setting random voltages on the wire. These voltages should then
 * be added together so that getVoltage() returns the sum of voltages
 * at any particular time.
 * 
 * Similarly any number of network cards may be attached to the wire and
 * each be setting voltages ... the wire should add all these voltages together.
 * 
 * @author K. Bryson
 */
class MyTwistedWirePair implements TwistedWirePair {

    //Wire: has a lit of objects connected to it
    //For each device it has a name and a voltage
    //Voltage of the wire is the sum of all device's voltage
	
    private double voltage = 0.0;
    private HashMap<String, Double> currentVoltages = new HashMap<String, Double>();

    public synchronized void setVoltage(String device, double voltage) {
        currentVoltages.put(device, voltage);
        updateWireVoltage();
    }

    /*
     * Update current voltage on the wire.
     */
    private void updateWireVoltage(){
    	
        voltage = 0.0;

        // Add all the currently set voltages together.
        for (double currentVoltage: currentVoltages.values()) {
        	voltage += currentVoltage;        	
        }
    }
        
    public synchronized double getVoltage(String device) {
        return voltage;
    }
}

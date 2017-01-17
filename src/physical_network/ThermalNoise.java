/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2016)
 *  
 *  YOU SHOULD NOT NEED TO MODIFY THIS CLASS.
 *  (You will only submit on Moodle two files: DataFrame.java and NetworkCard.java)
 */

package physical_network;

/**
 * 
 * This models thermal noise at a particular voltage level on the wire.
 *
 * @author K. Bryson
 */
public class ThermalNoise extends Thread {

	// Device name.
	private final String deviceName;

	// Thermal noise level in volts.
    private final double noiseLevel;

    // Shared wire object to add the thermal noise to.
    private final TwistedWirePair wire;
    
    
    /**
     * @param deviceName This provides the name of this device, i.e. "Network Card A".
     * @param noiseLevel The peak-to-peak noise level to set in volts.
     * @param wire       This is the shared wire that this network card is connected to.
     */
    public ThermalNoise(String deviceName, double noiseLevel, TwistedWirePair wire) {
    	this.deviceName = deviceName;
        this.noiseLevel = noiseLevel;
        this.wire = wire;
    }
    
    /**
     * Start sending random noise to the wire between -1/2 noiseLevel to 1/2 noiseLevel.
     */
    
    @Override
    public void run() {

        while (true) {
            wire.setVoltage(deviceName, (Math.random() - 0.5) * noiseLevel);
        }
        
    }
}

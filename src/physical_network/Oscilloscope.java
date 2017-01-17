/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2016)
 *  
 *  YOU SHOULD NOT NEED TO MODIFY THIS CLASS.
 *  (You will only submit on Moodle two files: DataFrame.java and NetworkCard.java)
 */

package physical_network;

import org.jfree.ui.RefineryUtilities;

/**
 * 
 * This models an oscilloscope connected to a wire.
 * Essentially it periodically gets the value of the voltage
 * on the wire and sets this value on the oscilloscope.
 *
 * @author K. Bryson
 */
class Oscilloscope extends Thread {
    
	private final String deviceName;
    private final TwistedWirePair wire;
    private final OscilloscopePanel panel;
    
    public Oscilloscope(String deviceName, TwistedWirePair wire) {
        
    	this.deviceName = deviceName;    	
        this.wire = wire;
        
        // Create the Oscilloscope panel and make it visible.        
        this.panel = new OscilloscopePanel();  

        panel.pack();
        RefineryUtilities.centerFrameOnScreen(panel);
        panel.setVisible(true);
    }

    
    @Override
    public void run() {
        
        try {
        	
        	while (true) {
                
                double voltage = wire.getVoltage(deviceName);                
                panel.setVoltage(voltage);
                
                sleep(10);
            }

        } catch (InterruptedException except) {
            System.out.println("Netword Card Interrupted: " + getName());
        }
        
    }
}

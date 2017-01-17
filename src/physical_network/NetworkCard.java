/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2016)
 *  
 *  YOU MAY MODIFY THIS CLASS TO IMPLEMENT Stop & Wait ARQ PROTOCOL.
 *  (You will submit this class to Moodle.)
 *
 */

package physical_network;

import java.util.Arrays;
import java.util.concurrent.*;


/**
 * 
 * Represents a network card that can be attached to a particular wire.
 * 
 * It has only two key responsibilities:
 * i) Allow the sending of data frames consisting of arrays of bytes using send() method.
 * ii) Receives data frames into an input queue with a receive() method to access them.
 *
 * @author K. Bryson
 */

public class NetworkCard {
    
	// Wire pair that the network card is atatched to.
    private final TwistedWirePair wire;

    // Unique device number and name given to the network card.
    private final int deviceNumber;
    private final String deviceName;

    // Default values for high, low and mid- voltages on the wire.
    private final double HIGH_VOLTAGE = 2.5;
    private final double LOW_VOLTAGE = -2.5;
    
    // Default value for a signal pulse width that should be used in milliseconds.
    private final int PULSE_WIDTH = 200;
    
    // Default value for maximum payload size in bytes.
    private final int MAX_PAYLOAD_SIZE = 1500;

    // Default value for input & output queue sizes.
    private final int QUEUE_SIZE = 5;

    // Output queue for dataframes being transmitted.
    private LinkedBlockingQueue<DataFrame> outputQueue = new LinkedBlockingQueue<DataFrame>(QUEUE_SIZE);
    
    // Input queue for dataframes being received.
    private LinkedBlockingQueue<DataFrame> inputQueue  = new LinkedBlockingQueue<DataFrame>(QUEUE_SIZE);

    // Transmitter thread.
    private Thread txThread;
    
    // Receiver thread.
    private Thread rxThread;
    
    /**
     * NetworkCard constructor.
     * @param deviceName This provides the name of this device, i.e. "Network Card A".
     * @param wire       This is the shared wire that this network card is connected to.
     * @param listener   A data frame listener that should be informed when data frames are received.
     *                   (May be set to 'null' if network card should not respond to data frames.)
     */
    public NetworkCard(int number, TwistedWirePair wire) {
    	
    	this.deviceNumber = number;
    	this.deviceName = "NetCard" + number;
    	this.wire = wire;
    	
    	txThread = this.new TXThread();
    	rxThread = this.new RXThread();
    }
    
    /*
     * Initialize the network card.
     */
    public void init() {
    	txThread.start();
    	rxThread.start();
    }
    
    
    public void send(DataFrame data) throws InterruptedException {
    	outputQueue.put(data);
    }

    public DataFrame receive() throws InterruptedException {
    	DataFrame data = inputQueue.take();
    	return data;
    }

    /*
     * Private inner thread class that transmits data.
     */
    private class TXThread extends Thread {
    	
    	public void run() {
    		
    		try {
	    		while (true) {
	    			
	    			// Blocks if nothing is in queue.
	    			DataFrame frame = outputQueue.take();
	    			transmitFrame(frame);
	    		}
    		} catch (InterruptedException except) {
    			System.out.println(deviceName + " Transmitter Thread Interrupted - terminated.");
    		}
    		
    	}

    	/**
         * Tell the network card to send this data frame across the wire.
         * NOTE - THIS METHOD ONLY RETURNS ONCE IT HAS TRANSMITTED THE DATA FRAME.
         * 
         * @param frame  Data frame to transmit across the network.
         */
        public void transmitFrame(DataFrame frame) throws InterruptedException {
        	
    		if (frame != null) {
    			
    			// Low voltage signal to get ready ...
    			wire.setVoltage(deviceName, LOW_VOLTAGE);
    			sleep(PULSE_WIDTH*4);
    			
    			byte[] payload = frame.getTransmittedBytes();
    			
    			// Send bytes in asynchronous style with 0.2 seconds gaps between them.
    			for (int i = 0; i < payload.length; i++) {
    				
    	    		// Byte stuff if required.
    	    		if (payload[i] == 0x7E || payload[i] == 0x7D)
    	    			transmitByte((byte)0x7D);
    	    		
    	    		transmitByte(payload[i]);
    			}
    			
    			// Append a 0x7E to terminate frame.
        		transmitByte((byte)0x7E);
    		}

    		
        }
        
    	private void transmitByte(byte value) throws InterruptedException {

    		// Low voltage signal ...
    		wire.setVoltage(deviceName, LOW_VOLTAGE);
    		sleep(PULSE_WIDTH*4);

    		// Set initial pulse for asynchronous transmission.
    		wire.setVoltage(deviceName, HIGH_VOLTAGE);
    		sleep(PULSE_WIDTH);
    		
    		// Go through bits in the value (big-endian bits first) and send pulses.
    		
            for (int bit = 0; bit < 8; bit++) {
                if ((value & 0x80) == 0x80) {
                    wire.setVoltage(deviceName, HIGH_VOLTAGE);
                } else {
                    wire.setVoltage(deviceName, LOW_VOLTAGE);
                }
                
                // Shift value.
                value <<= 1;  

                sleep(PULSE_WIDTH);
            }
    	}
    	
    }
    
    /*
     * Private inner thread class that receives data.
     */    
    private class RXThread extends Thread {
    	
    	public void run() {
    		
        	try {
        		
    			// Listen for data frames.
        		
	    		while (true) {

	    			byte[] bytePayload = new byte[MAX_PAYLOAD_SIZE];
	    			int bytePayloadIndex = 0;
		    		byte receivedByte;
	    			
	        		do {
	        			
	        			receivedByte = receiveByte();
	        			
	        			System.out.println(deviceName + " RECEIVED BYTE = " + Integer.toHexString(receivedByte & 0xFF));
	        			
	        			if ((receivedByte & 0xFF) != 0x7E) {
	            			// Unstuff if escaped.        			
		        			if (receivedByte == 0x7D) {
		        				receivedByte = receiveByte();
		        				System.out.println(deviceName + " ESCAPED RECEIVED BYTE = " + Integer.toHexString(receivedByte & 0xFF));
		        			}
		        			
		        			bytePayload[bytePayloadIndex] = receivedByte;
		        			bytePayloadIndex++;
	        			}
	        			
	        		} while ((receivedByte & 0xFF) != 0x7E);
	        			        		
	        		// Block receiving data if queue full.
	        		inputQueue.put(new DataFrame(Arrays.copyOfRange(bytePayload, 0, bytePayloadIndex)));
	    		}

            } catch (InterruptedException except) {
                System.out.println(deviceName + " Interrupted: " + getName());
            }
    		
    	}
    	
    	public byte receiveByte() throws InterruptedException {
    		
    		double thresholdVoltage = (LOW_VOLTAGE + 2.0 * HIGH_VOLTAGE)/3;
    		byte value = 0;
    		    		
    		while (wire.getVoltage(deviceName) < thresholdVoltage) {
    			sleep(PULSE_WIDTH/10);
    		}
    		
    		// Sleep till middle of next pulse.
    		sleep(PULSE_WIDTH + PULSE_WIDTH/2);
    		
    		// Use 8 next pulses for byte.
    		for (int i = 0; i < 8; i++) {
    			
    			value *= 2;
    		
        		if (wire.getVoltage(deviceName) > thresholdVoltage) {
        			value += 1;
        		}
        		
        		sleep(PULSE_WIDTH);
    		}
    		
    		return value;
    	}
    	
    }

    
}

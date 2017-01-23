/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2016)
 *  
 *  YOU MAY MODIFY THIS CLASS TO IMPLEMENT Stop & Wait ARQ PROTOCOL.
 *  (You will submit this class to Moodle.)
 *
 */

package physical_network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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

	//Ack
	private int myTransmission = 0; //0 or 1
	private int toSendTransmission = 0; //0 or 1
	private boolean ACKOK = false;


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

	private int calcSum(int checkSum, int count, byte b){
		//Since we do a 16-bits checksum
		if (count%2==0 ) {
			//if even
			checkSum = (checkSum + b * 256) & 0xffff; //deuxieme byte shifte de 8 bits (pour faire 16)
		} else{
			// else odd
			checkSum = (checkSum + b) & 0xffff;
		}
		return checkSum;
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
					try {
						transmitFrame(frame);
					} catch (IOException e) {
						e.printStackTrace();
					}
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
		public void transmitACK(byte[] ACK) throws InterruptedException {
			for (int i = 0; i < ACK.length; i++) {
				if (ACK[i] == 0x7E || ACK[i] == 0x7D) {
					System.out.println("Sending - Escape char in ACK at pos: "+i);
					transmitByte((byte) 0x7D);
				}

				transmitByte(ACK[i]);
			}
		}

        public void transmitFrame(DataFrame frame) throws InterruptedException, IOException {

    		if (frame != null) {

				int checkSum = 0;
				int count = 0;
				int length = 0;

    			// Low voltage signal to get ready ...
    			wire.setVoltage(deviceName, LOW_VOLTAGE);
    			sleep(PULSE_WIDTH*4);

				//add source to header
				byte[] sourceByte =  ByteBuffer.allocate(4).putInt(deviceNumber).array();

				ByteArrayOutputStream tempOutputStream = new ByteArrayOutputStream( );
				tempOutputStream.write( sourceByte );
				tempOutputStream.write( frame.getTransmittedBytes());

				byte[] payload = tempOutputStream.toByteArray();

				//add length to header
				length = payload.length;
				byte[] lengthByte =  ByteBuffer.allocate(4).putInt(length).array();
				byte[] transmissionByte = new byte[1];
				transmissionByte[0] = (byte) toSendTransmission;

				ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
				outputStream.write( transmissionByte );
				outputStream.write( lengthByte );
				outputStream.write( payload);

				payload = outputStream.toByteArray();


				//  [TransmisisonByte|Length|Source|Destination|Message|Checksum]
				//            1          4       4        4         X        2

    			// Send bytes in asynchronous style with 0.2 seconds gaps between them.
    			for (int i = 0; i < payload.length; i++) {

					if (0<i && i<=4){
						System.out.println("Emit lenght byte: "+Integer.toHexString(payload[i] & 0xFF));
					}
					// Byte stuff if required.
    	    		if (payload[i] == 0x7E || payload[i] == 0x7D) {
						System.out.println("Sending - Escape char at pos: "+i);
						transmitByte((byte) 0x7D);
						if(i>4) {
							checkSum = calcSum(checkSum, count, (byte) 0x7D);
							count++;
						}
					}
    	    		
    	    		transmitByte(payload[i]);
					if(i>4) {
						checkSum = calcSum(checkSum, count, payload[i]);
						count++;
					}
				}

				//Take the complement
				System.out.println(deviceName+": Checksum:"+checkSum);
				checkSum = (~checkSum) & 0xffff;
				System.out.println(deviceName+": Checksum emitted (complement):"+checkSum);

				byte[] checkSumByte =  ByteBuffer.allocate(4).putInt(checkSum).array();

				//send checksum
				for (int i = 0; i < 4; i++) {
					if (checkSumByte[i] == 0x7E || checkSumByte[i] == 0x7D) {
						System.out.println("Sending - Escape char at pos: "+i);
						transmitByte((byte) 0x7D);
					}

					transmitByte(checkSumByte[i]);
				}


				// Append a 0x7E to terminate frame.
        		transmitByte((byte)0x7E);

				sleep(5000);
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

			System.out.println("Start thread:" + deviceName);

        	try {
        		
    			// Listen for data frames.
        		
	    		while (true) {  // messages

	    			byte[] bytePayload = new byte[MAX_PAYLOAD_SIZE];
	    			int bytePayloadIndex = 0;
		    		byte receivedByte;
					int receivedTransmission = -1;

					byte[] lengthByte = new byte[4];
					byte[] destinationByte = new byte[4];
					byte[] sourceByte = new byte[4];

					boolean isForThisCard = true;
					int checkSum = 0;
					int count = 0;
					int length = 0;
					int source = 0;



	        		while(true) {  // bytes

	        			receivedByte = receiveByte();

						if (bytePayloadIndex==0 && (receivedByte & 0xFF) != 0x7E){
							receivedTransmission = (int) (receivedByte & 0xFF);
							if ((receivedTransmission & 0xfe)!=0){
								//corrupted ack (!= 0 and !=1)
								isForThisCard = false;
								System.out.println("Transmission byte corrupted.");
							}
							receivedByte = receiveByte();
						}

//						System.out.println(deviceName+ ": BytePayloadIndex: "+bytePayloadIndex);

						if ((receivedByte & 0xFF) == 0x7E){
							if (bytePayloadIndex==2){
								//IF IS ACK
								if ((int)(lengthByte[0] & 0xff) == deviceNumber){
									//ACK is for me
									if (receivedTransmission == toSendTransmission){
										//Sender Receives ACK
										isForThisCard = false; //No need to go through treatment.
										synchronized(this){  //Lock the var
											ACKOK = true;
										}
									}
								}
							}
							break;
						}

						//IF NOT ACK
						if (bytePayloadIndex==2 && receivedTransmission != myTransmission){
							//if transmission byte wrong, ignore (or duplicata)
							isForThisCard = false;
						}

						//handle escape byte + calcSum

						if ((receivedByte & 0xFF) == 0x7D) {
							if(length>0) {
								checkSum = calcSum(checkSum, count, receivedByte);
								count++;
							}
							receivedByte = receiveByte();
							System.out.println("Receiving - Escape char at pos: "+bytePayloadIndex);

						}
						//cheksum calcul
						if(length>0) {
							checkSum = calcSum(checkSum, count, receivedByte);
							count++;
							length--;
						}

						//HEADER
						if(isForThisCard) {

							//length
							if(bytePayloadIndex<4){
								lengthByte[bytePayloadIndex] = receivedByte;
								System.out.println(deviceName + ": Add byt to lenghtByte: "+Integer.toHexString(receivedByte & 0xFF)+" at index: "+bytePayloadIndex);
							}
							if(bytePayloadIndex==3){
								length = java.nio.ByteBuffer.wrap(lengthByte).getInt();
								System.out.println(deviceName +" - Length: "+length);
							}

							//source
							if(3<bytePayloadIndex && bytePayloadIndex<8){
								sourceByte[bytePayloadIndex-4] = receivedByte;
							}
							if(bytePayloadIndex==7){
								source = java.nio.ByteBuffer.wrap(sourceByte).getInt();
							}

							// Destination
							if(7<bytePayloadIndex && bytePayloadIndex<12){
								destinationByte[bytePayloadIndex-8] = receivedByte;
							}
							if(bytePayloadIndex==11){
								int destination = java.nio.ByteBuffer.wrap(destinationByte).getInt();
								if(destination == deviceNumber){
									System.out.println("Message is for me! (I am "+deviceName+" )");
								} else{
									System.out.println("Message isn't for me! (I am "+deviceName+" )");
									isForThisCard = false;
								}
							}
							// MESSAGE
							if (isForThisCard){

								if (bytePayloadIndex > 11 && (receivedByte & 0xFF) != 0x7E) {
									bytePayload[bytePayloadIndex - 12] = receivedByte;
									System.out.println("Byte added: "+Integer.toHexString(receivedByte & 0xFF)+" at pos "+(bytePayloadIndex-12));
								}
								bytePayloadIndex++;

							}
						}
	        			
	        		} //end - while
	        			        		
	        		// Block receiving data if queue full.
					if(isForThisCard) {
						//control checkSum
						byte[] checkSumByte = new byte[4];
						checkSumByte[0] = bytePayload[bytePayloadIndex - 16];
						checkSumByte[1] = bytePayload[bytePayloadIndex - 15];
						checkSumByte[2] = bytePayload[bytePayloadIndex - 14];
						checkSumByte[3] = bytePayload[bytePayloadIndex -13] ;

						int checkSumReceived = java.nio.ByteBuffer.wrap(checkSumByte).getInt();
						System.out.println(deviceName+": Checksum received:"+checkSumReceived);
						System.out.println(deviceName+": Checksum calculated:"+checkSum);


						if(checkSumReceived + checkSum == 0xffff){
							System.out.println("Not Corrupted!");
							inputQueue.put(new DataFrame(Arrays.copyOfRange(bytePayload, 0, bytePayloadIndex-16)));

							//Create ACK
							//ACK :   [transmissionByte|destination|end]
							//				1				  1		 1

							byte[] ACK = new byte[3];

							ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
							outputStream.write( myTransmission );
							outputStream.write( source );
							outputStream.write( (byte) 0x7E );

							ACK = outputStream.toByteArray();

							//Inverse myTransmission;
							if (myTransmission==0) myTransmission=1;
							if (myTransmission==1) myTransmission=0;

							//Transmit ACK


						} else {
							System.out.println("-- Error while transmitting: MSG Corrupted --");
						}


					}
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

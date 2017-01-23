/*
 *  (c) K.Bryson, Dept. of Computer Science, UCL (2016)
 *  
 *  YOU MAY MODIFY THIS CLASS TO IMPLEMENT Stop & Wait ARQ PROTOCOL.
 *  (You will submit this class to Moodle.)
 *  
 */

package physical_network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;

/**
 * Encapsulates the data for a network 'data frame'.
 * At the moment this just includes a payload byte array.
 * This may need to be extended to include necessary header information.
 * 
 * @author kevin-b
 *
 */

public class DataFrame {
	
	public final byte[] payload;
	private int destination = 0;
	private boolean isACK = false;

	public DataFrame(String payload) {
		this.payload = payload.getBytes();
	}
	
	public DataFrame(String payload, int destination) {
		this.payload = payload.getBytes();
		this.destination = destination;
	}

	public DataFrame(byte[] payload) {
		this.payload = payload;
	}
	
	public DataFrame(byte[] payload, int destination) {
		this.payload = payload;
		this.destination = destination;
	}

	public DataFrame(byte[] payload, boolean isACK) {
		this.payload = payload;
		this.isACK = isACK;
	}

	public int getDestination() {
		return destination;
	}
	
	public byte[] getPayload() {
		return payload;
	}

	public String toString() {
		return new String(payload);		
	}

	/*
	 * A factory method that can be used to create a data frame
	 * from an array of bytes that have been received.
	 */
	public static DataFrame createFromReceivedBytes(byte[] byteArray) {
	
		DataFrame created = new DataFrame(byteArray);
		
		return created;
	}
	
	/*
	 * This method should return the byte sequence of the transmitted bytes.
	 * At the moment it is just the payload data ... but extensions should
	 * include needed header information for the data frame.
	 * Note that this does not need sentinel or byte stuffing
	 * to be implemented since this is carried out as the data
	 * frame is transmitted and received.
	 */
	public byte[] getTransmittedBytes() throws IOException {

		if (isACK){
			return this.getPayload();
		}

		byte[] destinationByte =  ByteBuffer.allocate(4).putInt(this.destination).array();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
		outputStream.write( destinationByte );
		outputStream.write( this.getPayload() );

		byte result[] = outputStream.toByteArray();

		return result;
	}




}


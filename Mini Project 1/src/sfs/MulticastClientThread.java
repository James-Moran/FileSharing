package sfs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MulticastClientThread extends Thread {

	private String GROUPADDRESS;
	private int MULTICASTPORT;
	private ArrayList<String> HostNames;
	private MulticastSocket clientSocket;
	private boolean on;

	MulticastClientThread(String GROUPADDRESS, int MULTICASTPORT) {
		this.GROUPADDRESS = GROUPADDRESS;
		this.MULTICASTPORT = MULTICASTPORT;
	}

	public void run() {
		// Receives host names and adds them to array
		HostNames = new ArrayList<String>();
		try {
			// Sets up multicast client socket and adds it to pre-specified
			// group
			InetAddress groupAddress = InetAddress.getByName(GROUPADDRESS);
			clientSocket = new MulticastSocket(MULTICASTPORT);
			clientSocket.joinGroup(groupAddress);
			byte[] buffer = new byte[256];
			on = true;

			while (on) {
				String hostName = ReceiveMessage(clientSocket, buffer);
				if (!HostNames.contains(hostName)) {
					HostNames.add(hostName);
				}
			}
			clientSocket.close();

		} catch (UnknownHostException e) {
			System.err.println("Groupadress was not IPv4 format");

		} catch (IOException e1) {
			System.err.println("Problem setting up multicast socket");
		}

	}

	private String ReceiveMessage(MulticastSocket clientSocket, byte[] buffer) {
		String message = null;
		try {
			DatagramPacket messagePacket = new DatagramPacket(buffer, buffer.length);
			clientSocket.receive(messagePacket);
			message = new String(buffer, 0, buffer.length);
		} catch (IOException e) {
			System.err.println("Problem receiving message");
		}

		return message;
	}

	public void CloseThread() {
		on = false;
	}

	// Returns array of host names
	public ArrayList<String> getHostNames() {
		return HostNames;
	}

}

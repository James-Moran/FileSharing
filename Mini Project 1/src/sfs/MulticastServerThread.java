package sfs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MulticastServerThread extends Thread {

	private int MULTICASTPORT;
	private String GROUPADDRESS;
	private boolean on;

	MulticastServerThread(int MULTICASTPORT, String GROUPADDRESS) {
		this.MULTICASTPORT = MULTICASTPORT;
		this.GROUPADDRESS = GROUPADDRESS;
	}

	public void run() {
		// Sends packet containing host name every 0.5s
		try {
			InetAddress ADDRESS = InetAddress.getByName(GROUPADDRESS);
			DatagramSocket multicastServer = new DatagramSocket();
			String hostName = InetAddress.getLocalHost().getHostName();
			DatagramPacket hostNamePacket = new DatagramPacket(hostName.getBytes(), hostName.getBytes().length, ADDRESS,
					MULTICASTPORT);

			on = true;
			while (on) {
				multicastServer.send(hostNamePacket);
				Thread.sleep(500);

			}
			multicastServer.close();

		} catch (SocketException e) {
			System.out.println("Problem opening socket");
		} catch (UnknownHostException e) {
			System.err.println("Could not find IPv4 host");
		} catch (IOException e) {
			System.err.println("Problem sending out packet");
		} catch (InterruptedException e) {
			System.err.println("Thread interupted");
		}

	}

	public void endMulticastServer() {
		on = false;
	}

}

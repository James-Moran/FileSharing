package sfs;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerThread extends Thread {

	private boolean on;
	private File SHAREDDIRECTORY;
	private int PORT;
	private int MULTICASTPORT;
	private String GROUPADDRESS;
	private ServerSocket server;

	ServerThread(File SHAREDDIRECTORY, int PORT, int MULTICASTPORT, String GROUPADDRESS) {
		this.SHAREDDIRECTORY = SHAREDDIRECTORY;
		this.PORT = PORT;
		this.MULTICASTPORT = MULTICASTPORT;
		this.GROUPADDRESS = GROUPADDRESS;
	}

	public void run() {
		MulticastServerThread multicastServer = new MulticastServerThread(MULTICASTPORT, GROUPADDRESS);
		multicastServer.start();

		server = SetUpServer(PORT);

		if (server != null) {
			// Accepts clients trying to connect and starts processing them
			on = true;
			while (on) {
				Socket clientSocket = null;
				try {
					clientSocket = server.accept();
					ClientProcessingThread client = new ClientProcessingThread(clientSocket, SHAREDDIRECTORY);
					client.start();
				} catch (IOException e) {
					if (on) {
						System.err.println("Socket closed");
					}
				}
			}
		}
		multicastServer.endMulticastServer();
		stopServer();

	}

	private ServerSocket SetUpServer(int PORT) {
		ServerSocket server = null;
		try {
			server = new ServerSocket(PORT);
		} catch (IOException e) {
			System.err.println("Port in use, most likely program is already running");
			endProgram();

		}
		return server;
	}

	private void endProgram() {
		on = false;
		FileSharingMain.unlockWaiter();
	}

	public void stopServer() {
		on = false;
		try {
			server.close();
		} catch (IOException e) {
			System.err.println("Problem closing server");
		} catch (NullPointerException e) {
			
		}
	}

}

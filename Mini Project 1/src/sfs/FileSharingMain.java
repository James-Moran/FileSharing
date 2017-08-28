package sfs;

import java.io.File;

public class FileSharingMain {

	private static final Object moniter = new Object();

	private static final int PORT = 8000;
	private static final int MULTICASTPORT = 8001;

	private static final String GROUPADDRESS = "224.0.0.3";

	public static void main(String[] args) {

		final File SHAREDDIRECTORY = new File(args[0]);
		final File DOWNLOADDIRECTORY = new File(args[1]);

		// Checks the directories specified are in fact directories
		boolean valid = directoriesValid(SHAREDDIRECTORY, DOWNLOADDIRECTORY);

		if (valid) {
			System.out.println("Welcome to my simple file sharing system");

			// Set up server thread
			ServerThread server = new ServerThread(SHAREDDIRECTORY, PORT, MULTICASTPORT, GROUPADDRESS);
			server.start();
			// Setting up a client thread and linking it to the server
			ClientThread client = new ClientThread(DOWNLOADDIRECTORY, PORT, MULTICASTPORT, GROUPADDRESS);
			client.start();

			waiter();
			server.stopServer();
			client.stopClient();
		}
	}

	private static boolean directoriesValid(File sharedDirectory, File downloadDirectory) {
		boolean valid = true;
		if (!sharedDirectory.isDirectory()) {
			System.out.println("Please enter a valid shared directory");
			valid = false;
		} else if (!downloadDirectory.isDirectory()) {
			System.out.println("Please enter a valid download directory");
			valid = false;
		}
		return valid;
	}

	private static void waiter() {
		synchronized (moniter) {
			try {
				moniter.wait(); // wait until notified
			} catch (Exception e) {
			}
		}
	}

	public static void unlockWaiter() {
		synchronized (moniter) {
			moniter.notifyAll(); // unlock again
		}
	}

}

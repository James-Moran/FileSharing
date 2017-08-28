package sfs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

public class ClientProcessingThread extends Thread {

	private Socket connection;
	private File SHAREDDIRECTORY;
	private boolean on;

	ClientProcessingThread(Socket clientSocket, File SHAREDDIRECTORY) {
		this.SHAREDDIRECTORY = SHAREDDIRECTORY;
		this.connection = clientSocket;
	}

	public void run() {
		// Setting up output and input steams
		PrintStream printToClient = null;
		BufferedReader receiveFromClient = null;
		OutputStream output = null;
		try {
			// Output
			output = connection.getOutputStream();
			printToClient = new PrintStream(output, true);
			// Input
			InputStream input = connection.getInputStream();
			receiveFromClient = new BufferedReader(new InputStreamReader(input));

		} catch (IOException e) {
			System.err.println("Problem setting up IO streams");
		}

		// Receives the request the client wants the server to do and executes
		// it
		on = true;
		while (on) {
			DealWithRequest(printToClient, receiveFromClient, SHAREDDIRECTORY, output);
		}

		try {
			connection.close();
		} catch (IOException e) {
			System.err.println("Problem closing the connection");
			e.printStackTrace();
		}
	}

	private void DealWithRequest(PrintStream printToClient, BufferedReader receiveFromClient, File directory,
			OutputStream output) {
		try {
			// Receives message and breaks it into appropriate sections
			String clientMessage = receiveFromClient.readLine();
			String[] clientMessageArray = clientMessage.split(" ", 2);
			String clientRequest = clientMessageArray[0];

			if (clientRequest.equals("SEND-FILE")) {
				String clientFileName = clientMessageArray[1];
				sendFile(printToClient, output, clientFileName);

			} else if (clientRequest.equals("LIST-FILES")) {
				ListFiles(printToClient, directory);
			} else if (clientRequest.equals("CLOSE-CONNECTION")) {
				on = false;
			} else {
				printToClient.println("INVALID-RESPONCE");
			}

		} catch (IOException e) {
			System.err.println("Problem receiving client instuction");
		} catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Invalid message");
		} catch (NullPointerException e){
			//Client Disconnect
			on = false;
		}

	}

	private void sendFile(PrintStream printToClient, OutputStream output, String clientFileName) {
		File file = new File(SHAREDDIRECTORY.toString(), clientFileName);
		File[] listOfFiles = SHAREDDIRECTORY.listFiles();
		boolean inDirectory = isInDirectory(file, listOfFiles);

		if (inDirectory) {
			printToClient.println("FILENAME-RECEIVED");
			sendData(file, output);
		} else {
			printToClient.println("FILENAME-NOT-FOUND");
		}

	}

	// Sends data in the form of bytes
	private void sendData(File file, OutputStream output) {
		try {
			byte[] bytes = new byte[16 * 1024];

			InputStream in = new FileInputStream(file);
			@SuppressWarnings("unused")
			int count;
			while ((count = in.read(bytes)) > 0) {
				output.write(bytes);
			}			
			in.close();
		} catch (IOException e) {
			System.err.println("Problem sending file");
		}

	}

	// Checks if the file is already in the directory
	private boolean isInDirectory(File file, File[] listOfFiles) {
		boolean inDirectory = false;
		for (int i = 0; i < listOfFiles.length; i++) {
			;
			if (listOfFiles[i].toString().equals(file.toString())) {
				inDirectory = true;
			}
		}
		return inDirectory;
	}

	private void ListFiles(PrintStream printToClient, File directory) {

		File[] listOfFiles = directory.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				printToClient.println(listOfFiles[i].getName());
			}
		}

		printToClient.println("END-OF-FILESLIST");

	}

}

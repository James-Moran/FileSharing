package sfs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

public class ClientThread extends Thread {

	private File DOWNLOADDIRECTORY;
	private int PORT;
	private String GROUPADDRESS;
	private int MULTICASTPORT;
	private boolean connectedToServer;
	private boolean on = true;

	ClientThread(File DOWNLOADDIRECTORY, int PORT, int MULTICASTPORT, String GROUPADDRESS) {
		this.DOWNLOADDIRECTORY = DOWNLOADDIRECTORY;
		this.PORT = PORT;
		this.GROUPADDRESS = GROUPADDRESS;
		this.MULTICASTPORT = MULTICASTPORT;
	}

	public void run() {

		Scanner userInput = new Scanner(System.in);
		while (on) {
			// Matches instructions to methods
			listClientInstuctions();
			int instruction = getClientInstruction(userInput);

			if (instruction == 1) {
				listServers(GROUPADDRESS, MULTICASTPORT);
			} else if (instruction == 2) {
				connectToServer(userInput, PORT);

			} else if (instruction == 3) {
				searchFile(userInput, PORT);

			} else if (instruction == 4) {
				ListDownloads(DOWNLOADDIRECTORY);

			} else if (instruction == 5){
				endProgram();
			}
		}
	}

	private void listClientInstuctions() {
		System.out.println("Please enter instruction number, choose from:");
		System.out.println("1. List servers");
		System.out.println("2. Connect to server");
		System.out.println("3. Search for file");
		System.out.println("4. List downloaded files");
		System.out.println("5. Quit");
	}

	// Asks for instructions until valid number is entered
	private int getClientInstruction(Scanner userInput) {
		int instruction = 0;
		boolean found = false;
		while (!found) {

			try {
				int userInstruction = userInput.nextInt();

				if ((userInstruction < 6) && (userInstruction > 0)) {
					instruction = userInstruction;
					found = true;
				} else {
					System.out.println("Please enter valid number");
				}
			} catch (InputMismatchException i) {
				System.out.println("Please enter number");
				userInput.next();
			}

		}
		return instruction;

	}

	/**
	 *  1. List servers
	 * @param GROUPADDRESS The name of the group the multicast servers are sending out packets to
	 * @param MULTICASTPORT The port to set up the multicast client on
	 * Prints out list of active servers
	 */
	private void listServers(String GROUPADDRESS, int MULTICASTPORT) {

		ArrayList<String> hostNames = getActiveServers();

		for (int i = 0; i < hostNames.size(); i++) {
			System.out.println(hostNames.get(i).toString());
		}
	}

	/**
	 *  2. Connect to server
	 * @param userInput TO get user input
	 * @param PORT the port number to connect to the server over
	 * Connects to the server the user enters, displays the menu, handles the requests.
	 */
	private void connectToServer(Scanner userInput, int PORT) {
		InputStream input;
		try {
			String hostName = getHostName(userInput);
			Socket serverConnection = new Socket(hostName, PORT);

			// Setting up output stream
			OutputStream output = serverConnection.getOutputStream();
			PrintStream out = new PrintStream(output, true);

			// Setting up input
			input = serverConnection.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(input));

			connectedToServer = true;
			while (connectedToServer) {
				listServerInstruction();
				int serverInstruction = getServerInstruction(userInput);

				if (serverInstruction == 1) {
					listFiles(out, in);
				} else if (serverInstruction == 2) {
					fileDownloader(serverConnection, out, in, userInput, DOWNLOADDIRECTORY, input);
				} else {
					closeConnection(out);
					connectedToServer = false;
				}

			}
		} catch (IOException e) {
			System.out.println("Problem connecting to the server");
		}

	}

	private String getHostName(Scanner userInput) {
		System.out.println("Please enter the host name of the server:");
		String host = userInput.next();
		// Possibly check input is of valid format?
		return host;
	}

	private void listServerInstruction() {
		System.out.println("Input server instruction");
		System.out.println("1. List Files");
		System.out.println("2. Download File");
		System.out.println("3. Disconnect from server");
	}

	// Asks until valid server instruction is entered
	private int getServerInstruction(Scanner userInput) {
		int instruction = 0;
		boolean found = false;
		while (!found) {

			try {
				int userInstruction = userInput.nextInt();

				if ((userInstruction < 6) && (userInstruction > 0)) {
					instruction = userInstruction;
					found = true;
				} else {
					System.out.println("Please enter valid number");
				}
			} catch (InputMismatchException i) {
				System.out.println("Please enter number");
				userInput.next();
			}

		}
		return instruction;

	}

	/**
	 *  2.1 List files
	 * @param out printstream to write to server
	 * @param in buffered reader to receive from server
	 * Prints the list of files at the server
	 */
	private void listFiles(PrintStream out, BufferedReader in) {

		ArrayList<String> files = requestFiles(out, in);

		displayList(files);
	}

	/**
	 * 2.2 Download file
	 * @param serverConnection The connection to the server you're currently connected to
	 * @param out The printsteam to output to client
	 * @param in The reader to get input from server
	 * @param userInput TO get input from user
	 * @param DOWNLOADDIRECTORY To store the file you will download
	 * @param input To get input from server in byte from
	 * Downloads the specified file from the server
	 */
	private void fileDownloader(Socket serverConnection, PrintStream out, BufferedReader in, Scanner userInput,
			File DOWNLOADDIRECTORY, InputStream input) {

		File downloadFile = getFileInput(userInput);
		File fileToWrite = new File(DOWNLOADDIRECTORY.toString(), downloadFile.toString());

		// Testing whether the file has been downloaded already
		boolean downloaded = checkDownloads(DOWNLOADDIRECTORY, fileToWrite);

		if (!downloaded) {

			requestDownload(downloadFile, out);
			String response = getresponse(in);
			downloadFile(response, input, fileToWrite);
		}
	}

	private void requestDownload(File downloadFile, PrintStream out) {
		// Requesting file download
		out.println("SEND-FILE " + downloadFile.toString());

	}

	// Receives servers response about the file
	private String getresponse(BufferedReader in) {
		String received = null;
		try {
			received = in.readLine();
		} catch (IOException e) {
			System.err.println("Problem receiving response");
		}
		return received;
	}

	private void downloadFile(String response, InputStream input, File fileToWrite) {
		try {
			if (response.equals("FILENAME-RECEIVED")) {
				OutputStream fileWriter = new FileOutputStream(fileToWrite.toString());
				
				int count = input.read();
				while (count  > 0) {
					fileWriter.write(count);
					count = input.read();
				}
				
				finishedDownload();
				fileWriter.close();
			} else {
				System.err.println("Could not find file");
			}
		} catch (

		IOException e)

		{
			System.err.println("Problem receiveing and writing file data");
		} catch (

		NullPointerException e)

		{
			System.err.println("Server diconnected");
			connectedToServer = false;
		}

	}

	// Checks if the file has already been downloaded
	private boolean checkDownloads(File DOWNLOADDIRECTORY, File fileToWrite) {
		boolean downloaded = false;
		File[] listOfFiles = DOWNLOADDIRECTORY.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].toString().equals(fileToWrite.toString())) {
				downloaded = true;
			}
		}
		return downloaded;
	}
	
	private void finishedDownload() {
		System.out.println("Finished Downloading");		
	}
 
	/**
	 * 3. Search for file
	 * @param userInput, the scanner to receive user input
	 * @param PORT, the client sets up connections on
	 * Searches across all servers for a specified file and prints out the host names of those that contain it
	 */
	private void searchFile(Scanner userInput, int PORT) {
		File file = getFileInput(userInput);

		try {
			ArrayList<String> hostNames = getActiveServers();
			ArrayList<String> serversContainingFile = new ArrayList<String>();

			for (int i = 0; i < hostNames.size(); i++) {

				Socket connection = new Socket(hostNames.get(i), PORT);

				// Setting up output stream
				OutputStream output = connection.getOutputStream();
				PrintStream out = new PrintStream(output, true);
				// Setting up input stream
				InputStream input = connection.getInputStream();
				BufferedReader in = new BufferedReader(new InputStreamReader(input));

				ArrayList<String> files = requestFiles(out, in);
				// Checks if the server contains the file
				for (int j = 0; j < files.size(); j++) {
					if (files.get(j).equals(file.toString())) {
						// If so adds it to the list
						serversContainingFile.add(hostNames.get(i));
					}
				}
				closeConnection(out);
				connection.close();
			}

			displayList(serversContainingFile);

		} catch (UnknownHostException e) {
			System.err.println("Could not connect to host on list, possibly and probably closed");
		} catch (IOException e) {
			System.out.println("Problem setting up connection");
		}
	}

	/**
	 * List Download Files
	 * @param DOWNLOADDIRECTORY the downloads folder
	 * Prints out the files that have been downloaded.
	 */
	private void ListDownloads(File DOWNLOADDIRECTORY) {
		File[] listOfFiles = DOWNLOADDIRECTORY.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				System.out.println(listOfFiles[i].getName());
			}
		}
	}

	// ********************************************************************************************************************
	// Methods used by more than one instruction

	// Returns user input as file
	private File getFileInput(Scanner userInput) {
		System.out.println("Please enter the file name:");
		String filename = userInput.next();
		File file = new File(filename);

		return file;
	}

	// Requests the name of the files it can send and returns an array of them
	private ArrayList<String> requestFiles(PrintStream out, BufferedReader in) {
		out.println("LIST-FILES");
		ArrayList<String> files = receiveFiles(in);
		return files;
	}

	// Returns an array of the files a server can send
	private ArrayList<String> receiveFiles(BufferedReader in) {
		ArrayList<String> files = new ArrayList<String>();
		try {
			boolean on = true;
			while (on) {
				String filename = in.readLine();
				if (filename.equals("END-OF-FILESLIST")) {
					on = false;
				} else {
					files.add(filename);
				}

			}
		} catch (IOException e) {

		} catch (NullPointerException e) {
			System.err.println("Server disconnect");
			connectedToServer = false;
		}
		return files;

	}

	// Returns a lists of active servers
	private ArrayList<String> getActiveServers() {
		ArrayList<String> hostNames = new ArrayList<String>();
		try {
			MulticastClientThread activeServers = new MulticastClientThread(GROUPADDRESS, MULTICASTPORT);
			activeServers.start();
			Thread.sleep(1000);
			hostNames = activeServers.getHostNames();
			activeServers.CloseThread();
		} catch (InterruptedException e) {
			System.err.println("Thread interupted");
		}

		return hostNames;
	}

	// Prints out a given list
	private void displayList(ArrayList<String> list) {
		for (int i = 0; i < list.size(); i++) {
			System.out.println(list.get(i));
		}
	}

	private void closeConnection(PrintStream out) {
		out.println("CLOSE-CONNECTION");
	}

	// Sets into motion closing of entire program
	private void endProgram() {
		on = false;
		FileSharingMain.unlockWaiter();
	}

	public void stopClient() {
		on = false;
	}
}

import java.net.*;
import java.io.*;
import java.util.*;

public class Server {

	private static int sPort;// The server will be listening on
								// this port number

	static int client_count;
	private static File[] listOfFiles;
	public static File chunk_dir;
	public static String file_name;
	static String owner_chunk_file;
	public static String config_file;
	static boolean dir_result = false;
	static boolean chunk_dir_result = false;
	public static File configuration;
	public static File tmp; 

	public static void main(String[] args) throws Exception {
		System.out.println("Enter the name of the configuration file for the network: ");
		Scanner sc = new Scanner(System.in);
		config_file = sc.nextLine();
		configuration = new File(config_file);

		// count number of lines in config file to get client count
		try {

			Scanner file_scan = new Scanner(configuration);
			file_scan.nextInt();
			sPort = file_scan.nextInt();
			while (file_scan.hasNext()) {
				file_scan.nextInt();
				file_scan.nextInt();
				file_scan.nextInt();
				file_scan.nextInt();
				client_count++;
			}

			file_scan.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		// create Client and Chunk folders within the client folders
		for (int i = 1; i < 6; i++) {
			File client_dir = new File("Client" + i);
			File cl_chunks = new File(client_dir, "chunks");
			if (!client_dir.exists()) {
				dir_result = false;
			}
			try {
				client_dir.mkdirs();

				cl_chunks.mkdirs();

				dir_result = true;

			} catch (SecurityException e) {
				e.printStackTrace();
			}
		}

		System.out.println("The server is running on port " + sPort);

		System.out.println("Enter the file to split");

		try {
			Scanner s = new Scanner(System.in);
			file_name = s.next();
			s.close();

		}

		catch (Exception e) {
			e.printStackTrace();
		}

		tmp = new File(file_name);
		System.out.println("File size is: " + tmp.length());

		// creating chunks folder for file owner
		chunk_dir = new File("chunks");
		if (!chunk_dir.exists()) {
			chunk_dir_result = false;
		}
		try {
			chunk_dir.mkdirs();
			chunk_dir_result = true;
		} catch (SecurityException e) {
			e.printStackTrace();
		}

		FileSplit.splitFile(new File(file_name));
		// File folder = new

		// implement a text filter to filter out non txt chunks
		// chunks are being stored intermediately as .txt

/*		FilenameFilter textFilter = (dir, name) -> {
            String lowercaseName = name.toLowerCase();
            if (lowercaseName.endsWith(".txt") || lowercaseName.endsWith(".jpeg")) {
                return true;
            } else {
                return false;
            }
        };*/

		// store all the chunks of the file owner in the file array
		listOfFiles = chunk_dir.listFiles();
		Arrays.sort(listOfFiles);
		int tot_chunks = listOfFiles.length;
		System.out.println("Number of chunks is :" + tot_chunks);

		// Listen for clients on this port
		ServerSocket listener = new ServerSocket(sPort);
		int clientNum = 1;
		try {

			while (true) {
				// accept connection and spawn new thread for Client
				new Handler(listener.accept(), clientNum).start();
				System.out.println("Client " + clientNum + " is connected!");

				clientNum++;
			}

		} finally {
			listener.close();
		}

	}


	/**
	 * A handler thread class. Handlers are spawned from the listening loop and
	 * are responsible for dealing with a single client's requests.
	 */
	private static class Handler extends Thread {

		private Socket connection;
		private ObjectOutputStream out; // stream write to the socket
		private int no; // The index number of the client

		// get current client number and the thread
		public Handler(Socket connection, int no) {
			this.connection = connection;
			this.no = no;
		}

		public void run() {
			try {
				// Loop to ensure that only 5 peers are connected
				if (no > client_count) {
					System.out.println("Closing invalid client " + no);
					return;
				} // close if the client number is beyond 5

				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				out.writeObject(no);
				out.writeObject(file_name);
				out.writeObject(listOfFiles.length);
				int file_count = (listOfFiles.length - no) / client_count + 1;
				out.writeObject(file_count);

				// Distributing the chunks to the Clients by incrementing with
				// client count so that each chunk goes to only one peer
				for (int i = no - 1; i < listOfFiles.length; i += client_count) {
					InputStream in = new FileInputStream(listOfFiles[i]);
					// Read chunk list into the byte array 100kb at a time
					byte[] chunks = new byte[1024 * 100];
					int length = in.read(chunks);
					String fnm = listOfFiles[i].getName().substring(0, 3);
					System.out.println("Send chunk " + fnm + " of length " + length + " to Client " + no);
					// write the name of the chunk,size of chunk and the bytes
					// of the chunk to the inputstream in get_file() in Client
					out.writeObject(listOfFiles[i].toString());

					out.writeObject(length);

					out.write(chunks);
					in.close();
				}

			} catch (IOException ioException) {
				System.out.println("Disconnect with Client " + no);
				ioException.printStackTrace();
			} finally {
				// Close connections
				try {

					out.close();
					connection.close();
				} catch (IOException ioException) {
					System.out.println("Disconnect with Client " + no);
				}
			}
		}

	}
}

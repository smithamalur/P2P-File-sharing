import java.net.*;
import java.io.*;
import java.util.*;

public class Client {
	Socket requestSocket; // socket connect to the server
	ObjectOutputStream out; // stream write to the socket
	ObjectInputStream in; // stream read from the socket
	int sPort;
	static int client_num;
	static int file_count;
	static int num_of_chunks;
	static List<Integer> peer_ports;
	static List<Integer> peer_neighbours;
	static List<String> own_files;
	static PrintWriter pw;
	static String file_name;
	static int port;
	static int neighbour_no;
	static int download_port;

	public void Client() {
	}

	static void update_summary_file() throws FileNotFoundException, UnsupportedEncodingException {
		Collections.sort(own_files);
		File summary = new File("/home/anirudh/smitha/BitTorrent Implementation_v2/Client" + client_num,
				"summary.txt");
		pw = new PrintWriter(summary, "UTF-8");

		//update the summary file as and when new chunks are received
		for (int i = 0; i < own_files.size(); i++) {
			pw.println(own_files.get(i));
		}

		pw.close();
	}

	static void get_file(ObjectInputStream file_in) throws FileNotFoundException, IOException, ClassNotFoundException {

		// Get name of chunk from outputstream of file owner
		String name_of_chunk = (String) file_in.readObject();

		String chunk_name = name_of_chunk.substring(name_of_chunk.length() - 14);
		String cnm = name_of_chunk.substring(name_of_chunk.length() - 14).substring(7, 10);

		// Add received chunks to list of chunks of the current client
		own_files.add(name_of_chunk);

		// Get size of current chunk
		int file_len = (int) file_in.readObject();

		// set remaining length
		int rem_len = file_len;

		System.out.println("Receiving chunk " + cnm + " (" + file_len + " bytes)");

		// Store the chunks in the chunks folder of the client
		File newFile = new File("/home/anirudh/smitha/BitTorrent Implementation_v2/Client" + client_num, chunk_name);

		FileOutputStream file_out = new FileOutputStream(newFile);

		file_out.flush();
		// reduce the byte array size 100kb at a time so that files are stopped
		// being read once last chunk has been sent
		while (rem_len > 0) {
			byte[] bytes = new byte[rem_len];
			int count = file_in.read(bytes);
			rem_len -= count;
			file_out.write(bytes, 0, count);

		}

		file_out.close();
	}

	void DownloadFromFileOwner() throws ClassNotFoundException, FileNotFoundException {

		peer_ports = new ArrayList<Integer>();
		peer_neighbours = new ArrayList<Integer>();
		own_files = new ArrayList<String>();

		// Get ports and neighbours from the configuration file
		File configuration = new File("/home/anirudh/smitha/BitTorrent Implementation_v2/config.txt");
		try {
			Scanner sc = new Scanner(configuration);
			sc.nextInt();
			// FileOwner Port
			sPort = sc.nextInt();

			while (sc.hasNext()) {
				sc.nextInt();
				peer_ports.add(sc.nextInt());
				peer_neighbours.add(sc.nextInt());
				sc.nextInt();
			}
			sc.close();
		} finally {

		}

		try {

			// create a socket to connect to the FileOwner
			requestSocket = new Socket("localhost", sPort);

			// Input Stream to read from outputstream of file owner
			in = new ObjectInputStream(requestSocket.getInputStream());

			// Read Client no,chunk_name
			client_num = (int) in.readObject();
			file_name = (String) in.readObject();

			// Get neighbour,listening port and download_port for current client
			neighbour_no = peer_neighbours.get(client_num - 1);
			port = peer_ports.get(client_num - 1);
			download_port = peer_ports.get(peer_neighbours.get(client_num - 1) - 1);

			// Get total number of chunks of the file and the number of chunks
			// being currently sent
			num_of_chunks = (int) in.readObject();
			file_count = (int) in.readObject();

			// Print current Client number and number of chunks being received
			// from file owner
			System.out.println("Client :" + client_num);
			System.out.println("Number of files is " + file_count);

			try {
				for (int i = 0; i < file_count; i++)
					get_file(in);
				update_summary_file();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		catch (ConnectException e) {
			System.err.println("Connection refused. You need to initiate a server first.");
		}
		// catch ( ClassNotFoundException e ) {
		// System.err.println("Class not found");
		// }
		catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		} finally {
			// Close connections
			try {
				in.close();
				// out.close();
				requestSocket.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}

		}

	}
	// removed }

	private static class Handler_Client extends Thread {
		private Socket conn;
		private ObjectInputStream h_in;
		private ObjectOutputStream h_out;

		public Handler_Client(Socket conn) {
			this.conn = conn;

		}

		public void run() {

			try {
				h_in = new ObjectInputStream(conn.getInputStream());
				h_out = new ObjectOutputStream(conn.getOutputStream());
				while (true) {
					// System.out.println(h_in.available());

					String req = (String) h_in.readObject();
					
					//Send summary file to the requesting neighbour i.e upload neighbour
					if (req.equals("SUMMARY")) {
						System.out.println("Sending chunk list (" + own_files.size() + " chunks)");

						//Send number of chunks
						h_out.writeObject(own_files.size());
						
						//Send name of individual chunks
						for (int i = 0; i < own_files.size(); i++)
							h_out.writeObject(own_files.get(i).toString());
						
					} else if (req.equals("FILES")) {
						System.out.println("Sending received chunks");
						
						//Read number of missing chunks from upload neighbour
						int num_of_files = (int) h_in.readObject();
						
						for (int i = 0; i < num_of_files; i++) {
							//Get missing file name from neighbour
							File miss_file = new File((String) h_in.readObject());
							InputStream f_in = new FileInputStream(miss_file);
							byte[] bytes = new byte[1000 * 100];
							int len = f_in.read(bytes);
							String fnm = miss_file.getName().substring(0, 3);
							System.out.println("Sending the missing chunk  " + fnm + " of length " + len + " bytes");

							//Send missing chunks to the upload neighbour
							h_out.writeObject(miss_file.toString());
							h_out.writeObject(len);
							h_out.write(bytes);
						}
					}
					h_out.flush();

				}

			} catch (SocketException e) {
				System.out.println("Socket connection Reset");
			}
			catch (EOFException e){
				System.out.println("143");
			}

			catch (IOException e) {
				System.out.println("Disconnect with upstream neighbour");
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				System.out.println("Class not found");
			} finally {
				try {
					System.out.println("Close connection with upstream neighbour");

					h_in.close();
					h_out.close();
					conn.close();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private static class DownloadFiles extends Thread {
		private int download_port;
		private int neighbour_no;
		private ObjectInputStream sock_in;
		private ObjectOutputStream sock_out;
		private Socket conn1;

		//Set neighbour and download port for the current client
		public DownloadFiles(int neighbour_no, int download_port) {
			this.neighbour_no = neighbour_no;
			this.download_port = download_port;

		}

		public void run() {

			while (true) {
				boolean is_connected = false;
				try {
					//Connect to the download port of neighbour
					System.out.println("Attempting to connect to peer " + neighbour_no + " on port " + download_port);
					conn1 = new Socket("localhost", download_port);
					

					//initialize input and output streams for the current client
					sock_out = new ObjectOutputStream(conn1.getOutputStream());
					sock_in = new ObjectInputStream(conn1.getInputStream());
					is_connected = true;

				

					System.out.println("Connected to peer " + neighbour_no + " on port " + download_port);
				} catch (ConnectException e) {
					System.out.println("Connection refused to peer " + neighbour_no + " on port " + download_port);
				} catch (UnknownHostException e) {
					System.out.println("Trying to connect to unknown host");

					System.out.println(is_connected);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (is_connected) {

					try {
						//Request summary file from neighbour
						System.out.println("Getting summary file from peer " + neighbour_no);
						sock_out.writeObject("SUMMARY");
						ArrayList<String> missing_chunks = new ArrayList<String>();

						//Read number of chunks neighbour has
						int num_of_chunks = (int) sock_in.readObject();

						for (int i = 0; i < num_of_chunks; i++) {
							
							//Read missing chunk names from the upload neighbour and add it to missing chunk list
							String missing_neighbour_chunk = (String) sock_in.readObject();
							if (!own_files.contains(missing_neighbour_chunk)) {
								missing_chunks.add(missing_neighbour_chunk);
							}
						}
						
						//As long as missing chunks are present request files from upload neighbour
						if (missing_chunks.size() > 0) {
							System.out.println("Request for chunks from Peer " + neighbour_no);
							
							//Send request for chunks and number of missing chunks
							sock_out.writeObject("FILES");
							sock_out.writeObject(missing_chunks.size());

						//Send missing chunk names one at a time
							for (String p : missing_chunks) {
								sock_out.writeObject(p);
							}
							sock_out.flush();

							//Get missing chunks sent by upload neighbour
							for (String s : missing_chunks) {
								get_file(sock_in);

							}
							update_summary_file();
						} else {
							//When no more chunks are to be downloaded close connection with upload neighbour
							System.out.println("All chunks downloaded from peer " + neighbour_no);
						}

					} catch (IOException e) {
						System.out.println("Disconnect with peer " + neighbour_no);
					} catch (ClassNotFoundException e) {
						System.out.println("No Class");

					} finally {
						try {
							System.out.println("Closing connection with the peer " + neighbour_no);
							sock_in.close();
							sock_out.close();
							conn1.close();

						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				//Merge once all chunks have been received
				if (own_files.size() == num_of_chunks) {
					System.out.println("All chunks have been recieved.Proceeding with File Merge.");
					try {
						FileSplit.mergeFiles(own_files, "/home/anirudh/smitha/BitTorrent Implementation_v2/Client"
								+ client_num + "/" + "merged_file");
						

					} catch (IOException e) {
						System.out.println("Cannot merge");
						e.printStackTrace();
					} finally {
						break;

					}

				} else {
					try {
						//Wait until peer thread has been started
						System.out.println("Missing " + (num_of_chunks - own_files.size())
								+ " chunks. Waiting 6 seconds to reconnect with client " + neighbour_no);
						Thread.sleep(6000);
					} catch (InterruptedException iException) {
					}
				}
			}

		}
	}

	// main method
	public static void main(String args[]) throws ClassNotFoundException, IOException {
		Client client = new Client();
		// Get the file from the file owner
		client.DownloadFromFileOwner();

		//Download chunks from neighbour
		new DownloadFiles(neighbour_no, download_port).start();

		// establish server
		ServerSocket listener = new ServerSocket(port);
		System.out.println("Listening on port " + port);
		try {
			while (true) {
				new Handler_Client(listener.accept()).start();
			}
		} finally {
			listener.close();
		}
	}
}
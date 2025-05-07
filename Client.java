import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    // Server host address and port, and input request file
    private final String host;
    private final int port;
    private final String requestFile;

    // Constructor to initialize client settings
    public Client(String host, int port, String requestFile) {
        this.host = host;
        this.port = port;
        this.requestFile = requestFile;
    }

    public static void main(String[] args) {
        // Expect 3 arguments: host, port, and request file path
        if (args.length != 3) {
            System.out.println("Usage: java Client <host> <port> <requestFile>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String requestFile = args[2];

        try {
            // Create and run the client
            new Client(host, port, requestFile).run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Main method to run the client logic
    public void run() throws IOException {
        // Read input commands from the file
        Scanner scanner = new Scanner(new File(requestFile));

        // Connect to the server
        Socket socket = new Socket(host, port);

        // Setup I/O streams for communication
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Process each line from the input file
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;  // Skip empty lines

            // Split command into parts: command, key, and value
            String[] parts = line.split(" ", 3);
            String command = parts[0];
            String key = parts.length > 1 ? parts[1] : "";
            String value = parts.length > 2 ? parts[2] : "";

            // Reject lines exceeding the message limit
            if ((key + " " + value).length() > 970) {
                System.out.println(line + ": ERROR - exceeds 970 characters, ignored.");
                continue;
            }

            // Format the request message and send to server
            String requestMessage = buildRequestMessage(command, key, value);
            out.println(requestMessage);

            // Read and print the response from the server
            String response = in.readLine();
            System.out.println(line + ": " + response);
        }

        // Close the connection after all lines are processed
        socket.close();
    }

    // Build a properly formatted request message with length prefix
    private String buildRequestMessage(String command, String key, String value) {
        switch (command) {
            case "PUT":
                // "P" command includes both key and value
                return String.format("%03d P %s %s", 5 + key.length() + value.length(), key, value);
            case "GET":
                // "G" command includes only the key
                return String.format("%03d G %s", 4 + key.length(), key);
            case "READ":
                // "R" command includes only the key
                return String.format("%03d R %s", 4 + key.length(), key);
            default:
                // Invalid command
                System.out.println(command + ": ERROR - Unknown command");
                return "";
        }
    }
}


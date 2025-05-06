import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final int MAX_CLIENTS = 100;  // Maximum number of clients the server can handle at once
    private static final ConcurrentHashMap<String, String> tupleSpace = new ConcurrentHashMap<>();  // Shared tuple space
    private static final AtomicInteger totalOps = new AtomicInteger(0);  // Total number of operations
    private static final AtomicInteger putCount = new AtomicInteger(0);  // Count of PUT operations
    private static final AtomicInteger getCount = new AtomicInteger(0);  // Count of GET operations
    private static final AtomicInteger readCount = new AtomicInteger(0);  // Count of READ operations
    private static final AtomicInteger errorCount = new AtomicInteger(0);  // Count of errors
    private static final AtomicInteger clientCount = new AtomicInteger(0);  // Count of connected clients

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Server <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTS); // Create a thread pool

        // Start the statistics reporting thread
        new Thread(() -> reportStatistics()).start();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();  // Accept incoming client connections
                System.out.println("New client connected from " + clientSocket.getInetAddress());
                clientCount.incrementAndGet();
                pool.execute(() -> handleClient(clientSocket)); // Handle each client in a separate thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // PUT operation
    private static String put(String key, String value) {
        if (tupleSpace.containsKey(key)) {
            return "ERR " + key + " already exists";  // Key already exists
        }
        tupleSpace.put(key, value);
        return "OK (" + key + ", " + value + ") added";  // Success message for PUT operation
    }

    // GET operation
    private static String get(String key) {
        if (!tupleSpace.containsKey(key)) {
            return "ERR " + key + " does not exist";  // Key does not exist
        }
        String value = tupleSpace.remove(key);  // Remove and return the value
        return "OK (" + key + ", " + value + ") removed";  // Success message for GET operation
    }

    // READ operation
    private static String read(String key) {
        if (!tupleSpace.containsKey(key)) {
            return "ERR " + key + " does not exist";  // Key does not exist
        }
        String value = tupleSpace.get(key);  // Read but do not remove
        return "OK (" + key + ", " + value + ") read";  // Success message for READ operation
    }

    // Handle client requests
    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                String request = line.substring(4);  // Skip the first 3 characters (length prefix)
                String[] parts = request.split(" ", 3);
                String command = parts[0];
                String key = parts.length > 1 ? parts[1] : "";
                String value = parts.length > 2 ? parts[2] : "";

                String result = "";
                switch (command) {
                    case "P":
                        result = put(key, value);  // Execute PUT operation
                        break;
                    case "G":
                        result = get(key);  // Execute GET operation
                        break;
                    case "R":
                        result = read(key);  // Execute READ operation
                        break;
                    default:
                        result = "ERR unknown command";  // Unknown command
                        break;
                }

                // Send the result back to the client with a length prefix
                String response = String.format("%03d %s", result.length() + 4, result);
                out.println(response);
            }
        } catch (IOException e) {
            System.err.println("Client disconnected unexpectedly: " + e.getMessage());
        }
    }

    // Statistics reporting thread
    private static void reportStatistics() {
        while (true) {
            try {
                Thread.sleep(10000);  // Output statistics every 10 seconds
            } catch (InterruptedException e) {
                System.out.println("Statistics reporter interrupted.");
                return;
            }

            System.out.println("========== Tuple Space Statistics ==========");
            System.out.println("Tuple count       : " + tupleSpace.size());
            System.out.println("Avg tuple size    : " + String.format("%.2f", avgTupleSize()));
            System.out.println("Avg key size      : " + String.format("%.2f", avgKeySize()));
            System.out.println("Avg value size    : " + String.format("%.2f", avgValueSize()));
            System.out.println("Client count      : " + clientCount.get());
            System.out.println("Total operations  : " + totalOps.get());
            System.out.println("Total PUTs        : " + putCount.get());
            System.out.println("Total GETs        : " + getCount.get());
            System.out.println("Total READs       : " + readCount.get());
            System.out.println("Total ERRs        : " + errorCount.get());
            System.out.println("============================================");
        }
    }

    // Calculate average key size
    private static double avgKeySize() {
        return tupleSpace.keySet().stream().mapToInt(String::length).average().orElse(0);
    }

    // Calculate average value size
    private static double avgValueSize() {
        return tupleSpace.values().stream().mapToInt(String::length).average().orElse(0);
    }

    // Calculate average tuple size (key + value size)
    private static double avgTupleSize() {
        return tupleSpace.entrySet().stream()
                .mapToInt(entry -> entry.getKey().length() + entry.getValue().length())
                .average().orElse(0);
    }
}

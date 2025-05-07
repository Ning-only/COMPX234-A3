import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    // Maximum number of simultaneous clients supported
    private static final int MAX_CLIENTS = 100;

    // Tuple space shared among all clients (thread-safe)
    private static final ConcurrentHashMap<String, String> tupleSpace = new ConcurrentHashMap<>();

    // Operation counters
    private static final AtomicInteger totalOps = new AtomicInteger(0);
    private static final AtomicInteger putCount = new AtomicInteger(0);
    private static final AtomicInteger getCount = new AtomicInteger(0);
    private static final AtomicInteger readCount = new AtomicInteger(0);
    private static final AtomicInteger errorCount = new AtomicInteger(0);
    private static final AtomicInteger clientCount = new AtomicInteger(0);

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Server <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);

        // Thread pool for handling multiple clients concurrently
        ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTS);

        // Start the statistics reporting thread
        new Thread(() -> reportStatistics()).start();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected from " + clientSocket.getInetAddress());
                clientCount.incrementAndGet();
                pool.execute(() -> handleClient(clientSocket)); // Handle client in a separate thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handle client communication and requests
    private static void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.length() < 5) {
                    out.println("010 ERR invalid format");
                    continue;
                }

                // Extract the command portion of the request (after 3-digit length + space)
                String request = line.substring(4);
                String[] parts = request.split(" ", 3);
                String command = parts[0];
                String key = parts.length > 1 ? parts[1] : "";
                String value = parts.length > 2 ? parts[2] : "";

                String result = "";
                switch (command) {
                    case "P":
                        result = put(key, value);
                        break;
                    case "G":
                        result = get(key);
                        break;
                    case "R":
                        result = read(key);
                        break;
                    default:
                        result = "ERR unknown command";
                        break;
                }

                // Prefix result with total length for response formatting
                String response = String.format("%03d %s", result.length() + 4, result);
                out.println(response);
            }

        } catch (IOException e) {
            System.err.println("Client disconnected unexpectedly: " + e.getMessage());
        }
    }

    // Handle PUT command: adds a new key-value pair if the key doesn't exist
    private static String put(String key, String value) {
        totalOps.incrementAndGet();
        putCount.incrementAndGet();
        if (tupleSpace.containsKey(key)) {
            errorCount.incrementAndGet();
            return "ERR " + key + " already exists";
        }
        tupleSpace.put(key, value);
        return "OK (" + key + ", " + value + ") added";
    }

    // Handle GET command: retrieves and removes the key-value pair
    private static String get(String key) {
        totalOps.incrementAndGet();
        getCount.incrementAndGet();
        if (!tupleSpace.containsKey(key)) {
            errorCount.incrementAndGet();
            return "ERR " + key + " does not exist";
        }
        String value = tupleSpace.remove(key);
        return "OK (" + key + ", " + value + ") removed";
    }

    // Handle READ command: retrieves the key-value pair without removing it
    private static String read(String key) {
        totalOps.incrementAndGet();
        readCount.incrementAndGet();
        if (!tupleSpace.containsKey(key)) {
            errorCount.incrementAndGet();
            return "ERR " + key + " does not exist";
        }
        String value = tupleSpace.get(key);
        return "OK (" + key + ", " + value + ") read";
    }

    // Periodically report server statistics every 10 seconds
    private static void reportStatistics() {
        while (true) {
            try {
                Thread.sleep(10000); // Sleep for 10 seconds
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

    // Calculate average key size in the tuple space
    private static double avgKeySize() {
        return tupleSpace.keySet().stream().mapToInt(String::length).average().orElse(0);
    }

    // Calculate average value size in the tuple space
    private static double avgValueSize() {
        return tupleSpace.values().stream().mapToInt(String::length).average().orElse(0);
    }

    // Calculate average tuple size (key + value)
    private static double avgTupleSize() {
        return tupleSpace.entrySet().stream()
                .mapToInt(entry -> entry.getKey().length() + entry.getValue().length())
                .average().orElse(0);
    }
}


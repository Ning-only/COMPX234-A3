import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class Server {
    private static final int MAX_CLIENTS = 100; // 最多同时处理的客户端数
    private static final ConcurrentHashMap<String, String> tupleSpace = new ConcurrentHashMap<>(); // 共享元组空间
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

        ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTS);

        // 启动统计信息线程
        new Thread(() -> reportStatistics()).start();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected from " + clientSocket.getInetAddress());
                clientCount.incrementAndGet();
                pool.execute(() -> handleClient(clientSocket)); // 多线程处理客户端
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 处理客户端请求
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

                String request = line.substring(4);  // 跳过前 3 个长度数字 + 空格
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

                // 加上长度前缀后发送回客户端
                String response = String.format("%03d %s", result.length() + 4, result);
                out.println(response);
            }

        } catch (IOException e) {
            System.err.println("Client disconnected unexpectedly: " + e.getMessage());
        }
    }

    // PUT 操作
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

    // GET 操作
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

    // READ 操作
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

    // 统计信息线程
    private static void reportStatistics() {
        while (true) {
            try {
                Thread.sleep(10000); // 每 10 秒执行一次
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

    private static double avgKeySize() {
        return tupleSpace.keySet().stream().mapToInt(String::length).average().orElse(0);
    }

    private static double avgValueSize() {
        return tupleSpace.values().stream().mapToInt(String::length).average().orElse(0);
    }

    private static double avgTupleSize() {
        return tupleSpace.entrySet().stream()
                .mapToInt(entry -> entry.getKey().length() + entry.getValue().length())
                .average().orElse(0);
    }
}


import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
public class Server {
    private static final int MAX_CLIENTS = 100;  // 最多支持的客户端数
    private static final ConcurrentHashMap<String, String> tupleSpace = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Server <port>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTS); // 创建线程池

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();  // 接受客户端连接
                System.out.println("New client connected from " + clientSocket.getInetAddress());
                pool.execute(() -> handleClient(clientSocket)); // 多线程处理客户端
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static String put(String key, String value) {
        if (tupleSpace.containsKey(key)) {
            return "ERR " + key + " already exists";  // 键已存在
        }
        tupleSpace.put(key, value);
        return "OK (" + key + ", " + value + ") added";  // 返回成功消息
    }

    private static String get(String key) {
        if (!tupleSpace.containsKey(key)) {
            return "ERR " + key + " does not exist";  // 键不存在
        }
        String value = tupleSpace.remove(key);  // 删除并返回值
        return "OK (" + key + ", " + value + ") removed";
    }

    private static String read(String key) {
        if (!tupleSpace.containsKey(key)) {
            return "ERR " + key + " does not exist";  // 键不存在
        }
        String value = tupleSpace.get(key);  // 读取但不删除
        return "OK (" + key + ", " + value + ") read";
    }
    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                String request = line.substring(4);  // 跳过前3个字符（长度前缀）
                String[] parts = request.split(" ", 3);
                String command = parts[0];
                String key = parts.length > 1 ? parts[1] : "";
                String value = parts.length > 2 ? parts[2] : "";

                String result = "";
                switch (command) {
                    case "P":
                        result = put(key, value);  // 执行 PUT 操作
                        break;
                    case "G":
                        result = get(key);  // 执行 GET 操作
                        break;
                    case "R":
                        result = read(key);  // 执行 READ 操作
                        break;
                    default:
                        result = "ERR unknown command";  // 未知命令
                        break;
                }

                // 将结果加上长度前缀后返回给客户端
                String response = String.format("%03d %s", result.length() + 4, result);
                out.println(response);
            }
        } catch (IOException e) {
            System.err.println("Client disconnected unexpectedly: " + e.getMessage());
        }
    }
    private static void reportStatistics() {
        while (true) {
            try {
                Thread.sleep(10000);  // 每10秒输出一次统计信息
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

}

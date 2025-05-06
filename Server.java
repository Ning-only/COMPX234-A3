import java.io.*;
import java.net.*;
import java.util.concurrent.*;
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
}

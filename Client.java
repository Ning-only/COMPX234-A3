import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private final String host;
    private final int port;
    private final String requestFile;

    public Client(String host, int port, String requestFile) {
        this.host = host;
        this.port = port;
        this.requestFile = requestFile;
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java Client <host> <port> <requestFile>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String requestFile = args[2];

        try {
            new Client(host, port, requestFile).run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() throws IOException {
        Scanner scanner = new Scanner(new File(requestFile));
        Socket socket = new Socket(host, port);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split(" ", 3);
            String command = parts[0];
            String key = parts.length > 1 ? parts[1] : "";
            String value = parts.length > 2 ? parts[2] : "";

            // 检查长度限制
            if ((key + " " + value).length() > 970) {
                System.out.println(line + ": ERROR - exceeds 970 characters, ignored.");
                continue;
            }

            String requestMessage = buildRequestMessage(command, key, value);
            out.println(requestMessage);
            String response = in.readLine();
            System.out.println(line + ": " + response);
        }
        socket.close();
    }

    private String buildRequestMessage(String command, String key, String value) {
        switch (command) {
            case "PUT":
                return String.format("%03d P %s %s", 5 + key.length() + value.length(), key, value);
            case "GET":
                return String.format("%03d G %s", 4 + key.length(), key);
            case "READ":
                return String.format("%03d R %s", 4 + key.length(), key);
            default:
                System.out.println(command + ": ERROR - Unknown command");
                return "";
        }
    }
}



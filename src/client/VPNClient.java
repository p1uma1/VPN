package client;

import security.SSLUtils;

import javax.net.ssl.*;
import java.io.*;

public class VPNClient {
    private static final String SERVER_HOST = "localhost"; // Change to your server's IP
    private static final int SERVER_PORT = 8443;

    public static void main(String[] args) {
        try {
            SSLSocket socket = createSSLSocket();
            System.out.println("✅ Connected to VPN Server");

            try (BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader serverResponse = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                while (true) {
                    System.out.print("🌍 Enter URL to fetch (or 'exit' to quit): ");
                    String input = userInput.readLine();

                    if ("exit".equalsIgnoreCase(input)) {
                        System.out.println("🔴 Exiting...");
                        break;
                    }

                    writer.println(input);

                    // Read server response until double newline
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = serverResponse.readLine()) != null) {
                        if (line.isEmpty()) {
                            break;
                        }
                        response.append(line).append("\n");
                    }
                    System.out.println("📩 Server Response:\n" + response.toString());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static SSLSocket createSSLSocket() throws Exception {
        SSLSocketFactory ssf = SSLUtils.getSSLSocketFactory();
        SSLSocket socket = (SSLSocket) ssf.createSocket(SERVER_HOST, SERVER_PORT);

        // Configure SSL parameters
        socket.setEnabledProtocols(new String[] {"TLSv1.3", "TLSv1.2"});

        socket.startHandshake();
        return socket;
    }
}
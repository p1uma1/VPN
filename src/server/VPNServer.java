package server;

import security.SSLUtils;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VPNServer {
    private static final int SERVER_PORT = 8443;
    private static final int MAX_THREADS = 10;
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);

    public static void main(String[] args) {
        try {
            // Initialize SSL context with our custom SSLUtils
            SSLServerSocketFactory ssf = SSLUtils.getSSLServerSocketFactory();
            SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(SERVER_PORT);

            // Configure SSL parameters
            serverSocket.setNeedClientAuth(true); // Require client authentication
            serverSocket.setEnabledProtocols(new String[] {"TLSv1.3", "TLSv1.2"});

            System.out.println("✅ SSL VPN Server started on port " + SERVER_PORT);

            while (true) {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                threadPool.submit(() -> handleClient(clientSocket));
            }
        } catch (Exception e) {
            System.err.println("Server Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClient(SSLSocket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            String urlToFetch;
            while ((urlToFetch = in.readLine()) != null) {
                System.out.println("Client requested: " + urlToFetch);

                try {
                    String response = forwardRequest(urlToFetch);
                    out.write(response + "\n\n");  // Add double newline as separator
                    out.flush();
                } catch (Exception e) {
                    out.write("Error fetching URL: " + e.getMessage() + "\n\n");
                    out.flush();
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private static String forwardRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        // Handle both chunked and regular responses
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;

            // Read all lines from the input stream
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }

            // Ensure the response is properly returned
            return response.toString();
        } finally {
            // Close the connection after reading the response
            connection.disconnect();
        }
    }

}
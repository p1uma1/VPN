package server;

import Security.SSLUtils;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class Server {
    public static void main(String[] args) throws Exception {
        // Set to true if you need client authentication.
        boolean needClientAuth = false; // Usually false, unless you specifically require client certificates.
        SSLServerSocket serverSocket = (SSLServerSocket) SSLUtils.getSSLServerSocketFactory().createServerSocket(4443);

        System.out.println("Server started. Waiting for connections...");

        while (true) {
            try (SSLSocket clientSocket = (SSLSocket) serverSocket.accept()) {
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Received from client: " + inputLine);
                    out.println("Server response: " + inputLine.toUpperCase()); // Echo back in uppercase
                }

                System.out.println("Client disconnected.");
            } catch (IOException e) {
                System.err.println("Error handling client connection: " + e.getMessage());
                // Handle the exception appropriately (e.g., log, continue, break).
            }
        }
    }
}
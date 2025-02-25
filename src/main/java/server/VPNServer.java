package server;

import Security.SSLUtils;
import ui.VPNServerGUI;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.*;

public class VPNServer {
    private static final int PORT = 443; // Client-server communication via SSL
    private SSLServerSocket serverSocket;
    private boolean running = false;
    private VPNServerGUI ui;

    public VPNServer(VPNServerGUI ui) {
        this.ui = ui;
    }

    public void startServer() {
        try {
            // Use SSL for client-server communication
            serverSocket = (SSLServerSocket) SSLUtils.getSSLServerSocketFactory().createServerSocket(PORT);
            ui.log("✅ SSL VPN Server started on port " + PORT);
            running = true;

            while (running) {
                try {
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    String userAddress = clientSocket.getInetAddress().getHostAddress();
                    ui.addUser(userAddress);
                    new Thread(new VPNHandler(clientSocket, ui, userAddress)).start();
                } catch (IOException e) {
                    ui.log("❌ Error accepting connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            ui.log("❌ Server Error: " + e.getMessage());
        } catch (Exception e) {
            ui.log("❌ SSL Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            ui.log("🛑 VPN Server stopped.");
        } catch (IOException e) {
            ui.log("❌ Error stopping server: " + e.getMessage());
        }
    }
}

class VPNHandler implements Runnable {
    private final SSLSocket clientSocket;
    private final VPNServerGUI ui;
    private final String userAddress;

    public VPNHandler(SSLSocket socket, VPNServerGUI ui, String userAddress) {
        this.clientSocket = socket;
        this.ui = ui;
        this.userAddress = userAddress;
    }

    @Override
    public void run() {
        try (
                InputStream clientIn = clientSocket.getInputStream();
                OutputStream clientOut = clientSocket.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientIn));
                PrintWriter writer = new PrintWriter(clientOut, true)
        ) {
            // Read HTTP request from client.
            String requestLine = reader.readLine();
            if (requestLine == null) {
                ui.log("Client " + userAddress + " disconnected.");
                return;
            }

            ui.log("📥 Incoming Request from " + userAddress + ": " + requestLine);

            // Read additional headers; look for the Host header.
            String line, host = null;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("host: ")) {
                    host = line.substring(6).trim();
                }
            }

            // Validate host header.
            if (host == null) {
                ui.log("❌ No Host Header Found for " + userAddress + "!");
                sendErrorResponse(writer, 400, "Bad Request: Missing Host Header");
                return;
            }

            // Forward the request to the target website using a raw byte transfer.
            forwardHttpRequest(host, requestLine, clientOut);
        } catch (IOException e) {
            ui.log("❌ Error handling user " + userAddress + ": " + e.getMessage());
        } finally {
            ui.removeUser(userAddress);
            try {
                clientSocket.close();
            } catch (IOException e) {
                ui.log("❌ Error closing client socket for " + userAddress + ": " + e.getMessage());
            }
        }
    }

    /**
     * Forwards the HTTP request to the target website using a regular socket,
     * then relays the full response (headers and body) back to the client using raw byte transfer.
     */
    private void forwardHttpRequest(String host, String requestLine, OutputStream clientOut) {
        try (Socket serverSocket = new Socket(host, 80);
             // We still use a PrintWriter to send the HTTP request.
             PrintWriter serverWriter = new PrintWriter(serverSocket.getOutputStream(), true)
        ) {
            ui.log("🌍 Forwarding request from " + userAddress + " to " + host);

            // Validate and correct the request line if necessary.
            if (!requestLine.startsWith("GET") && !requestLine.startsWith("POST")) {
                requestLine = "GET / HTTP/1.1";
            }

            // Send a well-formed HTTP request to the target server.
            serverWriter.println(requestLine);
            serverWriter.println("Host: " + host);
            serverWriter.println("User-Agent: Mozilla/5.0 (Java VPN Client)");
            serverWriter.println("Accept: */*");
            serverWriter.println("Connection: close");
            serverWriter.println();
            serverWriter.flush();

            // Use raw byte streams to forward the response.
            InputStream serverIn = serverSocket.getInputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = serverIn.read(buffer)) != -1) {
                clientOut.write(buffer, 0, bytesRead);
            }
            clientOut.flush();
        } catch (IOException e) {
            ui.log("❌ Error forwarding request for user " + userAddress + ": " + e.getMessage());
            sendErrorResponse(new PrintWriter(clientOut, true), 502, "Bad Gateway: Unable to reach target host");
        }
    }

    /**
     * Sends a standard HTTP error response to the client.
     */
    private void sendErrorResponse(PrintWriter writer, int statusCode, String message) {
        writer.println("HTTP/1.1 " + statusCode + " " + message);
        writer.println("Content-Type: text/plain");
        writer.println("Connection: close");
        writer.println();
        writer.println(message);
        writer.flush();
    }
}

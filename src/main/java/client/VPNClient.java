package client;

import Security.SSLUtils;
import ui.VPNClientGUI;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLHandshakeException;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VPNClient {
    private static final String VPN_SERVER_IP = "localhost"; // Or remote VPN server IP
    private static final int VPN_SERVER_PORT = 443;            // VPN server's SSL port
    private static final int LOCAL_PROXY_PORT = 3128;          // Local proxy port for clients

    private final VPNClientGUI ui;
    private volatile boolean running = false;
    private ServerSocket proxyServer;
    private SSLSocket vpnSocket;
    private BufferedReader vpnIn;
    private PrintWriter vpnOut;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public VPNClient(VPNClientGUI ui) {
        this.ui = ui;
        // Shutdown hook for graceful exit
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopClient));
    }

    public void startClient() {
        running = true;
        // Start VPN connection in its own thread
        threadPool.execute(this::connectToVPNServer);
        // Start local proxy listener
        threadPool.execute(this::startProxy);
    }

    /**
     * Connects to the VPN server over SSL.
     */
    private synchronized void connectToVPNServer() {
        try {
            vpnSocket = (SSLSocket) SSLUtils.getSSLSocketFactory().createSocket(VPN_SERVER_IP, VPN_SERVER_PORT);
            // Initialize readers and writers for this VPN connection.
            vpnIn = new BufferedReader(new InputStreamReader(vpnSocket.getInputStream()));
            vpnOut = new PrintWriter(vpnSocket.getOutputStream(), true);
            ui.log("🌐 Connected to VPN Server at " + VPN_SERVER_IP + ":" + VPN_SERVER_PORT + " via SSL");
        } catch (SSLHandshakeException e) {
            ui.log("❌ SSL Handshake failed: " + e.getMessage());
        } catch (Exception e) {
            ui.log("❌ Error connecting to VPN server: " + e.getMessage());
        }
    }

    /**
     * Starts the local proxy server to accept client connections.
     */
    private void startProxy() {
        try (ServerSocket server = new ServerSocket()) {
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(LOCAL_PROXY_PORT));
            proxyServer = server;
            ui.log("✅ VPN Client running as local proxy on port " + LOCAL_PROXY_PORT);
            while (running) {
                Socket clientSocket = proxyServer.accept();
                threadPool.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            ui.log("❌ Error starting proxy: " + e.getMessage());
        }
    }

    /**
     * Handles an individual client connection.
     */
    private void handleClient(Socket clientSocket) {
        try (InputStream clientIn = clientSocket.getInputStream();
             OutputStream clientOut = clientSocket.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(clientIn));
             PrintWriter writer = new PrintWriter(clientOut, true)) {

            // Read the initial request line.
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                sendErrorResponse(writer, 400, "Bad Request");
                return;
            }
            ui.log("📥 Incoming request: " + requestLine);

            // We only support GET and POST in this example.
            String[] parts = requestLine.split(" ");
            if (parts.length < 3 ||
                    !(parts[0].equalsIgnoreCase("GET") || parts[0].equalsIgnoreCase("POST"))) {
                ui.log("❌ Rejecting non-HTTP/GET/POST request: " + requestLine);
                sendErrorResponse(writer, 405, "Method Not Allowed");
                return;
            }

            forwardToVPN(requestLine, reader, clientOut);
        } catch (IOException e) {
            ui.log("❌ Error handling client: " + e.getMessage());
        }
    }

    /**
     * Forwards the HTTP request to the VPN server and relays the full response (headers and body)
     * back to the local client. The response headers are read as raw bytes until the header terminator
     * (CRLF CRLF) is found, then the remainder of the response is forwarded without interpreting the data.
     */
    private synchronized void forwardToVPN(String request, BufferedReader clientReader, OutputStream clientOut) {
        try {
            // Ensure VPN connection is live.
            if (vpnSocket == null || vpnSocket.isClosed()) {
                ui.log("🔄 Reconnecting to VPN server...");
                connectToVPNServer();
            }

            // Send the request line.
            vpnOut.println(request);
            // Forward remaining headers from client.
            String headerLine;
            while ((headerLine = clientReader.readLine()) != null && !headerLine.isEmpty()) {
                vpnOut.println(headerLine);
            }
            // End of request headers.
            vpnOut.println();
            vpnOut.flush();
            ui.log("🌍 Forwarded request to VPN server: " + request);

            // --- Read the response headers as raw bytes ---
            InputStream vpnRaw = vpnSocket.getInputStream();
            ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
            int state = 0; // to detect CRLF CRLF
            int b;
            while ((b = vpnRaw.read()) != -1) {
                headerBuffer.write(b);
                // Simple state machine for "\r\n\r\n"
                if (state == 0 && b == '\r') state = 1;
                else if (state == 1 && b == '\n') state = 2;
                else if (state == 2 && b == '\r') state = 3;
                else if (state == 3 && b == '\n') {
                    state = 4;
                    break;
                } else {
                    state = 0;
                }
            }
            byte[] headerBytes = headerBuffer.toByteArray();
            String headerString = new String(headerBytes, "ISO-8859-1"); // Use ISO-8859-1 for HTTP headers
            // Parse headers
            String[] headerLines = headerString.split("\\r\\n");
            String statusLine = headerLines.length > 0 ? headerLines[0] : "";
            StringBuilder responseHeaders = new StringBuilder();
            responseHeaders.append(statusLine).append("\r\n");
            int contentLength = -1;
            boolean chunked = false;
            for (int i = 1; i < headerLines.length; i++) {
                String lineStr = headerLines[i];
                if (lineStr.isEmpty()) continue;
                responseHeaders.append(lineStr).append("\r\n");
                String lower = lineStr.toLowerCase();
                if (lower.startsWith("content-length:")) {
                    try {
                        contentLength = Integer.parseInt(lineStr.substring("content-length:".length()).trim());
                    } catch (NumberFormatException e) {
                        ui.log("❌ Error parsing Content-Length: " + e.getMessage());
                    }
                } else if (lower.startsWith("transfer-encoding:") && lower.contains("chunked")) {
                    chunked = true;
                }
            }
            responseHeaders.append("\r\n");

            // Write headers to the client.
            clientOut.write(responseHeaders.toString().getBytes("ISO-8859-1"));
            clientOut.flush();

            // Check if there are extra bytes already read beyond the headers.
            String headerTerminator = "\r\n\r\n";
            int headerEndIndex = headerString.indexOf(headerTerminator) + headerTerminator.length();
            byte[] initialBodyBytes = null;
            if (headerBytes.length > headerEndIndex) {
                int extraLength = headerBytes.length - headerEndIndex;
                initialBodyBytes = new byte[extraLength];
                System.arraycopy(headerBytes, headerEndIndex, initialBodyBytes, 0, extraLength);
            }

            // --- Forward the response body ---
            if (chunked) {
                // For chunked encoding, we can use a BufferedReader to read chunk-size lines.
                BufferedReader chunkedReader = new BufferedReader(new InputStreamReader(vpnRaw, "ISO-8859-1"));
                // If there are already body bytes, forward them first.
                if (initialBodyBytes != null && initialBodyBytes.length > 0) {
                    clientOut.write(initialBodyBytes);
                    clientOut.flush();
                }
                String chunkSizeLine;
                while ((chunkSizeLine = chunkedReader.readLine()) != null) {
                    clientOut.write((chunkSizeLine + "\r\n").getBytes("ISO-8859-1"));
                    clientOut.flush();
                    int chunkSize;
                    try {
                        chunkSize = Integer.parseInt(chunkSizeLine.trim(), 16);
                    } catch (NumberFormatException e) {
                        ui.log("❌ Error parsing chunk size: " + e.getMessage());
                        break;
                    }
                    if (chunkSize == 0) {
                        // Last chunk; forward any trailing headers.
                        String trailingLine;
                        while ((trailingLine = chunkedReader.readLine()) != null) {
                            clientOut.write((trailingLine + "\r\n").getBytes("ISO-8859-1"));
                            if (trailingLine.isEmpty()) break;
                        }
                        break;
                    }
                    byte[] chunkData = new byte[chunkSize];
                    int totalRead = 0;
                    while (totalRead < chunkSize) {
                        int bytesRead = vpnRaw.read(chunkData, totalRead, chunkSize - totalRead);
                        if (bytesRead == -1) break;
                        totalRead += bytesRead;
                    }
                    clientOut.write(chunkData, 0, totalRead);
                    // Read and forward the trailing CRLF after chunk data.
                    byte[] crlf = new byte[2];
                    int r = vpnRaw.read(crlf);
                    if (r == 2) {
                        clientOut.write(crlf);
                    }
                    clientOut.flush();
                }
            } else if (contentLength >= 0) {
                // Forward any already-read initial body bytes.
                if (initialBodyBytes != null && initialBodyBytes.length > 0) {
                    clientOut.write(initialBodyBytes);
                    clientOut.flush();
                }
                byte[] buffer = new byte[4096];
                int remaining = contentLength - (initialBodyBytes == null ? 0 : initialBodyBytes.length);
                int bytesRead;
                while (remaining > 0 && (bytesRead = vpnRaw.read(buffer, 0, Math.min(buffer.length, remaining))) != -1) {
                    clientOut.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
                clientOut.flush();
            } else {
                // No Content-Length and not chunked; forward until end of stream.
                if (initialBodyBytes != null && initialBodyBytes.length > 0) {
                    clientOut.write(initialBodyBytes);
                    clientOut.flush();
                }
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = vpnRaw.read(buffer)) != -1) {
                    clientOut.write(buffer, 0, bytesRead);
                }
                clientOut.flush();
            }
        } catch (IOException e) {
            ui.log("❌ Error forwarding HTTP request to VPN server: " + e.getMessage());
        }
    }

    /**
     * Sends an HTTP error response to the client.
     */
    private void sendErrorResponse(PrintWriter writer, int statusCode, String message) {
        writer.printf("HTTP/1.1 %d %s\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\n%s\r\n",
                statusCode, message, message);
        writer.flush();
    }

    public void stopClient() {
        running = false;
        threadPool.shutdownNow();
        try {
            if (vpnOut != null) vpnOut.close();
            if (vpnIn != null) vpnIn.close();
            if (vpnSocket != null) vpnSocket.close();
            if (proxyServer != null && !proxyServer.isClosed()) proxyServer.close();
            ui.log("🛑 VPN Client stopped.");
        } catch (IOException e) {
            ui.log("❌ Error closing VPN connection: " + e.getMessage());
        }
    }
}

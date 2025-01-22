import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class VPNServer {
    private static final int PORT = 4000;
    private SecretKey secretKey;

    public VPNServer() throws Exception {
        // Generate AES key
        secretKey = KeyGenerator.getInstance("AES").generateKey();
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("VPN Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Start a thread for each client
                new Thread(() -> handleClient(clientSocket)).start();
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (InputStream in = clientSocket.getInputStream();
             OutputStream out = clientSocket.getOutputStream()) {

            // Receive and decrypt client data
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] buffer = new byte[1024];
            int bytesRead = in.read(buffer);
            byte[] decryptedData = cipher.doFinal(buffer, 0, bytesRead);

            System.out.println("Decrypted Data: " + new String(decryptedData));

            // Encrypt and send response to the client
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] response = cipher.doFinal("Hello from Server".getBytes());
            out.write(response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public static void main(String[] args) throws Exception {
        VPNServer server = new VPNServer();
        server.start();
    }
}

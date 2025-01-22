import java.io.*;
import java.net.Socket;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class VPNClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public void start() {
        try {
            // Load the shared secret key
            SecretKey secretKey = loadKey();

            try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                 OutputStream out = socket.getOutputStream();
                 InputStream in = socket.getInputStream()) {

                // Encrypt data and send to the server
                Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                byte[] encryptedData = cipher.doFinal("Hello from Client".getBytes());
                out.write(encryptedData);

                // Receive and decrypt server response
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
                byte[] buffer = new byte[1024];
                int bytesRead = in.read(buffer);
                byte[] decryptedData = cipher.doFinal(buffer, 0, bytesRead);

                System.out.println("Decrypted Response: " + new String(decryptedData));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SecretKey loadKey() throws IOException {
        try (FileInputStream fis = new FileInputStream("secretKey.key")) {
            byte[] keyBytes = fis.readAllBytes();
            return new SecretKeySpec(keyBytes, "AES");
        }
    }

    public static void main(String[] args) {
        new VPNClient().start();
    }
}

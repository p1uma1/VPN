import java.io.*;
import java.net.*;

public class VPNClient2 {
    private static final String SERVER_IP = "127.0.0.2"; // Update if server IP is different
    private static final int SERVER_PORT = 12345;       // Ensure this matches the server's port
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public VPNClient2() throws IOException {
        socket = new Socket(SERVER_IP, SERVER_PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public void start() {
        try {
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            String message;
            while (true) {
                System.out.print("Client2 - Enter message for server: ");
                message = userInput.readLine();
                out.println(message);  // Send message to server

                String serverResponse = in.readLine();  // Receive response from server
                System.out.println("Server response: " + serverResponse);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        VPNClient2 client = new VPNClient2();
        client.start();
    }
}

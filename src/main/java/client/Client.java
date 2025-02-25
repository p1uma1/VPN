package client;

import Security.SSLUtils;

import javax.net.ssl.SSLSocket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.UnknownHostException;

public class Client {
    private static final int SERVER_PORT = 4443;
    private static final String SERVER_HOST = "localhost";
    public static void main(String[] args){
        try(
                SSLSocket socket= (SSLSocket) SSLUtils.getSSLSocketFactory().createSocket(SERVER_HOST,SERVER_PORT);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(),true);
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

                ) {
            System.out.println("connected to server success");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

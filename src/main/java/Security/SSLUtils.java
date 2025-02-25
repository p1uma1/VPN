package Security;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;

public class SSLUtils {
    // Server certificate files
    private static final String SERVER_KEYSTORE = "C:/Users/User/Documents/UOR/6th sem/VPN/vpn/src/main/java/certificates/server_keystore.jks";
    private static final String SERVER_TRUSTSTORE = "C:\\Users\\User\\Documents\\UOR\\6th sem\\VPN\\vpn\\src\\main\\java\\certificates\\server_truststore.jks";

    // Client certificate files
    private static final String CLIENT_KEYSTORE = "C:\\Users\\User\\Documents\\UOR\\6th sem\\VPN\\vpn\\src\\main\\java\\certificates\\client_keystore.jks";
    private static final String CLIENT_TRUSTSTORE = "C:\\Users\\User\\Documents\\UOR\\6th sem\\VPN\\vpn\\src\\main\\java\\certificates\\client_truststore.jks";

    private static final String PASSWORD = "password"; // Change this to match your certificate passwords

    public static SSLServerSocketFactory getSSLServerSocketFactory() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        // Initialize Server KeyStore
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream keyStoreFile = new FileInputStream(SERVER_KEYSTORE)) {
            keyStore.load(keyStoreFile, PASSWORD.toCharArray());
        }

        // Initialize Server TrustStore
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream trustStoreFile = new FileInputStream(SERVER_TRUSTSTORE)) {
            trustStore.load(trustStoreFile, PASSWORD.toCharArray());
        }

        // Initialize KeyManagerFactory for server
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, PASSWORD.toCharArray());

        // Initialize TrustManagerFactory for server
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        return sslContext.getServerSocketFactory();
    }

    public static SSLSocketFactory getSSLSocketFactory() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        // Initialize Client KeyStore
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream keyStoreFile = new FileInputStream(CLIENT_KEYSTORE)) {
            keyStore.load(keyStoreFile, PASSWORD.toCharArray());
        }

        // Initialize Client TrustStore
        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (FileInputStream trustStoreFile = new FileInputStream(CLIENT_TRUSTSTORE)) {
            trustStore.load(trustStoreFile, PASSWORD.toCharArray());
        }

        // Initialize KeyManagerFactory for client
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, PASSWORD.toCharArray());

        // Initialize TrustManagerFactory for client
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        return sslContext.getSocketFactory();
    }
}
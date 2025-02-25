package ui;

import server.VPNServer;
import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class VPNServerGUI extends JFrame {
    private JTextArea logArea, userListArea;
    private JTextField serverStatusField;
    private JButton startServerButton, stopServerButton;
    private VPNServer server;
    private Thread serverThread;
    private Set<String> connectedUsers = new HashSet<>();

    public VPNServerGUI() {
        server = new VPNServer(this);
        setTitle("VPN Server");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(30, 30, 30));

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.setBackground(new Color(45, 45, 45));
        serverStatusField = new JTextField("Stopped", 20);
        serverStatusField.setEditable(false);
        serverStatusField.setForeground(Color.WHITE);
        serverStatusField.setBackground(new Color(70, 70, 70));

        startServerButton = createStyledButton("Start Server", new Color(0, 128, 0));
        stopServerButton = createStyledButton("Stop Server", new Color(128, 0, 0));
        stopServerButton.setEnabled(false);

        topPanel.add(new JLabel("Server Status:"));
        topPanel.add(serverStatusField);
        topPanel.add(startServerButton);
        topPanel.add(stopServerButton);
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2));
        logArea = createStyledTextArea("Logs:\n");
        userListArea = createStyledTextArea("Connected Users:\n");

        centerPanel.add(new JScrollPane(logArea));
        centerPanel.add(new JScrollPane(userListArea));
        add(centerPanel, BorderLayout.CENTER);

        startServerButton.addActionListener(e -> {
            serverThread = new Thread(server::startServer);
            serverThread.start();
            serverStatusField.setText("Running");
            startServerButton.setEnabled(false);
            stopServerButton.setEnabled(true);
        });

        stopServerButton.addActionListener(e -> {
            server.stopServer();
            serverStatusField.setText("Stopped");
            startServerButton.setEnabled(true);
            stopServerButton.setEnabled(false);
            clearConnectedUsers();
        });
        setVisible(true);
    }

    private JTextArea createStyledTextArea(String initialText) {
        JTextArea area = new JTextArea(initialText);
        area.setEditable(false);
        area.setForeground(Color.GREEN);
        area.setBackground(new Color(20, 20, 20));
        area.setFont(new Font("Consolas", Font.PLAIN, 14));
        area.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 80)));
        return area;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        return button;
    }

    public void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    public void addUser(String username) {
        SwingUtilities.invokeLater(() -> {
            connectedUsers.add(username);
            updateUserList();
        });
    }

    public void removeUser(String username) {
        SwingUtilities.invokeLater(() -> {
            connectedUsers.remove(username);
            updateUserList();
        });
    }

    private void updateUserList() {
        userListArea.setText("Connected Users:\n" + String.join("\n", connectedUsers));
    }

    private void clearConnectedUsers() {
        connectedUsers.clear();
        updateUserList();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(VPNServerGUI::new);
    }
}

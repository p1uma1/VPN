package ui;

import client.VPNClient;
import javax.swing.*;
import java.awt.*;

public class VPNClientGUI extends JFrame {
    private VPNClient vpnClient;
    private JTextArea logArea;
    private JButton startButton;
    private JButton stopButton;

    public VPNClientGUI() {
        // Set up the window
        setTitle("VPN Client");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(50, 50, 50)); // Dark background
        setLayout(new BorderLayout());

        // Create a text area for logging with style
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(0, 255, 0));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);

        // Create a panel for buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.setBackground(new Color(60, 60, 60));

        startButton = new JButton("Start VPN");
        styleButton(startButton, new Color(0, 128, 0));

        stopButton = new JButton("Stop VPN");
        styleButton(stopButton, new Color(128, 0, 0));

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Create the VPNClient instance
        vpnClient = new VPNClient(this);

        // Button listeners
        startButton.addActionListener(e -> {
            vpnClient.startClient();
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        });

        stopButton.addActionListener(e -> {
            vpnClient.stopClient();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        });

        stopButton.setEnabled(false); // Start with stop disabled
    }

    // Thread-safe method to log messages in the GUI
    public void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength()); // Scroll to the bottom
        });
    }

    // Helper method to style buttons
    private void styleButton(JButton button, Color backgroundColor) {
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VPNClientGUI ui = new VPNClientGUI();
            ui.setVisible(true);
        });
    }
}

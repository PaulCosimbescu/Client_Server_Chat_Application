package ChatApplication;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.time.*;
import java.time.format.*;

public class ChatClient {

    private String serverAddress;
    private Scanner in;
    private PrintWriter out;
    private JFrame frame = new JFrame("Chat Application");
    private JTextField textField = new JTextField(50);
    private JTextArea messageArea = new JTextArea(16, 50);
    private Socket socket;

    public static void main(String[] args) throws Exception {

        ChatClient client = new ChatClient();

        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.pack();
        client.frame.setLocationRelativeTo(null);
        client.frame.setVisible(true);

        client.run();
    }

    public ChatClient() {

        textField.setEditable(false);
        messageArea.setEditable(false);
        this.frame.getContentPane().add(textField, BorderLayout.SOUTH);
        this.frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        this.frame.pack();

        while(true) {
            this.serverAddress = getIP();

            if(serverAddress.equals("localhost")) {
                break;
            }

            if(serverAddress.equals("127.0.0.1")) {
                break;
            }

            JOptionPane.showMessageDialog(this.frame,
                    "IP can only be localhost or 127.0.0.1",
                    "Error",
                    JOptionPane.WARNING_MESSAGE);
        }

        hostAvailabilityCheck();

        // Send on enter then clear to prepare for next message
        textField.addActionListener(e -> {
            out.println(textField.getText());
            textField.setText("");
        });
    }


    // Method for getting IP
    private String getIP() {
        String IP = JOptionPane.showInputDialog(
                this.frame,
                "Choose an IP to connect too:",
                "IP selection",
                JOptionPane.PLAIN_MESSAGE
        );

        if(IP == null) {
            System.exit(0);
        }
        return IP;
    }

    private String getName() {
        String name = JOptionPane.showInputDialog(
                this.frame,
                "Enter Username:",
                "Username",
                JOptionPane.PLAIN_MESSAGE
        );

        if(name == null) {
            System.exit(0);
        }
        return name;
    }

    private String setPort(boolean isFirstClient) {
        String messageToClient;
        if(isFirstClient) {
            messageToClient = "Port number to listen:";
        } else {
            messageToClient = "Port number to connect:";
        }

            String port = JOptionPane.showInputDialog(
                    this.frame,
                    messageToClient,
                    "Client Port",
                    JOptionPane.PLAIN_MESSAGE
            );

            if(port == null) {
                System.exit(0);
            }
            return port;

    }

    // Check if the server is online
    private void hostAvailabilityCheck() {
        try (Socket socket = new Socket(serverAddress, 59001)) {
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this.frame,
                    "Connection to the server cannot be made!",
                    "Error",
                    JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }
    }

    public void run() throws IOException {
        try {
            socket = new Socket(serverAddress, 59001);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);

            while (in.hasNextLine()) {
                String line = in.nextLine();

                if (line.startsWith("FIRST_CLIENT")) {

                    out.println(setPort(true));

                } else if (line.startsWith("SUBMIT_PORT_TO_CONNECT")) {

                    out.println(setPort(false));

                } else if (line.startsWith("SUBMIT_YOUR_PORT")) {

                    out.println(setPort(true));
                } else if (line.startsWith("SUBMIT_NAME")) {

                    out.println(getName());

                } else if (line.startsWith("NAME_ACCEPTED")) {

                    this.frame.setTitle("Chatter - " + line.substring(13));
                    textField.setEditable(true);
                    messageArea.append(getDateAndTime() + "You are connected. \n");

                } else if (line.startsWith("MESSAGE")) {

                    messageArea.append(getDateAndTime() + line.substring(8) + "\n");
                } else if (line.startsWith("ERROR")) {
                    showError(line.substring(5));
                }
            }
        } finally {
            this.frame.setVisible(false);
            this.frame.dispose();
        }
    }

    // Method for warning message if the user input is wrong
    private void showError(String message) {
        JOptionPane.showMessageDialog(this.frame,
                message,
                "Error",
                JOptionPane.WARNING_MESSAGE);
    }

    private static String getDateAndTime() {
        DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss - ");
        LocalDateTime LDT = LocalDateTime.now();

        return DTF.format(LDT);
    }
}
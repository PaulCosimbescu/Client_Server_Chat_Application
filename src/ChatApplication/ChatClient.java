/**
 * References:
 * https://www.w3schools.com/java/java_date.asp
 * https://stackoverflow.com/questions/17147352/checking-if-server-is-online-from-java-code
 * https://www.udemy.com/course/java-the-complete-java-developer-course/
 * https://www.udemy.com/course/java-socket-programming-build-a-chat-application/
 * https://www.udemy.com/course/java-network-programming/
 * https://youtu.be/Uo5DY546rKY
 */

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
    private int port;
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

        // Keep asking for IP and port to connect to until a connection is made.
        do {
            serverAddress = getIP();
            port = getPort();

        } while (!hostAvailabilityCheck(serverAddress, port));

        textField.setEditable(false);
        messageArea.setEditable(false);
        this.frame.getContentPane().add(textField, BorderLayout.SOUTH);
        this.frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        this.frame.pack();

        // Send on enter then clear to prepare for next message
        textField.addActionListener(e -> {
            out.println(textField.getText());
            textField.setText("");
        });
    }

    public void run() throws IOException {
        try {
            socket = new Socket(serverAddress, port);
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

                    this.frame.setTitle("Chat Application - " + line.substring(13));
                    textField.setEditable(true);
                    messageArea.append(getDateAndTime() + "You are connected. Write list commands \n");

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

    // Method for getting IP
    private String getIP() {
        String IP = JOptionPane.showInputDialog(
                this.frame,
                "Choose an IP to connect too:",
                "IP selection",
                JOptionPane.PLAIN_MESSAGE
        );

        // Close if cancel button is pressed
        if(IP == null) {
            System.exit(0);
        }
        return IP;
    }

    // Get Server port
    private int getPort() {
        int inputPort;
        String stringPort;

        while(true) {
            stringPort = JOptionPane.showInputDialog(
                    this.frame,
                    "Enter server port:",
                    "Server port",
                    JOptionPane.PLAIN_MESSAGE
            );

            // Close if cancel button is pressed
            if(stringPort == null) {
                System.exit(0);
            }

            try {
                inputPort = Integer.parseInt(stringPort);
            } catch (Exception e) {
                showError("Port must pe an integer!");
                continue;
            }

            if (inputPort < 1 || inputPort > 65535) {
                showError("The port must be between 1 and 65535!");
                continue;
            }
            break;
        }
        return inputPort;
    }

    //Method for getting the name of the user
    private String getName() {
        String name;

        //Keep asking for username until it doesn't have a space in it
        while(true) {
             name = JOptionPane.showInputDialog(
                    this.frame,
                    "Enter Username:",
                    "Username",
                    JOptionPane.PLAIN_MESSAGE
            );

            // Close if cancel button is pressed
            if(name == null) {
                System.exit(0);
            }

            // Check for spaces in Username
            if(name.contains(" ")) {
                showError("Username cannot contain spaces.");
                continue;
            }

            return name;
        }
    }

    private int setPort(boolean isFirstClient) {
        int intClientPort;
        String stringClientPort;
        String messageToClient;

        if(isFirstClient) {
            messageToClient = "Port number to listen:";
        } else {
            messageToClient = "Port number to connect:";
        }

        while(true) {
            stringClientPort = JOptionPane.showInputDialog(
                    this.frame,
                    messageToClient,
                    "Client Port",
                    JOptionPane.PLAIN_MESSAGE
            );

            // Close if cancel button is pressed
            if (stringClientPort == null) {
                System.exit(0);
            }

            // See if the port is an integer
            try {
                intClientPort = Integer.parseInt(stringClientPort);
            } catch (NumberFormatException e) {
                showError("The port must be an integer!");
                continue;
            }

            // See if the port number is in the feasible region
            if (intClientPort < 1 || intClientPort > 65535) {
                showError("The port must be between 1 and 65535!");
                continue;
            }
            return intClientPort;
        }
    }

    // Check if the server is online
    private boolean hostAvailabilityCheck(String serverAddress, int port) {
        try (Socket ignored = new Socket(serverAddress, port)) {
            return true;
        } catch (IOException ex) {
            showError("Connection to the server cannot be made!");
            return false;
        }
    }

    // Method for warning message if the user input is wrong
    private void showError(String message) {
        JOptionPane.showMessageDialog(this.frame,
                message,
                "Error",
                JOptionPane.WARNING_MESSAGE);
    }

    // Return current date and time
    private static String getDateAndTime() {
        DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss - ");
        LocalDateTime LDT = LocalDateTime.now();

        return DTF.format(LDT);
    }
}
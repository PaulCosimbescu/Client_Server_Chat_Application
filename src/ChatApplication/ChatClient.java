package ChatApplication;

import org.jetbrains.annotations.NotNull;

import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.time.*;
import java.time.format.*;

public class ChatClient {

    String serverAddress;
    Scanner in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JTextArea messageArea = new JTextArea(16, 50);

    public ChatClient(String serverAddress) {
        this.serverAddress = serverAddress;

        textField.setEditable(false);
        messageArea.setEditable(false);
        this.frame.getContentPane().add(textField, BorderLayout.SOUTH);
        this.frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        this.frame.pack();

        // Send on enter then clear to prepare for next message
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });
    }

    private String getName() {
        String name = JOptionPane.showInputDialog(
                this.frame,
                "Choose a screen name:",
                "Screen name selection",
                JOptionPane.PLAIN_MESSAGE
        );

        if(name == null) {
            System.exit(0);
        }
        return name;
    }

    private String getPort(int isFirstClient) {
        String port;
        if(isFirstClient == 1) {
            port = JOptionPane.showInputDialog(
                    this.frame,
                    "Choose a port with which other clients can connect with you:",
                    "Client port",
                    JOptionPane.PLAIN_MESSAGE
            );

            if(port == null) {
                System.exit(0);
            }
            return port;
        }

        port = JOptionPane.showInputDialog(
                this.frame,
                "Choose a port of an existing client:",
                "Client port",
                JOptionPane.PLAIN_MESSAGE
        );

        if(port == null) {
            System.exit(0);
        }
        return port;
    }

    public void run() throws IOException {
        try {
            Socket socket = new Socket(serverAddress, 59001);
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);

            while (in.hasNextLine()) {
                String line = in.nextLine();

                if (line.startsWith("FIRST_CLIENT")) {

                    out.println(getPort(1));

                } else if (line.startsWith("SUBMIT_PORT_TO_CONNECT")) {

                    out.println(getPort(0));

                } else if(line.startsWith("SUBMIT_YOUR_PORT")) {

                    out.println(getPort(1));
                }

                else if (line.startsWith("SUBMIT_NAME")) {

                    out.println(getName());

                } else if (line.startsWith("NAME_ACCEPTED")) {

                    this.frame.setTitle("Chatter - " + line.substring(13));
                    textField.setEditable(true);

                } else if (line.startsWith("MESSAGE")) {

                    messageArea.append(getDateAndTime() + line.substring(8) + "\n");
                }
            }
        } finally {
            this.frame.setVisible(false);
            this.frame.dispose();
        }
    }

    public static void main(String[] args) throws Exception {

        ChatClient client = new ChatClient("localhost");
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }


    @NotNull
    private static String getDateAndTime() {
        DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss - ");
        LocalDateTime LDT = LocalDateTime.now();

        return  DTF.format(LDT);
    }
}

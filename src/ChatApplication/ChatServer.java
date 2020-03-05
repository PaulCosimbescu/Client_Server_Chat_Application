package ChatApplication;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A multithreaded chat room server. When a client connects the server requests a screen
 * name by sending the client the text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received. After a client submits a unique name, the server acknowledges
 * with "NAMEACCEPTED". Then all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name. The broadcast messages are prefixed
 * with "MESSAGE".
 *
 * This is just a teaching example so it can be enhanced in many ways, e.g., better
 * logging. Another is to accept a lot of fun commands, like Slack.
 */
public class ChatServer {



    // All client names, so we can check for duplicates upon registration.
    private static Set<String> names = new HashSet<>();

    private static Set<String> clientIDs = new HashSet<>();

    private static Set<Integer> clientSocketHash = new HashSet<>();

    JFrame frame = new JFrame("ChatApplication.Server");
    JTextField textField = new JTextField(50);
    JTextArea messageArea = new JTextArea(16, 50);

    public ChatServer() {

        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.pack();

        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textField.getText();
                textField.setText("");
            }
        });
    }



    // The set of all the print writers for all the clients, used for broadcast.
    private static Set<PrintWriter> writers = new HashSet<>();

    public static void main(String[] args) throws Exception {


        ChatServer chatServer = new ChatServer();

        chatServer.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatServer.frame.setVisible(true);

        System.out.println("The chat server is running...");


        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(59001)) {
            while (true) {

                pool.execute(new Handler(listener.accept()));
            }
        }
    }

    /**
     * The client handler task.
     */
    private static class Handler implements Runnable {
        private String name;
        private String clientID;
        private int clientSocket;
        private Socket socket;
        private Scanner in;
        private PrintWriter out;

        /**
         * Constructs a handler thread, squirreling away the socket. All the interesting
         * work is done in the run method. Remember the constructor is called from the
         * server's main method, so this has to be as short as possible.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a screen name until a
         * unique one has been submitted, then acknowledges the name and registers the
         * output stream for the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {





            try {
                in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);



                // Keep requesting a ID until we get a unique one.

                if(clientSocketHash.isEmpty()) {
                    out.println("FIRSTCLIENT");
                    clientSocket = in.nextInt();


//                    while (true) {
//
//                        clientSocket = in.nextInt();
//                        if (clientSocket < 1) {
//                            return;
//                        }
//                        synchronized (clientSocketHash) {
//
//                                break;
//                        }
//                    }
                } else {
                    while (true) {
                        out.println("SUBMITSOCKET");
                        clientSocket = in.nextInt();
                        if (clientSocket < 1) {
                            return;
                        }
                        synchronized (clientSocketHash) {
                            if (!clientSocketHash.contains(clientSocket)) {

                                break;
                            }
                        }
                    }
                }

                // Keep requesting a ID until we get a unique one.
                while (true) {
                    out.println("SUBMITID");
                    clientID = in.nextLine();
                    if (clientID == null) {
                        return;
                    }
                    synchronized (clientIDs) {
                        if (!clientID.isEmpty() && !clientIDs.contains(clientID)) {
                            break;
                        }
                    }
                }



                // Keep requesting a name until we get a unique one.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.nextLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!name.isEmpty() && !names.contains(name)) {

                            System.out.println("New Client active: " + name
                                    + ", ID: "  + clientID
                                    + ", Socket: " + clientSocket);
                            break;
                        }
                    }
                }

                names.add(name);
                clientIDs.add(clientID);
                clientSocketHash.add(clientSocket);

                // Now that a successful name has been chosen, add the socket's print writer
                // to the set of all writers so this client can receive broadcast messages.
                // But BEFORE THAT, let everyone else know that the new person has joined!
                out.println("NAMEACCEPTED " + name + " ID - "  + clientID);
                    for (PrintWriter writer : writers) {
                    writer.println("MESSAGE " + name + " has joined");
                }
                writers.add(out);

                // Accept messages from this client and broadcast them.
                while (true) {
                    String input = in.nextLine();
                    if (input.toLowerCase().startsWith("/quit")) {
                        return;
                    }
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " +  name + ": " + input);
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            } finally {
                if (out != null) {
                    writers.remove(out);
                }
                if (name != null) {
                    System.out.println(name + " is leaving");
                    names.remove(name);
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + name + " has left");
                    }
                }
                try { socket.close(); } catch (IOException e) {}
            }
        }
    }

}

package ChatApplication;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {

    // All client names, and ports so we can check for duplicates upon registration.
    private static final Set<String> names = new HashSet<>();
    private static final Set<Integer> clientPortHash = new HashSet<>();

    // The HashMap used to send a private message to one user
    private static final HashMap<String, PrintWriter> clientHashMap = new HashMap<>();

    // The set of all the print writers for all the clients, used for broadcast.
    private static Set<PrintWriter> writers = new HashSet<>();

    private static JFrame frame = new JFrame("Server");
    private static JTextArea messageArea = new JTextArea(16, 50);

    public static void main(String[] args) throws Exception {

        messageArea.setEditable(false);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        messageArea.append(getDateAndTime() + "The chat server is running. \n");

        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(11111)) {
            while (true) {
                pool.execute(new Handler(listener.accept()));
            }
        }
    }

    private static class Handler implements Runnable {
        private String name;
        private String stringClientPort;
        private int intClientPort;
        private int yourPort;
        private Socket socket;
        private Scanner in;
        private PrintWriter out;

        public Handler(Socket socket) throws IOException {
            messageArea.append(getDateAndTime() + "Someone is trying to connect. \n");

            this.socket = socket;
            this.in = new Scanner(socket.getInputStream());
            this.out = new PrintWriter(socket.getOutputStream(), true);

        }

        public void run() {
            try {
                getClientPort();
                name = getName();

                out.println("NAME_ACCEPTED" + name);
                messageArea.append(getDateAndTime() + name + " has joined" + "\n");
                writers.add(out);
                clientHashMap.put(name, out);

                for (PrintWriter writer : writers) {
                    writer.println("MESSAGE " + name + " has joined");

                }

                // Accept messages from this client and broadcast them.
                acceptMessages();
            } catch (Exception e) {
                System.out.println(e.toString());
            } finally {

                if (out != null) {
                    writers.remove(out);
                }

                if(stringClientPort != null) {
                    clientPortHash.remove(intClientPort);
                }

                if (name != null) {
                    names.remove(name);
                    clientHashMap.remove(name, out);
                    messageArea.append(getDateAndTime() + name + " with port " + stringClientPort + ", has left" + "\n");

                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + name + " has left");
                    }
                }
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private String getName() {
            String clientName;

            while (true) {
                out.println("SUBMIT_NAME ");
                clientName = in.nextLine();

                if (clientName == null) {
                    continue;
                }

                synchronized (names) {
                    if (!clientName.isEmpty() && !names.contains(clientName)) {
                        names.add(clientName);
                        return clientName;
                    }
                    out.println("ERROR" + "Username already taken.");
                }
            }
        }

        private void getClientPort() {

            if(clientPortHash.isEmpty()) {
                out.println("FIRST_CLIENT");
                stringClientPort = in.nextLine();
                intClientPort = Integer.parseInt(stringClientPort);

                synchronized (clientPortHash) {
                    clientPortHash.add(intClientPort);
                }

            } else {
                while (true) {
                    out.println("SUBMIT_PORT_TO_CONNECT");
                    stringClientPort = in.nextLine();
                    intClientPort = Integer.parseInt(stringClientPort);

                    synchronized (clientPortHash) {
                        if (clientPortHash.contains(intClientPort)) {
                            break;
                        }
                        out.println("ERROR" + "No user with that port connected.");
                    }
                }

                while(true) {
                    out.println("SUBMIT_YOUR_PORT");
                    stringClientPort = in.nextLine();
                    yourPort = Integer.parseInt(stringClientPort);

                    synchronized (clientPortHash) {
                        if(!clientPortHash.contains(yourPort)) {
                            clientPortHash.add(yourPort);
                            break;
                        }
                        out.println("ERROR" + "The port number is already in use.");
                    }
                }
            }
        }

        private void acceptMessages() {
            while (true) {
                String input = in.nextLine();
                if (input.toLowerCase().startsWith("/quit")) {
                    return;
                }

                // Send the list of commands to the user that requires it
                if(input.toLowerCase().startsWith("list commands")) {
                    listCommands();
                } else if(input.contains(">>")) {

                    // Send a private message to one user
                    String person = input.substring(0, input.indexOf(">"));

                    if (clientHashMap.containsKey(person)) {
                        PrintWriter writer = clientHashMap.get(person);
                        writer.println("MESSAGE " + name + ": " + input);
                        out.println("MESSAGE " + name + ": " + input);
                    }
                } else {
                    // Show the list of active users
                    if(input.toLowerCase().startsWith("list users")) {
                        for (String s : names) {
                            out.println("MESSAGE " + s + "\n");
                        }
                    } else {
                        //Send message to everyone
                        for (PrintWriter writer : writers) {
                            writer.println("MESSAGE " + name + ": " + input);
                        }
                    }
                }
                messageArea.append(getDateAndTime() + name + ": " + input + "\n");
            }
        }

        // List of the current commands
        private void listCommands() {
            out.println("MESSAGE " + " list commands - Shows all the available commands");
            out.println("MESSAGE " + " list users - Lists all the active chat members \n");
            out.println("MESSAGE " + " /quit - Quit the chat application");
            out.println("MESSAGE " + " Enter the username of the person you want to send a private message and then put >>");

        }
    }

    // Return current date and time
    private static String getDateAndTime() {
        DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss - ");
        LocalDateTime LDT = LocalDateTime.now();

        return DTF.format(LDT);
    }
}

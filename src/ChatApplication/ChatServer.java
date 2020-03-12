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

        messageArea.append("The chat server is running. \n");

        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(59001)) {
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

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);

                // Keep requesting a port until we get a unique one.
                if(clientPortHash.isEmpty()) {
                    while(true) {
                        out.println("FIRST_CLIENT");
                        stringClientPort = in.nextLine();

                        try {
                            intClientPort = Integer.parseInt(stringClientPort);
                        } catch (NumberFormatException e) {
                            out.println("ERROR" + "The port must be an integer.");
                            continue;
                        }


                        if (intClientPort < 1 || intClientPort > 65535) {
                            out.println("ERROR" + "The port must be between 1 and 65535");
                            continue;
                        }

                        synchronized (clientPortHash) {
                            clientPortHash.add(intClientPort);
                            break;
                        }
                    }
                } else {
                    while (true) {
                        out.println("SUBMIT_PORT_TO_CONNECT");
                        stringClientPort = in.nextLine();

                        try {
                            intClientPort = Integer.parseInt(stringClientPort);
                        } catch (NumberFormatException e) {
                            out.println("ERROR" + "The port number must be an integer.");
                            continue;
                        }

                        if (intClientPort < 1 || intClientPort > 65535) {
                            out.println("ERROR" + "The port number must be between 1 and 65535.");
                            continue;
                        }

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

                        try {
                            yourPort = Integer.parseInt(stringClientPort);
                        } catch (NumberFormatException e) {
                            out.println("ERROR" + "The port number must be an integer.");
                            continue;
                        }

                        if (yourPort < 1 || yourPort > 65535) {
                            out.println("ERROR" + "The port number must be between 1 and 65535.");
                            continue;
                        }
                        synchronized (clientPortHash) {
                            if(!clientPortHash.contains(yourPort)) {
                                clientPortHash.add(yourPort);
                                break;
                            }
                            out.println("ERROR" + "The port number is already in use.");
                        }
                    }
                }

                // Keep requesting a name until we get a unique one.
                while (true) {
                    out.println("SUBMIT_NAME ");
                    name = in.nextLine();
                    if (name == null) {
                        continue;
                    }
                    synchronized (names) {
                        if (!name.isEmpty() && !names.contains(name)) {
                            names.add(name);
                            System.out.println("New Client active: " + name
                                    + ", Port: " + stringClientPort);
                            break;
                        }
                        out.println("ERROR" + "Username already taken.");
                    }
                }

                out.println("NAME_ACCEPTED" + name);
                messageArea.append(getDateAndTime() + name + " has joined" + "\n");

                for (PrintWriter writer : writers) {
                    writer.println("MESSAGE " + name + " has joined");

                }
                writers.add(out);

                clientHashMap.put(name, out);

                // Accept messages from this client and broadcast them.
                while (true) {
                    String input = in.nextLine();
                    if (input.toLowerCase().startsWith("/quit")) {
                        return;
                    }


                    // Send a private message to one user
                    if(input.contains(">>")) {

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
    }

    private static String getDateAndTime() {
        DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss - ");
        LocalDateTime LDT = LocalDateTime.now();

        return DTF.format(LDT);
    }
}

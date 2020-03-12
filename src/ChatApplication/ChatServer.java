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
    private static final HashMap<String, PrintWriter> clientHashMap = new HashMap<>();

    private static JFrame frame = new JFrame("Server");
    private static JTextArea messageArea = new JTextArea(16, 50);

    public ChatServer() {

    }

    // The set of all the print writers for all the clients, used for broadcast.
    private static Set<PrintWriter> writers = new HashSet<>();

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
                            continue;
                        }


                        if (intClientPort < 1 || intClientPort > 65535) {
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
                            continue;
                        }

                        if (intClientPort < 1 || intClientPort > 65535) {
                            continue;
                        }

                        synchronized (clientPortHash) {
                            if (clientPortHash.contains(intClientPort)) {
                                break;
                            }
                        }
                    }

                    while(true) {
                        out.println("SUBMIT_YOUR_PORT");
                        stringClientPort = in.nextLine();

                        try {
                            yourPort = Integer.parseInt(stringClientPort);
                        } catch (NumberFormatException e) {
                            continue;
                        }

                        if (yourPort < 1 || yourPort > 65535) {
                            continue;
                        }
                        synchronized (clientPortHash) {
                            if(!clientPortHash.contains(yourPort)) {
                                clientPortHash.add(yourPort);
                                break;
                            }
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

                    if(input.contains(">>")) {

                        String person = input.substring(0, input.indexOf(">"));    //extract the name of the destination user

                        if (clientHashMap.containsKey(person)) {
                            PrintWriter writer = clientHashMap.get(person);
                            writer.println("MESSAGE " + name + ": " + input);
                            out.println("MESSAGE " + name + ": " + input);
                        }
                    } else {

                        for (PrintWriter writer : writers) {
                            writer.println("MESSAGE " +  name + ": " + input);
                        }

                        if(input.toLowerCase().startsWith("list users")) {
                            for (String s : names) {
                                out.println("MESSAGE " + s + "\n");
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

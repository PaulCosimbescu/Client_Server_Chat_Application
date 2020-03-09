package ChatApplication;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {

    // All client names, so we can check for duplicates upon registration.
    private static final Set<String> names = new HashSet<>();

    private static final Set<Integer> clientPortHash = new HashSet<>();

    public static JFrame frame = new JFrame("Server");
    public static JTextField textField = new JTextField(50);
    public static JTextArea messageArea = new JTextArea(16, 50);

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

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        System.out.println("The chat server is running...");


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

                // Accept messages from this client and broadcast them.
                while (true) {
                    String input = in.nextLine();
                    if (input.toLowerCase().startsWith("/quit")) {
                        return;
                    }

                    messageArea.append(getDateAndTime() + name + ": " + input + "\n");
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " +  name + ": " + input);
                    }

                    if(input.toLowerCase().startsWith("list users")) {
                        for (String s : names) {
                            out.println("MESSAGE " + s + "\n");
                        }
                    }
                }
            } catch (Exception e) {

                System.out.println(e);
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
                try { socket.close(); } catch (IOException e) {}
            }
        }
    }

    @NotNull
    private static String getDateAndTime() {
        DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss - ");
        LocalDateTime LDT = LocalDateTime.now();

        return  DTF.format(LDT);
    }
}
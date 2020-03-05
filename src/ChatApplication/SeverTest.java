package ChatApplication;

import javax.swing.JFrame;


class ServerTest {
    public static void main(String[] args) {
        Server sally = new Server();
        sally.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        sally.startRunning();

    }

}

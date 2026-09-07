package client;

import javax.swing.SwingUtilities;

public class client {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            clientui app = new clientui();
            app.start();
        });
    }
}

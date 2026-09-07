package server;

import javax.swing.SwingUtilities;


//entry point de khoi chay server
public class server {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            serverui frame = new serverui();
            frame.setVisible(true);
        });
    }
}

package server;

import model.chatter;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class serverui extends JFrame {
    private JTextField txtPort;
    private JButton btnStartStop;
    private JTextArea txtLog;
    private JList<chatter> lstChatters;
    private DefaultListModel<chatter> listModel;

    private ServerSocket serverSocket;
    private boolean isRunning = false;

    // danh sach peer online
    private final List<chatter> onlineChatters = Collections.synchronizedList(new ArrayList<>());
    private final List<ClientHandler> clientHandlers = Collections.synchronizedList(new ArrayList<>());

    public serverui() {
        setTitle("P2P Hybrid Server");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents();
    }

    private void initComponents() {
        JPanel panelTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panelTop.add(new JLabel("Server Port:"));
        txtPort = new JTextField("5000", 8);
        panelTop.add(txtPort);

        btnStartStop = new JButton("Start Server");
        panelTop.add(btnStartStop);
        add(panelTop, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        lstChatters = new JList<>(listModel);
        JScrollPane scrollList = new JScrollPane(lstChatters);
        scrollList.setBorder(BorderFactory.createTitledBorder("Peers Online"));

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Server Log"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollList, scrollLog);
        splitPane.setDividerLocation(260);
        add(splitPane, BorderLayout.CENTER);

        btnStartStop.addActionListener(e -> {
            if (!isRunning) startServer();
            else stopServer();
        });
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    private void startServer() {
        try {
            int port = Integer.parseInt(txtPort.getText().trim());
            serverSocket = new ServerSocket(port);
            isRunning = true;
            btnStartStop.setText("Stop Server");
            txtPort.setEnabled(false);
            log(">>> Server chạy trên port: " + port);

            // chay luong nhan client
            new Thread(() -> {
                while (isRunning) {
                    try {
                        Socket socket = serverSocket.accept();
                        ClientHandler handler = new ClientHandler(socket);
                        clientHandlers.add(handler);
                        handler.start();
                    } catch (IOException e) {
                        if (!isRunning) break;
                    }
                }
            }).start();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null) serverSocket.close();
            synchronized (clientHandlers) {
                for (ClientHandler h : clientHandlers) h.closeConnection();
                clientHandlers.clear();
            }
            onlineChatters.clear();
            updateChatterListUI();
            btnStartStop.setText("Start Server");
            txtPort.setEnabled(true);
            log(">>> Server đã dừng.");
        } catch (IOException ignored) {}
    }

    private void updateChatterListUI() {
        SwingUtilities.invokeLater(() -> {
            listModel.clear();
            synchronized (onlineChatters) {
                for (chatter c : onlineChatters) listModel.addElement(c);
            }
        });
    }

    private void broadcast(String message, ClientHandler excludeHandler) {
        synchronized (clientHandlers) {
            for (ClientHandler handler : clientHandlers) {
                if (handler != excludeHandler) handler.sendMessage(message);
            }
        }
    }

    // xu ly tung ket noi
    private class ClientHandler extends Thread {
        private final Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private chatter currentChatter;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                String line;
                while ((line = reader.readLine()) != null) {
                    processMessage(line);
                }
            } catch (IOException ignored) {
            } finally {
                handleLogout();
            }
        }

        private void processMessage(String message) {
            if (message.startsWith("LOGIN|")) {
                String data = message.substring(6);
                chatter c = chatter.fromProtocolString(data);
                if (c != null) {
                    String remoteIp = socket.getInetAddress().getHostAddress();
                    if ("127.0.0.1".equals(c.getIp()) && !remoteIp.equals("127.0.0.1") && !remoteIp.equals("0:0:0:0:0:0:0:1")) {
                        c.setIp(remoteIp);
                    }
                    onlineChatters.removeIf(item -> item.getNickname().equalsIgnoreCase(c.getNickname()));
                    this.currentChatter = c;

                    onlineChatters.add(c);
                    updateChatterListUI();
                    log("[LOGIN] " + c);

                    // gui danh sach hien tai
                    StringBuilder sbList = new StringBuilder("INIT_LIST|");
                    synchronized (onlineChatters) {
                        for (int i = 0; i < onlineChatters.size(); i++) {
                            sbList.append(onlineChatters.get(i).toProtocolString());
                            if (i < onlineChatters.size() - 1) sbList.append(";");
                        }
                    }
                    sendMessage(sbList.toString());

                    // bao cho cac peer khac
                    broadcast("ADD_PEER|" + c.toProtocolString(), this);
                }
            } else if (message.startsWith("LOGOUT|")) {
                handleLogout();
            }
        }

        private void handleLogout() {
            if (currentChatter != null) {
                log("[LOGOUT] " + currentChatter);
                onlineChatters.remove(currentChatter);
                clientHandlers.remove(this);
                updateChatterListUI();
                broadcast("REMOVE_PEER|" + currentChatter.getNickname(), this);
                currentChatter = null;
                closeConnection();
            }
        }

        public void sendMessage(String msg) {
            if (writer != null) writer.println(msg);
        }

        public void closeConnection() {
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
    }
}

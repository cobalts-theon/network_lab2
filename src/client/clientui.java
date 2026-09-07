package client;

import model.chatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class clientui extends JFrame {
    private JLabel lblUserHeader;
    private JButton btnLogout;
    private JLabel lblListTitle;
    private JList<chatter> lstChatters;
    private DefaultListModel<chatter> listModel;
    private JLabel lblChatWith;
    private JTextArea txtChatArea;
    private JTextField txtMessage;
    private JButton btnSend;
    private JLabel lblStatus;

    private chatter myChatter;
    private boolean isLoggedIn = false;

    private ServerSocket p2pServerSocket;
    private Socket serverSocket;
    private BufferedReader serverReader;
    private PrintWriter serverWriter;

    private String serverIp = "127.0.0.1";
    private int serverPort = 5000;

    public clientui() {
        initComponents();
    }

    private void initComponents() {
        setTitle("P2P Hybrid Chat");
        setSize(750, 500);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel panelTop = new JPanel(new BorderLayout());
        panelTop.setBorder(new EmptyBorder(8, 12, 8, 12));
        panelTop.setBackground(new Color(240, 243, 246));

        lblUserHeader = new JLabel("Bạn: Chưa đăng nhập");
        lblUserHeader.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblUserHeader.setForeground(new Color(30, 60, 110));

        btnLogout = new JButton("Đăng Xuất");
        btnLogout.setFocusPainted(false);
        btnLogout.addActionListener(e -> logoutAndAskLogin());

        panelTop.add(lblUserHeader, BorderLayout.WEST);
        panelTop.add(btnLogout, BorderLayout.EAST);
        add(panelTop, BorderLayout.NORTH);

        JPanel panelLeft = new JPanel(new BorderLayout(5, 5));
        panelLeft.setBorder(new EmptyBorder(6, 6, 6, 6));

        lblListTitle = new JLabel("Người dùng online (0)");
        lblListTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        panelLeft.add(lblListTitle, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        lstChatters = new JList<>(listModel);
        lstChatters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstChatters.setFont(new Font("SansSerif", Font.PLAIN, 13));

        lstChatters.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateChatWithTarget();
            }
        });

        JScrollPane scrollList = new JScrollPane(lstChatters);
        panelLeft.add(scrollList, BorderLayout.CENTER);

        JPanel panelRight = new JPanel(new BorderLayout(6, 6));
        panelRight.setBorder(new EmptyBorder(6, 6, 6, 6));

        lblChatWith = new JLabel("Chưa chọn người nhận (chọn bên trái để chat)");
        lblChatWith.setFont(new Font("SansSerif", Font.BOLD, 13));
        panelRight.add(lblChatWith, BorderLayout.NORTH);

        txtChatArea = new JTextArea();
        txtChatArea.setEditable(false);
        txtChatArea.setLineWrap(true);
        txtChatArea.setWrapStyleWord(true);
        txtChatArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JScrollPane scrollChat = new JScrollPane(txtChatArea);
        panelRight.add(scrollChat, BorderLayout.CENTER);

        JPanel panelSend = new JPanel(new BorderLayout(6, 6));
        txtMessage = new JTextField();
        txtMessage.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnSend = new JButton("Gửi (P2P)");
        btnSend.setFocusPainted(false);
        panelSend.add(txtMessage, BorderLayout.CENTER);
        panelSend.add(btnSend, BorderLayout.EAST);

        panelRight.add(panelSend, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelLeft, panelRight);
        splitPane.setDividerLocation(230);
        add(splitPane, BorderLayout.CENTER);

        JPanel panelBottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        lblStatus = new JLabel("Sẵn sàng.");
        lblStatus.setFont(new Font("SansSerif", Font.ITALIC, 11));
        panelBottom.add(lblStatus);
        add(panelBottom, BorderLayout.SOUTH);

        btnSend.addActionListener(e -> sendP2PMessage());

        txtMessage.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendP2PMessage();
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanupConnections();
                System.exit(0);
            }
        });
    }

    private void updateChatWithTarget() {
        chatter selected = lstChatters.getSelectedValue();
        if (selected != null) {
            lblChatWith.setText("Đang chat trực tiếp với: " + selected.getNickname());
            txtMessage.requestFocusInWindow();
        } else {
            lblChatWith.setText("Chọn 1 người trong danh sách online bên trái để chat");
        }
    }

    private void updateOnlineHeader() {
        lblListTitle.setText("Người dùng online (" + listModel.size() + ")");
    }

    private void appendChatMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            txtChatArea.append(text + "\n");
            txtChatArea.setCaretPosition(txtChatArea.getDocument().getLength());
        });
    }

    public static int findAvailablePort(int startPort) {
        for (int port = startPort; port < startPort + 100; port++) {
            try (ServerSocket ss = new ServerSocket(port)) {
                return port;
            } catch (IOException ignored) {}
        }
        return startPort;
    }

    // hien popup dang nhap truoc
    public void start() {
        if (showLoginDialog()) {
            setVisible(true);
        } else {
            System.exit(0);
        }
    }

    public boolean showLoginDialog() {
        JDialog dialog = new JDialog((Frame) null, "Đăng Nhập Chat P2P", true);
        dialog.setSize(340, 150);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setResizable(false);

        JPanel panelFields = new JPanel(new BorderLayout(5, 5));
        panelFields.setBorder(new EmptyBorder(15, 20, 5, 20));

        JTextField txtNick = new JTextField();
        txtNick.setFont(new Font("SansSerif", Font.PLAIN, 13));

        panelFields.add(new JLabel("Nhập tên hiển thị:"), BorderLayout.NORTH);
        panelFields.add(txtNick, BorderLayout.CENTER);

        dialog.add(panelFields, BorderLayout.CENTER);

        JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        JButton btnCancel = new JButton("Thoát");
        JButton btnJoin = new JButton("Vào Chat");
        btnJoin.setFont(btnJoin.getFont().deriveFont(Font.BOLD));

        panelButtons.add(btnCancel);
        panelButtons.add(btnJoin);
        dialog.add(panelButtons, BorderLayout.SOUTH);

        final boolean[] success = {false};

        btnJoin.addActionListener(e -> {
            String nick = txtNick.getText().trim();
            if (nick.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập tên của bạn!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // tu dong tim port kha dung
            int myPort = findAvailablePort(6001);

            // mo port p2p
            try {
                p2pServerSocket = new ServerSocket(myPort);
                p2pServerSocket.setReuseAddress(true);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dialog, "Không thể mở cổng P2P!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // ket noi server
            try {
                serverSocket = new Socket(serverIp, serverPort);
                serverReader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream(), StandardCharsets.UTF_8));
                serverWriter = new PrintWriter(new OutputStreamWriter(serverSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException ex) {
                try { p2pServerSocket.close(); } catch (Exception ignored) {}
                JOptionPane.showMessageDialog(dialog, "Không thể kết nối đến Server trung tâm!\nHãy đảm bảo Server đã được bật.", "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // lay ip va gui login
            String myIp = serverSocket.getLocalAddress().getHostAddress();
            if (myIp == null || myIp.isEmpty() || myIp.equals("0.0.0.0")) {
                myIp = "127.0.0.1";
            }
            this.myChatter = new chatter(nick, myIp, myPort);

            serverWriter.println("LOGIN|" + myChatter.toProtocolString());

            // kiem tra phan hoi tu server
            try {
                String response = serverReader.readLine();
                if (response == null || response.startsWith("LOGIN_FAILED|")) {
                    String reason = (response != null && response.contains("|")) ? response.substring(response.indexOf("|") + 1) : "Tên đã tồn tại!";
                    try { p2pServerSocket.close(); } catch (Exception ignored) {}
                    try { serverSocket.close(); } catch (Exception ignored) {}
                    JOptionPane.showMessageDialog(dialog, reason, "Trùng Tên", JOptionPane.WARNING_MESSAGE);
                    txtNick.requestFocusInWindow();
                    txtNick.selectAll();
                    return;
                }

                if (response.startsWith("INIT_LIST|")) {
                    processServerSignal(response);
                }
            } catch (IOException ex) {
                try { p2pServerSocket.close(); } catch (Exception ignored) {}
                try { serverSocket.close(); } catch (Exception ignored) {}
                JOptionPane.showMessageDialog(dialog, "Lỗi kết nối: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            isLoggedIn = true;
            new Thread(new ServerListenerTask()).start();
            new Thread(new PeerListenerTask()).start();

            lblUserHeader.setText("Bạn: " + myChatter.getNickname());
            setTitle("P2P Hybrid Chat - " + myChatter.getNickname());
            lblStatus.setText("Sẵn sàng chat.");

            success[0] = true;
            dialog.dispose();
        });

        btnCancel.addActionListener(e -> {
            dialog.dispose();
        });

        txtNick.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    btnJoin.doClick();
                }
            }
        });

        dialog.setVisible(true);
        return success[0];
    }

    private void logoutAndAskLogin() {
        cleanupConnections();
        setVisible(false);
        txtChatArea.setText("");
        listModel.clear();
        updateOnlineHeader();
        lblChatWith.setText("Chọn 1 người trong danh sách online bên trái để chat");

        start();
    }

    private void cleanupConnections() {
        isLoggedIn = false;
        try {
            if (serverWriter != null && myChatter != null) {
                serverWriter.println("LOGOUT|" + myChatter.getNickname());
            }
        } catch (Exception ignored) {}

        try {
            if (p2pServerSocket != null && !p2pServerSocket.isClosed()) {
                p2pServerSocket.close();
            }
        } catch (IOException ignored) {}

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
    }

    private boolean isSelf(chatter c) {
        if (myChatter == null || c == null) return false;
        return c.getNickname().equalsIgnoreCase(myChatter.getNickname()) ||
               (c.getIp().equals(myChatter.getIp()) && c.getPort() == myChatter.getPort());
    }

    private void processServerSignal(String line) {
        if (line.startsWith("INIT_LIST|")) {
            String data = line.substring(10);
            SwingUtilities.invokeLater(() -> {
                listModel.clear();
                if (!data.isEmpty()) {
                    String[] items = data.split(";");
                    for (String item : items) {
                        chatter c = chatter.fromProtocolString(item);
                        // chi lay nguoi khac
                        if (c != null && !isSelf(c)) {
                            listModel.addElement(c);
                        }
                    }
                }
                updateOnlineHeader();
                if (!listModel.isEmpty() && lstChatters.getSelectedIndex() == -1) {
                    lstChatters.setSelectedIndex(0);
                }
            });
        } else if (line.startsWith("ADD_PEER|")) {
            String data = line.substring(9);
            chatter c = chatter.fromProtocolString(data);
            if (c != null && !isSelf(c)) {
                SwingUtilities.invokeLater(() -> {
                    if (!listModel.contains(c)) {
                        listModel.addElement(c);
                        updateOnlineHeader();
                        if (lstChatters.getSelectedIndex() == -1) {
                            lstChatters.setSelectedIndex(0);
                        }
                    }
                });
                appendChatMessage("[Hệ thống]: '" + c.getNickname() + "' đã online.");
            }
        } else if (line.startsWith("REMOVE_PEER|")) {
            String nickname = line.substring(12);
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < listModel.size(); i++) {
                    if (listModel.get(i).getNickname().equalsIgnoreCase(nickname)) {
                        listModel.remove(i);
                        updateOnlineHeader();
                        break;
                    }
                }
                updateChatWithTarget();
            });
            appendChatMessage("[Hệ thống]: '" + nickname + "' đã thoát.");
        }
    }

    // lang nghe server
    private class ServerListenerTask implements Runnable {
        @Override
        public void run() {
            try {
                String line;
                while (isLoggedIn && (line = serverReader.readLine()) != null) {
                    processServerSignal(line);
                }
            } catch (IOException e) {
                if (isLoggedIn) {
                    appendChatMessage("[Hệ thống]: Mất kết nối tới Central Server.");
                    SwingUtilities.invokeLater(() -> lblStatus.setText("Mất kết nối tới Server."));
                }
            }
        }
    }

    // lang nghe tin p2p
    private class PeerListenerTask implements Runnable {
        @Override
        public void run() {
            while (isLoggedIn && p2pServerSocket != null && !p2pServerSocket.isClosed()) {
                try {
                    Socket incomingSocket = p2pServerSocket.accept();
                    new Thread(() -> handleIncomingP2PConnection(incomingSocket)).start();
                } catch (IOException e) {
                    if (!isLoggedIn) break;
                }
            }
        }

        private void handleIncomingP2PConnection(Socket socket) {
            try (socket;
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                socket.setSoTimeout(5000);
                String line = reader.readLine();
                if (line != null && line.startsWith("P2P_MSG|")) {
                    String[] parts = line.substring(8).split("\\|", 2);
                    if (parts.length == 2) {
                        String senderNick = parts[0];
                        String msgText = parts[1];

                        writer.println("ACK");
                        appendChatMessage("[" + senderNick + " -> Tôi]: " + msgText);
                    }
                }
            } catch (IOException ignored) {}
        }
    }

    // gui tin p2p truc tiep
    private void sendP2PMessage() {
        chatter targetPeer = lstChatters.getSelectedValue();
        if (targetPeer == null) {
            if (listModel.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Hiện tại không có người nào khác online để chat!", "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            } else {
                lstChatters.setSelectedIndex(0);
                targetPeer = lstChatters.getSelectedValue();
            }
        }

        String msgText = txtMessage.getText().trim();
        if (msgText.isEmpty()) return;

        final chatter destination = targetPeer;
        final String content = msgText;

        txtMessage.setText("");

        new Thread(() -> {
            try {
                Socket p2pSocket = new Socket();
                p2pSocket.connect(new InetSocketAddress(destination.getIp(), destination.getPort()), 3000);
                p2pSocket.setSoTimeout(3000);

                PrintWriter writer = new PrintWriter(new OutputStreamWriter(p2pSocket.getOutputStream(), StandardCharsets.UTF_8), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(p2pSocket.getInputStream(), StandardCharsets.UTF_8));

                writer.println("P2P_MSG|" + myChatter.getNickname() + "|" + content);
                String ack = reader.readLine();
                p2pSocket.close();

                appendChatMessage("[Tôi -> " + destination.getNickname() + "]: " + content);

            } catch (IOException ex) {
                appendChatMessage("[Lỗi P2P]: Không gửi được tới " + destination.getNickname() + 
                                  " (" + destination.getIp() + ":" + destination.getPort() + "): " + ex.getMessage());
            }
        }).start();
    }
}

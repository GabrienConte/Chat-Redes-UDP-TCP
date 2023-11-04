package br.ufsm.politecnico.csi.redes;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Mensageiro {

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton exitButton;
    private JList<String> userList;
    private List<String> users = new ArrayList<>();



    private Map<String, Long> userAtivos = new HashMap<>();
    private final long tempoInativo = 30000;

    public Mensageiro() throws IOException {
        frame = new JFrame("Chat App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setWrapStyleWord(true);
        chatArea.setLineWrap(true);

        this.exitButton = new JButton("Sair");
        this.exitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exitChat();
            }
        });
        this.frame.add(this.exitButton, "South");
        this.frame.setVisible(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        frame.add(scrollPane, BorderLayout.CENTER);
        inputField = new JTextField();
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    sendMessage();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        frame.add(inputField, BorderLayout.SOUTH);
        frame.setVisible(true);

        new Thread(() -> {
            try {
                recebeMensagem();
            } catch (IOException e) {
                RuntimeException a;
                throw a = new RuntimeException(e);
            }
        }).start();
        users = new ArrayList<>();
        userList = new JList<>(users.toArray(new String[0]));
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUser = userList.getSelectedValue();
                if (selectedUser != null) {
                    openChatWindow(selectedUser);
                }
            }
        });
    }

    private void openChatWindow(String user) {
        JFrame chatFrame = new JFrame("Chat com " + user);
        chatFrame.setSize(300, 200);
        chatFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JLabel nameLabel = new JLabel("Conversando com " + user);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        chatFrame.add(nameLabel);
        chatFrame.setVisible(true);
    }


    private void checagemInatividade(String user) {
        long currentTime = System.currentTimeMillis();
        Long lastActivity = userAtivos.get(user);

        if (lastActivity != null && currentTime - lastActivity > tempoInativo) {
            userAtivos.remove(user);
        }
    }

    private void remocaoInativo() {
        long currentTime = System.currentTimeMillis();
        userAtivos.keySet().removeIf(user -> currentTime - userAtivos.get(user) > tempoInativo);
    }

    private void recebeMensagem() throws IOException {
        DatagramSocket socket = new DatagramSocket(8085);
        while (true) {
            byte[] buffer = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String msg = new String(buffer, 0, packet.getLength(), StandardCharsets.UTF_8);
            String endID = packet.getAddress().getHostAddress();
            userAtivos.put(endID, System.currentTimeMillis());
            checagemInatividade(endID);
            synchronized (this.chatArea) {
                this.chatArea.append(endID + ": " + msg + "\n");
            }
        }
    }

    private void exitChat() {
        int option = JOptionPane.showConfirmDialog(frame, "Você deseja sair do chat?", "Aviso", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private void sendMessage() throws IOException {
        remocaoInativo();
        String message = this.inputField.getText();
        if (!message.isEmpty()) {
            synchronized (chatArea) {
                chatArea.append("Você: " + message + "\n");
            }
            inputField.setText("");
            String ip = "255.255.255.255";
            if (message.startsWith("%")) {
                String[] msgArr = message.split(" ");
                ip = msgArr[0].replace("%", "");
                message = message.replace(msgArr[0], "");
            }

            if (!ip.equals("255.255.255.255")) {
                DatagramSocket socket = new DatagramSocket();
                byte[] byteArr = message.getBytes(StandardCharsets.UTF_8);
                DatagramPacket pacote = new DatagramPacket(byteArr, byteArr.length, InetAddress.getByName(ip), 8085);
                socket.send(pacote);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    new Mensageiro();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}

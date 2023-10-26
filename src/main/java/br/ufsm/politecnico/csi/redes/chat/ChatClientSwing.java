package br.ufsm.politecnico.csi.redes.chat;

import br.ufsm.politecnico.csi.redes.model.Mensagem;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ChatClientSwing extends JFrame {

    private Usuario meuUsuario;
    private final String endBroadcast = "255.255.255.255";
    private JList listaChat;
    private DefaultListModel dfListModel;
    private JTabbedPane tabbedPane = new JTabbedPane();
    private Set<Usuario> chatsAbertos = new HashSet<>();

    public class RecebeSonda implements Runnable {

        @SneakyThrows
        @Override
        public void run() {
            DatagramSocket socket = new DatagramSocket(8085);
            ObjectMapper om = new ObjectMapper();
            while (true) {
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                Mensagem sonda = om.readValue(buf, 0, packet.getLength(), Mensagem.class);
                //if (!sonda.getUsuario().equals(meuUsuario.nome)) {
                    System.out.println("[SONDA RECEBIDA] " + sonda);
                    int idx = dfListModel.indexOf(new Usuario(sonda.getUsuario(),
                            StatusUsuario.valueOf(sonda.getStatus()), packet.getAddress()));
                    if (idx == -1) {
                        dfListModel.addElement(new Usuario(sonda.getUsuario(),
                                StatusUsuario.valueOf(sonda.getStatus()), packet.getAddress()));
                    } else {
                        Usuario usuario = (Usuario) dfListModel.getElementAt(idx);
                        usuario.setStatus(StatusUsuario.valueOf(sonda.getStatus()));
                        dfListModel.remove(idx);
                        dfListModel.add(idx, usuario);
                    }
                //}
            }
        }
    }
    public class EnviaSonda implements Runnable {

        @SneakyThrows
        @Override
        public void run() {
            synchronized (this) {
                if (meuUsuario == null) {
                    this.wait();
                }
            }
            DatagramSocket socket = new DatagramSocket();
            while (true) {
                Mensagem mensagem = new Mensagem(
                        "sonda",
                        meuUsuario.nome,
                        ChatClientSwing.this.meuUsuario.status.toString());
                ObjectMapper om = new ObjectMapper();
                byte[] msgJson = om.writeValueAsBytes(mensagem);
                //enviam sonda para lista de IPs
                for (int n = 1; n < 255; n++) {
                    DatagramPacket packet = new DatagramPacket(msgJson,
                            msgJson.length,
                            InetAddress.getByName("192.168.0." + n), 8085);
                    socket.send(packet);
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) { }
            }
        }
    }
    public ChatClientSwing() throws UnknownHostException {
        setLayout(new GridBagLayout());
        new Thread(new EnviaSonda()).start();
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Status");

        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem(StatusUsuario.DISPONIVEL.name());
        rbMenuItem.setSelected(true);
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(StatusUsuario.DISPONIVEL);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(StatusUsuario.NAO_PERTURBE.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(StatusUsuario.NAO_PERTURBE);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem(StatusUsuario.VOLTO_LOGO.name());
        rbMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                ChatClientSwing.this.meuUsuario.setStatus(StatusUsuario.VOLTO_LOGO);
            }
        });
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        menuBar.add(menu);
        this.setJMenuBar(menuBar);

        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu popupMenu =  new JPopupMenu();
                    final int tab = tabbedPane.getUI().tabForCoordinate(tabbedPane, e.getX(), e.getY());
                    JMenuItem item = new JMenuItem("Fechar");
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PainelChatPVT painel = (PainelChatPVT) tabbedPane.getTabComponentAt(tab);
                            tabbedPane.remove(tab);
                            chatsAbertos.remove(painel.getUsuario());
                        }
                    });
                    popupMenu.add(item);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
        add(new JScrollPane(criaLista()), new GridBagConstraints(0, 0, 1, 1, 0.1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        add(tabbedPane, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        setSize(800, 600);
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final int x = (screenSize.width - this.getWidth()) / 2;
        final int y = (screenSize.height - this.getHeight()) / 2;
        this.setLocation(x, y);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Chat P2P - Redes de Computadores");
        String nomeUsuario = JOptionPane.showInputDialog(this, "Digite seu nome de usuário: ");
        synchronized (this) {
            this.meuUsuario = new Usuario(nomeUsuario, StatusUsuario.DISPONIVEL, InetAddress.getLocalHost());
            this.notify();
        }
        setVisible(true);
        new Thread(new EnviaSonda()).start();
        new Thread(new RecebeSonda()).start();
    }

    private JComponent criaLista() {
        dfListModel = new DefaultListModel();
        //dfListModel.addElement(new Usuario("Fulano", StatusUsuario.NAO_PERTURBE, null));
        //dfListModel.addElement(new Usuario("Cicrano", StatusUsuario.DISPONIVEL, null));
        listaChat = new JList(dfListModel);
        listaChat.addMouseListener(new MouseAdapter() {
            @SneakyThrows
            public void mouseClicked(MouseEvent evt) {
                JList list = (JList) evt.getSource();
                if (evt.getClickCount() == 2) {
                    int index = list.locationToIndex(evt.getPoint());
                    Usuario user = (Usuario) list.getModel().getElementAt(index);
                    Socket soc = new Socket(user.getEndereco(), 8086);
                    if (chatsAbertos.add(user)) {
                        tabbedPane.add(user.toString(), new PainelChatPVT(user, soc));
                    }
                }
            }
        });
        return listaChat;
    }

    class PainelChatPVT extends JPanel {

        JTextArea areaChat;
        JTextField campoEntrada;
        Usuario usuario;

        Socket socket;

        PainelChatPVT(Usuario usuario, Socket socket) {
            setLayout(new GridBagLayout());
            areaChat = new JTextArea();
            this.usuario = usuario;
            areaChat.setEditable(false);
            campoEntrada = new JTextField();
            this.socket = socket;
            campoEntrada.addActionListener(new ActionListener() {
                @SneakyThrows
                @Override
                public void actionPerformed(ActionEvent e) {
                    ((JTextField) e.getSource()).setText("");
                    areaChat.append(meuUsuario.getNome() + "> " + e.getActionCommand() + "\n");
                    socket.getOutputStream().write(e.getActionCommand().getBytes());
                }
            });
            add(new JScrollPane(areaChat), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
            add(campoEntrada, new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.SOUTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        }

        public Usuario getUsuario() {
            return usuario;
        }

        public void setUsuario(Usuario usuario) {
            this.usuario = usuario;
        }

    }

    public static void main(String[] args) throws UnknownHostException {
        new ChatClientSwing();

    }

    public enum StatusUsuario {
        DISPONIVEL, NAO_PERTURBE, VOLTO_LOGO
    }

    public class Usuario {

        private String nome;
        private StatusUsuario status;
        private InetAddress endereco;

        public Usuario(String nome, StatusUsuario status, InetAddress endereco) {
            this.nome = nome;
            this.status = status;
            this.endereco = endereco;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public StatusUsuario getStatus() {
            return status;
        }

        public void setStatus(StatusUsuario status) {
            this.status = status;
        }

        public InetAddress getEndereco() {
            return endereco;
        }

        public void setEndereco(InetAddress endereco) {
            this.endereco = endereco;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Usuario usuario = (Usuario) o;
            return Objects.equals(nome, usuario.nome) && Objects.equals(endereco, usuario.endereco);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nome, endereco);
        }

        public String toString() {
            return this.getNome() + " (" + getStatus().toString() + ")";
        }
    }

}

package casper.admin.ui;

import casper.admin.Admin;
import casper.admin.Configuration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

public class ServersGUI extends JFrame {

    /**
     * Displays the list of servers.
     */
    private JList<String> serverList;

    /**
     * Creates a new instance of the ServerGUI.
     */
    public ServersGUI() {
        try {
            this.initComponents();
            setLocationRelativeTo(getParent());
            setSize(250, 300);
            setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the window.
     */
    public void initComponents() {

        this.setTitle(Configuration.PROJECT_NAME + ":: Servers");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JScrollPane scrollPane = new JScrollPane();
        serverList = new JList<String>();
        model.addElement("127.0.0.1:8817");
        serverList.setModel(model);
        serverList.addMouseListener(new ContextMenuListener());

        Container container = getContentPane();
        container.setLayout(new GridBagLayout());
        ((GridBagLayout) container.getLayout()).columnWeights = new double[]{1.0};
        ((GridBagLayout) container.getLayout()).rowWeights = new double[]{1.0};

        scrollPane.setViewportView(serverList);

        container.add(scrollPane,
                      new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                             new Insets(0, 0, 0, 0), 0, 0));

        pack();

    }

    /**
     * Stores the data for the JList
     */
    private DefaultListModel<String> model = new DefaultListModel<String>();

    /**
     * Adds a server to the model.
     *
     * @param server The server to add.
     */
    protected void addServer(String server) {
        model.addElement(server);
    }

    /**
     * Removes a server from the model.
     *
     * @param server The server to remove.
     */
    protected void delServer(String server) {
        model.removeElement(server);
    }

    /**
     * Closes the servers gui.
     */
    protected void closeGUI() {
        this.setVisible(false);
    }

    /**
     * Handles everything pertaining to the right click options.
     */
    private class ContextMenu extends JPopupMenu {

        JMenuItem addServer = new JMenuItem("Add");
        JMenuItem delServer = new JMenuItem("Delete");
        JMenuItem connServers = new JMenuItem("Save & Connect");

        public ContextMenu() {
            addServer.setIcon(new ImageIcon(getClass().getResource("icons/add.png")));
            addServer.addActionListener(add);
            delServer.setIcon(new ImageIcon(getClass().getResource("icons/del.png")));
            delServer.addActionListener(delete);
            connServers.setIcon(new ImageIcon(getClass().getResource("icons/connect.png")));
            connServers.addActionListener(connect);
            this.add(addServer);
            this.add(delServer);
            this.add(connServers);
        }

        private ActionListener add = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String server = JOptionPane.showInputDialog(getParent(), "Server info? IP:PORT");
                if (server.contains(":") && server.contains(".")) addServer(server);
                else JOptionPane.showMessageDialog(getParent(), "Could not add the server you specified.\n" +
                        "    \"" + server + "\"\n" +
                        "The correct syntax is something like the following:\n" +
                        "    127.0.0.1:8817\n" +
                        "    server.website.com:8814", "Error!", JOptionPane.OK_OPTION);
            }
        };

        private ActionListener delete = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int server = JOptionPane.showConfirmDialog(getParent(),
                                                                 "Are you sure you want to remove this server?");
                if (server == 0)
                    delServer(serverList.getSelectedValue());
            }
        };

        private ActionListener connect = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.size() > 0) {
                    final int size = model.getSize();
                    final String[] servArr = new String[size];
                    final Enumeration<String> servers = model.elements();
                    int i = 0;
                    while (servers.hasMoreElements()) {
                        servArr[i] = servers.nextElement();
                        System.out.println("servArr[" + i + "] = " + servArr[i]);
                    }
                    Admin.setServers(servArr);
                    closeGUI();
                } else {
                    JOptionPane.showMessageDialog(getParent(), "You have to add at least one server.", "Error!",
                                                  JOptionPane.OK_OPTION);
                }
            }
        };

    }

    private class ContextMenuListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            ContextMenu menu = new ContextMenu();
            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }


}

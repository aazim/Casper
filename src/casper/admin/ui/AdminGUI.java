package casper.admin.ui;

import casper.admin.Configuration;
import casper.admin.MessageListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Not going to comment this, it should all be pretty self-expanatory. It was mostly created in JFormDesigner.
 *
 * @author Aazim
 */
public class AdminGUI extends JFrame implements MessageListener {

    private volatile int botCount = 0;
    private MessageListener listener;

    private final int SERVER_MESSAGE = 0;
    private final int CHAT_MESSAGE = 1;
    private final int COMMAND_MESSAGE = 2;
    private final int CLIENT_MESSAGE = 3;

    private final DecimalFormat formatter = new DecimalFormat("###,###,###");

    private Runnable titleUpdater = new Runnable() {

        public int rand() {
            Random random = new Random();
            return random.nextInt(9);
        }

        public void run() {
            AdminGUI.this.setTitle(
                    Configuration.PROJECT_NAME + " - " + formatter.format(AdminGUI.this.botCount) + " bots online.");
        }

    };

    public AdminGUI(MessageListener listener) {
        this.listener = listener;
        setDefaultCloseOperation(3);
        setLayout(new BorderLayout());
        setSize(850, 350);
        initComponents();
        setLocationRelativeTo(null);
    }

    private DateFormat df = new SimpleDateFormat("hh:mm");

    private String html;

    private void setHtml() {
        html = "<html>\n\t<head></head>\n\t<body>\n\t\t<table width=\"100%\">\n\t\t</table>\n\t</body>\n</html>\n";
        mainWindow.setText(html);
    }

    private String createNewRow(String color, String title, String message) {
        return "\n\t\t\t<tr color=" + color + ">" +
                "\n\t\t\t\t<td width=\"55\">[" + df.format(new Date()) + "]</td>" +
                "\n\t\t\t\t<td width=\"65\" align=\"right\">" + title + "</td>" +
                "\n\t\t\t\t<td>&nbsp;&nbsp;" + message + "</td>" +
                "\n\t\t\t</tr>";
    }

    private void addLine(int messageType, String title, String message) {
        String line = "";
        switch (messageType) {
            case SERVER_MESSAGE:
                line = createNewRow(Configuration.SERVER_COLOR, title, message);
                break;
            case CHAT_MESSAGE:
                line = createNewRow(Configuration.CHAT_COLOR, title, message);
                break;
            case COMMAND_MESSAGE:
                line = createNewRow(Configuration.COMMAND_COLOR, title, message);
                break;
            case CLIENT_MESSAGE:
                line = createNewRow(Configuration.CLIENT_COLOR, title, message);
        }
        String[] tokens = html.split("\n\t\t</table>");
        html = tokens[0] + line + "\n\t\t</table>" + tokens[1];
        mainWindow.setText(html);
    }

    private void initComponents() {
        JScrollPane mainScroll = new JScrollPane();
        mainWindow = new JEditorPane();
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu viewMenu = new JMenu("View");
        JMenu helpMenu = new JMenu("Help");
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
        JScrollPane botScroll = new JScrollPane();
        model = new DefaultListModel<String>();
        model.addElement("Loading bots...");
        botList = new JList<String>();
        botList.setModel(model);
        input = new JTextField();
        input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!AdminGUI.this.input.getText().isEmpty()) {
                    AdminGUI.this.listener.messageReceived("", AdminGUI.this.input.getText());
                }
                AdminGUI.this.input.setText("");
            }
        });

        setTitle(Configuration.PROJECT_NAME + " v" + Configuration.PROJECT_VERSION + " :: by Aazim");

        Container contentPane = getContentPane();
        contentPane.setLayout(new GridBagLayout());

        ((GridBagLayout) contentPane.getLayout()).columnWidths = new int[]{565, 150};
        ((GridBagLayout) contentPane.getLayout()).rowHeights = new int[]{365, 0};
        ((GridBagLayout) contentPane.getLayout()).columnWeights = new double[]{1.0, 0.0};
        ((GridBagLayout) contentPane.getLayout()).rowWeights = new double[]{1.0, 0.0};
        {
            mainWindow.setContentType("text/html");
            setHtml();
            mainWindow.setEditable(false);
            mainScroll.setViewportView(mainWindow);
        }
        contentPane.add(mainScroll, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                           GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                           new Insets(0, 0, 5, 5), 0, 0));
        {
            botScroll.setViewportView(botList);
            botScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }
        contentPane.add(botScroll, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                                          GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                          new Insets(0, 0, 5, 0), 0, 0));
        contentPane.add(input, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
                                                      GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                      new Insets(0, 0, 0, 0), 0, 0));
        pack();
        setLocationRelativeTo(getOwner());
    }

    private JList<String> botList;
    private DefaultListModel<String> model;
    private JEditorPane mainWindow;
    private JTextField input;

    @Override
    public void messageReceived(String server, String line) {
        System.out.println(line);
        if (line.startsWith("Listing")) {
            int start = line.lastIndexOf(":") + 2;
            int end = line.length() - 1;
            try {
                this.botCount = Integer.parseInt(line.substring(start, end));
                model = new DefaultListModel<String>();
                for (String s : line.split("\n")) {
                    if (!s.contains(":")) {
                        model.addElement(s.trim());
                    }
                }
                botList.setModel(model);
            } catch (Exception ignored) {
                this.botCount = 0;
            }
            EventQueue.invokeLater(this.titleUpdater);
            addLine(SERVER_MESSAGE, "[SERVER]", "Welcome " + Configuration.ADMIN_DISPLAY);
            return;
        }
        if (line.startsWith("REGISTER")) {
            String host = line.substring(9);
            model.addElement(host);
            botList.setModel(model);
            this.botCount += 1;
            EventQueue.invokeLater(this.titleUpdater);
            return;
        }
        if (line.startsWith("UNREGISTER")) {
            String host = line.substring(11);
            model.removeElement(host);
            botList.setModel(model);
            this.botCount -= 1;
            EventQueue.invokeLater(this.titleUpdater);
            return;
        }
        if (line.startsWith("AUTH")) {
            this.listener.messageReceived("", "/list");
            this.input.requestFocus();
            return;
        }
        if (line.startsWith("COMMAND")) {
            final String[] tokens = line.split(" ");
            String sender = tokens[1];
            String command = "";
            for (int i = 4; i < tokens.length; i++) {
                command += (i == 4 ? tokens[i].toUpperCase() : tokens[i]) + " ";
            }
            addLine(COMMAND_MESSAGE, sender, line.substring(line.indexOf(tokens[2]), line.indexOf(tokens[4]))
                    + " " + command);
            return;
        } else if (line.startsWith("ADMIN_MSG")) {
            final String[] tokens = line.split(" ");
            String sender = tokens[1];
            addLine(CHAT_MESSAGE, sender, line.substring(line.indexOf(tokens[2])));
            return;
        } else if (line.startsWith("CLIENT_MSG")) {
            final String[] tokens = line.split(" ");
            String sender = tokens[1];
            addLine(CLIENT_MESSAGE, sender, line.substring(line.indexOf(sender) + sender.length()));
            return;
        }
        addLine(SERVER_MESSAGE, "[SERVER]", line);

    }

}

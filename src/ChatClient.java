import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;

public class ChatClient {
    ObjectInputStream in;
    ObjectOutputStream out;
    private final JFrame frame = new JFrame("Chatter");
    private final JTextField textField = new JTextField();
    private final JTextArea messageArea = new JTextArea();
    private final DefaultListModel usersListModel = new DefaultListModel();

    private String userName = null;
    private String userSelected = null;

    private ChatClient() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(1200, 700));
        textField.setEditable(false);
        messageArea.setEditable(false);
        JList usersList = new JList();
        usersList.setModel(usersListModel);
        usersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        usersList.setLayoutOrientation(JList.VERTICAL);
        usersList.setVisibleRowCount(-1);
        usersList.addListSelectionListener(e -> {
            int index = e.getFirstIndex();
            if (index == 0) {
                // Messages to all
                userSelected = null;
            } else {
                userSelected = (String) usersListModel.get(index);
            }
        });
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        messagePanel.add(BorderLayout.CENTER, new JScrollPane(messageArea));
        messagePanel.add(BorderLayout.SOUTH, textField);
        frame.add(BorderLayout.CENTER, messagePanel);
        JScrollPane listScroll = new JScrollPane(usersList);
        listScroll.setPreferredSize(new Dimension(300, 100));
        frame.add(BorderLayout.WEST, listScroll);

        populateUsersList(null);

        textField.addActionListener(e -> {
            try {
                out.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.MESSAGE_BROADCAST).setData(textField.getText()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            textField.setText("");
        });

        frame.setVisible(true);
    }

    private String getServerAddress() {
        return JOptionPane.showInputDialog(
                frame,
                "Enter IP Address of the Server:",
                "Welcome to the Chat",
                JOptionPane.QUESTION_MESSAGE);
    }

    private String getName() {
        return JOptionPane.showInputDialog(
                frame,
                "Choose a username:",
                "Username selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    private void run() throws IOException, ClassNotFoundException {
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, ChatServer.PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        while (true) {
            ClientServerMessage message = (ClientServerMessage) in.readObject();
            switch (message.getMessageType()) {
                case CLIENT_NAME_REQUEST:
                    out.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.CLIENT_NAME_SUBMIT).setData(getName()));
                    break;
                case CLIENT_NAME_ACCEPTED:
                    textField.setEditable(true);
                    break;
                case MESSAGE_BROADCAST:
                    if (userSelected == null) {
                        messageArea.append(message.getSender() + ": " + message.getData() + "\n");
                    }
                    break;
                case CONTACT_LIST:
                    populateUsersList(message.getData().split(";"));
                    break;
                case USER_LOGGED_IN:
                    addUserToList(message.getData());
                    break;
                case USER_LOGGED_OUT:
                    removeUserFromList(message.getData());
                    break;
            }
        }
    }

    void addUserToList(String userName) {
        for (int i = 1; i < usersListModel.getSize(); ++i) {
            if (usersListModel.get(i).equals(userName)) {
                return;
            }
        }
        usersListModel.addElement(userName);
    }

    void removeUserFromList(String userName) {
        for (int i = 1; i < usersListModel.getSize(); ++i) {
            if (usersListModel.get(i).equals(userName)) {
                usersListModel.remove(i);
                return;
            }
        }
    }

    void populateUsersList(String[] userNames) {
        usersListModel.clear();
        usersListModel.addElement("Common Chat");
        if (userNames != null) {
            Arrays.stream(userNames).forEach(usersListModel::addElement);
        }
    }

    void displayMessages(String userName) {

    }

    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.run();
    }
}

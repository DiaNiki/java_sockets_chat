import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;

public class ChatClient {
    ObjectInputStream in;
    ObjectOutputStream out;
    private final JFrame frame = new JFrame("Chat app");
    private final JTextField textField = new JTextField();
    private final JTextArea messageArea = new JTextArea();
    private final DefaultListModel usersListModel = new DefaultListModel();

    private String userName = null;
    private String userSelected = null;

    private final HashMap<String, String> messages = new HashMap<>();
    private String broadMessages = "";

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
            int index = usersList.getSelectedIndex();
            if (index == -1) {
                return;
            }
            if (index == 0) {
                // Messages to all
                userSelected = null;
            } else {
                userSelected = (String) usersListModel.get(index);
            }
            displayMessages(userSelected);
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
        usersList.setSelectedIndex(0);

        textField.addActionListener(e -> {
            try {
                if (userSelected == null) {
                    out.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.MESSAGE_BROADCAST).setData(textField.getText()));
                } else {
                    if (!userSelected.equals(userName)) {
                        messages.put(userSelected, messages.getOrDefault(userSelected, "") +
                                userName + ": " + textField.getText() + "\n");
                        messageArea.append(userName + ": " + textField.getText() + "\n");
                    }
                    out.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.MESSAGE_DIRECT)
                            .setReceiver(userSelected).setData(textField.getText()));
                }
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
                    userName = message.getReceiver();
                    frame.setTitle("Chat app: " + userName);
                    break;
                case MESSAGE_BROADCAST:
                    if (userSelected == null) {
                        messageArea.append(message.toString());
                    }
                    broadMessages += message.toString();
                    break;
                case MESSAGE_DIRECT:
                    if (userSelected != null && userSelected.equals(message.getSender())) {
                        messageArea.append(message.toString());
                    }
                    messages.put(message.getSender(), messages.getOrDefault(message.getSender(), "") + message.toString());
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
                case CHAT_MESSAGES_UPDATE:
                    if (message.getSender() == null) {
                        if (userSelected == null) {
                            messageArea.setText(message.getData());
                        }
                        broadMessages = message.getData();
                    }
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
        if (userName == null) {
            messageArea.setText(broadMessages);
        } else {
            if (!messages.containsKey(userName)) {
                messages.put(userName, "");
            }
            messageArea.setText(messages.get(userName));
        }
    }

    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.run();
    }
}

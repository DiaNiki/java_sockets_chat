import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class ChatClient {
    ObjectInputStream in;
    ObjectOutputStream out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);

    public ChatClient() {
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        frame.pack();

        textField.addActionListener(e -> {
            try {
                out.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.MESSAGE_BROADCAST).setData(textField.getText()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            textField.setText("");
        });
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
            if (message.getMessageType() == ClientServerMessage.MessageType.CLIENT_NAME_REQUEST) {
                out.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.CLIENT_NAME_SUBMIT).setData(getName()));
            } else if (message.getMessageType() == ClientServerMessage.MessageType.CLIENT_NAME_ACCEPTED) {
                textField.setEditable(true);
            } else if (message.getMessageType() == ClientServerMessage.MessageType.MESSAGE_BROADCAST) {
                messageArea.append(message.getSender() + ": " + message.getData() + "\n");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}

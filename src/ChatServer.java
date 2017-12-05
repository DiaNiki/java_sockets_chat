import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ChatServer {
    static final int PORT = 9876;
    private static final HashSet<String> names = new HashSet<>();
    private static final HashMap<String, ObjectOutputStream> writers = new HashMap<>();
    private static final ArrayList<ClientServerMessage> broadMessages = new ArrayList<>();
    private static final HashMap<String, HashMap<String, ArrayList<ClientServerMessage>>> messages = new HashMap<>();

    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    private static class Handler extends Thread {
        private String name;
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new ObjectInputStream(socket.getInputStream());
                out = new ObjectOutputStream(socket.getOutputStream());

                if (!requestNameFromClient()) {
                    return;
                }

                // Notify client that name has been accepted
                out.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.CLIENT_NAME_ACCEPTED).setReceiver(name));
                broadCastMessage(new ClientServerMessage(ClientServerMessage.MessageType.USER_LOGGED_IN).setData(name));
                synchronized (broadMessages) {
                    out.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.CHAT_MESSAGES_UPDATE)
                            .setData(broadMessages.stream().map(ClientServerMessage::toString).collect(Collectors.joining(""))));
                }

                synchronized (names) {
                    writers.put(name, out);
                }

                sendClientChatData();

                while (true) {
                    ClientServerMessage input = (ClientServerMessage) in.readObject();
                    if (input == null) {
                        return;
                    }
                    input.setSender(name);
                    switch (input.getMessageType()) {
                        case MESSAGE_BROADCAST:
                            synchronized (broadMessages) {
                                broadMessages.add(input);
                            }
                            broadCastMessage(input);
                            break;
                        case MESSAGE_DIRECT:
                            synchronized (names) {
                                writers.get(input.getReceiver()).writeObject(input);
                            }
                            storeUserMessage(input);
                            break;
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                shutDownClient();
            }
        }

        boolean requestNameFromClient() throws IOException, ClassNotFoundException {
            while (true) {
                out.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.CLIENT_NAME_REQUEST));
                name = ((ClientServerMessage) in.readObject()).getData();
                if (name == null) {
                    return false;
                }
                synchronized (names) {
                    if (!names.contains(name)) {
                        names.add(name);
                        return true;
                    }
                }
            }
        }

        void sendClientChatData() throws IOException {
            synchronized (names) {
                out.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.CONTACT_LIST).setData(
                        names.stream().collect(Collectors.joining(";"))
                ));
            }
        }

        void broadCastMessage(ClientServerMessage message) {
            synchronized (names) {
                for (ObjectOutputStream writer : writers.values()) {
                    try {
                        writer.writeObject(message);
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        void shutDownClient() {
            synchronized (names) {
                if (name != null) {
                    names.remove(name);
                }
                if (out != null) {
                    writers.remove(name);
                }
                synchronized (messages) {
                    messages.remove(name);
                }
            }
            broadCastMessage(new ClientServerMessage(ClientServerMessage.MessageType.USER_LOGGED_OUT).setData(name));
            try {
                socket.close();
            } catch (IOException e) {
            }
        }

        void storeUserMessage(ClientServerMessage message) {
            storeUserMessage(message.getSender(), message.getReceiver(), message);
            storeUserMessage(message.getReceiver(), message.getSender(), message);
        }

        void storeUserMessage(String userA, String userB, ClientServerMessage message) {
            synchronized (messages) {
                if (!messages.containsKey(userA)) {
                    messages.put(userA, new HashMap<>());
                }
                if (!messages.get(userA).containsKey(userB)) {
                    messages.get(userA).put(userB, new ArrayList<>());
                }
                messages.get(userA).get(userB).add(message);
            }
        }
    }
}

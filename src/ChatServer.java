import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ChatServer {
    static final int PORT = 9876;
    private static final HashSet<String> names = new HashSet<>();
    private static HashMap<String, ObjectOutputStream> writers = new HashMap<>();

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

                while (true) {
                    out.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.CLIENT_NAME_REQUEST));
                    name = ((ClientServerMessage) in.readObject()).getData();
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!names.contains(name)) {
                            names.add(name);
                            break;
                        }
                    }
                }

                out.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.CLIENT_NAME_ACCEPTED).setReceiver(name));
                for (ObjectOutputStream writer : writers.values()) {
                    writer.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.USER_LOGGED_IN).setData(name));
                }

                writers.put(name, out);

                out.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.CONTACT_LIST).setData(
                        names.stream().collect(Collectors.joining(";"))
                ));

                while (true) {
                    ClientServerMessage input = (ClientServerMessage) in.readObject();
                    if (input == null) {
                        return;
                    }
                    switch (input.getMessageType()) {
                        case MESSAGE_BROADCAST:
                            for (ObjectOutputStream writer : writers.values()) {
                                writer.writeObject(input.setSender(name));
                            }
                            break;
                        case MESSAGE_DIRECT:
                            writers.get(input.getReceiver()).writeObject(input.setSender(name));
                            break;
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (name != null) {
                    names.remove(name);
                }
                if (out != null) {
                    writers.remove(name);
                }
                for (ObjectOutputStream writer : writers.values()) {
                    try {
                        writer.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.USER_LOGGED_OUT).setData(name));
                    } catch (IOException e) {
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}

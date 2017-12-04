import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ChatServer {
    static final int PORT = 9876;
    private static final HashSet<String> names = new HashSet<>();
    private static HashSet<ObjectOutputStream> writers = new HashSet<>();

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

                out.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.CLIENT_NAME_ACCEPTED));
                for (ObjectOutputStream writer : writers) {
                    writer.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.USER_LOGGED_IN).setData(name));
                }

                writers.add(out);

                out.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.CONTACT_LIST).setData(
                        names.stream().collect(Collectors.joining(";"))
                ));

                while (true) {
                    ClientServerMessage input = (ClientServerMessage) in.readObject();
                    if (input == null) {
                        return;
                    }
                    for (ObjectOutputStream writer : writers) {
                        writer.writeObject(input.setSender(name));
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
                    writers.remove(out);
                }
                for (ObjectOutputStream writer : writers) {
                    try {
                        writer.writeObject(new ClientServerMessage(ClientServerMessage.MessageType.USER_LOGGED_OUT).setData(name));
                    } catch (IOException e) {
                        e.printStackTrace();
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

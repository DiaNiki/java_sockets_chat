import java.io.Serializable;

public class ClientServerMessage implements Serializable {
    enum MessageType {
        CLIENT_NAME_REQUEST,
        CLIENT_NAME_SUBMIT,
        CLIENT_NAME_ACCEPTED,
        MESSAGE_DIRECT,
        MESSAGE_BROADCAST,

        CONTACT_LIST,
        USER_LOGGED_IN,
        USER_LOGGED_OUT
    }

    private MessageType messageType;
    private String sender;
    private String receiver;
    private String data;

    public ClientServerMessage(MessageType messageType) {
        this.messageType = messageType;
    }

    public MessageType getMessageType() { return messageType; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getData() { return data; }

    public ClientServerMessage setSender(String sender) {
        this.sender = sender;
        return this;
    }

    public ClientServerMessage setReceiver(String receiver) {
        this.receiver = receiver;
        return this;
    }

    public ClientServerMessage setData(String data) {
        this.data = data;
        return this;
    }
}

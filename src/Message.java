import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

public class Message  implements Serializable {

    private String sender;
    private String reciver;
    private String messageText;
    private String token;
    private String status;
    private long timestamp;
    private TreeMap<MessageStorage.UniqueTimestamp, MessageStorage.Message> msgList;
    private int port;

    //first client msg for login
    public Message(String sender, String token, String status) {
        this.sender = sender;
        this.status = status;
        this.token = token;
        this.timestamp = System.currentTimeMillis();
    }

    //first server answer
    public Message(TreeMap<MessageStorage.UniqueTimestamp, MessageStorage.Message> msgList, String status, int port) {
        this.msgList = msgList;
        this.status = status;
        this.port = port;
        this.timestamp = System.currentTimeMillis();
    }
    //normal chat msg
    public Message(String sender, String token, String reciver, String messageText) {
        this.sender = sender;
        this.token = token;
        this.reciver = reciver;
        this.messageText = messageText;
        this.status = "SEND";
        this.timestamp = System.currentTimeMillis();
    }
    public Message(String status) {
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }

    public TreeMap<MessageStorage.UniqueTimestamp, MessageStorage.Message> getMsgList(){
        return this.msgList;
    }

    public long getTimestamp(){
        return this.timestamp;
    }

    public String getStatus(){
        return this.status;
    }

    public int getPort(){
        return this.port;
    }
    public String getToken() {
        return token;
    }

    public String getSender() {
        return sender;
    }

    public String getReciver() {
        return reciver;
    }

    public String getMessageText() {
        return messageText;
    }
}

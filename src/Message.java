import java.io.Serializable;
import java.net.InetAddress;

public class Message  implements Serializable {

    private String username;
    private String sender;
    private String reciver;
    private String messageText;

    public UserStorage getUserStorage() {
        return userStorage;
    }

    private String token;
    private String status;
    private long timestamp;
    private MessageStorage messageStorage;
    private int port;
    private InetAddress inetAddress;
    private Message message;
    private UserStorage userStorage;
    private UserStorage.Body body;

    public Message (String sender, String token, String reciver, Message message ){
        this.sender = sender;
        this.status = "SYNC_MESSAGE";
        this.reciver = reciver;
        this.token = token;
        this.message = message;
    }

    //first client msg for login
    public Message(String sender, String token, String status) {
        this.sender = sender;
        this.status = status;
        this.token = token;
        this.timestamp = System.currentTimeMillis();
    }
    public Message (String sender, String token, String reciver, String username , InetAddress inetAddress, int assignedPort ){
        this.sender = sender;
        this.inetAddress = inetAddress;
        this.port = assignedPort;
        this.status = "SYNC_USER";
        this.reciver = reciver;
        this.token = token;
        this.username = username;
    }
    public Message(String sender, String token, String reciver, UserStorage.Body body, String status){
        this.sender = sender;
        this.token = token;
        this.reciver = reciver;
        this.body = body;
        this.status = status;
    }

    //first server answer
    public Message(MessageStorage msgList, String status, int port) {
        this.messageStorage = msgList;
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

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public Message(String status) {
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(String sender, String token, String status, MessageStorage msgList, UserStorage userStorage){
        this.sender = sender;
        this.token = token;
        this.status = status;
        this.messageStorage = msgList;
        this.userStorage = userStorage;
    }


    public MessageStorage getMessageStorage(){
        return this.messageStorage;
    }

    public long getTimestamp(){
        return this.timestamp;
    }
    public String getUsername(){
        return this.username;
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
    public Message getMessage(){
        return this.message;
    }

}

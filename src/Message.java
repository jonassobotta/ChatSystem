import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;

public class Message  implements Serializable {

    private String sender;
    private String reciver;
    private String messageText;
    private String token;
    private String status;
    private Timestamp timestamp;
    private ArrayList<Message> msgList;

    //first client msg for login
    public Message(String sender, String token, String status) {
        this.sender = sender;
        this.status = status;
        this.token = token;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    //first server answer
    public Message(ArrayList<Message> msgList, String status) {
        this.msgList = msgList;
        this.status = status;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }
    //normal chat msg
    public Message(String sender, String token, String reciver, String messageText) {
        this.sender = sender;
        this.token = token;
        this.reciver = reciver;
        this.messageText = messageText;
        this.status = "SEND";
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }
    public Message(String status) {
        this.status = status;
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    public ArrayList<Message> getMsgList(){
        return this.msgList;
    }

    public Timestamp getTimestamp(){
        return this.timestamp;
    }

    public String getStatus(){
        return this.status;
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

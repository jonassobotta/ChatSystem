import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

public class ClientLogic {
    int[] serverPorts = {8888, 8888};
    private final String serverAdress = "192.168.178.81";
    private BufferedReader reader;
    private Socket socket = null;
    private ServerSocket serverSocket;
    private String username;
    private String token;
    private String receiver;
    private static int listenPort;
    private MessageStorage messageStorage;
    private ChatUI chatUI;


    public ClientLogic(ChatUI chatUI) {
        try {
            this.chatUI = chatUI;
            this.reader = new BufferedReader(new InputStreamReader(System.in));
            this.listenPort = -1;
            this.messageStorage = new MessageStorage();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    public void setUsername(String username){
        this.username = username;
    }
    public void setPassword(String password){
        this.token = generateToken(password);
    }
    public void setReceiver(String receiver){
        this.receiver = receiver;
    }
    public ArrayList<String> getAllChatPartners(String username){
        return this.messageStorage.getChatPartnersForUser(username);
    }

    public TreeMap<UniqueTimestamp, Message> printHistoryOfChat(String receiver) {
        //Server.printTreeMap(messageStorage.getMessages(username, receiver));
        return messageStorage.getMessages(username, receiver);
    }

    class Listener extends Thread {
        public void run() {
            System.out.println("Writer running");
            //Hier broadcast verschicken
            try {
                //was da loooos
                while (listenPort == -1) {
                    sleep(1);
                }
                serverSocket = new ServerSocket(listenPort);
                System.out.println("Listenserver created with port " + serverSocket.getLocalPort());
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    // Get input and output streams to communicate with the client
                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                    Message message = (Message) in.readObject();
                    addMessageToHistory(message);
                    chatUI.updateChatList();

                    in.close();
                    out.close();
                    clientSocket.close();

                }
            } catch (Exception e) {

            }
        }
    }

    public void start() {
        new Listener().start();
    }

    public void addMessageToHistory(Message message) {
        this.messageStorage.addMessage(message);
        chatUI.initializeChatView();
        System.out.println(message.getSender() + ": " + message.getMessageText());
    }
    public boolean getServers() throws IOException, ClassNotFoundException {
        //send message with userdata to random server and receive all chat history
        Message answer = sendMessage2(new Message(username, token, "WHERE_ARE_YOU"));
        if (answer.getStatus().equals("OK")) {
            //add history
            listenPort = answer.getPort();
            return true;
        } else {
            return false;
        }
    }
    public String checkUserData() throws IOException, ClassNotFoundException {
        //send message with userdata to random server and receive all chat history
        try{
            Message answer = sendMessage2(new Message(username, token, "GET"));
            if (answer.getStatus().equals("OK")) {
                //add history
                listenPort = answer.getPort();
                this.messageStorage = answer.getMessageStorage();
                return "OK";
            } else {
                return "FAILED";
            }
        }catch (Exception e){
            System.out.println("CONNECTION_ERROR TRY AGAIN....");
            return checkUserData();
        }
    }

    public Message sendMessage(String messageText) throws IOException, ClassNotFoundException {
        //was wenn server ausfällt -> wie oben anpassen ... ggf alle über sendmessag2
        Message message = new Message(username, token, receiver, messageText);
        return sendMessage2(message);
    }
    public Message sendMessage2(Message message) throws IOException, ClassNotFoundException {
        if(message.getStatus().equals("SEND")){
            addMessageToHistory(message);
        }
        socket = new Socket(serverAdress, serverPorts[randomNumber()]);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        out.writeObject(message);
        Message answer = (Message) in.readObject();
        return answer;
    }

    public static int randomNumber() {
        Random random = new Random();
        int randomNumber = random.nextInt(2);
        return randomNumber;
    }

    public static String generateToken(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}

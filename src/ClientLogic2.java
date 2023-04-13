import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

public class ClientLogic2 extends Thread {
    int[] serverPorts = {7777, 8888, 9999};
    public final String serverAdress = "192.168.178.29";
    private BufferedReader reader;
    private Socket socket = null;
    public ServerSocket serverSocket;
    private String username;
    private String token;
    private String receiver;
    public int listenPort;
    private MessageStorage messageStorage;
    private ChatUI chatUI;
    public String interrupt = "NO";

    public ClientLogic2(ChatUI chatUI) {
        try {
            this.chatUI = chatUI;
            this.reader = new BufferedReader(new InputStreamReader(System.in));
            this.listenPort = -1;
            this.messageStorage = new MessageStorage();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return this.username;
    }

    public void setPassword(String password) {
        this.token = generateToken(password);
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public ArrayList<String> getAllChatPartners(String username) {
        return this.messageStorage.getChatPartnersForUser(username);
    }

    public TreeMap<UniqueTimestamp, Message> printHistoryOfChat(String receiver) {
        //Server.printTreeMap(messageStorage.getMessages(username, receiver));
        return messageStorage.getMessages(username, receiver);
    }

    public void run() {
        try {
            while (listenPort == -1 && !Thread.currentThread().isInterrupted()) {
                sleep(1);
            }
            System.out.println("test");
            serverSocket = new ServerSocket(listenPort);
            System.out.println("Listenserver created with port " + serverSocket.getLocalPort());
            while (!Thread.currentThread().isInterrupted()) {
                try{
                    Socket clientSocket = serverSocket.accept();
                    // Get input and output streams to communicate with the client
                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                    Message message = (Message) in.readObject();
                    if(message.getStatus() != null && message.getStatus().equals("INTERRUPT")){
                        this.interrupt();
                        System.out.println("ghjklkjhgfghjklö");
                    }else{
                        addMessageToHistory(message);
                        chatUI.updateChatList();
                    }
                    in.close();
                    out.close();
                    clientSocket.close();
                }catch (Exception e){
                    System.out.println("server socket closed -> please interrupt Thread");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void addMessageToHistory(Message message) {
        this.messageStorage.addMessage(message);
        chatUI.initializeChatView();
        System.out.println(message.getSender() + ": " + message.getMessageText());
    }

    public boolean getServers() throws IOException, ClassNotFoundException {
        //send message with userdata to random server and receive all chat history
        Message answer = sendMessage2(new Message(username, token, "WHERE_ARE_YOU"), 0);
        if (answer.getStatus().equals("OK")) {
            //add history
            listenPort = answer.getPort();
            return true;
        } else {
            return false;
        }
    }

    public String checkUserData(int index) throws IOException, ClassNotFoundException {
        //send message with userdata to random server and receive all chat history

        Message answer = sendMessage2(new Message(username, token, "GET"), 0);
        if (answer.getStatus().equals("OK")) {
            //add history
            listenPort = answer.getPort();
            this.messageStorage = answer.getMessageStorage();
        }
        return answer.getStatus();
    }

    public Message sendMessage(String messageText) throws IOException, ClassNotFoundException {
        //was wenn server ausfällt -> wie oben anpassen ... ggf alle über sendmessag2
        Message message = new Message(username, token, receiver, messageText);
        return sendMessage2(message, 0);


    }

    public Message sendMessage2(Message message, int index) {
        if (index == 7) {
            return new Message("FAILED");
        }
        try {
            socket = new Socket(serverAdress, serverPorts[randomNumber()]);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(message);
            Message answer = (Message) in.readObject();
            if (message.getStatus().equals("SEND")) {
                addMessageToHistory(message);
            }
            return answer;
        } catch (Exception e) {
            e.getMessage();
            return sendMessage2(message, index + 1);
        }
    }

    public static int randomNumber() {
        Random random = new Random();
        int randomNumber = random.nextInt(3);
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

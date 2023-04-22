import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

public class ClientLogic extends Thread {
    private ArrayList<ConnectionInetPortList> partnerServerList;
    private BufferedReader reader;
    public ServerSocket serverSocket;
    private String username;
    private String token;
    private String receiver;
    private int listenPort;
    private MessageStorage messageStorage;
    private ChatUI1 chatUI;


    public ClientLogic(ChatUI1 chatUI) {
        try {
            this.partnerServerList = new ArrayList<>();
            this.partnerServerList.add(new ConnectionInetPortList("192.168.178.29", 7777));
            this.partnerServerList.add(new ConnectionInetPortList("192.168.178.29", 8888));
            this.chatUI = chatUI;
            this.reader = new BufferedReader(new InputStreamReader(System.in));
            this.listenPort = 0;
            this.messageStorage = new MessageStorage();
        } catch (Exception e) {
            System.out.println("Error in ClientLogic Konstruktor: " + e.getMessage());
        }
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getUsername() {
        return username;
    }
//TODO: ist das nicht eher reader running?
    //TODO: iwo das mit unseren timeouts erwähnen... vllt in doku
    public void run() {
        System.out.println("Writer running");
        try {
            //Startet erst dann wenn ihm der Port vom Server zugewiesen wurde
            while (listenPort == 0) {
                sleep(1);
            }
            serverSocket = new ServerSocket(listenPort);
            System.out.println("Listenserver created with port " + listenPort);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(1000);
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
            System.out.println(e.getMessage());
        }
    }


    public void addMessageToHistory(Message message) {
        this.messageStorage.addMessage(message, this.username);
        //UI aktuallisiert wenn neue message empfangen wird
        chatUI.initializeChatView();
    }
//TODO: hier den index raus?
    public String checkUserData(int index) throws IOException, ClassNotFoundException {
        //send message with userdata to random server and receive all chat history if userdata is correct
        Message answer = sendMessageByObejct(new Message(username, token, "GET"));
        if (answer.getStatus().equals("OK")) {
            //add history
            listenPort = answer.getPort();
            this.messageStorage = answer.getMessageStorage();
        }
        return answer.getStatus();
    }

    public Message sendMessageByString(String messageText) throws IOException, ClassNotFoundException {
        Message message = new Message(username, token, receiver, messageText);
        return sendMessageByObejct(message);
    }

    public Message sendMessageByObejct(Message message) throws IOException, ClassNotFoundException {
        TCPConnection socket = getConnection(0);
        //Für Demo Zwecke
        if(message.getMessageText() != null){
            if(message.getMessageText().equals("INTERRUPT")){
                System.out.println("interrupted by message \"INTERRUPT\"");
                return new Message("FAILED");
            }
            if(message.getMessageText().equals("DELAY")){
                System.out.println("delayed by message \"DELAY\"");
                sieveOfEratosthenes(1000000000); //Advanced Delay ;)
                System.out.println("try to send delayed message");
            }
        }
        Message answer = socket.sendMessage(message).receiveAnswer();
        //Wenn der User etwas schreibt wird es auch hinzu gefügt
        if (message.getStatus().equals("SEND")) {
            addMessageToHistory(message);
        }
        return answer;
    }
//Damit Server random ausgewählt werden
    public static int randomNumber() {
        Random random = new Random();
        int randomNumber = random.nextInt(2);
        return randomNumber;
    }
//Damit danach der andere Server ausgewählt wird
    public static int getInverse(int i) {
        return (i + 1) % 2;
    }
//Probiert sich maximal zwei mal mit den Servern zu verbinden
    public TCPConnection getConnection(int index) throws IOException {
        if (index == 2) throw new IOException();
        TCPConnection myConnection;
        int first = randomNumber();
        try {
            myConnection = new TCPConnection(partnerServerList.get(first).getInetAddress(), partnerServerList.get(first).getPartnerPort());
            return myConnection;
        } catch (Exception e) {
            try {
                myConnection = new TCPConnection(partnerServerList.get(getInverse(first)).getInetAddress(), partnerServerList.get(getInverse(first)).getPartnerPort());
                return myConnection;
            } catch (Exception e2) {
                return getConnection(index + 1);
            }
        }
    }
//Passwort wird nicht im Klartext verschickt
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
    //Primahlberchnung für delay
    public static void sieveOfEratosthenes(int limit) {
        boolean[] prime = new boolean[limit + 1];
        for(int i = 0; i < limit; i++) {
            prime[i] = true;
        }

        for(int p = 2; p * p <= limit; p++) {
            if(prime[p] == true) {
                for(int i = p * p; i <= limit; i += p) {
                    prime[i] = false;
                }
            }
        }

        System.out.println(prime[0]);
    }

}

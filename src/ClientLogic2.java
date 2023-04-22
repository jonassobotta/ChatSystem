import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;
//Ähnlich zu clientLogic1 -> Es werden nicht alle Kommentare wiederholt
public class ClientLogic2 extends Thread {
    private ArrayList<ConnectionInetPortList> partnerServerList;
    private BufferedReader reader;
    private Socket socket = null;
    public ServerSocket serverSocket;
    private String username;
    private String token;
    private String receiver;
    public int listenPort;
    private MessageStorage messageStorage;
    private ChatUI2 chatUI;
    public String interrupt = "NO";

    public ClientLogic2(ChatUI2 chatUI) {
        this.partnerServerList = new ArrayList<>();
        this.partnerServerList.add(new ConnectionInetPortList("192.168.178.71", 7777));
        this.partnerServerList.add(new ConnectionInetPortList("192.168.178.71", 8888));
        this.partnerServerList.add(new ConnectionInetPortList("192.168.178.71", 9999));
        try {
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
        System.out.println("client logic started");
        try {
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
        chatUI.initializeChatView();
    }
    public String checkUserData() throws IOException, ClassNotFoundException {
        //send message with userdata to random server and receive all chat history

        Message answer = sendMessageByObject(new Message(username, token, "GET"));
        if (answer.getStatus().equals("OK")) {
            //add history
            listenPort = answer.getPort();
            this.messageStorage = answer.getMessageStorage();
        }
        return answer.getStatus();
    }

    public Message sendMessageByString(String messageText) {
        Message message = new Message(username, token, receiver, messageText);
        return sendMessageByObject(message);
    }

    public Message sendMessageByObject(Message message){
        TCPConnection socket = null;
        Message answer = null;
        try{
            socket = getConnection(0);
        }catch(Exception e){
            System.out.println("Could not connect to any server");
            return new Message("FAILED");
        }
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
        try{
            answer = socket.sendMessage(message).receiveAnswer();
        }catch(Exception e){
            if(message.getMessageText().equals("DELAY")){
                message.setMessageText("clientDelaySecondTry");
            }
            System.out.println("Could not send message, trying again");
            return sendMessageByObject(message);
        }

        if (message.getStatus().equals("SEND") && answer.getStatus().equals("OK")) {
            addMessageToHistory(message);
        }
        return answer;
    }


    public TCPConnection getConnection(int index) throws IOException{
        if (index == 2) throw new IOException();
        TCPConnection myConnection;
        int first = randomNumber();
        //Das muss nur zweimal probiert werden, wenn zwei Server down sind bringt es dem Client auch nichts sich mit dem dritten Server zu verbinden (MCS)
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
    public static int randomNumber() {
        Random random = new Random();
        int randomNumber = random.nextInt(3);
        return randomNumber;
    }
    //Gibt zufällig eine der anderen beiden Zahlen aus
    public static int getInverse(int zahl) {
        // Erzeugung eines Zufallsgenerators
        Random random = new Random();
        int andereZahl;
        // Verwendung eines switch-Statements, um die andere Zahl je nach gegebener Zahl auszuwählen
        switch (zahl) {
            case 1:
                // Generiere eine Zufallszahl zwischen 2 und 3 (einschließlich)
                andereZahl = random.nextInt(2) + 2;
                break;
            case 2:
                // Generiere entweder 1 oder 3 (Zufall)
                andereZahl = random.nextInt(2) * 2 + 1;
                break;
            case 3:
                // Generiere eine Zufallszahl zwischen 1 und 2 (einschließlich)
                andereZahl = random.nextInt(2) + 1;
                break;
            default:
                // Wenn eine ungültige Zahl übergeben wurde, gib eine Fehlermeldung aus
                throw new IllegalArgumentException("Ungültige Zahl! Bitte 1, 2 oder 3 verwenden.");
        }

        return andereZahl;
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

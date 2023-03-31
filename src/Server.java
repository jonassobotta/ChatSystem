import java.io.*;
import java.net.*;
import java.util.*;

public class Server extends Thread {
    public int serverPort;
    public ArrayList<Message> msgList = new ArrayList<>();
    public ArrayList<User> userList = new ArrayList<>();
    public MessageStorage messageStorage;
    private UserStorage userPortStorage;

    public static void main(String[] args) throws Exception {
        // Set the port number for the server
        new Server(7777).start();
        //new Server(8888).start();
    }

    public Server(int serverPort) {
        this.userPortStorage = new UserStorage();
        this.messageStorage = new MessageStorage();
        this.serverPort = serverPort;
        userList.add(new User("joel", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));
        userList.add(new User("nico", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));

    }

    public void run() {

        try {
            // Create a server socket that listens on the specified port
            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("Listenserver created with address " + serverSocket.getLocalSocketAddress() + ":" + serverSocket.getLocalPort());

            System.out.println("Server listening on port " + serverPort);

            while (true) {
                boolean forwarding = false;
                // Accept incoming client connections
                Socket clientSocket = serverSocket.accept();

                System.out.println("Client connected from " + clientSocket.getInetAddress().getHostAddress());

                // Get input and output streams to communicate with the client
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

                // Read messages from the client and send responses
                Message message = (Message) in.readObject();
                boolean validUser = false;
                for (User user : userList) {
                    if (user.validate(message.getSender(), message.getToken())) validUser = true;
                }
                if (validUser == true) {
                    //ad user port to list
                    if (message.getReciver() != null && message.getReciver().equals(message.getSender())) {
                        out.writeObject(new Message("FAILED"));
                    } else {
                        if (userPortStorage.containsUser(message.getSender()) == false) {
                            int assignedPort = generateUniqueRandomNumber();
                            System.out.println(assignedPort);
                            userPortStorage.addUser(message.getSender(), clientSocket.getInetAddress(), assignedPort);
                        }
                        if (message.getStatus().equals("GET")) {
                            out.writeObject(new Message(getRelevantMessages(message), "OK", userPortStorage.getUser(message.getSender()).getPort()));
                            System.out.println("sent message history of user " + message.getSender());
                        } else if (message.getStatus().equals("SEND")) {
                            messageStorage.addMessage(message.getSender(), message.getReciver(), message.getMessageText(), message.getTimestamp());
                            //msg forwarding
                            sendMessageToReceiver(message);
                            printMap(messageStorage);
                            out.writeObject(new Message("OK"));
                        }
                    }
                } else {
                    System.out.println("invalid user");
                    out.writeObject(new Message("FAILED"));
                }

                // Close the client socket and streams
                in.close();
                out.close();
                clientSocket.close();

                if (forwarding) {
                    sendMessageToReceiver(message);
                }
                System.out.println("Client disconnected");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private TreeMap<MessageStorage.UniqueTimestamp, MessageStorage.Message> getRelevantMessages(Message message) {
        System.out.println("History " + messageStorage.getMessagesPerUser(message.getSender()).size());
        for (Map.Entry<MessageStorage.UniqueTimestamp, MessageStorage.Message> entry : messageStorage.getMessagesPerUser(message.getSender()).entrySet()) {
            MessageStorage.UniqueTimestamp uniqueTimestamp = entry.getKey();
            MessageStorage.Message messaget = entry.getValue();
            System.out.println(uniqueTimestamp.timestamp + " - " + uniqueTimestamp.user + ": " + messaget.getMessageText());
        }
        return messageStorage.getMessagesPerUser(message.getSender());
    }

    //geht net
    private void sendMessageToReceiver(Message message) {
        try {
            UserStorage.Body user;
            if ((user = userPortStorage.getUser(message.getReciver())) != null) {
                Socket socket = new Socket(user.getInetAddress().toString().substring(1), user.getPort());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                out.writeObject(message);
                in.close();
                out.close();
                socket.close();
                System.out.println("Forwarded Message from " + message.getSender() + " to " + message.getReciver() + " with address " + user.getInetAddress().toString().substring(1) + ":" + user.getPort());
            } else {
                System.out.println("Message from " + message.getSender() + " to " + message.getReciver() + "could not be forwarded due to missing information");
            }
        } catch (Exception e) {
            System.out.println("send to reciever error: " + e.getMessage());
        }
    }

    public void printMap(MessageStorage myStorage) {
        TreeMap<MessageStorage.UniqueTimestamp, MessageStorage.Message> myMap = myStorage.getMessages("joel", "nico");
        if (myMap != null) {
            for (Map.Entry<MessageStorage.UniqueTimestamp, MessageStorage.Message> entry : myMap.entrySet()) {
                MessageStorage.UniqueTimestamp uniqueTimestamp = entry.getKey();
                MessageStorage.Message message = entry.getValue();
                System.out.println(uniqueTimestamp.timestamp + " - " + uniqueTimestamp.user + ": " + message.getMessageText());
            }
        } else {
            System.out.println("No messages found");
        }
    }

    private Set<Integer> usedNumbers = new HashSet<>();

    public int generateUniqueRandomNumber() {
        Random rand = new Random();
        int num;
        do {
            num = rand.nextInt(9000) + 1000; // Zufällige Zahl zwischen 1000 und 9999
        } while (usedNumbers.contains(num));
        usedNumbers.add(num);
        if (usedNumbers.size() >= 9000) { // Wenn alle möglichen Zahlen aufgebraucht sind, leere die Set-Liste
            usedNumbers.clear();
        }
        return num;
    }
}

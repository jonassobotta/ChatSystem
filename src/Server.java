import java.io.*;
import java.net.*;
import java.util.*;

public class Server extends Thread {
    private final String serverAdress = "192.168.178.29";
    public int serverPort;
    public ArrayList<User> userList = new ArrayList<>();
    public MessageStorage messageStorage;
    private UserStorage userPortStorage;
    private Set<Integer> usedNumbers = new HashSet<>();
    private final String serverToken = "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa";


    public static void main(String[] args) throws Exception {
        // Set the port number for the server
        new Server(7777).start();
        new Server(8888).start();
    }

    public Server(int serverPort) {
        this.userPortStorage = new UserStorage();
        this.messageStorage = new MessageStorage();
        this.serverPort = serverPort;
        userList.add(new User("joel", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));
        userList.add(new User("nico", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));
        userList.add(new User("jonas", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));
        userList.add(new User("luca", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));
        userList.add(new User("Server1", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));
        userList.add(new User("Server2", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));

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
                        if (userPortStorage.containsUser(message.getSender()) == false && message.getSender().contains("Server") == false) {
                            int assignedPort = generateUniqueRandomNumber();
                            System.out.println(assignedPort);
                            syncUserPortStorage(message.getSender(), clientSocket.getInetAddress(), assignedPort);
                        }
                        if (message.getStatus().equals("GET")) {
                            MessageStorage relevantMsg = getRelevantMessages(message);

                            Message myMsg = new Message(relevantMsg, "OK", userPortStorage.getUser(message.getSender()).getPort());

                            out.writeObject(myMsg);
                            System.out.println("sent message history of user " + message.getSender());
                        } else if (message.getStatus().equals("SEND")) {
                            messageStorage.addMessage(message);
                            syncServerMessageStorage(message);
                            sendMessageToReceiver(message);
                            out.writeObject(new Message("OK"));
                        } else if (message.getStatus().equals("SYNC_USER")) {
                            messageStorage.addMessage(message);
                            userPortStorage.addUser(message.getUsername(), message.getInetAddress(), message.getPort());
                            //System.out.println("user sind sync");
                            userPortStorage.print();
                        } else if (message.getStatus().equals("SYNC_MESSAGE")) {
                            System.out.println("DEMO: " + message.getMessage().getMessageText());
                            this.messageStorage.addMessage(message.getMessage());
                            //System.out.println("messages sind sync");
                            messageStorage.print();
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
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void syncUserPortStorage(String sender, InetAddress inetAddress, int assignedPort) {
        userPortStorage.addUser(sender, inetAddress, assignedPort);
        try {
            int connectionPort = 8888;
            String serverSender = "Server2";
            String serverReciver = "Server1";
            if (this.serverPort == 8888) {
                connectionPort = 7777;
                serverSender = "Server1";
                serverReciver = "Server2";
            }
            System.out.println("Sync User Data");
            Socket socket = new Socket(serverAdress, connectionPort);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(new Message(serverSender, serverToken, serverReciver, sender, inetAddress, assignedPort));

            in.close();
            out.close();
            socket.close();

        } catch (IOException e) {
            System.out.println("Sync failed" + e.getMessage());
        }
    }

    private  MessageStorage getRelevantMessages(Message message) {
        return messageStorage.getChatsForUser(message.getSender());
    }

    private void sendMessageToReceiver(Message message) {
        new Thread(() -> {
            try {
                UserStorage.Body user;
                if ((user = userPortStorage.getUser(message.getReciver())) != null) {
                    System.out.println("Try to forwarde Message from " + message.getSender() + " to " + message.getReciver() + " with address " + user.getInetAddress().toString().substring(1) + ":" + user.getPort());
                    Socket socket = new Socket(user.getInetAddress().toString().substring(1), user.getPort());
                    System.out.println("nach socket");
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
        }).start();
    }

    private void syncServerMessageStorage(Message message) {
        new Thread(() -> {
            int connectionPort = 8888;
            String serverSender = "Server2";
            String serverReciver = "Server1";
            if (this.serverPort == 8888) {
                connectionPort = 7777;
                serverSender = "Server1";
                serverReciver = "Server2";
            }
            try {
                Socket socket = new Socket(serverAdress, connectionPort);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                out.writeObject(new Message(serverSender, serverToken, serverReciver, message));
                in.close();
                out.close();
                socket.close();
            } catch (Exception e) {
                System.out.println("send to reciever error: " + e.getMessage());
            }
        }).start();
    }

    public static void printTreeMap(TreeMap<UniqueTimestamp, Message> map) {
        if (map == null) {
            System.out.println("Map is null");
            return;
        }
        System.out.println("Size of map: " + map.size());
        for (Map.Entry<UniqueTimestamp, Message> entry : map.entrySet()) {
            UniqueTimestamp uniqueTimestamp = entry.getKey();
            Message message = entry.getValue();
            System.out.println(uniqueTimestamp.timestamp + " - " + uniqueTimestamp.user + ": " + message.getMessageText());
        }
    }

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

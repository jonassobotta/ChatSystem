import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Server extends Thread {
    private final String serverAdress = "192.168.178.29";
    public int serverPort;
    public ArrayList<User> userList = new ArrayList<>();
    public MessageStorage messageStorage;
    private String serverName;
    public UserStorage userPortStorage;
    private int partnerPort;
    private Set<Integer> usedNumbers = new HashSet<>();
    private final String serverToken = "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa";
    public static void main(String[] args) throws Exception {
        // Set the port number for the server
        new Server(7777).start();
        new Server(8888).start();

    }
    public Server(int serverPort) {
        usedNumbers.add(7777);
        usedNumbers.add(8888);
        this.partnerPort = serverPort == 7777 ? 8888 : 7777;
        this.serverName = serverPort == 7777 ? "Server1" : "Server2";
        this.userPortStorage = new UserStorage();
        this.messageStorage = new MessageStorage();
        this.serverPort = serverPort;
        userList.add(new User("joel", "f11a60c74bca3ace583fac190409a5c32f83e61e1d2f7097de9674ad2c4ea877"));//Passwort ist JoelPw
        userList.add(new User("jonas", "1c461504c316958f1b46ce6f387dde8981ee548572a682a69abf708ecb3ca94c"));//Passwort ist JonasPw
        userList.add(new User("luca", "bb525174242421707805642da8e45a984bcef043ed6235476d00e9b626ae520d"));//Passwort ist LucaPw
        userList.add(new User("sarah", "be069a5e1fc5c8e3117cbb51ba554403a75d15545a735b09480cb4de5c3fdf3e"));//Passwort ist SarahPw
        userList.add(new User("Server1", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));//Passwort ist joel
        userList.add(new User("Server2", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));//Passwort ist joel
    }

    public void run() {

        try {
            // Create a server socket
            ServerSocket serverSocket = new ServerSocket(serverPort);
            printOfServer("Server listening on port " + serverPort);

            initServerSync();

            while (true) {
                // Accept incoming client connections
                Socket clientSocket = serverSocket.accept();
                printOfServer("Client connected from " + clientSocket.getInetAddress().getHostAddress());

                // Get input and output streams
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

                // Read messages from the client and handle commands
                Message message = (Message) in.readObject();

                //check if user is listed
                boolean validUser = false;
                for (User user : userList) {
                    if (user.validate(message.getSender(), message.getToken())) validUser = true;
                }
                if (validUser == true) {
                    //ad user port to list
                    if (message.getReciver() != null && message.getReciver().equals(message.getSender())) {
                        //report error if sender and receiver are the same user
                        out.writeObject(new Message("FAILED"));
                    } else {
                        //assign listen port to user
                        assignListenPort(message, clientSocket.getInetAddress(), clientSocket);

                        //handel commands of user
                        handleClientCommands(message.getStatus(), message, out);
                    }
                } else {
                    printOfServer("invalid user");
                    out.writeObject(new Message("INVALID_USER"));
                }
                // Close the client socket and streams
                in.close();
                out.close();
                clientSocket.close();

                printOfServer("Client disconnected");
            }
        } catch (Exception e) {
            printOfServer("Error: " + e.getMessage());
        }
    }

    private void initServerSync() {
        new Thread(() -> {
                Message answer;
                try {
                    System.out.println("test");
                    answer = new TCPConnection(serverAdress, partnerPort).sendMessage(new Message(this.serverName, this.serverToken, "REBOOT")).receiveAnswer();
                    printOfServer("Server is updating with other server");
                    messageStorage.print();
                    if(answer.getMessageStorage() != null) messageStorage.join(answer.getMessageStorage());
                    if(answer.getUserStorage() != null) userPortStorage.join(answer.getUserStorage());
                    printOfServer("Server is updated");
                    messageStorage.print();

                }catch (Exception e){
                    printOfServer(e.getMessage());
                }
        }).start();
    }

    private void assignListenPort(Message message, InetAddress inetAddress, Socket client) {
        if (userPortStorage.containsUser(message.getSender()) == false && message.getSender().contains("Server") == false) {
            int assignedPort = generateUniqueRandomNumber();
            userPortStorage.addUser(message.getSender(), client.getInetAddress(), assignedPort);
            printOfServer(Integer.toString(assignedPort));
            syncUserPortStorage(message.getSender(), inetAddress, assignedPort);
        } else if (userPortStorage.containsUser(message.getSender()) && inetAddress.equals(userPortStorage.getUser(message.getSender()).getInetAddress()) == false) {
            userPortStorage.getUser(message.getSender()).setInetAddress(message.getInetAddress());
        }
    }

    private void handleClientCommands(String inputCommand, Message message, ObjectOutputStream out) throws IOException {
        switch (inputCommand) {
            case "GET":
                MessageStorage relevantMsg = this.messageStorage.getChatsForUser(message.getSender());
                out.writeObject(new Message(relevantMsg, "OK", this.userPortStorage.getUser(message.getSender()).getPort()));
                printOfServer("sent message history of user " + message.getSender());
                break;
            case "SEND":
                this.messageStorage.addMessage(message);
                syncServerMessageStorage(message);
                sendMessageToReceiver(message);
                out.writeObject(new Message("OK"));
                break;
            case "SYNC_USER":
                this.messageStorage.addMessage(message);
                this.userPortStorage.addUser(message.getUsername(), message.getInetAddress(), message.getPort());
                this.userPortStorage.print();
                break;
            case "SYNC_MESSAGE":
                printOfServer("DEMO: " + message.getMessage().getMessageText());
                this.messageStorage.addMessage(message.getMessage());
                this.messageStorage.print();
                break;
            case "REBOOT":
                out.writeObject(new Message(this.serverName, this.serverToken, "OK", this.messageStorage, this.userPortStorage));
            default:
                out.writeObject(new Message("FAILED"));
                break;
        }
    }

    private void syncUserPortStorage(String sender, InetAddress inetAddress, int assignedPort) {
        new Thread(() -> {
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
                printOfServer("Sync User Data");
                new TCPConnection(serverAdress, connectionPort).sendMessage(new Message(serverSender, serverToken, serverReciver, sender, inetAddress, assignedPort)).closeConnection();

            } catch (IOException e) {
                System.out.println("Sync failed" + e.getMessage());
            }
        }).start();
    }

    private void sendMessageToReceiver(Message message) {
        new Thread(() -> {
            try {
                UserStorage.Body user;
                if ((user = userPortStorage.getUser(message.getReciver())) != null) {
                    printOfServer("Try to forwarde Message from " + message.getSender() + " to " + message.getReciver() + " with address " + user.getInetAddress().toString().substring(1) + ":" + user.getPort());

                    new TCPConnection(user.getInetAddress().toString().substring(1), user.getPort()).sendMessage(message).closeConnection();

                    printOfServer("Forwarded Message from " + message.getSender() + " to " + message.getReciver() + " with address " + user.getInetAddress().toString().substring(1) + ":" + user.getPort());
                } else {
                    printOfServer("Message from " + message.getSender() + " to " + message.getReciver() + "could not be forwarded due to missing information");
                }
            } catch (Exception e) {
                printOfServer("send to receiver error: " + e.getMessage());
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
                new TCPConnection(serverAdress, connectionPort).sendMessage(new Message(serverSender, serverToken, serverReciver, message)).closeConnection();
            } catch (Exception e) {
                printOfServer("message storage sync error: " + e.getMessage());
            }
        }).start();
    }

    public int generateUniqueRandomNumber() {
        //get random 4 digit number, but never the same
        Random rand = new Random();
        int num;
        do {
            num = rand.nextInt(9000) + 1000;
        } while (usedNumbers.contains(num));
        usedNumbers.add(num);
        if (usedNumbers.size() >= 9000) {
            usedNumbers.clear();
        }
        return num;
    }

    public void printOfServer(String printText) {
        System.out.println(this.serverAdress + ":" + this.serverPort + " -> " + printText);
    }
}
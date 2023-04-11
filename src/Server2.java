import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Server2 extends Thread {
    private final String serverAdress = "192.168.178.29";
    public int serverPort;
    public ArrayList<User> userList = new ArrayList<>();
    public MessageStorage messageStorage;
    private String serverName;
    public UserStorage userPortStorage;
    private int partnerPort;
    private ArrayList<Integer> partnerPorts;
    private Set<Integer> usedNumbers = new HashSet<>();
    private final String serverToken = "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa";
    private String startStatus;

    public static void main(String[] args) throws Exception {
        // Set the port number for the server
        new Server2(7777, "START").start();
        new Server2(8888, "START").start();
        new Server2(9999, "START").start();

    }

    public Server2(int serverPort, String startStatus) {
        partnerPorts = new ArrayList<>();
        partnerPorts.add(7777);
        partnerPorts.add(8888);
        partnerPorts.add(9999);
        usedNumbers.add(7777);
        usedNumbers.add(8888);
        usedNumbers.add(9999);
        this.partnerPort = serverPort == 7777 ? partnerPorts.remove(0) : serverPort == 8888 ? partnerPorts.remove(1) : partnerPorts.remove(2);
        this.serverName = serverPort == 7777 ? "Server1" : serverPort == 8888 ? "Server2" : "Server3";
        this.userPortStorage = new UserStorage();
        this.messageStorage = new MessageStorage();
        this.serverPort = serverPort;
        userList.add(new User("joel", "f11a60c74bca3ace583fac190409a5c32f83e61e1d2f7097de9674ad2c4ea877"));//Passwort ist JoelPw
        userList.add(new User("jonas", "1c461504c316958f1b46ce6f387dde8981ee548572a682a69abf708ecb3ca94c"));//Passwort ist JonasPw
        userList.add(new User("luca", "bb525174242421707805642da8e45a984bcef043ed6235476d00e9b626ae520d"));//Passwort ist LucaPw
        userList.add(new User("sarah", "be069a5e1fc5c8e3117cbb51ba554403a75d15545a735b09480cb4de5c3fdf3e"));//Passwort ist SarahPw
        userList.add(new User("Server1", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));//Passwort ist joel
        userList.add(new User("Server2", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));//Passwort ist joel
        userList.add(new User("Server3", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));//Passwort ist joel
    }

    public void run() {

        try {
            // Create a server socket
            ServerSocket serverSocket = new ServerSocket(serverPort);
            printOfServer("Server listening on port " + serverPort);
            if (this.startStatus == "REBOOT") {
                dannIssesSo();
            }
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
                    printOfServer("validUser: " + message.getSender());
                    //ad user port to list
                    if (message.getReciver() != null && message.getReciver().equals(message.getSender())) {
                        //report error if sender and receiver are the same user
                        out.writeObject(new Message("FAILED"));
                    } else {
                        //assign listen port to user
                        if (message.getSender().contains("Server") || assignListenPort(message, clientSocket.getInetAddress(), clientSocket)) {
                            //handel commands of user
                            handleClientCommands(message.getStatus(), message, out);
                        } else {
                            out.writeObject(new Message("FAILED"));
                        }
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

    private void dannIssesSo() {
        try {
            TCPConnection server1 = new TCPConnection(serverAdress, partnerPorts.get(0));
            TCPConnection server2 = new TCPConnection(serverAdress, partnerPorts.get(1));

            Message answer1 = server1.sendMessage(new Message(this.serverName, this.serverToken, "REBOOT")).receiveAnswer();
            Message answer2 = server2.sendMessage(new Message(this.serverName, this.serverToken, "REBOOT")).receiveAnswer();

            server1.closeConnection();
            server2.closeConnection();

            this.messageStorage.join(answer1.getMessageStorage());
            this.messageStorage.join(answer2.getMessageStorage());
            this.userPortStorage.join(answer1.getUserStorage());
            this.userPortStorage.join(answer2.getUserStorage());
        }catch (Exception e){
            printOfServer("Server Error, please contact system admin");
        }
    }

    private boolean assignListenPort(Message message, InetAddress inetAddress, Socket client) {
        if (userPortStorage.containsUser(message.getSender()) == false && message.getSender().contains("Server") == false) {
            int assignedPort = generateUniqueRandomNumber();
            if (syncUserPortStorage(message.getSender(), inetAddress, assignedPort)) {
                printOfServer(Integer.toString(assignedPort));
                return true;
            } else {
                return false;
            }
        } else if (userPortStorage.containsUser(message.getSender()) && inetAddress.equals(userPortStorage.getUser(message.getSender()).getInetAddress()) == false) {
            if (syncUserInetAdress(message.getSender(), inetAddress, userPortStorage.getUser(message.getSender()).getPort())) {
                userPortStorage.getUser(message.getSender()).setInetAddress(message.getInetAddress());
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean syncUserInetAdress(String sender, InetAddress inetAddress, int assignedPort) {
        Message answer;
        int first = randomNumber();
        try {
            new TCPConnection(serverAdress, partnerPorts.get(first)).sendMessage(new Message(this.serverName, serverToken, "Server1", sender, inetAddress, assignedPort)).closeConnection();
        } catch (Exception e) {
            try {
                new TCPConnection(serverAdress, partnerPorts.get(getInverse(first))).sendMessage(new Message(this.serverName, serverToken, "Server1", sender, inetAddress, assignedPort)).closeConnection();
            } catch (Exception e2) {
                System.out.println("Sync failed" + e.getMessage());
                return false;
            }
        }
        userPortStorage.getUser(sender).setInetAddress(inetAddress);
        printOfServer("User Data synced");
        return true;
    }

    private void handleClientCommands(String inputCommand, Message message, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        printOfServer(inputCommand);
        switch (inputCommand) {
            case "GET":
                if (getMessageStorrageFromOtherServer()) {
                    MessageStorage relevantMsg = this.messageStorage.getChatsForUser(message.getSender());
                    out.writeObject(new Message(relevantMsg, "OK", this.userPortStorage.getUser(message.getSender()).getPort()));
                    printOfServer("sent message history of user " + message.getSender());
                } else {
                    out.writeObject(new Message("CONNECTION_ERROR"));
                }
                break;
            case "SEND":
                if (syncServerMessageStorage(message)) {
                    this.messageStorage.addMessage(message);
                    sendMessageToReceiver(message);
                    out.writeObject(new Message("OK"));
                } else {
                    out.writeObject(new Message("CONNECTION_ERROR"));
                }
                break;
            case "SYNC_USER":
                if (this.userPortStorage.containsUser(message.getUsername())) {
                    if (this.userPortStorage.getUser(message.getUsername()).getInetAddress().equals(message.getInetAddress())) {
                        out.writeObject(new Message(this.serverName, this.serverToken, message.getSender(), this.userPortStorage.getUser(message.getUsername()), "AVAILABLE"));
                    } else {
                        this.userPortStorage.getUser(message.getUsername()).setInetAddress(message.getInetAddress());
                        out.writeObject(new Message(this.serverName, this.serverToken, message.getSender(), this.userPortStorage.getUser(message.getUsername()), "AVAILABLE"));
                    }
                } else {
                    this.userPortStorage.addUser(message.getUsername(), message.getInetAddress(), message.getPort());
                    out.writeObject(new Message(this.serverName, this.serverToken, message.getSender(), this.userPortStorage.getUser(message.getUsername()), "ADDED"));
                }
                this.userPortStorage.print();
                break;
            case "SYNC_MESSAGE":
                printOfServer("DEMO: " + message.getMessage().getMessageText());
                this.messageStorage.addMessage(message.getMessage());
                this.messageStorage.print();
                break;
            case "SYNC_MESSAGE_STORAGE":
                out.writeObject(new Message(this.serverName, this.serverToken, "OK", this.messageStorage, this.userPortStorage));
                break;
            case "READ_USER":
                out.writeObject(new Message(this.userPortStorage));
                break;
            case "REBOOT":
                out.writeObject(new Message(this.serverName, this.serverToken, "OK", this.messageStorage, this.userPortStorage));
            default:
                out.writeObject(new Message("FAILED"));
                break;
        }
    }

    private boolean getMessageStorrageFromOtherServer() {
        TCPConnection myConnection;
        Message answer;
        int first = randomNumber();
        try {
            myConnection = new TCPConnection(serverAdress, partnerPorts.get(first));
            answer = myConnection.sendMessage(new Message(this.serverName, this.serverToken, "SYNC_MESSAGE_STORAGE")).receiveAnswer();
        } catch (Exception e) {
            try {
                myConnection = new TCPConnection(serverAdress, partnerPorts.get(getInverse(first)));
                answer = myConnection.sendMessage(new Message(this.serverName, this.serverToken, "SYNC_MESSAGE_STORAGE")).receiveAnswer();
            } catch (Exception e2) {
                return false;
            }
        }
        this.messageStorage.join(answer.getMessageStorage());
        return true;
    }

    private boolean syncUserPortStorage(String sender, InetAddress inetAddress, int assignedPort) {
        Message answer;
        TCPConnection myConnection;
        int first = randomNumber();
        try {
            myConnection = new TCPConnection(serverAdress, partnerPorts.get(first));
            answer = myConnection.sendMessage(new Message(this.serverName, serverToken, "Server1", sender, inetAddress, assignedPort)).receiveAnswer();
        } catch (Exception e) {
            try {
                myConnection = new TCPConnection(serverAdress, partnerPorts.get(getInverse(first)));
                answer = myConnection.sendMessage(new Message(this.serverName, serverToken, "Server1", sender, inetAddress, assignedPort)).receiveAnswer();
            } catch (Exception e2) {
                System.out.println("Sync failed" + e.getMessage());
                return false;
            }
        }

        if (answer.getStatus().equals("AVAILABLE")) {
            userPortStorage.addUser(sender, answer.getInetAddress(), answer.getPort());
        } else if (answer.getStatus().equals("ADDED")) {
            userPortStorage.addUser(sender, inetAddress, assignedPort);
        }
        printOfServer("User Data synced");
        try {
            myConnection.closeConnection();

        } catch (Exception e) {
            e.getMessage();
        }
        return true;
    }

    private void sendMessageToReceiver(Message message) {
        new Thread(() -> {
            try {
                UserStorage.Body user;
                TCPConnection connection = getConnection(0);
                if (connection != null) {
                    Message answer = connection.sendMessage(new Message(this.serverName, this.serverToken, "READ_USER")).receiveAnswer();
                    connection.closeConnection();
                    UserStorage buffer = this.userPortStorage;
                    buffer.join(answer.getUserStorage());
                    if ((user = buffer.getUser(message.getReciver())) != null) {
                        printOfServer("Try to forwarde Message from " + message.getSender() + " to " + message.getReciver() + " with address " + user.getInetAddress().toString().substring(1) + ":" + user.getPort());

                        new TCPConnection(user.getInetAddress().toString().substring(1), user.getPort()).sendMessage(message).closeConnection();

                        printOfServer("Forwarded Message from " + message.getSender() + " to " + message.getReciver() + " with address " + user.getInetAddress().toString().substring(1) + ":" + user.getPort());
                    } else {
                        printOfServer("Message from " + message.getSender() + " to " + message.getReciver() + "could not be forwarded due to missing information");
                    }
                }
            } catch (Exception e) {
                printOfServer("send to receiver error: " + e.getMessage());
            }
        }).start();
    }

    private boolean syncServerMessageStorage(Message message) {
        TCPConnection connection = getConnection(0);
        if (connection == null) {
            System.out.println("Sync failed");
            return false;
        } else {
            try {
                connection.sendMessage(new Message(this.serverName, serverToken, "Server2", message)).closeConnection();
            } catch (IOException e) {
                System.out.println("Sync failed");
                return false;
            }
        }
        return true;
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

    public static int randomNumber() {
        Random random = new Random();
        int randomNumber = random.nextInt(2);
        return randomNumber;
    }

    public static int getInverse(int i) {
        return (i + 1) % 2;
    }

    public TCPConnection getConnection(int index) {
        if (index == 5) return null;
        TCPConnection myConnection;
        int first = randomNumber();
        try {
            myConnection = new TCPConnection(serverAdress, partnerPorts.get(first));
            return myConnection;
        } catch (Exception e) {
            try {
                myConnection = new TCPConnection(serverAdress, partnerPorts.get(getInverse(first)));
                return myConnection;
            } catch (Exception e2) {
                return getConnection(index + 1);
            }
        }
    }
}
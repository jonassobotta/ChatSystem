import java.io.*;
import java.net.*;
import java.util.*;

public class Server2 extends Thread {
    private String serverName;
    private String serverAddress;
    public int serverPort;
    private final String serverToken = "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa";
    private String startStatus;
    private ArrayList<PartnerServerList> partnerServerList;
    private Set<Integer> usedPorts;
    public ArrayList<User> userList;
    public MessageStorage messageStorage;
    private UserStorage userPortStorage;

    public class PartnerServerList {
        private int partnerPort;
        private String inetAddress;

        public PartnerServerList(String inetAddress, int partnerPort) {
            this.partnerPort = partnerPort;
            this.inetAddress = inetAddress;
        }

        public int getPartnerPort() {
            return partnerPort;
        }

        public void setPartnerPort(int partnerPort) {
            this.partnerPort = partnerPort;
        }

        public String getInetAddress() {
            return inetAddress;
        }

        public void setInetAddress(String inetAddress) {
            this.inetAddress = inetAddress;
        }
    }

    public static void main(String[] args) throws Exception {
        // Set the port number for the server
        new Server2("Server1", "START").start();
        new Server2("Server2", "START").start();
        //new Server2("Server3", "START").start();

    }

    public Server2(String serverName, String startStatus) {

        this.serverName = serverName;
        this.startStatus = startStatus;

        //setup all partner server
        this.partnerServerList = new ArrayList<>();
        this.partnerServerList.add(new PartnerServerList("192.168.178.29", 7777));
        this.partnerServerList.add(new PartnerServerList("192.168.178.29", 8888));
        this.partnerServerList.add(new PartnerServerList("192.168.178.81", 9999));

        //save used ports by servers
        this.usedPorts = new HashSet<>();
        this.usedPorts.add(7777);
        this.usedPorts.add(8888);
        this.usedPorts.add(9999);

        //remove own server information in server partner list
        if (serverName == "Server1") {
            this.serverAddress = partnerServerList.get(0).getInetAddress();
            this.serverPort = partnerServerList.get(0).getPartnerPort();
            this.partnerServerList.remove(0);
        } else if (serverName == "Server2") {
            this.serverAddress = partnerServerList.get(1).getInetAddress();
            this.serverPort = partnerServerList.get(1).getPartnerPort();
            this.partnerServerList.remove(1);
        } else {
            this.serverAddress = partnerServerList.get(2).getInetAddress();
            this.serverPort = partnerServerList.get(2).getPartnerPort();
            this.partnerServerList.remove(2);
        }

        //initialize storage components
        this.userPortStorage = new UserStorage();
        this.messageStorage = new MessageStorage();

        //initialize and setup user with password and token
        this.userList = new ArrayList<>();
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
            ServerSocket serverSocket = new ServerSocket(serverPort);
            printOfServer("Server listening on port " + serverPort);

            //reboot option, if server crashed and need to sync with the majority of the serverlist
            if (this.startStatus == "REBOOT") {
                serverReconnection();
            }

            while (true) {
                // Accept incoming client connections
                Socket clientSocket = serverSocket.accept();
                //printOfServer("Client connected from " + clientSocket.getInetAddress().getHostAddress());

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
                    printOfServer("Client connected from " + clientSocket.getInetAddress().getHostAddress() + " with valid user information -> name: " + message.getSender());
                    if (message.getReciver() != null && message.getReciver().equals(message.getSender())) {
                        //report error if sender and receiver are the same user
                        printOfServer("message with same sender and receiver not possible: " + message.getSender() + " to " + message.getReciver());
                        out.writeObject(new Message("FAILED"));
                    } else {
                        //assign listen port to user
                        if (message.getSender().contains("Server") || assignListenPort(message, clientSocket.getInetAddress(), clientSocket)) {
                            //handel commands of user
                            handleClientCommands(message.getStatus(), message, out);
                        } else {
                            printOfServer("something went wrong");
                            out.writeObject(new Message("FAILED"));
                        }
                    }
                } else {
                    printOfServer("Client connected from " + clientSocket.getInetAddress().getHostAddress() + " with invalid user information -> name: " + message.getSender());
                    out.writeObject(new Message("INVALID_USER"));
                }
                // Close the client socket and stream
                in.close();
                out.close();
                clientSocket.close();

                printOfServer("Client " + message.getSender() + " disconnected");

                if (message.getSender().contains("Server") == false) System.out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void serverReconnection() {
        try {
            TCPConnection server1 = new TCPConnection(partnerServerList.get(0).getInetAddress(), partnerServerList.get(0).getPartnerPort());
            TCPConnection server2 = new TCPConnection(partnerServerList.get(1).getInetAddress(), partnerServerList.get(1).getPartnerPort());

            Message answer1 = server1.sendMessage(new Message(this.serverName, this.serverToken, "REBOOT")).receiveAnswer();
            Message answer2 = server2.sendMessage(new Message(this.serverName, this.serverToken, "REBOOT")).receiveAnswer();

            server1.closeConnection();
            server2.closeConnection();

            this.messageStorage.join(answer1.getMessageStorage());
            this.messageStorage.join(answer2.getMessageStorage());
            this.userPortStorage.join(answer1.getUserStorage());
            this.userPortStorage.join(answer2.getUserStorage());
        } catch (Exception e) {
            printOfServer("Server Error, please contact system admin");
        }
    }

    private boolean assignListenPort(Message message, InetAddress inetAddress, Socket client) {
        if (userPortStorage.containsUser(message.getSender()) == false && message.getSender().contains("Server") == false) {
            int assignedPort = generateUniqueRandomNumber();
            if (syncUserPortStorage(message.getSender(), inetAddress, assignedPort)) {
                return true;
            } else {
                return false;
            }
        } else if (userPortStorage.containsUser(message.getSender()) && inetAddress.equals(userPortStorage.getUser(message.getSender()).getInetAddress()) == false && message.getSender().contains("Server") == false) {
            if (syncUserInetAdress(message.getSender(), inetAddress, userPortStorage.getUser(message.getSender()).getPort())) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean syncUserInetAdress(String sender, InetAddress inetAddress, int assignedPort) {
        TCPConnection connection = getConnection(0);
        try {
            connection.sendMessage(new Message(this.serverName, serverToken, "Server", sender, inetAddress, assignedPort));
            userPortStorage.getUser(sender).setInetAddress(inetAddress);
            printOfServer("User Data synced: InetAddress");
            return true;
        } catch (Exception e) {
            System.out.println("Sync failed" + e.getMessage());
            return false;
        }


    }

    private void handleClientCommands(String inputCommand, Message message, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        printOfServer("input command: " + inputCommand);
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
                        printOfServer("matching user information available and sent back");
                    } else {
                        this.userPortStorage.getUser(message.getUsername()).setInetAddress(message.getInetAddress());
                        out.writeObject(new Message(this.serverName, this.serverToken, message.getSender(), this.userPortStorage.getUser(message.getUsername()), "AVAILABLE"));
                        printOfServer("user information with different InetAddress -> changed InetAddress and sent back");
                    }
                } else {
                    this.userPortStorage.addUser(message.getUsername(), message.getInetAddress(), message.getPort());
                    out.writeObject(new Message(this.serverName, this.serverToken, message.getSender(), this.userPortStorage.getUser(message.getUsername()), "ADDED"));
                    printOfServer("no user information found -> added user Information and sent back");
                }
                break;
            case "SYNC_MESSAGE":
                this.messageStorage.addMessage(message.getMessage());
                printOfServer("added message to storage");
                break;
            case "SYNC_MESSAGE_STORAGE":
                out.writeObject(new Message(this.serverName, this.serverToken, "OK", this.messageStorage, this.userPortStorage));
                printOfServer("sent message storage to " + message.getSender());
                break;
            case "READ_USER":
                out.writeObject(new Message(this.userPortStorage));
                printOfServer("sent user port storage to " + message.getSender());
                break;
            case "REBOOT":
                out.writeObject(new Message(this.serverName, this.serverToken, "OK", this.messageStorage, this.userPortStorage));
                printOfServer("sent all storage content to " + message.getSender());
            default:
                out.writeObject(new Message("FAILED"));
                printOfServer("not able to handle command from " + message.getSender());
                break;
        }
    }

    private boolean getMessageStorrageFromOtherServer() {
        TCPConnection connection = getConnection(0);
        Message answer;
        try {
            answer = connection.sendMessage(new Message(this.serverName, this.serverToken, "SYNC_MESSAGE_STORAGE")).receiveAnswer();
            this.messageStorage.join(answer.getMessageStorage());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean syncUserPortStorage(String sender, InetAddress inetAddress, int assignedPort) {
        Message answer;
        TCPConnection connection = getConnection(0);
        if (connection != null) {
            try {
                answer = connection.sendMessage(new Message(this.serverName, serverToken, "Server", sender, inetAddress, assignedPort)).receiveAnswer(); // status: "SYNC_USER"
                if (answer.getStatus().equals("AVAILABLE")) {
                    userPortStorage.addUser(sender, answer.getBody().getInetAddress(), answer.getBody().getPort());
                    printOfServer("user data synced with port " + answer.getBody().getPort() + " of user " + sender);
                } else if (answer.getStatus().equals("ADDED")) {
                    userPortStorage.addUser(sender, inetAddress, assignedPort);
                    printOfServer("user data synced -> assigned port " + assignedPort + " to user " + sender);
                }
                return true;
            } catch (Exception e) {
                printOfServer("sync failed" + e.getMessage());
            }
        } else {
            printOfServer("sync failed");
        }
        return false;
    }

    private void sendMessageToReceiver(Message message) {
        new Thread(() -> {
            try {
                TCPConnection connection = getConnection(0);
                if (connection != null) {
                    Message answer = connection.sendMessage(new Message(this.serverName, this.serverToken, "READ_USER")).receiveAnswer();
                    connection.closeConnection();
                    UserStorage buffer = this.userPortStorage;
                    buffer.join(answer.getUserStorage());
                    //TODO: Hier muss geguckt werden dass die inet nicht null ist ... einen body gibt er immer zurück
                    UserStorage.Body userBody = buffer.getUser(message.getReciver());
                    if ((userBody.getInetAddress()) != null) {
                        printOfServer("Try to forwarde Message from " + message.getSender() + " to " + message.getReciver() + " with address " + userBody.getInetAddress().toString().substring(1) + ":" + userBody.getPort());
                        new TCPConnection(userBody.getInetAddress().toString().substring(1), userBody.getPort()).sendMessage(message).closeConnection();
                        printOfServer("Forwarded Message from " + message.getSender() + " to " + message.getReciver() + " with address " + userBody.getInetAddress().toString().substring(1) + ":" + userBody.getPort());
                    } else {
                        printOfServer("Message from " + message.getSender() + " to " + message.getReciver() + " could not be forwarded due to missing information");
                    }
                }
            } catch (Exception e) {
                printOfServer("send to receiver error: " + e.getMessage() + " -> user not reachable");
            }
        }).start();
    }

    private boolean syncServerMessageStorage(Message message) {
        TCPConnection connection = getConnection(0);
        if (connection == null) {
            return false;
        } else {
            try {
                connection.sendMessage(new Message(this.serverName, serverToken, "Server", message)).closeConnection();
            } catch (IOException e) {
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
        } while (usedPorts.contains(num));
        usedPorts.add(num);
        if (usedPorts.size() >= 9000) {
            usedPorts.clear();
        }
        return num;
    }

    public void printOfServer(String printText) {
        System.out.println(this.serverAddress + ":" + this.serverPort + " -> " + printText);
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
}
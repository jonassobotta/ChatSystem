import java.io.*;
import java.net.*;
import java.util.*;

public class Server2 extends Server {
    private String startStatus;
    private ArrayList<ConnectionInetPortList> partnerServerList;
    public String interruptStatus = "";

    public static void main(String[] args) throws Exception {
        // Set the port number for the server
        new Server2("Server1", "START").start();
        new Server2("Server2", "START").start();
        new Server2("Server3", "START").start();
    }

    public Server2(String serverName, String startStatus) {
        super(serverName);
        this.startStatus = startStatus;

        //setup all partner server
        this.partnerServerList = new ArrayList<>();
        this.partnerServerList.add(new ConnectionInetPortList("192.168.178.29", 7777));
        this.partnerServerList.add(new ConnectionInetPortList("192.168.178.29", 8888));
        this.partnerServerList.add(new ConnectionInetPortList("192.168.178.29", 9999));

        //save used ports by servers
        this.usedPorts = new HashSet<>();
        this.usedPorts.add(7777);
        this.usedPorts.add(8888);
        this.usedPorts.add(9999);

        //remove own server information in server partner list
        if (serverName.equals("Server1")) {
            this.serverAddress = partnerServerList.get(0).getInetAddress();
            this.serverPort = partnerServerList.get(0).getPartnerPort();
            this.partnerServerList.remove(0);
        } else if (serverName.equals("Server2")) {
            this.serverAddress = partnerServerList.get(1).getInetAddress();
            this.serverPort = partnerServerList.get(1).getPartnerPort();
            this.partnerServerList.remove(1);
        } else {
            this.serverAddress = partnerServerList.get(2).getInetAddress();
            this.serverPort = partnerServerList.get(2).getPartnerPort();
            this.partnerServerList.remove(2);
        }
    }

    public void stopServer() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {

        try {
            serverSocket = new ServerSocket(serverPort);
            printOfServer("Server listening on port " + serverPort);

            //reboot option, if server crashed and need to sync with the majority of the serverlist
            serverReconnection();

            while (true) {
                // Accept incoming client connections
                Socket clientSocket = serverSocket.accept();
                //printOfServer("Client connected from " + clientSocket.getInetAddress().getHostAddress());
                if(interruptStatus.equals("interrupt")){
                    serverSocket.close();
                    printOfServer("interrupted by server controller");
                    this.stop();
                }
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
            System.out.println(serverName + ": Error " + e.getMessage() + " -> shutdown");
        }

    }

    private void serverReconnection() {

        this.messageStorage = MessageStorage.readFromTextFile(this.serverName);
        this.userPortStorage = UserStorage.readFromTextFile(this.serverName);

        /*try {
            TCPConnection server1 = new TCPConnection(partnerServerList.get(0).getInetAddress(), partnerServerList.get(0).getPartnerPort());
            TCPConnection server2 = new TCPConnection(partnerServerList.get(1).getInetAddress(), partnerServerList.get(1).getPartnerPort());

            Message answer1 = server1.sendMessage(new Message(this.serverName, this.serverToken, "REBOOT")).receiveAnswer();
            Message answer2 = server2.sendMessage(new Message(this.serverName, this.serverToken, "REBOOT")).receiveAnswer();

            server1.closeConnection();
            server2.closeConnection();

            this.messageStorage.join(answer1.getMessageStorage());
            this.messageStorage.join(answer2.getMessageStorage());
            this.messageStorage.print();
            this.userPortStorage.join(answer1.getUserStorage());
            this.userPortStorage.join(answer2.getUserStorage());
            this.userPortStorage.print();
            printOfServer("reboot successful");
        } catch (Exception e) {
            printOfServer("Server Error, please contact system admin");
        }*/
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
        try {
            TCPConnection connection = getConnection(0);
            connection.sendMessage(new Message(this.serverName, serverToken, "Server", sender, inetAddress, assignedPort));
            userPortStorage.getUser(sender).setInetAddress(inetAddress);
            printOfServer("User Data synced: InetAddress from " + sender + " to " + inetAddress);
            return true;
        } catch (Exception e) {
            System.out.println("Sync failed" + e.getMessage());
            return false;
        }
    }

    private void handleClientCommands(String inputCommand, Message message, ObjectOutputStream out) throws IOException {
        printOfServer("input command: " + inputCommand);
        switch (inputCommand) {
            case "GET":
                if (getMessageStorrageFromOtherServer()) {
                    MessageStorage relevantMsg = this.messageStorage.getChatsForUser(message.getSender());
                    out.writeObject(new Message(relevantMsg, "OK", this.userPortStorage.getUser(message.getSender()).getPort()));
                    printOfServer(this.messageStorage.print());
                    printOfServer("sent message history of user " + message.getSender());
                } else {
                    out.writeObject(new Message("CONNECTION_ERROR"));
                    printOfServer("sync failed");
                }
                break;
            case "SEND":
                if (syncServerMessageStorage(message)) {
                    this.messageStorage.addMessage(message, this.serverName);
                    sendMessageToReceiver(message);
                    out.writeObject(new Message("OK"));
                } else {
                    out.writeObject(new Message("CONNECTION_ERROR"));
                    printOfServer("sync failed");
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
                        printOfServer("user information with different InetAddress -> changed InetAddress from " + message.getUsername() + " to " + message.getInetAddress() + "and sent back");
                    }
                } else {
                    this.userPortStorage.addUser(message.getUsername(), message.getInetAddress(), message.getPort(), this.serverName);
                    out.writeObject(new Message(this.serverName, this.serverToken, message.getSender(), this.userPortStorage.getUser(message.getUsername()), "ADDED"));
                    printOfServer("no user information found -> added user Information and sent back");
                }
                break;
            case "SYNC_MESSAGE":
                this.messageStorage.addMessage(message.getMessage(), this.serverName);
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
                break;
            default:
                out.writeObject(new Message("FAILED"));
                printOfServer("not able to handle command from " + message.getSender());
                break;
        }
    }

    private boolean getMessageStorrageFromOtherServer() {
        try {
            TCPConnection connection = getConnection(0);
            Message answer;
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
                    userPortStorage.addUser(sender, answer.getBody().getInetAddress(), answer.getBody().getPort(), this.serverName);
                    printOfServer("user data synced with port " + answer.getBody().getPort() + " of user " + sender);
                } else if (answer.getStatus().equals("ADDED")) {
                    userPortStorage.addUser(sender, inetAddress, assignedPort, this.serverName);
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

    public TCPConnection getConnection(int index){
        if (index == 2) return null;
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
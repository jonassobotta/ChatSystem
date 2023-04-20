import java.io.*;
import java.net.*;
import java.util.*;

public class Server1 extends Server {
    private ArrayList<ConnectionInetPortList> partnerServerList;


    public static void main(String[] args) throws Exception {
        // Set the port number for the server
        new Server1("Server1").start();
        new Server1("Server2").start();
    }

    public Server1(String serverName) {
        super(serverName);

        //setup all partner server
        this.partnerServerList = new ArrayList<>();
        this.partnerServerList.add(new ConnectionInetPortList("192.168.178.29", 7777));
        this.partnerServerList.add(new ConnectionInetPortList("192.168.178.29", 8888));

        //save used ports by servers
        this.usedPorts = new HashSet<>();
        this.usedPorts.add(7777);
        this.usedPorts.add(8888);

        //remove own server information in server partner list
        if (serverName == "Server1") {
            this.serverAddress = partnerServerList.get(0).getInetAddress();
            this.serverPort = partnerServerList.get(0).getPartnerPort();
            this.partnerServerList.remove(0);
        } else {
            this.serverAddress = partnerServerList.get(1).getInetAddress();
            this.serverPort = partnerServerList.get(1).getPartnerPort();
            this.partnerServerList.remove(1);
        }
    }

    public void run() {

        try {
            // Create a server socket
            serverSocket = new ServerSocket(serverPort);
            printOfServer("Server listening on port " + serverPort);

            serverReconnection();

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

    private void serverReconnection() {

        this.messageStorage = MessageStorage.readFromTextFile(this.serverName);
        this.userPortStorage = UserStorage.readFromTextFile(this.serverName);

        new Thread(() -> {
            Message answer;
            try {
                System.out.println("test");
                answer = new TCPConnection(partnerServerList.get(0).getInetAddress(), partnerServerList.get(0).getPartnerPort()).sendMessage(new Message(this.serverName, this.serverToken, "REBOOT", this.messageStorage, this.userPortStorage)).receiveAnswer();
                printOfServer("Server is updating with other server");
                messageStorage.print();
                if (answer.getMessageStorage() != null) messageStorage.join(answer.getMessageStorage());
                if (answer.getUserStorage() != null) userPortStorage.join(answer.getUserStorage());
                printOfServer("Server is updated");
                messageStorage.print();

            } catch (Exception e) {
                printOfServer(e.getMessage());
            }
        }).start();
    }

    private boolean assignListenPort(Message message, InetAddress inetAddress, Socket client) {
        if (userPortStorage.containsUser(message.getSender()) == false && message.getSender().contains("Server") == false) {
            int assignedPort = generateUniqueRandomNumber();
            printOfServer(Integer.toString(assignedPort));
            syncUserPortStorage(message.getSender(), inetAddress, assignedPort);
        } else if (userPortStorage.containsUser(message.getSender()) && inetAddress.equals(userPortStorage.getUser(message.getSender()).getInetAddress()) == false) {
            userPortStorage.getUser(message.getSender()).setInetAddress(message.getInetAddress());
        }
        return true;
    }

    public void handleClientCommands(String inputCommand, Message message, ObjectOutputStream out) throws IOException {
        switch (inputCommand) {
            case "GET":
                MessageStorage relevantMsg = this.messageStorage.getChatsForUser(message.getSender());
                out.writeObject(new Message(relevantMsg, "OK", this.userPortStorage.getUser(message.getSender()).getPort()));
                printOfServer("sent message history of user " + message.getSender());
                break;
            case "SEND":
                this.messageStorage.addMessage(message, this.serverName);
                syncServerMessageStorage(message);
                sendMessageToReceiver(message);
                out.writeObject(new Message("OK"));
                break;
            case "SYNC_USER":
                this.messageStorage.addMessage(message, this.serverName);
                this.userPortStorage.addUser(message.getUsername(), message.getInetAddress(), message.getPort(), this.serverName);
                this.userPortStorage.print();
                break;
            case "SYNC_MESSAGE":
                printOfServer("DEMO: " + message.getMessage().getMessageText());
                this.messageStorage.addMessage(message.getMessage(), this.serverName);
                this.messageStorage.print();
                break;
            case "REBOOT":
                if(message.getMessageStorage() != null){
                    this.messageStorage.join(message.getMessageStorage());
                }
                if(message.getUserStorage() != null){
                    this.userPortStorage.join(message.getUserStorage());
                }
                out.writeObject(new Message(this.serverName, this.serverToken, "OK", this.messageStorage, this.userPortStorage));
            default:
                out.writeObject(new Message("FAILED"));
                break;
        }
    }



    private void syncUserPortStorage(String sender, InetAddress inetAddress, int assignedPort) {
        new Thread(() -> {
            userPortStorage.addUser(sender, inetAddress, assignedPort, this.serverName);
            try {
                getConnection(0).sendMessage(new Message(serverName, serverToken, "Server", sender, inetAddress, assignedPort)).closeConnection();
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
            try {
                getConnection(0).sendMessage(new Message(serverName, serverToken, "Server", message)).closeConnection();
            } catch (Exception e) {
                printOfServer("message storage sync error: " + e.getMessage());
            }
        }).start();
    }

    public TCPConnection getConnection(int index) {
        if (index == 3) return null;
        TCPConnection myConnection;
        try {
            myConnection = new TCPConnection(partnerServerList.get(0).getInetAddress(), partnerServerList.get(0).getPartnerPort());
            return myConnection;
        } catch (Exception e) {
            return getConnection(index + 1);
        }
    }
}
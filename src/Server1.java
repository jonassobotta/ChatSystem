import java.io.*;
import java.net.*;
import java.util.*;
//Server für Aufgabe 1
//Viele der Methoden sind in Threads ausgelagert, da bei Aufgabe 1 nicht gewartet werden muss
public class Server1 extends Server {
    private ArrayList<ConnectionInetPortList> partnerServerList;
    //Interrupt für Demo Zwecke
    public String interruptStatus = "";

    public static void main(String[] args) throws Exception {
        // Set the port number for the server
        new Server1("Server1").start();
        new Server1("Server2").start();
    }

    public Server1(String serverName) {
        super(serverName);

        //setup all partner server
        this.partnerServerList = new ArrayList<>();
        this.partnerServerList.add(new ConnectionInetPortList("192.168.178.71", 7777));
        this.partnerServerList.add(new ConnectionInetPortList("192.168.178.71", 8888));

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
                try {
                    // Accept incoming client connections
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(1000);
                    printOfServer("Client connected from " + clientSocket.getInetAddress().getHostAddress());
                    if (interruptStatus.equals("interrupt")) {
                        serverSocket.close();
                        interruptStatus = "";
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

                    if (message.getSender().contains("Server") == false) System.out.println();
                }catch (SocketTimeoutException exception){
                    printOfServer("Timeout: " + exception.getMessage());
                }
            }
        } catch (Exception e) {
            printOfServer("Server closed");
        }
    }

    private void serverReconnection() {
//Befüllt die Storage mit den Daten aus den Datein
        this.messageStorage = MessageStorage.readFromTextFile(this.serverName);
        this.userPortStorage = UserStorage.readFromTextFile(this.serverName);
//Beide Server updaten sich gegenseitig
//In einem Thread damit der Server nicht blockiert wird
        new Thread(() -> {
            Message answer;
            try {
                answer = new TCPConnection(partnerServerList.get(0).getInetAddress(), partnerServerList.get(0).getPartnerPort()).sendMessage(new Message(this.serverName, this.serverToken, "REBOOT", this.messageStorage, this.userPortStorage)).receiveAnswer();
                printOfServer("Server is updating with other server");
                if (answer.getMessageStorage() != null) messageStorage.join(answer.getMessageStorage(), this.serverName);
                if (answer.getUserStorage() != null) userPortStorage.join(answer.getUserStorage(), serverName);
                printOfServer("Server is updated");
                printOfServer(messageStorage.print());
            } catch (Exception e) {
                printOfServer("Server update failed: " + e.getMessage());
            }
        }).start();
    }
//Gewährleistset, dass die Benutzer sich von verschiedenen Geräten anmelden können
    private boolean assignListenPort(Message message, InetAddress inetAddress, Socket client) {
        //Falls der User noch nicht in der Storage ist, wird ihm ein Port zugewiesen
        if (userPortStorage.containsUser(message.getSender()) == false && message.getSender().contains("Server") == false) {
            int assignedPort = generateUniqueRandomNumber();
            printOfServer("Assigned port: " + Integer.toString(assignedPort) + " to user " + message.getSender());
            syncUserPortStorage(message.getSender(), inetAddress, assignedPort);
        //Falls der User schon in der Storage ist, aber sich von einem anderen Gerät anmeldet, wird seine InetAddress geändert
        } else if (userPortStorage.containsUser(message.getSender()) && inetAddress.equals(userPortStorage.getUser(message.getSender()).getInetAddress()) == false) {
            userPortStorage.getUser(message.getSender()).setInetAddress(client.getInetAddress());
            printOfServer("Changed InetAddress from " + message.getSender() + " to " + client.getInetAddress());
            syncUserPortStorage(message.getSender(), inetAddress, userPortStorage.getUser(message.getSender()).getPort());
        }
        //Sonst muss nichts passieren
        return true;
    }

    public void handleClientCommands(String inputCommand, Message message, ObjectOutputStream out) throws IOException {
        printOfServer("input command: " + inputCommand);
        switch (inputCommand) {
            //Client will message history beim anmelden
            case "GET":
                MessageStorage relevantMsg = this.messageStorage.getChatsForUser(message.getSender());
                printOfServer(userPortStorage.print());
                out.writeObject(new Message(relevantMsg, "OK", this.userPortStorage.getUser(message.getSender()).getPort()));
                printOfServer("sent message history of user " + message.getSender());
                break;
            //Client will message senden
            case "SEND":
                this.messageStorage.addMessage(message, this.serverName);
                sendMessageToReceiver(message);
                syncServerMessageStorage(message);
                out.writeObject(new Message("OK"));
                break;
            case "SYNC_USER":
                this.userPortStorage.addUser(message.getUsername(), message.getInetAddress(), message.getPort(), this.serverName);
                printOfServer(this.userPortStorage.print());
                break;
            //Nachricht kommt vom anderen Server (da dieser ein SEND erhalten hat) und wird in den Storage geschrieben
            case "SYNC_MESSAGE":
                this.messageStorage.addMessage(message.getMessage(), this.serverName);
                printOfServer(this.messageStorage.print());
                break;
            //Bei einem Neustart eines Servers updaten sich beide Server gegenseitig
            case "REBOOT":
                if(message.getMessageStorage() != null){
                    this.messageStorage.join(message.getMessageStorage(), this.serverName);
                }
                if(message.getUserStorage() != null){
                    this.userPortStorage.join(message.getUserStorage(),serverName);
                }
                printOfServer("Server is updated");
                printOfServer(this.messageStorage.print());
                out.writeObject(new Message(this.serverName, this.serverToken, "OK", this.messageStorage, this.userPortStorage));
            default:
                out.writeObject(new Message("FAILED"));
                break;
        }
    }
    private void syncUserPortStorage(String sender, InetAddress inetAddress, int assignedPort) {
        userPortStorage.addUser(sender, inetAddress, assignedPort, this.serverName);
        new Thread(() -> {
            try {
                getConnection(0).sendMessage(new Message(serverName, serverToken, "Server", sender, inetAddress, assignedPort)).closeConnection();
            } catch (IOException e) {
                System.out.println("Sync failed");
            }
        }).start();
    }
//Nachricht wird weitergeleitet
    private void sendMessageToReceiver(Message message) {
        new Thread(() -> {
            try {
                UserStorage.Body user;
                //Wenn der Empfänger sich bereits angemeldet hat, wird die Nachricht direkt an ihn weitergeleitet
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
//Get Connection ausgelagert
//Probiert sich zweimal mit dem anderen Server zu verbinden
    public TCPConnection getConnection(int index) throws IOException {
        if (index == 2) throw new IOException();
        TCPConnection myConnection;
        try {
            myConnection = new TCPConnection(partnerServerList.get(0).getInetAddress(), partnerServerList.get(0).getPartnerPort());
            return myConnection;
        } catch (Exception e) {
            return getConnection(index + 1);
        }
    }
}
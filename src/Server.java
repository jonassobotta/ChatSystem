import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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
                    userPortStorage.addUser(message.getSender(), clientSocket.getInetAddress(), clientSocket.getPort());
                     if(message.getStatus().equals("GET")){
                         //hier noch randomgenerator mit abgleich der liste
                        out.writeObject(new Message(msgList, "OK",9999));
                    }else if(message.getStatus().equals("SEND")){
                        messageStorage.addMessage(message.getSender(),message.getReciver(), message.getMessageText(), message.getTimestamp());
                        //msg forwarding
                        //sendMessageToReceiver(message);
                        forwarding = true;
                        printMap(messageStorage);
                        out.writeObject(new Message("OK"));
                    }
                } else {
                    System.out.println("invalid user");
                    out.writeObject(new Message("server", "FAILED", "invalid user"));
                }

                // Close the client socket and streams
                in.close();
                out.close();
                clientSocket.close();

                if (forwarding){
                    sendMessageToReceiver(message);
                }
                System.out.println("Client disconnected");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
//geht net
    private void sendMessageToReceiver(Message message) {
        try {
            UserStorage.Body user;
            if ((user = userPortStorage.getUser(message.getReciver())) != null) {
                System.out.println("body: " + user.getInetAddress() + user.getPort());
                Socket socket = new Socket(user.getInetAddress(), user.getPort());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                out.writeObject(message);
                in.close();
                out.close();
                socket.close();
            }
        }catch (Exception e){
            System.out.println("send to reciever error: " + e.getMessage());
        }
    }

    public void printMap(MessageStorage myStorage){
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
}

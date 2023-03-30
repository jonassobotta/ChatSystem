import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server extends Thread {
    public int serverPort;
    public ArrayList<Message> msgList = new ArrayList<>();
    public ArrayList<User> userList = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        // Set the port number for the server
        new Server(7777).start();
        new Server(8888).start();
    }

    public Server(int serverPort) {
        this.serverPort = serverPort;
        userList.add(new User("joel", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));
    }

    public void run() {

        try {
            // Create a server socket that listens on the specified port
            ServerSocket serverSocket = new ServerSocket(serverPort);
            System.out.println("Server listening on port " + serverPort);

            while (true) {
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
                    if(message.getStatus().equals("GET")){
                        out.writeObject(new Message(msgList, "OK"));
                    }else if(message.getStatus().equals("SEND")){
                        msgList.add(message);
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
                System.out.println("Client disconnected");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}

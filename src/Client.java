import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

public class Client {
    public static ArrayList<Message> msgList;

    public static void main(String[] args) throws Exception {
        // Set the IP address and port number of the server
        String serverAddress = "127.0.0.1"; // Replace with your server IP address
        int[] serverPorts = {7777, 7777};
        boolean firstMessage = true;
        boolean reciverSelected = false;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        String username = "";
        String token = "";
        String reciver = "";

        while (true) {

            if (firstMessage == true) {
                System.out.print("Enter username: ");
                username = reader.readLine();
                System.out.print("Enter password: ");
                token = generateToken(reader.readLine());
                // Create a socket to connect to the server
                socket = new Socket(serverAddress, serverPorts[randomNumber()]);
                // Get input and output streams to communicate with the server
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                // Send the message to the server
                out.writeObject(new Message(username, token, "GET"));

                // Read the response from the server
                Message ansMessage = (Message) in.readObject();
                if (ansMessage.getStatus().equals("OK")) {
                    System.out.println("login successful");
                    firstMessage = false;
                    //msgList = ansMessage.getMsgList();
                } else {
                    System.out.println("login unsuccessful");
                }

            } else {
                if (reciverSelected == false) {
                    // Get input from the command line
                    System.out.print("Enter a reciver: ");
                    reciver = reader.readLine();
                    //is receiver available
                    reciverSelected = true;
                } else {
                    ServerSocket serverSocket = new ServerSocket(9999);

                    System.out.print("Enter a message: ");
                    Socket clientSocket = null;
                    String messageText;
                    while ((messageText = reader.readLine()) == null || (clientSocket = serverSocket.accept()) == null) {
                        if (messageText != null) {
                            System.out.println("Message");
                            if (messageText.equals("EXIT")) {
                                reciverSelected = false;
                            } else {

                                // Create a socket to connect to the server
                                socket = new Socket(serverAddress, serverPorts[randomNumber()]);

                                // Get input and output streams to communicate with the server
                                out = new ObjectOutputStream(socket.getOutputStream());
                                in = new ObjectInputStream(socket.getInputStream());

                                out.writeObject(new Message(username, token, reciver, messageText));

                                // Read the response from the server
                                Message ansMessage = (Message) in.readObject();
                                if (ansMessage.getStatus().equals("OK") == false) {
                                    System.out.println("message transfer unsuccessful");
                                }
                            }
                        } else if (clientSocket != null) {
                            System.out.println("Socket");
                        }
                    }

                }

            }
            // Close the socket and streams
            in.close();
            out.close();
            socket.close();

        }
    }

    public static int randomNumber() {
        Random random = new Random();
        int randomNumber = random.nextInt(2);
        return randomNumber;
    }

    public static String generateToken(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
    class MessageList{
        TreeMap<MessageStorage.UniqueTimestamp, MessageStorage.Message> msgList;
        public MessageList(TreeMap<MessageStorage.UniqueTimestamp, MessageStorage.Message> msgList){
            this.msgList = msgList;
        }
        public void print(){

        };
    }
}

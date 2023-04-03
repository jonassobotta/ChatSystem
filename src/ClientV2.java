import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;

public class ClientV2 {
    public enum state {
        none,
        passwordRequired,
        receiverRequired,
        messageRequired,
    }

    int[] serverPorts = {7777, 8888};
    private final String serverAdress = "192.168.178.81";
    private state clientState;
    private BufferedReader reader;
    private Socket socket = null;
    private ServerSocket serverSocket;
    private String username;
    private String token;
    private String receiver;
    private static int listenPort;
    private MessageStorage messageStorage;

    public static void main(String[] args) {
        ClientV2 halloo = new ClientV2();
        halloo.start();
    }

    public ClientV2() {
        try {
            this.clientState = state.none;
            this.reader = new BufferedReader(new InputStreamReader(System.in));
            this.listenPort = -1;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    class Writer extends Thread {
        public void run() {
            System.out.print("enter username: ");
            while (true) {
                try {
                    String readerText = reader.readLine();

                    if (readerText.equals("BACK")) {

                    } else if (clientState == state.none) {
                        username = readerText;
                        clientState = state.passwordRequired;
                        System.out.print("enter password: ");
                    } else if (clientState == state.passwordRequired) {
                        token = generateToken(readerText);
                        if (checkUserData()) {
                            clientState = state.receiverRequired;
                            System.out.println("login successful");
                            System.out.print("enter receiver: ");
                        } else {
                            clientState = state.none;
                            System.out.println("login unsuccessful");
                            System.out.print("enter username");
                        }
                    } else if (clientState == state.receiverRequired) {
                        clientState = state.messageRequired;
                        receiver = readerText;
                        printHistoryOfChat(receiver);
                        System.out.print("enter message: ");
                    } else if (clientState == state.messageRequired) {
                        clientState = state.messageRequired;
                        sendMessage(new Message(username, token, receiver, readerText));
                        System.out.print("enter message: ");
                    }

                } catch (Exception e) {

                }
            }
        }
    }

    private void printHistoryOfChat(String receiver) {
        Server.printTreeMap(messageStorage.getMessages(username, receiver));
    }

    class Listener extends Thread {
        public void run() {
            System.out.println("Writer running");
            try {
                //was da loooos
                while (listenPort == -1) {
                    sleep(1);
                }
                serverSocket = new ServerSocket(listenPort);
                System.out.println("Listenserver created with port " + serverSocket.getLocalPort());
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    // Get input and output streams to communicate with the client
                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                    Message message = (Message) in.readObject();

                    addMessageToHistory(message);

                    in.close();
                    out.close();
                    clientSocket.close();

                }
            } catch (Exception e) {

            }
        }
    }

    public void start() {
        new Listener().start();
        new Writer().start();
    }

    private void addMessageToHistory(Message message) {
        System.out.println(message.getSender() + ": " + message.getMessageText());
    }

    private boolean checkUserData() throws IOException, ClassNotFoundException {
        //send message with userdata to random server and receive all chat history
        Message answer = sendMessage(new Message(username, token, "GET"));
        if (answer.getStatus().equals("OK")) {
            //add history
            listenPort = answer.getPort();
            this.messageStorage = answer.getMsgList();
            return true;
        } else {
            return false;
        }
    }

    private Message sendMessage(Message message) throws IOException, ClassNotFoundException {
        socket = new Socket(serverAdress, serverPorts[randomNumber()]);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        out.writeObject(message);
        Message answer = (Message) in.readObject();
        return answer;
    }

    private static int randomNumber() {
        Random random = new Random();
        int randomNumber = random.nextInt(2);
        return randomNumber;
    }

    private static String generateToken(String input) {
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
}

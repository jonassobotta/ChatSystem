import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class ClientV2 {
    public enum state {
        none,
        passwordRequired,
        receiverRequired,
        messageRequired,
    }

    int[] serverPorts = {7777, 8888};
    private final String serverAdress = "127.0.0.1";
    private state clientState;
    private BufferedReader reader;
    private Socket socket = null;
    private ServerSocket serverSocket;
    private String username;
    private String token;
    private String receiver;

    public static void main(String[] args) {
        ClientV2 halloo = new ClientV2();
        halloo.start();
    }

    public ClientV2() {
        try {
            this.clientState = state.none;
            this.reader = new BufferedReader(new InputStreamReader(System.in));
            this.serverSocket = new ServerSocket(9999);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    class Reader extends Thread {
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
                        System.out.print("enter message: ");
                    } else if (clientState == state.messageRequired) {
                        clientState = state.messageRequired;
                        //sendMessage(readerText);
                        System.out.print("enter message: ");
                    }

                } catch (Exception e) {

                }
            }
        }
    }

    class Writer extends Thread {
        public void run() {
            try {
                Socket clientSocket = serverSocket.accept();
                // Get input and output streams to communicate with the client
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                Message message = (Message) in.readObject();

                addMessageToHistory(message);

                in.close();
                out.close();
                clientSocket.close();
            } catch (Exception e) {

            }

        }
    }

    public void start() {
        new Writer().start();
        new Reader().start();
    }

    private void addMessageToHistory(Message message) {

    }

    private boolean checkUserData () throws IOException, ClassNotFoundException {
        //send message with userdata to random server and receive all chat history
        Message answer = sendMessage(new Message(username, token, "GET"));
        if(answer.getStatus().equals("OK")){
            //add history
            return true;
        }else {
            return false;
        }
    }

    private Message sendMessage(Message message) throws IOException, ClassNotFoundException {
        socket = new Socket(serverAdress, serverPorts[randomNumber()]);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        out.writeObject(message);
        return (Message) in.readObject();
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

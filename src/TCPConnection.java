import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
public class TCPConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    public TCPConnection(String serverAddress, int port) throws IOException {
        this.socket = new Socket(serverAddress, port);
        this.out = new ObjectOutputStream(this.socket.getOutputStream());
        this.in = new ObjectInputStream(this.socket.getInputStream());
    }
    public TCPConnection sendMessage(Message message) throws IOException {
        this.out.writeObject(message);
        return this;
    }
    public Message receiveAnswer() throws IOException, ClassNotFoundException {
        return (Message) this.in.readObject();
    }
    public void closeConnection() throws IOException {
        this.in.close();
        this.out.close();
        this.socket.close();
        //for garbage collector
        this.in = null;
        this.out = null;
        this.socket = null;
    }
}

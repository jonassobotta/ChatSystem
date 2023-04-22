import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
//TCP Connection ausgelagert
//Methoden machen das was der Name sagt
public class TCPConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    //TODO: Warum hier nochmal zwei
    public TCPConnection(String serverAddress, int port) throws IOException {
        this.socket = new Socket();
        socket.connect(new InetSocketAddress(serverAddress, port), 1000);
        socket.setSoTimeout(1000);
        this.out = new ObjectOutputStream(this.socket.getOutputStream());
        this.in = new ObjectInputStream(this.socket.getInputStream());
    }
    public TCPConnection(String serverAddress, int port, int timeout) throws IOException {
        this.socket = new Socket();
        socket.connect(new InetSocketAddress(serverAddress, port));
        socket.setSoTimeout(timeout);
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

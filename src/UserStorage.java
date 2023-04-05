import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class UserStorage implements Serializable {
    private Map<String, Body> map = new HashMap<>();

    public void addUser(String username, InetAddress inetAddress, int port) {
        Body body = new Body(inetAddress, port);
        map.put(username, body);
    }

    public Body getUser(String username) {
        return map.get(username);
    }

    public void removeUser(String username) {
        map.remove(username);
    }

    //TODO: abfragen nach inet und user damit multiple device für einen nutzer möglich
    public boolean containsUser(String username) {
        return map.containsKey(username);
    }

    public static class Body implements Serializable{
        private InetAddress inetAddress;
        private int port;

        public Body(InetAddress inetAddress, int port) {
            this.inetAddress = inetAddress;
            this.port = port;
        }

        public InetAddress getInetAddress() {
            return inetAddress;
        }

        public int getPort() {
            return port;
        }

        public void setInetAddress(InetAddress inetAddress) {
            this.inetAddress = inetAddress;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
    public void print() {
        for (String username : map.keySet()) {
            Body body = map.get(username);
            System.out.println(username + ": " + body.getInetAddress().getHostAddress() + ":" + body.getPort());
        }
    }
    public void join(UserStorage other) {
        for (String username : other.map.keySet()) {
            if (!map.containsKey(username)) {
                // User does not exist in current storage, add it
                Body body = other.map.get(username);
                map.put(username, body);
            }
        }
    }


}

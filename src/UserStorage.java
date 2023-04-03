import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class UserStorage {
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

    public boolean containsUser(String username) {
        return map.containsKey(username);
    }

    public static class Body {
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
    }
    public void print() {
        for (String username : map.keySet()) {
            Body body = map.get(username);
            System.out.println(username + ": " + body.getInetAddress().getHostAddress() + ":" + body.getPort());
        }
    }

}

import java.io.Serializable;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class UserStorage implements Serializable {
    private Map<String, Body> map = new HashMap<>();

    public void addUser(String username, InetAddress inetAddress, int port) {
        Body body = new Body(inetAddress, port);
        map.put(username, body);
    }

    public Body getUser(String username) {
        if(map.get(username) != null){
            return map.get(username);
        }else {
            return new Body(null, -1);
        }
    }

    public void removeUser(String username) {
        map.remove(username);
    }

    //TODO: abfragen nach inet und user damit multiple device für einen nutzer möglich
    public boolean containsUser(String username) {
        return map.containsKey(username);
    }

    public static class Body implements Serializable {
        private InetAddress inetAddress;
        private int port;
        private long timestamp;

        public Body(InetAddress inetAddress, int port) {
            this.inetAddress = inetAddress;
            this.port = port;
            this.timestamp = System.currentTimeMillis();
        }

        public InetAddress getInetAddress() {
            return inetAddress;
        }

        public int getPort() {
            return port;
        }
        public long getTimestamp(){
            return this.timestamp;
        }

        public void setInetAddress(InetAddress inetAddress) {
            this.inetAddress = inetAddress;
        }

        public void setPort(int port) {
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Body body = (Body) o;
            if (port != body.port) return false;
            return inetAddress != null ? inetAddress.equals(body.inetAddress) : body.inetAddress == null;
        }

        @Override
        public int hashCode() {
            int result = inetAddress != null ? inetAddress.hashCode() : 0;
            result = 31 * result + port;
            return result;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserStorage that = (UserStorage) o;
        return map.equals(that.map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }


}

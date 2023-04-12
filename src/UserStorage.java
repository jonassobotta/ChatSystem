import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
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
            System.out.println("der username wo ein leerer body erstellt wird ist " + username);
            System.out.println("hier wird das aufgrufen: " + Thread.currentThread().getStackTrace());
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
            System.out.println("setzte inet adress to: " + inetAddress);
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
            if(body.getInetAddress() != null){
                System.out.println(username + ": " + body.getInetAddress().getHostAddress() + ":" + body.getPort() + " // "+ body.getTimestamp());
            }else {
                System.out.println(username + "als username hat einen empty body");
            }
        }
    }

    public void join(UserStorage other) { //Falls bei Read User zwei server unterschiedliches inet adresse von einem user haben wird die neuste genommen
        for (String username : other.map.keySet()) {
            Body otherBody = other.map.get(username);
            if (map.containsKey(username)) {
                // User already exists in current storage, compare timestamps
                Body currentBody = map.get(username);
                if (otherBody.getTimestamp() > currentBody.getTimestamp()) {
                    // If otherBody has a more recent timestamp, update it in current storage
                    map.put(username, otherBody);
                }
            } else {
                // User does not exist in current storage, add it
                map.put(username, otherBody);
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

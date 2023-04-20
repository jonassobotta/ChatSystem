import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class UserStorage implements Serializable {
    private Map<String, Body> map = new HashMap<>();

    public void addUser(String username, InetAddress inetAddress, int port, String serverName) {
        Body body = new Body(inetAddress, port);
        map.put(username, body);

        // Schreibe Benutzerinformationen in Textdatei
        String os = System.getProperty("os.name").toLowerCase();
        String trenner;
        if (os.contains("win")) {
            // Windows
            trenner = "\\\\";
        } else {
            // Unix-basierte Systeme (z.B. Mac OS X, Linux)
            trenner = "/";
        }

        String filename = "src" + trenner + "userData" + trenner + serverName + "Users.txt";
        FileWriter fileWriter = null; // FileWriter zum Schreiben in die Datei (true bedeutet, dass die Datei angehängt wird)
        try {
            fileWriter = new FileWriter(filename, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter); // BufferedWriter zum Puffern der Ausgabe

            // Schreibe Benutzerinformationen in Textdatei
            bufferedWriter.write(username + ";" + inetAddress.getHostAddress() + ";" + port + ";" + new Timestamp(System.currentTimeMillis()));
            bufferedWriter.newLine();

            // Schließe den BufferedWriter
            bufferedWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void writeToTextFile(String serverName) {
        String os = System.getProperty("os.name").toLowerCase();
        String trenner;
        if (os.contains("win")) {
            // Windows
            trenner = "\\\\";
        } else {
            // Unix-basierte Systeme (z.B. Mac OS X, Linux)
            trenner = "/";
        }

        String filename = "src" + trenner + "userData" + trenner + serverName + "Users.txt";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            for (String username : map.keySet()) {
                Body body = map.get(username);
                bw.write(username + ";" + body.getInetAddress().getHostAddress() + ";" + body.getPort() + ";" + new Timestamp(body.getTimestamp()));
                bw.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public Body getUser(String username) {
        if(map.get(username) != null){
            return map.get(username);
        }else {
            return new Body(null, -1);
        }
    }

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

    public String print() {
        StringBuilder sb = new StringBuilder();
        sb.append(" \n === User Port Storage ===\n");

        for (String username : map.keySet()) {
            Body body = map.get(username);
            if (body.getInetAddress() != null) {
                sb.append(username)
                        .append(": ")
                        .append(body.getInetAddress().getHostAddress())
                        .append(":")
                        .append(body.getPort())
                        .append(" Timestamp: ")
                        .append(body.getTimestamp())
                        .append(System.lineSeparator());
            } else {
                //sb.append(username).append(" als username hat einen empty body").append(System.lineSeparator());
            }
        }

        return sb.toString();
    }

    public void join(UserStorage other,String servername) { //Falls bei Read User zwei server unterschiedliches inet adresse von einem user haben wird die neuste genommen
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
        writeToTextFile(servername);
    }
    public static UserStorage readFromTextFile(String serverName) {
        UserStorage userStorage = new UserStorage();
        String os = System.getProperty("os.name").toLowerCase();
        String trenner;
        if (os.contains("win")) {
            // Windows
            trenner = "\\\\";
        } else {
            // Unix-basierte Systeme (z.B. Mac OS X, Linux)
            trenner = "/";
        }

        String filename = "src" + trenner + "userData" + trenner + serverName + "Users.txt";

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(";");
                if (tokens.length != 4) {
                    continue;
                }
                String username = tokens[0];
                InetAddress inetAddress = InetAddress.getByName(tokens[1]);
                int port = Integer.parseInt(tokens[2]);
                long timestamp = Timestamp.valueOf(tokens[3]).getTime();
                UserStorage.Body body = new UserStorage.Body(inetAddress, port);
                userStorage.map.put(username, body);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return userStorage;
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

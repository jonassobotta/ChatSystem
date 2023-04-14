import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public abstract class Server extends Thread {
    public Set<Integer> usedPorts = new HashSet<>();
    public ArrayList<User> userList = new ArrayList<>();
    public String serverAddress;
    public int serverPort;
    public String serverName;
    public UserStorage userPortStorage;
    public MessageStorage messageStorage;
    public final String serverToken = "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa";
    public ServerSocket serverSocket;

    public Server(String serverName) {

        this.serverName = serverName;

        //initialize storage components
        this.userPortStorage = new UserStorage();
        this.messageStorage = new MessageStorage();

        //initialize and setup user with password and token
        userList.add(new User("joel", "f11a60c74bca3ace583fac190409a5c32f83e61e1d2f7097de9674ad2c4ea877"));//Passwort ist JoelPw
        userList.add(new User("jonas", "1c461504c316958f1b46ce6f387dde8981ee548572a682a69abf708ecb3ca94c"));//Passwort ist JonasPw
        userList.add(new User("luca", "bb525174242421707805642da8e45a984bcef043ed6235476d00e9b626ae520d"));//Passwort ist LucaPw
        userList.add(new User("sarah", "be069a5e1fc5c8e3117cbb51ba554403a75d15545a735b09480cb4de5c3fdf3e"));//Passwort ist SarahPw
        userList.add(new User("Server1", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));//Passwort ist joel
        userList.add(new User("Server2", "2c8b7961168c40b75911c208b59be1083b540d496a6e0d28c26d3a53562a15aa"));//Passwort ist joel
    }


    public int generateUniqueRandomNumber() {
        //get random 4 digit number, but never the same
        Random rand = new Random();
        int num;
        do {
            num = rand.nextInt(9000) + 1000;
        } while (usedPorts.contains(num));
        usedPorts.add(num);
        if (usedPorts.size() >= 9000) {
            usedPorts.clear();
        }
        return num;
    }

    public static int randomNumber() {
        Random random = new Random();
        int randomNumber = random.nextInt(2);
        return randomNumber;
    }

    public static int getInverse(int i) {
        return (i + 1) % 2;
    }

    public void printOfServer(String printText) {
        System.out.println(this.serverAddress + ":" + this.serverPort + " -> " + printText);
    }
}

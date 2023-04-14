public class Playground {
    public static void main(String[] args) {
        MessageStorage messageStorage = MessageStorage.readFromTextFile("Server1");
        messageStorage.print();

        UserStorage userStorage = UserStorage.readFromTextFile("Server1");
        userStorage.print();
    }
}

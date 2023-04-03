import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class MessageStorage implements Serializable{

    private Map<String, Chat> storage;
    private ArrayList<String> userList;

    public MessageStorage() {
        storage = new TreeMap<>();
        userList = new ArrayList<>();
    }

    public MessageStorage getChatsForUser(String user) {
        MessageStorage result = new MessageStorage();
        for (Map.Entry<String, Chat> entry : storage.entrySet()) {
            String userCombination = entry.getKey();
            Chat chat = entry.getValue();

            String[] users = userCombination.split(":");
            if (users.length != 2) {
                continue;
            }

            if (users[0].equals(user) || users[1].equals(user)) {
                result.storage.put(userCombination, chat);
            }
        }

        return result;
    }





    public void addMessage(Message message) {
        String userCombination = getUserCombinationKey(message.getSender(), message.getReciver());
        Chat chat = storage.getOrDefault(userCombination, new Chat());
        chat.addMessage(message);
        storage.put(userCombination, chat);

        if (userList.contains(message.getSender()) == false) userList.add(message.getSender());
        if (userList.contains(message.getReciver()) == false) userList.add(message.getReciver());
    }

    public TreeMap<UniqueTimestamp, Message> getMessages(String userOne, String userTwo) {
        String userCombination = getUserCombinationKey(userOne, userTwo);
        Chat chat = storage.get(userCombination);
        if (chat == null) {
            return null;
        }
        return chat.getMessages();
    }

    private String getUserCombinationKey(String userOne, String userTwo) {
        return userOne.compareTo(userTwo) < 0 ? userOne + ":" + userTwo : userTwo + ":" + userOne;
    }

    public class Chat implements Serializable {
        private TreeMap<UniqueTimestamp, Message> chat;

        public Chat() {
            chat = new TreeMap<>();
        }

        public void addMessage(Message message) {
            UniqueTimestamp uniqueTimestamp = new UniqueTimestamp(message.getSender(), message.getTimestamp());
            chat.put(uniqueTimestamp, message);
        }

        public TreeMap<UniqueTimestamp, Message> getMessages() {
            return chat;
        }
    }
    public void print() {
        System.out.println("=== Message Storage ===");
        for (Map.Entry<String, Chat> entry : storage.entrySet()) {
            String userCombination = entry.getKey();
            Chat chat = entry.getValue();

            System.out.println("Users: " + userCombination);
            TreeMap<UniqueTimestamp, Message> messages = chat.getMessages();
            for (Map.Entry<UniqueTimestamp, Message> msgEntry : messages.entrySet()) {
                UniqueTimestamp uniqueTimestamp = msgEntry.getKey();
                Message message = msgEntry.getValue();
                Timestamp timestamp = new Timestamp(message.getTimestamp());
                System.out.println(String.format("  [%s] %s: %s", timestamp.toString(), uniqueTimestamp.user, message.getMessageText()));
            }
        }
        System.out.println("=======================");
    }
    public ArrayList<String> getChatPartnersForUser(String username) {
        ArrayList<String> chatPartners = new ArrayList<>();
        for (String key : storage.keySet()) {
            String[] users = key.split(":");
            if (users.length != 2) {
                continue;
            }
            if (users[0].equals(username)) {
                chatPartners.add(users[1]);
            } else if (users[1].equals(username)) {
                chatPartners.add(users[0]);
            }
        }
        return chatPartners;
    }
}

// Empty message

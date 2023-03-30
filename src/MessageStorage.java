import java.sql.Timestamp;
import java.util.Map;
import java.util.TreeMap;

public class MessageStorage {
    public static void main(String[] args) {
        MessageStorage myStorage = new MessageStorage();
        myStorage.addMessage("joel", "nico", "hallo", System.nanoTime());
        myStorage.addMessage("nico", "joel", "hallo du opfer", System.nanoTime());
        myStorage.addMessage("luca", "joel", "hallo du opfer", System.nanoTime());
        myStorage.addMessage("joel", "luca", "hallo", System.nanoTime());

        TreeMap<MessageStorage.UniqueTimestamp, MessageStorage.Message> myMap = myStorage.getMessages("joel", "nico");
        System.out.println(myMap.size());
        if (myMap != null) {
            for (Map.Entry<MessageStorage.UniqueTimestamp, MessageStorage.Message> entry : myMap.entrySet()) {
                MessageStorage.UniqueTimestamp uniqueTimestamp = entry.getKey();
                MessageStorage.Message message = entry.getValue();
                System.out.println(uniqueTimestamp.timestamp + " - " + uniqueTimestamp.user + ": " + message.getMessageText());
            }
        } else {
            System.out.println("No messages found");
        }

    }

    private Map<String, Chat> storage;

    public MessageStorage() {
        storage = new TreeMap<>();
    }

    public void addMessage(String sender, String recipient, String messageText, long timestamp) {
        String userCombination = getUserCombinationKey(sender, recipient);
        Chat chat = storage.getOrDefault(userCombination, new Chat());
        chat.addMessage(sender, messageText, timestamp);
        storage.put(userCombination, chat);
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

    private class Chat {
        private TreeMap<UniqueTimestamp, Message> chat;

        public Chat() {
            chat = new TreeMap<>();
        }

        public void addMessage(String sender, String messageText, long timestamp) {
            UniqueTimestamp uniqueTimestamp = new UniqueTimestamp(sender, timestamp);
            Message message = new Message(sender, messageText);
            chat.put(uniqueTimestamp, message);
        }

        public TreeMap<UniqueTimestamp, Message> getMessages() {
            return chat;
        }
    }

    private static class UniqueTimestamp implements Comparable<UniqueTimestamp> {
        private String user;
        private long timestamp;

        public UniqueTimestamp(String user, long timestamp) {
            this.user = user;
            this.timestamp = timestamp;
        }

        @Override
        public int compareTo(UniqueTimestamp o) {
            int compare = user.compareTo(o.user);
            if (compare == 0) {
                compare = Long.compare(timestamp, o.timestamp);
            }
            return compare;
        }
    }

    private static class Message {
        private String sender;
        private String messageText;

        public Message(String sender, String messageText) {
            this.sender = sender;
            this.messageText = messageText;
        }

        public String getSender() {
            return sender;
        }

        public String getMessageText() {
            return messageText;
        }
    }

}
